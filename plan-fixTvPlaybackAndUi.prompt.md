TL;DR — Triage and fix compile errors in `TVViewModel.kt`, stabilize fullscreen (stop repeated ExoPlayer/MediaCodec init/release), make play/pause toggle reflect player state, ensure category/channel selection + scroll highlight behave, update movie/series poster grid visuals and overlays in Compose, add accessibility and automated tests, and run quality gates. Each step lists files to change, key pseudocode fixes, tests, exact UI attributes, concurrency/codec notes, and an estimated time/cutover strategy.

### Steps
1. Investigate & fix Kotlin compile errors in `TVViewModel.kt` (`app/src/main/java/com/iptv/playxy/ui/tv/TVViewModel.kt`).
2. Investigate fullscreen reproduce logs; fix player lifecycle and codec churn in `PlayerManager.kt`, `TVMiniPlayer.kt`, `FullscreenPlayer.kt`.
3. Implement robust play/pause state propagation in `PlayerManager.kt` and UI components (`TVMiniPlayer.kt`, `MovieMiniPlayer.kt`, player controls).
4. Ensure category switching selects first channel and returning to a category highlights/scrolls to currently playing channel (`TVViewModel.kt`, `ChannelListView.kt`, `TVScreen.kt`).
5. Update posters UI for movies/series (`MoviesScreen.kt`, `SeriesScreen.kt`) to match poster aspect, 3-min columns, overlay gradient, badges and labels.
6. Add accessibility attributes, edge-case handling, and automated tests (unit + UI Compose tests).
7. Quality gates, verification steps, and rollback/mitigation plan.

---

### Step 1 — Fix Kotlin compile errors in `TVViewModel.kt` (1–3 hours)
What to do
- Files: `app/src/main/java/com/iptv/playxy/ui/tv/TVViewModel.kt`
- Actions:
  1. Re-run the Gradle compile error (./gradlew assembleDebug) and capture exact error lines to correlate with source lines.
  2. Inspect `TVViewModel.kt` for:
     - Duplicate top-level declarations (functions or stray `}`) that might move code outside the class.
     - Unresolved references `_currentChannel`, `_filteredChannels`, `playChannel`: check scope and that declarations are inside class.
     - Lambdas using `it` causing ambiguous reference: replace `it` with explicit lambda param name where nested scopes exist.
     - “missing operator modifiers” — search for use of `plus`, `invoke`, `get` etc. If errors mention `operator` on functions, add `operator` modifier or remove uses that expect `operator`.
  3. Concrete fixes/pseudocode:
     - Ensure backing properties declared exactly inside class:
       - `private val _currentChannel = MutableStateFlow<LiveStream?>(null)` and corresponding `val currentChannel: StateFlow<LiveStream?> = _currentChannel.asStateFlow()`
     - Avoid accidental top-level duplicates—make sure all functions (e.g., `playNextChannel`, `playPreviousChannel`) are inside `class TVViewModel { ... }`.
     - Replace ambiguous lambda `filter { it -> ... }` if nested; prefer `filter { stream -> stream.streamId == ... }`.
     - If compile error references missing `operator` for `plus` or similar, add `operator fun plus(...)` only if needed; otherwise change call site to use explicit method.
  4. Rebuild until compile errors cleared.
- Edge-cases:
  - Partial fixes may leave generated Hilt classes stale — run a clean build if needed.
- Success criteria: `./gradlew :app:assembleDebug` compiles without errors.

Notes / Example fixes (pseudocode inline)
- If you see "Expecting top level declaration" near end-of-file: remove stray `}` or move functions inside `class TVViewModel`.
- If a lambda uses nested `it`, change:
  - before: `favorites.map { it.channelId }.toSet()`
  - OK to keep; but if nested `it` ambiguous, use `favorites.map { f -> f.channelId }`

Time estimate: 1–3 hours (depends on number of stray declarations).

---

