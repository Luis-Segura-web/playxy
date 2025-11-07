# Playxy IPTV - Architecture Diagram

## Application Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                      PLAYXY IPTV APPLICATION                     │
│                    (Portrait-Only Android App)                   │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                         UI LAYER (Compose)                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌──────────────┐   ┌──────────────┐   ┌──────────────┐        │
│  │   Splash     │ → │    Login     │ → │   Loading    │        │
│  │   Screen     │   │    Screen    │   │   Screen     │        │
│  └──────────────┘   └──────────────┘   └──────────────┘        │
│         │                   │                   │               │
│         ├───────────────────┴───────────────────┘               │
│         ↓                                                        │
│  ┌──────────────────────────────────────────────────────┐       │
│  │              Main Screen (Bottom Nav)                │       │
│  ├──────────────────────────────────────────────────────┤       │
│  │  Home  │   TV   │  Movies  │  Series  │  Settings  │       │
│  │ (Stats)│ (TODO) │  (TODO)  │  (TODO)  │  (Logout)  │       │
│  └──────────────────────────────────────────────────────┘       │
│                                                                  │
└────────────────────────┬─────────────────────────────────────────┘
                         │
                         ↓
┌─────────────────────────────────────────────────────────────────┐
│                    VIEWMODEL LAYER (State)                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │   Splash     │  │    Login     │  │   Loading    │          │
│  │  ViewModel   │  │  ViewModel   │  │  ViewModel   │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
│                                                                  │
│  ┌──────────────────────────────────────────────────┐           │
│  │              Main ViewModel                      │           │
│  └──────────────────────────────────────────────────┘           │
│                                                                  │
└────────────────────────┬─────────────────────────────────────────┘
                         │ StateFlow
                         ↓
┌─────────────────────────────────────────────────────────────────┐
│                    REPOSITORY LAYER                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌───────────────────────────────────────────────────┐          │
│  │          IptvRepository (Singleton)               │          │
│  │  ┌─────────────────────────────────────────────┐ │          │
│  │  │  Cache-First Strategy (24h expiration)     │ │          │
│  │  │  - Check cache validity                    │ │          │
│  │  │  - Load from Room if valid                 │ │          │
│  │  │  - Fetch from API if expired               │ │          │
│  │  │  - Save to Room for next time              │ │          │
│  │  └─────────────────────────────────────────────┘ │          │
│  └───────────────────────────────────────────────────┘          │
│               │                           │                     │
│               ↓                           ↓                     │
│      ┌────────────────┐         ┌────────────────┐             │
│      │  Local Source  │         │ Remote Source  │             │
│      │     (Room)     │         │   (Retrofit)   │             │
│      └────────────────┘         └────────────────┘             │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
                         │                     │
         ┌───────────────┴──────┬──────────────┘
         ↓                      ↓
┌──────────────────┐  ┌──────────────────────┐
│   DATA LAYER     │  │   DATA LAYER (API)   │
│   (Database)     │  │    (Network)         │
├──────────────────┤  ├──────────────────────┤
│                  │  │                      │
│  Room Database:  │  │  Retrofit Service:   │
│  ┌────────────┐  │  │  ┌────────────────┐ │
│  │ Profiles   │  │  │  │ get_live       │ │
│  │ LiveStream │  │  │  │ get_vod        │ │
│  │ VodStream  │  │  │  │ get_series     │ │
│  │ Series     │  │  │  │ get_categories │ │
│  │ Categories │  │  │  │ validate_user  │ │
│  │ Cache Meta │  │  │  └────────────────┘ │
│  └────────────┘  │  │                      │
│                  │  │  OkHttp Client:      │
│  DAOs:           │  │  - HTTP support      │
│  - CRUD ops      │  │  - 30s timeout       │
│  - Suspend fns   │  │  - Logging           │
│  - Flow streams  │  │                      │
│                  │  │  Gson Converters:    │
│  Type Converters:│  │  - Custom adapters   │
│  - List<String>  │  │  - Lenient parsing   │
│                  │  │                      │
└──────────────────┘  └──────────────────────┘
         ↑                      ↑
         │                      │
         └──────────┬───────────┘
                    ↓
┌─────────────────────────────────────────────────────────────────┐
│                    DOMAIN LAYER (Models)                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │ LiveStream   │  │  VodStream   │  │   Series     │          │
│  │ (11 props)   │  │  (12 props)  │  │  (14 props)  │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
│                                                                  │
│  ┌──────────────┐  ┌──────────────────────────────┐            │
│  │  Category    │  │      UserProfile             │            │
│  │  (3 props)   │  │      (7 props)               │            │
│  └──────────────┘  └──────────────────────────────┘            │
│                                                                  │
│  All models include:                                            │
│  - Null safety (String?, nullable types)                        │
│  - Default values (empty strings, 0, false)                     │
│  - Type-safe fields (no mixed types)                            │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
                              ↑
                              │
