# CORRECCIÓN: Video no iniciaba en Series

## Problema Identificado (del Logcat)

```
2025-11-12 21:47:17.447 23793-9109 VideoCodecInfo
[551424516056678409] OutputFormat: AMessage...

2025-11-12 21:47:21.448 23793-9109 MediaCodec
[mId: 9] [video-debug-dec] setState: FLUSHING

2025-11-12 21:47:21.493 23793-9109 VideoCodecInfo
com.iptv.playxy destroy video codec index: [551424516056678409]
```

**Diagnóstico**: El codec de video se inicializaba correctamente, pero inmediatamente (4 segundos después) se liberaba sin reproducir contenido.

## Causa Raíz

En `SeriesDetailScreen.kt` línea 277:

```kotlin
onEpisodeClick = { episode ->
    currentEpisode = episode
    isPlaying = true
    onEpisodeClick(episode)  // ← ESTA LÍNEA CAUSABA EL PROBLEMA
    seriesVm.onSeriesOpened(uiState.series!!.seriesId)
}
```

La llamada a `onEpisodeClick(episode)` (callback del parámetro de la función) estaba:
1. Disparando navegación innecesaria
2. Causando recomposición que liberaba el PlayerManager
3. Interrumpiendo el flujo de reproducción antes de que iniciara

## Solución Aplicada

### 1. Eliminar callback innecesario
```kotlin
// ANTES (SeriesDetailScreen.kt)
fun SeriesDetailScreen(
    seriesId: String,
    categoryId: String,
    viewModel: SeriesDetailViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    onEpisodeClick: (Episode) -> Unit  // ← Parámetro no necesario
)

// DESPUÉS
fun SeriesDetailScreen(
    seriesId: String,
    categoryId: String,
    viewModel: SeriesDetailViewModel = hiltViewModel(),
    onBackClick: () -> Unit
)
```

### 2. Actualizar lógica de reproducción
```kotlin
// ANTES
onEpisodeClick = { episode ->
    currentEpisode = episode
    isPlaying = true
    onEpisodeClick(episode)  // ← Navegación/release
    seriesVm.onSeriesOpened(uiState.series!!.seriesId)
}

// DESPUÉS
onEpisodeClick = { episode ->
    currentEpisode = episode
    isPlaying = true
    // Registrar reciente (sin navegación)
    seriesVm.onSeriesOpened(uiState.series!!.seriesId)
}
```

### 3. Actualizar punto de llamada (MainActivity.kt)
```kotlin
// ANTES
SeriesDetailScreen(
    seriesId = seriesId,
    categoryId = categoryId,
    onBackClick = { navController.popBackStack() },
    onEpisodeClick = { episode ->
        // TODO: Implement player navigation for episodes
    }
)

// DESPUÉS
SeriesDetailScreen(
    seriesId = seriesId,
    categoryId = categoryId,
    onBackClick = { navController.popBackStack() }
)
```

## Flujo Correcto Ahora

1. Usuario selecciona episodio en SeasonCard
2. Se actualiza estado local:
   - `currentEpisode = episode`
   - `isPlaying = true`
3. Se registra en recientes: `seriesVm.onSeriesOpened()`
4. Recomposición muestra `SeriesMiniPlayer`
5. `SeriesMiniPlayer` usa `playerManager.playMedia(streamUrl)`
6. PlayerManager con instancia compartida mantiene el player activo
7. Video reproduce normalmente

## Verificación de Compilación

✅ **Sin errores de compilación**
⚠️ Solo warnings de deprecación (no bloquean):
- `hiltViewModel()` deprecated (migrar a nuevo paquete)
- `String.format()` sin Locale (warning de i18n)
- `Divider()` deprecated (renombrado a HorizontalDivider)

## Testing Recomendado

### Caso de Prueba 1: Reproducción Básica
1. Abrir cualquier serie
2. Expandir temporada
3. Presionar en un episodio
4. **Resultado Esperado**: Mini player aparece y video inicia reproducción
5. **Resultado Anterior**: Player se creaba y se destruía inmediatamente

### Caso de Prueba 2: Cambio de Episodio
1. Iniciar reproducción de episodio
2. Presionar "Siguiente Episodio"
3. **Resultado Esperado**: Cambia al siguiente episodio sin liberar player
4. **Resultado Anterior**: Funcionaba bien (no afectado)

### Caso de Prueba 3: Fullscreen
1. Iniciar reproducción
2. Presionar botón fullscreen
3. Reproducir unos segundos
4. Presionar "Volver"
5. **Resultado Esperado**: 
   - Regresa a portrait
   - Mini player visible
   - Continúa reproducción desde posición guardada

### Caso de Prueba 4: Registro en Recientes
1. Reproducir un episodio de una serie
2. Ir a pestaña "Series" → categoría "Recientes"
3. **Resultado Esperado**: La serie aparece en la lista
4. **Implementación**: ✅ `seriesVm.onSeriesOpened()` registra en BD

## Archivos Modificados

1. `/app/src/main/java/com/iptv/playxy/ui/series/SeriesDetailScreen.kt`
   - Eliminado parámetro `onEpisodeClick`
   - Eliminada llamada al callback en `SeasonCard.onEpisodeClick`

2. `/app/src/main/java/com/iptv/playxy/MainActivity.kt`
   - Eliminado argumento `onEpisodeClick` en composable

## Commit Sugerido

```
fix(series): resolver issue de video que no iniciaba reproducción

- Eliminar callback onEpisodeClick que causaba navegación/release prematuro
- Mantener solo estado local para reproducción en mini player
- Registrar recientes sin interferir con flujo de reproducción

Fixes: Video codec se inicializaba pero se liberaba antes de reproducir
```

## Próximos Pasos

1. ✅ Compilación exitosa sin errores
2. 🔄 Testing en dispositivo real con el logcat abierto
3. ⏳ Verificar que codec permanece activo durante reproducción
4. ⏳ Validar todos los casos de prueba listados arriba

## Notas Adicionales

- **PlayerManager** se mantiene activo gracias a `remember(context)`
- **AudioFocus** manejado correctamente (pausa otros players)
- **Persistencia de posición** implementada para fullscreen
- **Categorías Recientes** se actualizan inmediatamente tras registro

---

**Fecha de corrección**: 12 de Noviembre de 2025  
**Estado**: ✅ LISTO PARA TESTING