### Step 2 — Investigate & fix fullscreen immediate-exit and codec churn (3–8 hours)
What to do
- Files: `app/src/main/java/com/iptv/playxy/ui/player/PlayerManager.kt`, `TVMiniPlayer.kt`, `FullscreenPlayer.kt`, `TVScreen.kt`, `app/src/main/java/com/iptv/playxy/ui/tv/components/ChannelListView.kt`
- Investigation checklist:
  1. Reproduce the issue with device and collect full logcat focused on:
     - MediaCodec messages (connect/disconnect/reconfigure)
     - ExoPlayer init/release logs (PlayerManager.initialize, release)
     - UI events on full screen toggle
  2. Search code for places that call `PlayerManager.release()` or create a new ExoPlayer instance when entering fullscreen or when Compose recomposes.
- Root causes to consider:
  - Player instance being released/created on UI recomposition or on full-screen toggle. Releasing recreates decoder (MediaCodec) causing connect/disconnect logs and abrupt exit/back to small player.
  - Fullscreen activity or Compose route recreating PlayerView/Surface and not reattaching player properly.
- Recommended fixes:
  1. Adopt single-source-of-truth Player instance:
     - Keep ExoPlayer instance in `PlayerManager` as singleton per Activity or Application lifecycle (depending on app design).
     - Add explicit lifecycle-binding methods: initializeOnce(context), attachPlayer(surfaceView), detachPlayer(keepAlive = true), releaseWhenAppStops.
  2. Avoid release on entering fullscreen. Only detach view and reattach new PlayerView (do not call `.release()`).
     - Implement `PlayerManager.attach(playerView: PlayerView) { playerView.player = player }` and `PlayerManager.detach(playerView) { playerView.player = null }`.
  3. Use Player.Listener callbacks to persist playback position, state and avoid re-preparing the media unnecessarily when reattaching.
  4. Add a debug wrapper: track create/release counts and log them to detect duplicates.
- Concurrency / codec notes:
  - MediaCodec reconfigure/disconnect indicates resource churn. Minimize release calls to avoid codec recreation (costly and risk of errors).
  - Guard against multiple coroutines calling `PlayerManager.initialize()` simultaneously — use synchronized or atomic `initialized` flag.
  - Handle transient Surface re-creation: do not stop playback if only Surface lost; instead reattach.
- Small pseudocode:
  - PlayerManager exposes:
    - `fun initialize(context)` (idempotent)
    - `fun play(url)` (reuses existing player, set mediaItem + prepare)
    - `fun attach(playerView)` / `fun detach(playerView, retainPlayback = true)`
    - `fun release(force = false)` (only called on app shutdown)
- Success criteria:
  - Fullscreen toggle does not trigger ExoPlayer release/recreate.
  - Logcat no longer shows repeated MediaCodec connect/disconnect cycles for normal toggles.

Time estimate: 3–8 hours (investigate logs 1–2h, changes + manual QA 2–6h).

---

### Step 3 — Fix play/pause button state toggle (0.5–2 hours)
What to do
- Files: `PlayerManager.kt`, `TVMiniPlayer.kt`, `MovieMiniPlayer.kt`, any Compose player controls.
- Root cause pattern:
  - UI reads `player.isPlaying` but not observing updates; or `player.pause()` executed but UI state not updated because listener not wired to a StateFlow/LiveData.
- Recommended solution:
  1. Add `isPlayingState: MutableStateFlow<Boolean>` inside `PlayerManager` and update it on Player events:
     - `player.addListener(object : Player.Listener { override fun onIsPlayingChanged(isPlaying) { isPlayingState.value = isPlaying } })`
  2. In UI (Compose), collect `isPlayingState.collectAsState()` and render icon accordingly (Play vs Pause).
  3. Ensure control actions call `PlayerManager.play()` / `PlayerManager.pause()` and do not directly mutate UI state — rely on Player.Listener for authoritative state.
- Pseudocode:
  - PlayerManager:
    - `val isPlayingState = MutableStateFlow(false)`
    - on listener: `isPlayingState.value = isPlaying`
  - Compose:
    - `val isPlaying by playerManager.isPlayingState.collectAsState()`
    - `IconButton(onClick = { if (isPlaying) playerManager.pause() else playerManager.play() }) { Icon(if (isPlaying) PauseIcon else PlayIcon) }`
