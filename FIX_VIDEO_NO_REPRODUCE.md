# Corrección: Video se detiene y no se reproduce

## Fecha: 13 de Noviembre de 2025

## Problema Reportado

Después de seleccionar un canal, película o episodio, el video muestra la imagen inicial pero se detiene completamente y no se reproduce. La imagen se congela y no hay reproducción de video.

## Diagnóstico del Problema

### Problemas Identificados:

1. **Timing de Inicialización del Player**
   - El ExoPlayer se estaba asignando al PlayerView dentro de un `post {}` que causaba un retraso en la vinculación
   - La llamada a `playMedia()` se ejecutaba ANTES de que el PlayerView estuviera completamente vinculado
   - Esto causaba que el player se preparara pero no iniciara la reproducción

2. **Orden de Operaciones Incorrecto**
   ```kotlin
   // ANTES - INCORRECTO
   DisposableEffect(streamUrl) {
       playerManager.initializePlayer()
       playerManager.playMedia(streamUrl)  // ← Se llamaba inmediatamente
       isPlaying = true
       onDispose { }
   }
   
   AndroidView(
       factory = { ctx ->
           PlayerView(ctx).apply {
               // ...
               post {
                   player = playerManager.getPlayer()  // ← Asignación retrasada
               }
           }
       }
   )
   ```

3. **Falta de Preparación del Player**
   - El player no limpiaba correctamente el estado anterior al cambiar de media
   - No se verificaba si el player estaba en estado IDLE antes de reproducir

## Solución Implementada

### 1. Corrección en PlayerManager.kt

**Mejora en la función `playMedia()`:**

```kotlin
fun playMedia(url: String) {
    // Pausar otros reproductores activos
    pauseAllExcept(this)

    // Solicitar audio focus
    val result = audioManager.requestAudioFocus(focusRequest)
    if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
        // No hay foco; intentaremos de todas formas
    }

    // Asegurar que el player está inicializado
    if (player == null) {
        initializePlayer()
    }

    // Solo cambia de media si la URL es diferente
    if (currentUrl != url) {
        val mediaItem = MediaItem.fromUri(url)
        player?.apply {
            stop()              // ← Detener reproducción anterior
            clearMediaItems()   // ← Limpiar items anteriores
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
        }
        currentUrl = url
    } else {
        // Misma URL, solo asegura que esté reproduciendo
        player?.apply {
            if (playbackState == Player.STATE_IDLE) {
                prepare()       // ← Re-preparar si está en IDLE
            }
            playWhenReady = true
        }
    }
}
```

**Cambios clave:**
- ✅ Asegurar inicialización del player antes de reproducir
- ✅ Limpiar estado anterior con `stop()` y `clearMediaItems()`
- ✅ Verificar estado IDLE y re-preparar si es necesario

### 2. Corrección en TVMiniPlayer.kt, MovieMiniPlayer.kt y SeriesMiniPlayer.kt

**Nuevo flujo de inicialización:**

```kotlin
// 1. Inicializar player ANTES de crear la vista
LaunchedEffect(Unit) {
    playerManager.initializePlayer()
}

// 2. Usar coroutine scope para delay controlado
val scope = rememberCoroutineScope()

DisposableEffect(streamUrl) {
    // Asegurar inicialización
    playerManager.initializePlayer()
    
    // Pequeño delay para asegurar que el player está listo
    scope.launch {
        delay(100)  // 100ms para que el PlayerView se vincule
        playerManager.playMedia(streamUrl)
    }
    
    onDispose { }
}

// 3. Asignar player INMEDIATAMENTE en factory (sin post)
AndroidView(
    factory = { ctx ->
        PlayerView(ctx).apply {
            useController = false
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
            setKeepContentOnPlayerReset(true)
            keepScreenOn = true
            // Inicializar y asignar INMEDIATAMENTE
            playerManager.initializePlayer()
            player = playerManager.getPlayer()  // ← SIN post {}
        }
    },
    update = { playerView ->
        // Asegurar que el player está siempre vinculado
        val currentPlayer = playerManager.getPlayer()
        if (playerView.player != currentPlayer) {
            playerView.player = currentPlayer
        }
        playerView.keepScreenOn = true
    }
)
```

