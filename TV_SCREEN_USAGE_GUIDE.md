# TV Screen Usage Guide

## For Developers

### Adding a New Virtual Category

To add a new virtual category (like "Trending", "Recently Added", etc.):

1. **Update TVViewModel.loadCategories()**:
```kotlin
val allCategories = buildList {
    add(Category("all", "Todos", "0"))
    add(Category("favorites", "Favoritos", "0"))
    add(Category("recents", "Recientes", "0"))
    add(Category("trending", "Tendencias", "0"))  // NEW
    addAll(providerCategories)
}
```

2. **Update TVViewModel.filterChannels()**:
```kotlin
val channels = when (category.categoryId) {
    "all" -> repository.getLiveStreams().distinctBy { it.streamId }
    "favorites" -> loadFavoriteChannels()
    "recents" -> loadRecentChannels()
    "trending" -> loadTrendingChannels()  // NEW
    else -> repository.getLiveStreamsByCategory(category.categoryId)
}
```

3. **Implement the loading method**:
```kotlin
private suspend fun loadTrendingChannels(): List<LiveStream> {
    // Your custom logic here
    return emptyList()
}
```

### Integrating VLC Player

Replace the placeholder VLCPlayer component:

1. **Add VLC dependency** to `build.gradle.kts`:
```kotlin
implementation("org.videolan.android:libvlc-all:3.5.0")
```

2. **Update VLCPlayer.kt**:
```kotlin
@Composable
fun VLCPlayer(
    url: String,
    modifier: Modifier = Modifier,
    onBuffering: () -> Unit = {},
    onPlaying: () -> Unit = {},
    onError: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val libVlc = remember {
        LibVLC(context, ArrayList<String>().apply {
            add("--no-drop-late-frames")
            add("--no-skip-frames")
            add("--rtsp-tcp")
        })
    }
    
    val mediaPlayer = remember { MediaPlayer(libVlc) }
    
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            SurfaceView(ctx).apply {
                mediaPlayer.attachViews(this, null, false, false)
            }
        }
    )
    
    LaunchedEffect(url) {
        Media(libVlc, Uri.parse(url)).apply {
            mediaPlayer.media = this
            release()
        }
        mediaPlayer.play()
    }
    
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer.release()
            libVlc.release()
        }
    }
}
```

3. **Update MiniPlayerView.kt** to handle callbacks:
```kotlin
VLCPlayer(
    url = channel.directSource ?: "",
    modifier = Modifier.fillMaxSize(),
    onBuffering = viewModel::onBuffering,
    onPlaying = viewModel::onPlaying,
    onError = viewModel::onError
)
```

### Customizing the UI

#### Change Mini-Player Aspect Ratio
In `MiniPlayerView.kt`:
```kotlin
.aspectRatio(16f / 9f)  // Change to 4f / 3f for 4:3 ratio
```

#### Adjust Channel Logo Size
In `ChannelRow.kt`:
```kotlin
.size(48.dp)  // Change to 64.dp for larger logos
```

#### Modify Category Chip Style
In `CategoryChipBar.kt`:
```kotlin
FilterChip(
    selected = category.categoryId == selected?.categoryId,
    onClick = { onCategorySelected(category) },
    label = { Text(category.categoryName) },
    colors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = MaterialTheme.colorScheme.primary,
        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
    )
)
```

### Adding EPG (Electronic Program Guide)

1. **Create EPG entities**:
```kotlin
@Entity(tableName = "epg_programs")
data class EpgProgramEntity(
    @PrimaryKey val id: String,
    val channelId: String,
    val title: String,
    val description: String,
    val startTime: Long,
    val endTime: Long
)
```

2. **Update CurrentChannelInfoView** to show current program:
```kotlin
@Composable
fun CurrentChannelInfoView(
    channel: LiveStream?,
    currentProgram: EpgProgram?,  // NEW
    modifier: Modifier = Modifier
) {
    if (channel == null) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(...)
            Text(channel.name, ...)
        }
        
        // EPG Info
        if (currentProgram != null) {
            Text(
                text = currentProgram.title,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
```

### Performance Optimization

#### Pagination for Large Channel Lists