- Edge cases:
  - Network buffering -> `isPlaying` may be false while buffering. Consider exposing `playbackState` with buffering/ready/ended.
- Success criteria:
  - Play/pause button toggles icon reliably when user taps or playback state changes.

Time estimate: 0.5–2 hours.

---

### Step 4 — Category switching and channel selection/scroll behavior (1–4 hours)
What to do
- Files: `TVViewModel.kt`, `ChannelListView.kt`, `TVScreen.kt`
- Goals:
  - When changing TV category, show/select first channel if none playing.
  - When returning to a category (or returning to TV screen) highlight the currently-playing channel and scroll to it.
- Implementation plan:
  1. `TVViewModel` changes:
     - Add `fun selectCategory(category)` (already exists) to set `_selectedCategory` and after `filterChannels(category)` ensure selected channel defaults:
       - If `_currentChannel.value == null` and `filteredChannels` not empty: set `_currentChannel.value = filteredChannels.first()`
       - Optionally expose `selectedChannelId: StateFlow<String?>` derived from `_currentChannel`.
  2. Compose `ChannelListView`:
     - Accept `currentChannelId: String?` and `onChannelClick`.
     - Use `LazyColumn`/`LazyRow` with `LazyListState` and a `LaunchedEffect(currentChannelId)` to `animateScrollToItem(indexOf(currentChannelId))`.
     - When new category selected, if `currentChannelId` is null, scroll to index 0.
 3. When navigating away/back:
     - Keep `TVViewModel.currentChannel` persisted; when `TVScreen` Composable is recomposed, pass `currentChannelId` and let `LaunchedEffect` bring it into view.
- Pseudocode (behavior):
  - In `selectCategory`:
    - call `filterChannels`
    - if `_currentChannel.value == null && _filteredChannels.value.isNotEmpty()` then set `_currentChannel.value = _filteredChannels.value[0]`
  - In `ChannelListView`:
    - `val listState = rememberLazyListState()`
    - `LaunchedEffect(currentChannelId, filteredChannels) { val idx = filteredChannels.indexOfFirst { it.streamId == currentChannelId } if (idx >= 0) listState.animateScrollToItem(idx) else listState.animateScrollToItem(0) }`
- Edge-cases:
  - `filteredChannels` could be loaded async: use LaunchedEffect keyed on both `currentChannelId` and `filteredChannels`.
- Success criteria:
  - User sees first channel after category change if nothing playing.
  - Returning shows current playing channel highlighted and scrolled.

Time estimate: 1–4 hours (including QA on focus/navigation).

---

### Step 5 — Poster grid UI changes for movies & series (2–6 hours)
What to do
- Files: `app/src/main/java/com/iptv/playxy/ui/movies/MoviesScreen.kt`, `app/src/main/java/com/iptv/playxy/ui/series/SeriesScreen.kt`
- Visual spec (exact attributes for Compose):
  1. Grid:
     - Use LazyVerticalGrid(columns = GridCells.Fixed(3)) — minimum 3 posters across.
     - Adaptive behavior: on wide screens increase fixed columns e.g., 4 or 5 if width permits.
  2. Poster card:
     - Use Box { AsyncImage(modifier = Modifier.fillMaxWidth().aspectRatio(2f/3f), ...) } to enforce poster aspect ratio 2:3.
     - Card padding: 8.dp; corner shape: 8.dp.
  3. Overlays:
     - Full-top translucent black background with downward gradient: Box(modifier = Modifier.matchParentWidth().height(48.dp).background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent))))
     - Place this overlay aligned to Top of poster.
  4. Badges:
     - Favorite button top-left: small IconButton (size 36.dp) with background CircleShape and contentDescription = "Favorite".
     - Rating badge top-right: small rounded rectangle with bold rating text; background black with alpha 0.75 and padding 4.dp.
  5. Title:
     - Place title under the poster image (or as bottom overlay) with maxLines = 3 and overflow = TextOverflow.Ellipsis.
  6. Accessibility:
     - Provide contentDescription for poster image: `"Poster of {title}"`.
