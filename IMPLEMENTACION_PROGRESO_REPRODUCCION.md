# Implementación de Progreso de Reproducción

## Fecha: 12 de Noviembre de 2025

---

## Funcionalidades Implementadas

### 1. **Categoría Recientes de Series - Solo la Última Reproducida**

#### Comportamiento
- La categoría "Recientes" en Series muestra **solo la última serie reproducida**
- No se muestran duplicados ni múltiples series
- Se mantiene el registro completo en base de datos para futuras funcionalidades

#### Implementación
```kotlin
// SeriesViewModel.kt
private fun loadRecentSeries() {
    viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isLoading = true)
        try {
            val all = repository.getSeries()
            // Mostrar solo la última serie reproducida
            val lastSeriesId = recentIds.firstOrNull()
            val recents = if (lastSeriesId != null) {
                all.filter { it.seriesId == lastSeriesId }
            } else {
                emptyList()
            }
            _uiState.value = _uiState.value.copy(series = recents, isLoading = false)
        } catch (e: Exception) {
            e.printStackTrace()
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }
}
```

---

### 2. **Botón "Continuar" en Series con Último Episodio Visto**

#### Características
- Aparece en `SeriesDetailScreen` si hay progreso guardado
- Muestra "T{temporada}E{episodio}" y título del episodio
- Al presionar, reproduce desde el último episodio visto
- Diseño destacado con color `primaryContainer`

#### Pantalla
```
┌─────────────────────────────────────┐
│  Último episodio visto              │
│  T1E5                               │
│  "El Episodio Final"                │
│                    [Continuar] ▶    │
└─────────────────────────────────────┘
```

#### Implementación
- Base de datos: Tabla `series_progress`
  - `seriesId`: ID de la serie
  - `lastEpisodeId`: ID del último episodio
  - `lastSeasonNumber`: Número de temporada
  - `lastEpisodeNumber`: Número de episodio
  - `positionMs`: Posición en el episodio (para futuro)
  - `timestamp`: Última actualización

#### Código
```kotlin
// SeriesDetailScreen.kt - Botón Continuar
if (uiState.lastEpisode != null) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row {
            Column {
                Text("T${uiState.lastEpisode.season}E${uiState.lastEpisode.episodeNum}")
                Text(uiState.lastEpisode.title)
            }
            Button(onClick = {
                currentEpisode = uiState.lastEpisode
                isPlaying = true
                seriesVm.onSeriesOpened(uiState.series!!.seriesId)
            }) {
                Text("Continuar")
            }
        }
    }
}
```

#### Guardado Automático
- **Al seleccionar episodio**: Se guarda como último episodio visto
- **Cada 10 segundos**: Se actualiza la posición en el episodio actual
- **Al cambiar de episodio**: Se guarda el nuevo episodio

```kotlin
// Guardado periódico cada 10 segundos
LaunchedEffect(isPlaying, currentEpisode) {
    while (isPlaying && currentEpisode != null) {
        delay(10000)
        val currentPos = playerManager.getCurrentPosition()
        if (currentPos > 0) {
            viewModel.saveProgress(seriesId, currentEpisode, currentPos)
        }
    }
}
```

---

### 3. **Diálogo "Continuar o Empezar desde el Inicio" en Películas**

#### Comportamiento
- Al presionar "Reproducir" en `MovieDetailScreen`:
  - **Si hay progreso guardado (>0% y <95%)**: Muestra diálogo
  - **Si no hay progreso**: Reproduce directamente desde el inicio

#### Diálogo
```
┌─────────────────────────────────────┐
│  Continuar reproducción              │
│                                      │
│  ¿Deseas continuar donde lo dejaste │
│  o empezar desde el inicio?          │
│                                      │
│  [Desde el inicio]  [Continuar]     │
└─────────────────────────────────────┘
```

#### Implementación
- Base de datos: Tabla `movie_progress`
  - `streamId`: ID de la película
  - `positionMs`: Posición en milisegundos
  - `durationMs`: Duración total
  - `timestamp`: Última actualización

#### Código
```kotlin
// MovieDetailScreen.kt - Lógica del botón Reproducir
Button(onClick = {
    val progress = uiState.movieProgress
    if (progress != null && 
        progress.positionMs > 0 && 
        progress.positionMs < progress.durationMs * 0.95) {
        // Mostrar diálogo
        showResumeDialog = true
    } else {
        // Reproducir desde el inicio
        onPlayClick(movie)
        isPlaying = true
        viewModel.onMoviePlayed(movie)
    }
}) {
    Text("Reproducir")
}

// AlertDialog
if (showResumeDialog) {
    AlertDialog(
        title = { Text("Continuar reproducción") },
        text = { Text("¿Deseas continuar donde lo dejaste o empezar desde el inicio?") },
        confirmButton = {
            TextButton(onClick = {
                showResumeDialog = false
                lastPositionMs = uiState.movieProgress?.positionMs ?: 0L
                onPlayClick(movie)
                isPlaying = true
                viewModel.onMoviePlayed(movie)
            }) {
                Text("Continuar")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                showResumeDialog = false
                viewModel.deleteMovieProgress(movie.streamId)
                lastPositionMs = 0L
                onPlayClick(movie)
                isPlaying = true
                viewModel.onMoviePlayed(movie)
            }) {
                Text("Desde el inicio")
            }
        }
    )
}
```

