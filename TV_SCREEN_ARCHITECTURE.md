# TV Screen Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              TVScreen (Composable)                       │
│                                                                          │
│  ┌────────────────────────────────────────────────────────────────────┐ │
│  │ TVViewModel (Hilt injected)                                        │ │
│  │  - playerState: StateFlow<PlayerState>                             │ │
│  │  - currentChannel: StateFlow<LiveStream?>                          │ │
│  │  - categories: StateFlow<List<Category>>                           │ │
│  │  - selectedCategory: StateFlow<Category?>                          │ │
│  │  - filteredChannels: StateFlow<List<LiveStream>>                   │ │
│  │  - favoriteChannelIds: StateFlow<Set<String>>                      │ │
│  └────────────────────────────────────────────────────────────────────┘ │
│                                                                          │
│  Column (fillMaxSize, no scroll) {                                      │
│                                                                          │
│    ┌──────────────────────────────────────────────────────────────┐    │
│    │ 1. MiniPlayerView (conditional: if currentChannel != null)   │    │
│    │    - Aspect ratio: 16:9                                      │    │
│    │    - VLCPlayer component (placeholder)                       │    │
│    │    - Overlay controls (Close, Play/Pause, Prev/Next)         │    │
│    │    - Buffering indicator                                     │    │
│    │    - Error display                                           │    │
│    └──────────────────────────────────────────────────────────────┘    │
│                                                                          │
│    ┌──────────────────────────────────────────────────────────────┐    │
│    │ 2. CurrentChannelInfoView (conditional: if channel != null)  │    │
│    │    - Channel logo (64dp, rounded)                            │    │
│    │    - Channel name (max 3 lines)                              │    │
│    └──────────────────────────────────────────────────────────────┘    │
│                                                                          │
│    ┌──────────────────────────────────────────────────────────────┐    │
│    │ 3. CategoryChipBar (fixed, horizontal scroll)                │    │
│    │    LazyRow {                                                 │    │
│    │      FilterChip("Todos")                                     │    │
│    │      FilterChip("Favoritos")                                 │    │
│    │      FilterChip("Recientes")                                 │    │
│    │      FilterChip("Category 1")                                │    │
│    │      FilterChip("Category 2")                                │    │
│    │      ...                                                      │    │
│    │    }                                                          │    │
│    └──────────────────────────────────────────────────────────────┘    │
│                                                                          │
│    ┌──────────────────────────────────────────────────────────────┐    │
│    │ 4. ChannelListView (weight=1f, vertical scroll)              │    │
│    │    LazyColumn {                                              │    │
│    │      ┌────────────────────────────────────────────────────┐  │    │
│    │      │ ChannelRow                                         │  │    │
│    │      │  [Logo] Channel Name                    [★ Icon]   │  │    │
│    │      └────────────────────────────────────────────────────┘  │    │
│    │      ┌────────────────────────────────────────────────────┐  │    │
│    │      │ ChannelRow                                         │  │    │
│    │      │  [Logo] Channel Name                    [☆ Icon]   │  │    │
│    │      └────────────────────────────────────────────────────┘  │    │
│    │      ...                                                     │    │
│    │    }                                                          │    │
│    └──────────────────────────────────────────────────────────────┘    │
│  }                                                                       │
└─────────────────────────────────────────────────────────────────────────┘


Data Flow:
┌─────────────┐     ┌──────────────┐     ┌─────────────┐     ┌─────────┐
│ TVViewModel │────▶│ IptvRepository│────▶│ PlayxyDB    │────▶│ Room    │
│             │     │               │     │ (v3)        │     │ DAOs    │
└─────────────┘     └──────────────┘     └─────────────┘     └─────────┘
       │                                         │
       │                                         │
       ▼                                         ▼
   StateFlows                          ┌──────────────────┐
   - playerState                       │ LiveStreamDao    │
   - currentChannel                    │ CategoryDao      │
   - categories                        │ FavoriteDao      │
   - filteredChannels                  │ RecentDao        │
   - favoriteChannelIds                └──────────────────┘


User Interactions:

1. Select Category
   User clicks FilterChip → viewModel.selectCategory(category)
   → filterChannels() → Update filteredChannels StateFlow

2. Play Channel
   User clicks ChannelRow → viewModel.playChannel(channel)
   → Update currentChannel, playerState
   → Insert into RecentChannelDao

3. Toggle Favorite
   User clicks Star Icon → viewModel.toggleFavorite(channel)
   → Insert/Delete in FavoriteChannelDao
   → Reload favoriteChannelIds
   → If in "Favoritos" category, refresh filteredChannels

4. Playback Controls
   User clicks Play/Pause → viewModel.togglePlayPause()
   → Update playerState (Playing ↔ Paused)

5. Close Player
   User clicks X → viewModel.closePlayer()
   → Set currentChannel = null, playerState = Idle


Category Filtering Logic:

┌─────────────────────────────────────────────────────────────────┐
│ selectedCategory.categoryId                                      │
├─────────────────────────────────────────────────────────────────┤
│ "all"        → repository.getLiveStreams()                      │
│                .distinctBy { it.streamId }                      │
├─────────────────────────────────────────────────────────────────┤
│ "favorites"  → Get favorite IDs from FavoriteChannelDao         │
│                Filter all streams by those IDs                  │
├─────────────────────────────────────────────────────────────────┤
│ "recents"    → Get recent IDs from RecentChannelDao (limit 50)  │
│                Map IDs to LiveStream objects in order           │
├─────────────────────────────────────────────────────────────────┤
│ [category]   → repository.getLiveStreamsByCategory(categoryId)  │
└─────────────────────────────────────────────────────────────────┘
```

## State Management

All UI state is managed through Kotlin StateFlows in the ViewModel:

```kotlin
// Player state transitions
Idle → Playing (when channel selected)
Playing → Paused (when pause clicked)
Paused → Playing (when play clicked)
Playing → Buffering (when loading)
Buffering → Playing (when ready)
* → Error (on playback error)
Error → Playing (on retry)
* → Idle (when player closed)
```

## Database Schema (v3)

```
┌──────────────────────────┐
│ favorite_channels        │
├──────────────────────────┤
│ channelId (PK)   TEXT    │
│ timestamp        INTEGER │
└──────────────────────────┘

┌──────────────────────────┐
│ recent_channels          │
├──────────────────────────┤
│ id (PK)          INTEGER │ (auto-increment)
│ channelId        TEXT    │
│ timestamp        INTEGER │
└──────────────────────────┘

Note: recent_channels limited to 50 items via query
```

## Composable Hierarchy

```
TVScreen
├── MiniPlayerView
│   └── VLCPlayer (placeholder)
├── CurrentChannelInfoView
│   └── AsyncImage (Coil)
├── CategoryChipBar
│   └── LazyRow
│       └── FilterChip (for each category)
└── ChannelListView
    └── LazyColumn
        └── ChannelRow (for each channel)
            ├── AsyncImage (logo)
            ├── Text (name)
            └── IconButton (favorite)
```