- Example Compose attribute suggestions (descriptive, not code block):
  - Poster Image: Modifier.aspectRatio(2f / 3f).clip(RoundedCornerShape(8.dp))
  - Grid: LazyVerticalGrid(columns = GridCells.Fixed(3), contentPadding = PaddingValues(8.dp))
  - Title Text: fontSize = 14.sp, maxLines = 3, overflow = TextOverflow.Ellipsis
  - Badge backgrounds: Color.Black.copy(alpha = 0.6f) and use Brush.verticalGradient for full-top effect.
- Files to update: `MoviePosterItem` and `SeriesPosterItem` functions inside the two screen files.
- Success criteria:
  - Posters render with 2:3 aspect.
  - Minimum of 3 items across on standard phone widths.
  - Favorite and rating badges are visible in specified positions and overlay gradient applied.
  - Titles limited to 3 lines.

Time estimate: 2–6 hours (layout + responsive tweaks + QA).

---

### Step 6 — Accessibility, edge cases, and tests to add (4–12 hours)
What to do
- Accessibility items (apply across player and poster UIs):
  - Add contentDescription for all images (poster, channel icons).
  - Buttons: set semantic descriptions and focusable (IconButton contentDescription = "Play", "Pause", "Favorite").
  - Ensure correct focus order in TV/remote navigation: set focusable/focusRequesters on channel rows and poster cards.
  - Add TalkBack strings for state: "Playback paused", "Playing channel X", "Added to favorites".
- Edge-cases to handle in code:
  - Network failure while playing: show a retry overlay and do not crash player.
  - Empty categories: show empty-state UI with friendly message.
  - DB Errors for favorites/recents: fallback silently and log.
  - Player concurrency: prevent multiple play requests overlapping (use mutex or atomic boolean to guard `startPlaying`).
- Automated tests
  - Unit tests (JVM):
    - `TVViewModelTest`:
      - test `filterChannels` for categories "all", "favorites", "recents".
      - test `playNextChannel`/`playPreviousChannel` boundary conditions.
      - test `toggleFavorite` updates `_favoriteChannelIds`.
    - `PlayerManager` unit tests:
      - idempotent `initialize` creates single player instance.
      - `attach`/`detach` does not release player when detach(retain = true).
  - Compose UI tests:
    - `PlayPauseToggleTest`:
      - simulate button click and assert icon changes and `PlayerManager.isPlayingState` updates.
    - `FullscreenPersistenceTest`:
      - simulate fullscreen toggle and verify Player instance not released (spy on PlayerManager).
    - `PosterGridTest`:
      - check that grid has at least 3 columns at given width, badges exist, and title truncation occurs.
  - Integration/Instrumentation:
    - `ChannelScrollTest`:
      - select category, verify first item selected and scrolled.
      - start playing a channel, navigate away/back, assert scroll & highlight.
- Tools / frameworks:
  - Use JUnit, Mockito/MockK, ComposeTestRule.
  - Add simple test harnesses under `app/src/test` and `app/src/androidTest`.
- Success criteria:
  - Unit tests pass locally.
  - Compose tests validate UI states.

Time estimate: 4–12 hours (writing tests + making code testable + running CI).

---

### Step 7 — Quality gates and verification steps (0.5–2 hours)
What to run after changes (commands described; run in repo root):
- Build: `./gradlew :app:assembleDebug`
- Run unit tests: `./gradlew :app:testDebugUnitTest`
- Run lint: `./gradlew :app:lint`
- Run Compose/UI tests (connected device/emulator): `./gradlew :app:connectedDebugAndroidTest`
- Smoke verification steps (manual test checklist):
  1. Launch app, open TV screen, switch categories: first channel appears when no channel playing.
  2. Start playback, enter fullscreen, verify player remains playing and no immediate exit.
  3. Toggle play/pause, verify button icon changes and playback state toggles.
  4. Favorite/unfavorite channel, return to categories -> favorites shows correct list.
  5. Open Movies/Series screens: posters show 2:3 aspect, 3 columns minimum, badges positioned, titles max 3 lines.
  6. Run accessibility checks (TalkBack or Accessibility Scanner) to ensure contentDescription presence.