#### Guardado Automático
- **Cada 10 segundos**: Mientras reproduce, guarda posición actual
- **Al pausar/cerrar**: Se mantiene el último progreso guardado
- **Al elegir "Desde el inicio"**: Elimina el progreso guardado

```kotlin
// Guardado periódico cada 10 segundos
LaunchedEffect(isPlaying) {
    while (isPlaying) {
        delay(10000)
        val currentPos = playerManager.getCurrentPosition()
        val totalDuration = playerManager.getDuration()
        if (currentPos > 0 && totalDuration > 0) {
            viewModel.saveMovieProgress(movie.streamId, currentPos, totalDuration)
        }
    }
}
```

---

## Estructura de Base de Datos

### Nuevas Tablas

#### `movie_progress`
```sql
CREATE TABLE movie_progress (
    streamId TEXT PRIMARY KEY NOT NULL,
    positionMs INTEGER NOT NULL,
    durationMs INTEGER NOT NULL,
    timestamp INTEGER NOT NULL
);
```

#### `series_progress`
```sql
CREATE TABLE series_progress (
    seriesId TEXT PRIMARY KEY NOT NULL,
    lastEpisodeId TEXT NOT NULL,
    lastSeasonNumber INTEGER NOT NULL,
    lastEpisodeNumber INTEGER NOT NULL,
    positionMs INTEGER NOT NULL,
    timestamp INTEGER NOT NULL
);
```

### DAOs Implementados

#### `MovieProgressDao`
```kotlin
@Dao
interface MovieProgressDao {
    @Query("SELECT * FROM movie_progress WHERE streamId = :streamId")
    suspend fun getProgress(streamId: String): MovieProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProgress(progress: MovieProgressEntity)

    @Query("DELETE FROM movie_progress WHERE streamId = :streamId")
    suspend fun deleteProgress(streamId: String)
}
```

#### `SeriesProgressDao`
```kotlin
@Dao
interface SeriesProgressDao {
    @Query("SELECT * FROM series_progress WHERE seriesId = :seriesId")
    suspend fun getProgress(seriesId: String): SeriesProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProgress(progress: SeriesProgressEntity)

    @Query("DELETE FROM series_progress WHERE seriesId = :seriesId")
    suspend fun deleteProgress(seriesId: String)
}
```

---

## ViewModels Actualizados

### MoviesViewModel
**Nuevas funciones:**
- `saveMovieProgress(streamId, positionMs, durationMs)`: Guarda progreso
- `deleteMovieProgress(streamId)`: Elimina progreso (empezar desde inicio)
- `loadMovieInfo(vodId)`: Ahora también carga el progreso guardado

**Nuevo campo en UiState:**
```kotlin
data class MoviesUiState(
    // ...existing fields...
    val movieProgress: MovieProgressEntity? = null
)
```

### SeriesDetailViewModel
**Nuevas funciones:**
- `saveProgress(seriesId, episode, positionMs)`: Guarda progreso de episodio
- `getProgress(seriesId)`: Obtiene progreso guardado
- `loadSeriesInfo(seriesId, categoryId)`: Ahora carga último episodio visto

**Nuevo campo en UiState:**
```kotlin
data class SeriesDetailUiState(
    // ...existing fields...
    val lastEpisode: Episode? = null
)
```

### SeriesViewModel
**Función modificada:**
- `loadRecentSeries()`: Ahora muestra solo la última serie reproducida

---

## Cambios en la Base de Datos

### Versión
- **Anterior**: 5
- **Nueva**: 6

### Migration Strategy
- `fallbackToDestructiveMigration(true)`: Elimina y recrea tablas
- **Nota**: En producción, se recomienda implementar migraciones adecuadas

---

## Archivos Modificados

### Base de Datos
1. `Entities.kt`: Agregadas `MovieProgressEntity` y `SeriesProgressEntity`
2. `Daos.kt`: Agregados `MovieProgressDao` y `SeriesProgressDao`
3. `PlayxyDatabase.kt`: 
   - Versión incrementada a 6
   - Agregadas nuevas entidades y DAOs

