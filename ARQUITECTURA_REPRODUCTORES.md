# Arquitectura de Reproductores - PlayXY

## Diagrama de Componentes

```
┌─────────────────────────────────────────────────────────────────┐
│                         PlayXY App                               │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌───────────────┐  ┌───────────────┐  ┌────────────────────┐  │
│  │   TVScreen    │  │ MoviesScreen  │  │  SeriesScreen      │  │
│  │               │  │               │  │                    │  │
│  │ [Categories]  │  │ [Categories]  │  │ [Categories]       │  │
│  │ [Channels]    │  │ [Movies]      │  │ [Series]           │  │
│  │               │  │               │  │                    │  │
│  │   ↓ Click     │  │   ↓ Click     │  │   ↓ Click Series   │  │
│  │               │  │               │  │                    │  │
│  │ TVMiniPlayer  │  │ MovieDetail   │  │ SeriesDetail       │  │
│  │ ┌───────────┐ │  │ Screen        │  │ Screen             │  │
│  │ │  [Video]  │ │  │ ┌───────────┐ │  │ [Seasons]          │  │
│  │ │           │ │  │ │  [Image]  │ │  │ [Episodes]         │  │
│  │ │ [⏮ ⏯ ⏭ ❌]│ │  │ │  [Info]   │ │  │   ↓ Click Episode  │  │
│  │ └───────────┘ │  │ │  [Play]   │ │  │                    │  │
│  │     ↓ Click   │  │ └───────────┘ │  │ SeriesMiniPlayer   │  │
│  │               │  │   ↓ Click     │  │ ┌───────────┐      │  │
│  │               │  │               │  │ │  [Video]  │      │  │
│  └───────────────┘  │ MovieMini     │  │ │ T1 E1     │      │  │
│         ↓           │ Player        │  │ │ [⏮ ⏯ ⏭ ❌]│      │  │
│                     │ ┌───────────┐ │  │ └───────────┘      │  │
│  FullscreenPlayer   │ │  [Video]  │ │  │     ↓ Click        │  │
│  (Landscape)        │ │ [━━━━━━━] │ │  └────────────────────┘  │
│  ┌──────────────┐   │ │  [⏯ ❌]  │ │         ↓                │
│  │ [← Title]    │   │ └───────────┘ │                          │
│  │              │   │     ↓ Click   │  FullscreenPlayer        │
│  │   [Video]    │   │               │  (Landscape)             │
│  │              │   └───────────────┘  ┌──────────────┐        │
│  │     [⏯]      │         ↓            │ [← Title]    │        │
│  │              │                      │              │        │
│  │ [⏮ ━━━━━ ⏭] │   FullscreenPlayer   │   [Video]    │        │
│  │  00:00/99:99 │   (Landscape)        │              │        │
│  └──────────────┘   ┌──────────────┐   │     [⏯]      │        │
│                     │ [← Title]    │   │              │        │
│                     │              │   │ [━━━━━━━━━]  │        │
│                     │   [Video]    │   │  00:00/99:99 │        │
│                     │              │   └──────────────┘        │
│                     │     [⏯]      │                           │
│                     │              │                           │
│                     │ [━━━━━━━━━]  │                           │
│                     │  00:00/99:99 │                           │
│                     └──────────────┘                           │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

## Flujo de Estados

### TV Channel Playback
```
┌─────────────┐     ┌──────────────┐     ┌─────────────────┐
│  TVScreen   │────▶│ Select       │────▶│  currentChannel │
│  (Idle)     │     │  Channel     │     │  != null        │
└─────────────┘     └──────────────┘     └─────────────────┘
                                                   │
                                                   ▼
                                         ┌──────────────────┐
                                         │  TVMiniPlayer    │
                                         │  (Portrait)      │
                                         │  Playing...      │
                                         └──────────────────┘
                                                   │
                              ┌────────────────────┼────────────────────┐
                              ▼                    ▼                    ▼
                        ┌──────────┐        ┌───────────┐        ┌──────────┐
                        │  Click   │        │  Previous │        │   Next   │
                        │  Player  │        │  Channel  │        │ Channel  │
                        └──────────┘        └───────────┘        └──────────┘
                              │                    │                    │
                              ▼                    └────────┬───────────┘
                    ┌──────────────────┐                   │
                    │ FullscreenPlayer │                   │
                    │   (Landscape)    │◀──────────────────┘
                    │   Playing...     │
                    └──────────────────┘
                              │
                    ┌─────────┴─────────┐
                    ▼                   ▼
              ┌──────────┐        ┌──────────┐
              │   Back   │        │  Close   │
              │  Button  │        │  Button  │
              └──────────┘        └──────────┘
                    │                   │
                    ▼                   ▼
           ┌──────────────┐      ┌──────────┐
           │ TVMiniPlayer │      │  Stop    │
           │  (Portrait)  │      │ Playback │
           └──────────────┘      └──────────┘
