# Guía de Uso de los Reproductores

## Descripción General

Se han implementado reproductores de video con Media3 (ExoPlayer 1.8.0) para las tres secciones principales de la aplicación PlayXY:

1. **TV (Canales en Vivo)**
2. **Movies (Películas)**
3. **Series (Series de TV)**

## Características por Tipo de Contenido

### 📺 TV - Canales en Vivo

#### Mini Reproductor (Portrait)
- **Ubicación:** Parte superior de TVScreen cuando un canal está reproduciéndose
- **Tamaño:** Ancho completo x 250dp de altura
- **Controles:**
  - ⏮️ Canal Anterior
  - ⏯️ Play/Pause
  - ⏭️ Canal Siguiente
  - ❌ Cerrar
- **Interacción:** Click en el reproductor para expandir a pantalla completa

#### Reproductor Pantalla Completa (Landscape)
- **Activación:** Automática al hacer click en el mini reproductor
- **Orientación:** Se fuerza landscape automáticamente
- **Controles:**
  - Barra superior: Botón atrás + Nombre del canal
  - Centro: Botón play/pause grande
  - Barra inferior: Botones anterior/siguiente canal
  - Auto-hide: Los controles se ocultan después de 3 segundos

### 🎬 Movies - Películas

#### Mini Reproductor (Portrait)
- **Ubicación:** Parte superior de MovieDetailScreen cuando una película está reproduciéndose
- **Tamaño:** Ancho completo x 280dp de altura
- **Controles:**
  - Barra de progreso (seek bar) con tiempo actual/total
  - ⏯️ Play/Pause
  - ❌ Cerrar
- **Características:** 
  - Actualización en tiempo real de la posición
  - Arrastre para buscar posición específica
- **Interacción:** Click en el reproductor para expandir a pantalla completa

#### Reproductor Pantalla Completa (Landscape)
- **Activación:** Automática al hacer click en el mini reproductor
- **Orientación:** Se fuerza landscape automáticamente
- **Controles:**
  - Barra superior: Botón atrás + Título de la película
  - Centro: Botón play/pause grande
  - Barra inferior: Seek bar con indicadores de tiempo (00:00 / 00:00)
  - Auto-hide: Los controles se ocultan después de 3 segundos

### 📺 Series - Episodios

#### Mini Reproductor (Portrait)
- **Ubicación:** Parte superior de SeriesDetailScreen cuando un episodio está reproduciéndose
- **Tamaño:** Ancho completo x 250dp de altura
- **Controles:**
  - ⏮️ Episodio Anterior
  - ⏯️ Play/Pause
  - ⏭️ Episodio Siguiente
  - ❌ Cerrar
- **Información:** Muestra "T{temporada} E{episodio}" y título del episodio
- **Interacción:** Click en el reproductor para expandir a pantalla completa

#### Reproductor Pantalla Completa (Landscape)
- **Activación:** Automática al hacer click en el mini reproductor
- **Orientación:** Se fuerza landscape automáticamente
- **Controles:**
  - Barra superior: Botón atrás + "{Serie} - T{temp} E{ep}"
  - Centro: Botón play/pause grande
  - Barra inferior: Botones anterior/siguiente episodio
  - Auto-hide: Los controles se ocultan después de 3 segundos
- **Navegación:** Reproduce episodios en orden secuencial

## Flujo de Uso

### Para TV:
1. Usuario navega a la sección TV
2. Selecciona categoría y canal
3. Click en canal → Inicia reproducción en mini reproductor
4. Click en mini reproductor → Expande a pantalla completa (landscape)
5. Puede navegar entre canales con los botones
6. Botón atrás → Vuelve al mini reproductor (portrait)
7. Botón cerrar → Detiene reproducción

### Para Movies:
1. Usuario navega a la sección Movies
2. Selecciona película y ve los detalles
3. Click en "Reproducir" → Inicia reproducción en mini reproductor
4. Click en mini reproductor → Expande a pantalla completa (landscape)
5. Puede usar seek bar para adelantar/retroceder
6. Botón atrás → Vuelve al mini reproductor (portrait)
7. Botón cerrar → Detiene reproducción

### Para Series:
1. Usuario navega a la sección Series
2. Selecciona serie y ve temporadas/episodios
3. Click en episodio → Inicia reproducción en mini reproductor
4. Click en mini reproductor → Expande a pantalla completa (landscape)
5. Puede navegar entre episodios con los botones
6. Botón atrás → Vuelve al mini reproductor (portrait)
7. Botón cerrar → Detiene reproducción

## Gestión de Recursos

### Inicialización
- El reproductor se inicializa solo cuando se necesita
- Se carga el contenido y comienza la reproducción automáticamente

### Liberación
- Los recursos se liberan automáticamente cuando:
  - Se cierra el reproductor
  - Se sale de la pantalla
  - Se destruye el componente
- Uso de `DisposableEffect` para gestión del ciclo de vida

### Orientación
- **Portrait:** Orientación normal de la app con mini reproductores
- **Landscape:** Forzada en pantalla completa, se restaura al salir
- **Sistema:** Barras del sistema ocultas en pantalla completa

## Formato de URLs

El sistema utiliza `StreamUrlBuilder` para construir las URLs correctas:

```kotlin
// TV Channels
http://url:port/live/username/password/stream_id.ts

// Movies
http://url:port/movie/username/password/stream_id.mp4

// Series Episodes
http://url:port/series/username/password/episode_id.mp4
```

## Solución de Problemas

### El video no se reproduce
1. Verificar conexión a internet
2. Verificar que las credenciales del usuario son correctas
3. Verificar que la URL del stream es válida
4. Revisar logs de ExoPlayer

### El reproductor no gira a landscape
1. Verificar permisos de orientación en AndroidManifest
2. Verificar que no hay otra configuración bloqueando la rotación

### Los controles no aparecen
1. Hacer click en el reproductor para mostrar controles
2. Los controles se auto-ocultan después de 3 segundos

### La navegación entre items no funciona
1. Verificar que hay items anteriores/siguientes disponibles
2. Los botones se deshabilitan si no hay más items en esa dirección

## Dependencias

Asegúrate de que estas dependencias estén en tu `build.gradle.kts`:

```kotlin
implementation("androidx.media3:media3-exoplayer:1.8.0")
implementation("androidx.media3:media3-ui:1.8.0")
implementation("androidx.media3:media3-exoplayer-hls:1.8.0")
```

## Notas Técnicas

- **Formato soportado:** HLS, MP4, TS y otros formatos de streaming comunes
- **Buffer:** ExoPlayer gestiona automáticamente el buffering
- **Calidad:** Se adapta automáticamente según la conexión (adaptive streaming)
- **DRM:** No implementado en esta versión
- **PiP:** No implementado en esta versión
- **Subtítulos:** No implementado en esta versión

## Próximas Mejoras (Opcionales)

1. **Picture-in-Picture (PiP):** Reproducción mientras se navega por la app
2. **Controles de velocidad:** 0.5x, 1x, 1.5x, 2x
3. **Subtítulos:** Soporte para archivos .srt
4. **Lista de reproducción:** Auto-play del siguiente episodio
5. **Marcadores:** Guardar posición de reproducción
6. **Estadísticas:** Tiempo visto, contenido favorito
7. **Control de calidad:** Selección manual de resolución
8. **Chromecast:** Enviar contenido a TV