### Dependency Injection
4. `AppModule.kt`: Agregados providers para nuevos DAOs

### ViewModels
5. `MoviesViewModel.kt`: Integración de progreso de películas
6. `SeriesViewModel.kt`: Filtro de recientes a última serie
7. `SeriesDetailViewModel.kt`: Gestión de progreso de episodios

### Screens
8. `MovieDetailScreen.kt`:
   - Diálogo de continuar/empezar desde inicio
   - Guardado automático cada 10 segundos
   - Carga de progreso al abrir
9. `SeriesDetailScreen.kt`:
   - Botón "Continuar T1E5"
   - Guardado automático cada 10 segundos
   - Guardado al cambiar de episodio

---

## Testing Recomendado

### Series

#### Caso 1: Botón Continuar
1. Reproducir un episodio de una serie (ej: T1E3)
2. Esperar 15 segundos
3. Cerrar reproductor
4. Volver a la pantalla de detalle de la serie
5. **Esperado**: Debe aparecer botón "Continuar T1E3"
6. Presionar "Continuar"
7. **Esperado**: Debe reproducir el episodio T1E3

#### Caso 2: Cambio de Episodio
1. Reproducir episodio T1E1
2. Presionar "Siguiente Episodio"
3. Cerrar reproductor
4. Volver a pantalla de detalle
5. **Esperado**: Botón muestra "Continuar T1E2"

#### Caso 3: Categoría Recientes
1. Reproducir episodio de "Serie A"
2. Reproducir episodio de "Serie B"
3. Ir a categoría "Recientes"
4. **Esperado**: Solo debe aparecer "Serie B" (la última)

### Películas

#### Caso 1: Primera Reproducción
1. Abrir película nunca vista
2. Presionar "Reproducir"
3. **Esperado**: Reproduce directamente sin diálogo

#### Caso 2: Continuar donde se quedó
1. Reproducir película 30 segundos
2. Cerrar reproductor
3. Volver a detalle de película
4. Presionar "Reproducir"
5. **Esperado**: Aparece diálogo con opciones
6. Presionar "Continuar"
7. **Esperado**: Reproduce desde donde se quedó (~30s)

#### Caso 3: Empezar desde el inicio
1. Con progreso guardado
2. Presionar "Reproducir"
3. En diálogo, presionar "Desde el inicio"
4. **Esperado**: Reproduce desde 0:00

#### Caso 4: Película casi terminada
1. Reproducir película hasta 96% (ej: 48min de 50min)
2. Cerrar
3. Abrir de nuevo y presionar "Reproducir"
4. **Esperado**: Reproduce desde inicio (sin diálogo)

---

## Estado de Compilación

✅ **Sin errores de compilación**

⚠️ **Warnings** (no bloquean):
- `hiltViewModel()` deprecated → migrar a androidx.hilt.lifecycle.viewmodel.compose
- `String.format()` sin Locale → agregar `Locale.getDefault()`
- `Divider()` deprecated → renombrar a `HorizontalDivider()`
- Import sin usar en MovieDetailScreen

---

## Mejoras Futuras

### Alta Prioridad
- [ ] Restaurar posición exacta en series al volver de fullscreen
- [ ] Indicador visual de progreso en pósters (barra de % visto)
- [ ] Sincronización de progreso con servidor (multi-dispositivo)

### Media Prioridad
- [ ] Marcar como "visto" automáticamente al llegar al 95%
- [ ] Historial de reproducción completo
- [ ] Estadísticas de tiempo total visto

### Baja Prioridad
- [ ] Opción "Eliminar progreso" en menú de póster
- [ ] Notificación cuando haya nuevo episodio de serie en progreso
- [ ] Recomendaciones basadas en historial

---

## Notas Técnicas

### Frecuencia de Guardado
- **Cada 10 segundos**: Balance entre precisión y rendimiento
- **Al cambiar episodio**: Garantiza registro del episodio correcto
- **No se guarda si posición es 0**: Evita sobrescribir con datos incorrectos

### Condición para Mostrar Diálogo (Películas)
```kotlin
progress.positionMs > 0 && progress.positionMs < progress.durationMs * 0.95
```
- `> 0`: Tiene progreso guardado
- `< 95%`: No está casi terminada (evita diálogo si ya vio la película)

### Persistencia
- Room Database con `@Insert(onConflict = REPLACE)`
- Transacciones asíncronas con coroutines
- Fallback gracioso si falla guardado (no interrumpe reproducción)

---

**Versión de la app**: 1.0.0  
**Database version**: 6  
**Fecha de implementación**: 12 de Noviembre de 2025  
**Estado**: ✅ **COMPLETO Y LISTO PARA TESTING**