```

### Movie Playback
```
┌──────────────┐    ┌──────────────┐    ┌──────────────────┐
│ MoviesScreen │───▶│ Select Movie │───▶│ MovieDetailScreen│
└──────────────┘    └──────────────┘    └──────────────────┘
                                                  │
                                                  ▼
                                          ┌──────────────┐
                                          │ Click Play   │
                                          │ Button       │
                                          └──────────────┘
                                                  │
                                                  ▼
                                         ┌─────────────────┐
                                         │ MovieMiniPlayer │
                                         │   (Portrait)    │
                                         │   Playing...    │
                                         │   [Seek Bar]    │
                                         └─────────────────┘
                                                  │
                                    ┌─────────────┼─────────────┐
                                    ▼             ▼             ▼
                              ┌─────────┐   ┌─────────┐   ┌─────────┐
                              │  Click  │   │  Seek   │   │  Close  │
                              │ Player  │   │   Bar   │   │  Button │
                              └─────────┘   └─────────┘   └─────────┘
                                    │             │             │
                                    ▼             └─────┬───────┘
                          ┌──────────────────┐         │
                          │ FullscreenPlayer │         │
                          │   (Landscape)    │◀────────┘
                          │   [Seek Bar]     │
                          └──────────────────┘
```

### Series Episode Playback
```
┌──────────────┐    ┌───────────────┐    ┌──────────────────┐
│SeriesScreen  │───▶│ Select Series │───▶│SeriesDetailScreen│
└──────────────┘    └───────────────┘    └──────────────────┘
                                                   │
                                                   ▼
                                          ┌──────────────────┐
                                          │ [Seasons List]   │
                                          │ Expand Season    │
                                          └──────────────────┘
                                                   │
                                                   ▼
                                          ┌──────────────────┐
                                          │ Click Episode    │
                                          └──────────────────┘
                                                   │
                                                   ▼
                                         ┌──────────────────┐
                                         │SeriesMiniPlayer  │
                                         │  (Portrait)      │
                                         │  T1 E1           │
                                         │  [⏮ ⏯ ⏭ ❌]      │
                                         └──────────────────┘
                                                   │
                              ┌────────────────────┼────────────────────┐
                              ▼                    ▼                    ▼
                        ┌──────────┐        ┌───────────┐        ┌──────────┐
                        │  Click   │        │  Previous │        │   Next   │
                        │  Player  │        │  Episode  │        │ Episode  │
                        └──────────┘        └───────────┘        └──────────┘
                              │                    │                    │
                              ▼                    └────────┬───────────┘
                    ┌──────────────────┐                   │
                    │ FullscreenPlayer │                   │
                    │   (Landscape)    │◀──────────────────┘
                    │   T1 E1          │
                    └──────────────────┘
```

## Clases Principales

### PlayerManager
```kotlin
class PlayerManager {
    - player: ExoPlayer?
    
    + initializePlayer(): ExoPlayer
    + playMedia(url: String)
    + play()
    + pause()
    + seekTo(positionMs: Long)
    + getCurrentPosition(): Long
    + getDuration(): Long
    + isPlaying(): Boolean
    + release()
    + getPlayer(): Player?
}
```

### TVMiniPlayer
```kotlin
@Composable
fun TVMiniPlayer(
    streamUrl: String,
    channelName: String,
    onPreviousChannel: () -> Unit,
    onNextChannel: () -> Unit,
    onClose: () -> Unit
)
```

### MovieMiniPlayer
```kotlin
@Composable
fun MovieMiniPlayer(
    streamUrl: String,
    movieTitle: String,
    onClose: () -> Unit
)
```

### SeriesMiniPlayer
```kotlin
@Composable
fun SeriesMiniPlayer(
    streamUrl: String,
    episodeTitle: String,
    seasonNumber: Int,
    episodeNumber: Int,
    onPreviousEpisode: () -> Unit,
    onNextEpisode: () -> Unit,
    onClose: () -> Unit,
    hasPrevious: Boolean,
    hasNext: Boolean
)
```

### FullscreenPlayer
```kotlin
@Composable
fun FullscreenPlayer(
    streamUrl: String,
    title: String,
    playerType: PlayerType,
    onBack: () -> Unit,
    onPreviousItem: (() -> Unit)?,
    onNextItem: (() -> Unit)?,
    hasPrevious: Boolean,
    hasNext: Boolean
)

enum class PlayerType {
    TV,
    MOVIE,
    SERIES
}
```

## Gestión de Ciclo de Vida

```
┌─────────────────────┐
│  Composable Enter   │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│ DisposableEffect    │
│ {                   │
│   player.init()     │
│   player.play()     │
│ }                   │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│   Playing Video     │
│   - Update UI       │
│   - Handle Controls │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│ onDispose {         │
│   player.release()  │
│ }                   │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│  Composable Exit    │
└─────────────────────┘
```

## Estructura de Archivos

```
app/src/main/java/com/iptv/playxy/
├── ui/
│   ├── player/
│   │   ├── PlayerManager.kt
│   │   ├── TVMiniPlayer.kt
│   │   ├── MovieMiniPlayer.kt
│   │   ├── SeriesMiniPlayer.kt
│   │   └── FullscreenPlayer.kt
│   ├── tv/
│   │   ├── TVScreen.kt
│   │   └── TVViewModel.kt
│   ├── movies/
│   │   ├── MovieDetailScreen.kt
│   │   └── MoviesViewModel.kt
│   └── series/
│       ├── SeriesDetailScreen.kt
│       └── SeriesDetailViewModel.kt
└── util/
    └── StreamUrlBuilder.kt
```