┌─────────────────────────────────────────────────────────────────┐
│                    UTILITY LAYER (Mappers)                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌────────────────────────────────────────────────┐             │
│  │         Type Adapters (Gson)                   │             │
│  │  ┌──────────────────────────────────────────┐ │             │
│  │  │ StringToBooleanAdapter  "0"/"1" → Bool  │ │             │
│  │  │ SafeFloatAdapter        String → Float  │ │             │
│  │  │ SafeIntAdapter          String → Int    │ │             │
│  │  │ SafeStringAdapter       null → ""       │ │             │
│  │  └──────────────────────────────────────────┘ │             │
│  └────────────────────────────────────────────────┘             │
│                                                                  │
│  ┌────────────────────────────────────────────────┐             │
│  │              Mappers                           │             │
│  │  ┌──────────────────────────────────────────┐ │             │
│  │  │ ResponseMapper  API → Domain             │ │             │
│  │  │ EntityMapper    Entity ↔ Domain          │ │             │
│  │  └──────────────────────────────────────────┘ │             │
│  └────────────────────────────────────────────────┘             │
│                                                                  │
│  ┌────────────────────────────────────────────────┐             │
│  │            EncodingUtils                       │             │
│  │  - Base64 encode/decode                        │             │
│  └────────────────────────────────────────────────┘             │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│               DEPENDENCY INJECTION (Hilt)                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  @HiltAndroidApp  →  PlayxyApp                                  │
│                                                                  │
│  @Singleton Providers:                                          │
│  - Gson (with custom adapters)                                  │
│  - OkHttpClient (logging + timeouts)                            │
│  - Retrofit (dynamic base URL)                                  │
│  - IptvApiService                                               │
│  - Room Database                                                │
│  - Repository                                                   │
│                                                                  │
│  @HiltViewModel  →  All ViewModels                              │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                   CONFIGURATION                                  │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  AndroidManifest.xml:                                           │
│  - android:usesCleartextTraffic="true"  (HTTP support)          │
│  - android:screenOrientation="portrait" (Portrait only)         │
│  - INTERNET & ACCESS_NETWORK_STATE permissions                  │
│                                                                  │
│  Build Configuration:                                           │
│  - minSdk: 24 (Android 7.0+)                                    │
│  - targetSdk: 35 (Android 15)                                   │
│  - Kotlin 1.9.22                                                │
│  - Compose Compiler 1.5.10                                      │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

## Data Flow Example: Loading Content

```
1. User lands on LoadingScreen
   │
   ↓
2. LoadingViewModel.init()
   │
   ↓
3. Check cache validity
   │
   ├─→ Cache Valid? → Load from Room → Navigate to Main
   │
   └─→ Cache Invalid/Missing?
       │
       ↓
4. Get UserProfile from Repository
   │
   ↓
5. Call Repository.loadAllContent(username, password)
   │
   ├─→ API: get_live_streams
   │   ├─→ Response: List<LiveStreamResponse>
   │   ├─→ Map to: List<LiveStream> (via ResponseMapper)
   │   └─→ Save to: Room (via EntityMapper)
   │
   ├─→ API: get_vod_streams
   │   ├─→ Response: List<VodStreamResponse>
   │   ├─→ Map to: List<VodStream>
   │   └─→ Save to: Room
   │
   ├─→ API: get_series
   │   ├─→ Response: List<SeriesResponse>
   │   ├─→ Map to: List<Series>
   │   └─→ Save to: Room
   │
   └─→ API: get_categories (x3)
       ├─→ Response: List<CategoryResponse>
       ├─→ Map to: List<Category>
       └─→ Save to: Room
   │
   ↓
6. Update CacheMetadata with current timestamp
   │
   ↓
7. Update UI with progress (0% → 100%)
   │
   ↓
8. Navigate to MainScreen
```

## Key Design Patterns

1. **MVVM (Model-View-ViewModel)**
   - Clear separation of concerns
   - Testable business logic
   - Reactive UI updates

2. **Repository Pattern**
   - Single source of truth
   - Abstraction over data sources
   - Cache-first strategy

3. **Dependency Injection**
   - Hilt for compile-time safety
   - Singleton scope for shared instances
   - Easy testing and mocking

4. **Type Safety**
   - Custom Gson adapters
   - Null safety throughout
   - Default values for resilience

5. **Reactive Programming**
   - StateFlow for state management
   - Kotlin Coroutines for async
   - Flow for data streams