Update `ChannelListView.kt`:
```kotlin
@Composable
fun ChannelListView(
    channels: List<LiveStream>,
    favoriteChannelIds: Set<String>,
    onChannelClick: (LiveStream) -> Unit,
    onFavoriteClick: (LiveStream) -> Unit,
    onLoadMore: () -> Unit,  // NEW
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(channels, key = { "${it.streamId}_${it.categoryId}" }) { channel ->
            ChannelRow(...)
        }
        
        // Load more trigger
        item {
            LaunchedEffect(Unit) {
                onLoadMore()
            }
        }
    }
}
```

#### Image Caching

Coil automatically caches images, but you can customize it in `Application.onCreate()`:
```kotlin
ImageLoader.Builder(context)
    .memoryCache {
        MemoryCache.Builder(context)
            .maxSizePercent(0.25)
            .build()
    }
    .diskCache {
        DiskCache.Builder()
            .directory(context.cacheDir.resolve("image_cache"))
            .maxSizePercent(0.02)
            .build()
    }
    .build()
```

## For Users

### Using the TV Screen

1. **Navigate to TV tab** in the bottom navigation bar

2. **Browse categories**:
   - Swipe horizontally through category chips
   - Tap a category to filter channels

3. **Play a channel**:
   - Tap any channel in the list
   - Video will appear in the mini-player at the top

4. **Favorite a channel**:
   - Tap the star icon on any channel
   - Filled star = favorited
   - Empty star = not favorited

5. **View favorites**:
   - Select "Favoritos" category
   - Only your favorited channels will appear

6. **View recent**:
   - Select "Recientes" category
   - See your 50 most recently watched channels

7. **Playback controls**:
   - Tap pause/play button in mini-player
   - Use previous/next to switch channels
   - Tap X to close the player

## Troubleshooting

### Channels not loading
- Check internet connection
- Verify profile credentials in Settings
- Try "Forzar Recarga de Contenido" in Settings tab

### Favorites not persisting
- Database may need to be cleared
- Check app permissions
- Ensure app is not in battery optimization mode

### Player not working
- VLC SDK needs to be integrated (currently placeholder)
- Check stream URL format
- Some streams may require specific codecs

### Categories empty
- Pull to refresh or force reload content
- Check if provider has channels in that category
- Verify category data in database

## API Reference

### TVViewModel Public Methods

```kotlin
// Category selection
fun selectCategory(category: Category)

// Playback control
fun playChannel(channel: LiveStream)
fun closePlayer()
fun togglePlayPause()
fun playNextChannel()
fun playPreviousChannel()

// Favorites
fun toggleFavorite(channel: LiveStream)

// Player state callbacks (for VLC integration)
fun onBuffering()
fun onPlaying()
fun onError(message: String)
```

### StateFlow Observables

```kotlin
// Observe in composables with:
val playerState by viewModel.playerState.collectAsState()
val currentChannel by viewModel.currentChannel.collectAsState()
val categories by viewModel.categories.collectAsState()
val selectedCategory by viewModel.selectedCategory.collectAsState()
val filteredChannels by viewModel.filteredChannels.collectAsState()
val favoriteChannelIds by viewModel.favoriteChannelIds.collectAsState()
```

## Testing

### Unit Test Example for TVViewModel

```kotlin
@Test
fun `selectCategory filters channels correctly`() = runTest {
    // Given
    val viewModel = TVViewModel(mockRepository, mockFavoritesDao, mockRecentsDao)
    val testCategory = Category("1", "Sports", "0")
    
    // When
    viewModel.selectCategory(testCategory)
    advanceUntilIdle()
    
    // Then
    assertEquals(testCategory, viewModel.selectedCategory.value)
    verify(mockRepository).getLiveStreamsByCategory("1")
}
```

### UI Test Example

```kotlin
@Test
fun clickingChannelStartsPlayback() {
    composeTestRule.setContent {
        TVScreen()
    }
    
    // Click first channel
    composeTestRule
        .onNodeWithText("ESPN")
        .performClick()
    
    // Verify mini-player is shown
    composeTestRule
        .onNodeWithContentDescription("Cerrar")
        .assertIsDisplayed()
}
```

## Future Feature Ideas

1. **Picture-in-Picture** mode for background playback
2. **Parental controls** with PIN protection for adult content
3. **Multi-audio/subtitle** tracks selection
4. **Recording** functionality for archive-enabled channels
5. **Search** functionality across all channels
6. **Custom channel lists** created by users
7. **Share** channel links with other users
8. **Sleep timer** for auto-close after duration
9. **Chromecast** support for casting to TV
10. **Download** for offline viewing (VOD only)