Time estimate: 0.5–2 hours.

---

### Further Considerations
1. Clarifying question: Should ExoPlayer lifecycle be single-app-wide (Application-scoped) or Activity/Screen-scoped? Recommendation: Activity-scoped PlayerManager is simpler and safer; only make it Application-scoped if several activities share playback.
2. Option A / Minimal risk: Make changes incrementally (fix compile first → player state → fullscreen) and run tests after each. Option B / Fast but riskier: Change player lifecycle first to prevent fullscreen churn (bigger change).
3. Rollback plan: Implement each change in a small PR. If a regression appears:
   - Revert the PR quickly.
   - If player lifecycle fix causes regressions in other screens, fall back to previous release/init behavior but add safe guards (e.g., keep old code and toggle new logic behind a feature flag).

---

### File-by-file likely changes (concise)
- `app/src/main/java/com/iptv/playxy/ui/tv/TVViewModel.kt`
  - Ensure all properties/functions inside class.
  - Add `selectedChannelId` flow, select-first-channel logic in `selectCategory`.
- `app/src/main/java/com/iptv/playxy/ui/tv/components/ChannelListView.kt`
  - Accept `currentChannelId`, use `LazyListState` and `LaunchedEffect` to scroll/highlight.
- `app/src/main/java/com/iptv/playxy/ui/player/PlayerManager.kt`
  - Make initialization idempotent, expose `isPlayingState: StateFlow<Boolean>`, `attach/detach` methods, avoid release on fullscreen toggle.
- `app/src/main/java/com/iptv/playxy/ui/player/TVMiniPlayer.kt`, `MovieMiniPlayer.kt`
  - Collect `isPlayingState` and render play/pause icon; call `playerManager.play()` / `pause()`.
- `app/src/main/java/com/iptv/playxy/ui/player/FullscreenPlayer.kt`
  - Use attach/detach API to reattach view without releasing player.
- `app/src/main/java/com/iptv/playxy/ui/movies/MoviesScreen.kt`, `SeriesScreen.kt`
  - Update `MoviePosterItem` / `SeriesPosterItem` to use aspectRatio(2/3), overlays with vertical gradient, badges top-left/top-right, title maxLines=3.
- Tests:
  - `app/src/test/java/.../TVViewModelTest.kt`, `PlayerManagerTest.kt`
  - `app/src/androidTest/.../PlayPauseToggleTest.kt`, `PosterGridTest.kt`, `ChannelScrollTest.kt`

---

### Estimated ordering & total time (rough)
1. Fix compile errors in `TVViewModel.kt` — 1–3h (blocker)
2. PlayerManager idempotent init & attach/detach — 3–8h
3. Play/pause state propagation UI changes — 0.5–2h
4. Category/channel selection + scroll — 1–4h
5. Poster UI changes — 2–6h
6. Accessibility + tests — 4–12h
7. Quality gates + verification — 0.5–2h

Total estimated implementation time: approximately 12–37 hours depending on unknowns and CI flakiness.

---

### Short rollback / mitigation plan
- Work in feature branches for each area (TVViewModel fix, PlayerManager lifecycle, Poster UI).
- Run all unit/Compose tests locally and CI after each PR.
- If fullscreen regression occurs: revert the PlayerManager change and reintroduce a reduced-scope fix (e.g., prevent release only on specific UI path).
- Provide a feature flag (boolean in `PlayerManager`) to toggle the new attach/detach behavior for quick rollback without code revert.

---

Please review this draft plan and tell me:
- Which change you want prioritized (compile fix first is recommended).
- Preference for PlayerManager scope (Activity vs Application).
- If you want me to produce a PR-by-PR breakdown (one PR per step) with exact code-level TODOs for each file.