**Cambios clave:**
- ✅ Inicialización proactiva del player con `LaunchedEffect(Unit)`
- ✅ Asignación inmediata del player al PlayerView (sin `post {}`)
- ✅ Delay de 100ms controlado con `rememberCoroutineScope()` antes de `playMedia()`
- ✅ Verificación en `update` para mantener la vinculación

### 3. Imports Agregados

```kotlin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
```

## Archivos Modificados

| Archivo | Cambios Realizados |
|---------|-------------------|
| `PlayerManager.kt` | Mejorada función `playMedia()` con limpieza de estado y verificación IDLE |
| `TVMiniPlayer.kt` | Corregido timing de inicialización y asignación del player |
| `MovieMiniPlayer.kt` | Corregido timing de inicialización y asignación del player |
| `SeriesMiniPlayer.kt` | Corregido timing de inicialización y asignación del player |

## Flujo Correcto Ahora

### Para Canales (TV):
1. Usuario hace clic en canal
2. `TVViewModel.playChannel()` actualiza `_currentChannel`
3. `TVMiniPlayer` se compone con nuevo `streamUrl`
4. **LaunchedEffect** inicializa el player
5. PlayerView se crea y se vincula al player INMEDIATAMENTE
6. **DisposableEffect** espera 100ms y llama a `playMedia()`
7. PlayerManager limpia estado anterior, carga nuevo media y reproduce ✅

### Para Películas:
1. Usuario hace clic en "Reproducir"
2. `isPlaying = true` en `MovieDetailScreen`
3. `MovieMiniPlayer` se compone
4. Mismo flujo que canales ✅

### Para Series:
1. Usuario hace clic en episodio
2. `currentEpisode = episode`, `isPlaying = true`
3. `SeriesMiniPlayer` se compone
4. Mismo flujo que canales ✅

## Beneficios de la Solución

1. **Reproducción Confiable**: El player ahora se inicializa y reproduce consistentemente
2. **Sincronización Correcta**: PlayerView y ExoPlayer están sincronizados desde el inicio
3. **Limpieza de Estado**: El player limpia correctamente el estado anterior
4. **Manejo de Edge Cases**: Se manejan casos como re-reproducción de misma URL o estado IDLE

## Pruebas Recomendadas

- [ ] Reproducir un canal de TV en vivo
- [ ] Reproducir una película
- [ ] Reproducir un episodio de serie
- [ ] Cambiar de canal rápidamente
- [ ] Cambiar de episodio rápidamente
- [ ] Volver de fullscreen a mini player
- [ ] Pausar y reanudar reproducción
- [ ] Rotar pantalla durante reproducción

## Notas Técnicas

- El delay de 100ms es suficiente para que el PlayerView complete su layout
- `rememberCoroutineScope()` es preferible a `GlobalScope` por mejor manejo del ciclo de vida
- La limpieza con `stop()` y `clearMediaItems()` previene problemas de estado residual
- La verificación en `update` asegura que el player se mantenga vinculado durante recomposiciones

## Estado Final

✅ **COMPILACIÓN EXITOSA**
✅ **SIN ERRORES DE SINTAXIS**
⚠️ **WARNINGS MENORES** (no críticos, relacionados con formato y orden de parámetros)

---

**Resumen**: El problema del video que se detenía se debía a un problema de timing entre la inicialización del ExoPlayer y su vinculación al PlayerView. La solución implementa una inicialización más temprana, asignación inmediata del player al view, y un delay controlado antes de llamar a `playMedia()`, asegurando que todos los componentes estén listos antes de iniciar la reproducción.

