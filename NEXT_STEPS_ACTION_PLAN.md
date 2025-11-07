# Plan de Acción: Integración de Claves Compuestas en la UI

## ✅ Completado

1. ✅ Modificación del schema de base de datos (v1 → v2)
2. ✅ Implementación de claves primarias compuestas
3. ✅ Actualización de DAOs con nuevos métodos
4. ✅ Extensión del Repository con consultas por categoría
5. ✅ Documentación completa creada
6. ✅ Suite de pruebas unitarias implementada

## 🎯 Próximos Pasos Recomendados

### Paso 1: Actualizar LoadingViewModel

El `LoadingViewModel` necesita asegurarse de que los datos se carguen correctamente con el nuevo schema.

**Ubicación**: `ui/loading/LoadingViewModel.kt`

**Cambios necesarios**: Ninguno - Ya está usando los métodos correctos del repositorio.

**Verificación**:
```kotlin
// El método loadAllContent() ya maneja correctamente:
repository.loadAllContent(username, password)
// Este método internamente usa deleteAll() + insertAll()
// que funciona perfectamente con claves compuestas
```

### Paso 2: Actualizar MainViewModel

Agregar soporte para cargar categorías y permitir filtrado.

**Ubicación**: `ui/main/MainViewModel.kt`

**Cambios sugeridos**:
```kotlin
// Agregar estado para categorías
private val _liveCategories = MutableStateFlow<List<Category>>(emptyList())
val liveCategories: StateFlow<List<Category>> = _liveCategories

private val _vodCategories = MutableStateFlow<List<Category>>(emptyList())
val vodCategories: StateFlow<List<Category>> = _vodCategories

private val _seriesCategories = MutableStateFlow<List<Category>>(emptyList())
val seriesCategories: StateFlow<List<Category>> = _seriesCategories

// Cargar categorías al inicio
init {
    loadCategories()
}

private fun loadCategories() {
    viewModelScope.launch {
        _liveCategories.value = repository.getCategories("live")
        _vodCategories.value = repository.getCategories("vod")
        _seriesCategories.value = repository.getCategories("series")
    }
}
```

### Paso 3: Crear ViewModels para TV, Películas, Series

Actualmente estas pantallas muestran "En Construcción". Es momento de implementarlas.

#### 3.1 Crear LiveTVViewModel

**Archivo nuevo**: `ui/main/LiveTVViewModel.kt`

```kotlin
@HiltViewModel
class LiveTVViewModel @Inject constructor(
    private val repository: IptvRepository
) : ViewModel() {
    
    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories
    
    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory
    
    private val _streams = MutableStateFlow<List<LiveStream>>(emptyList())
    val streams: StateFlow<List<LiveStream>> = _streams
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    init {
        loadCategories()
    }
    
    private fun loadCategories() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val cats = repository.getCategories("live")
                _categories.value = cats
                
                // Seleccionar primera categoría por defecto
                if (cats.isNotEmpty()) {
                    selectCategory(cats.first().categoryId)
                }
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun selectCategory(categoryId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _selectedCategory.value = categoryId
            try {
                // USAR EL NUEVO MÉTODO POR CATEGORÍA
                _streams.value = repository.getLiveStreamsByCategory(categoryId)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun refresh() {
        _selectedCategory.value?.let { selectCategory(it) }
    }
}
```

#### 3.2 Crear MoviesViewModel

**Archivo nuevo**: `ui/main/MoviesViewModel.kt`

```kotlin
@HiltViewModel
class MoviesViewModel @Inject constructor(
    private val repository: IptvRepository
) : ViewModel() {
    
    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories
    
    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory
    
    private val _movies = MutableStateFlow<List<VodStream>>(emptyList())
    val movies: StateFlow<List<VodStream>> = _movies
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    init {
        loadCategories()
    }
    
    private fun loadCategories() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val cats = repository.getCategories("vod")
                _categories.value = cats
                
                if (cats.isNotEmpty()) {
                    selectCategory(cats.first().categoryId)
                }
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun selectCategory(categoryId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _selectedCategory.value = categoryId
            try {
                // USAR EL NUEVO MÉTODO POR CATEGORÍA
                _movies.value = repository.getVodStreamsByCategory(categoryId)
            } finally {
                _isLoading.value = false
            }
        }
    }
}
```

#### 3.3 Crear SeriesViewModel

**Archivo nuevo**: `ui/main/SeriesViewModel.kt`

```kotlin
@HiltViewModel
class SeriesViewModel @Inject constructor(
    private val repository: IptvRepository
) : ViewModel() {
    
    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories
    
    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory
    
    private val _series = MutableStateFlow<List<Series>>(emptyList())
    val series: StateFlow<List<Series>> = _series
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    init {
        loadCategories()
    }
    
    private fun loadCategories() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val cats = repository.getCategories("series")
                _categories.value = cats
                
                if (cats.isNotEmpty()) {
                    selectCategory(cats.first().categoryId)
                }
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun selectCategory(categoryId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _selectedCategory.value = categoryId
            try {
                // USAR EL NUEVO MÉTODO POR CATEGORÍA
                _series.value = repository.getSeriesByCategory(categoryId)
            } finally {
                _isLoading.value = false
            }
        }
    }
}
```

### Paso 4: Actualizar Composables de UI

#### 4.1 Actualizar LiveTVContent

**Ubicación**: `ui/main/MainScreen.kt` (o crear archivo separado)

```kotlin
@Composable
fun LiveTVContent(
    viewModel: LiveTVViewModel = hiltViewModel()
) {
    val categories by viewModel.categories.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val streams by viewModel.streams.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Tabs de categorías
        if (categories.isNotEmpty()) {
            ScrollableTabRow(
                selectedTabIndex = categories.indexOfFirst { 
                    it.categoryId == selectedCategory 
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                categories.forEach { category ->
                    Tab(
                        selected = category.categoryId == selectedCategory,
                        onClick = { viewModel.selectCategory(category.categoryId) },
                        text = { Text(category.categoryName) }
                    )
                }
            }
        }
        
        // Contenido
        Box(modifier = Modifier.fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (streams.isEmpty()) {
                Text(
                    "No hay canales en esta categoría",
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(streams, key = { "${it.streamId}_${it.categoryId}" }) { stream ->
                        LiveStreamCard(
                            stream = stream,
                            onClick = { /* TODO: Reproducir */ }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LiveStreamCard(
    stream: LiveStream,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // TODO: Cargar imagen con Coil
            Icon(
                imageVector = Icons.Default.Tv,
                contentDescription = null,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stream.name,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}
```

### Paso 5: Agregar Coil para Cargar Imágenes

Las imágenes de streams (`streamIcon`, `cover`) necesitan cargarse desde URLs.

**Agregar dependencia** en `app/build.gradle.kts`:
```kotlin
implementation("io.coil-kt:coil-compose:2.5.0")
```

**Uso en Composables**:
```kotlin
AsyncImage(
    model = stream.streamIcon,
    contentDescription = stream.name,
    modifier = Modifier.size(48.dp),
    error = painterResource(R.drawable.ic_launcher_foreground),
    placeholder = painterResource(R.drawable.ic_launcher_foreground)
)
```

## 📋 Checklist de Implementación

### Fase 1: ViewModels (Alta Prioridad)
- [ ] Crear `LiveTVViewModel.kt`
- [ ] Crear `MoviesViewModel.kt`
- [ ] Crear `SeriesViewModel.kt`
- [ ] Actualizar `MainViewModel.kt` para cargar categorías

### Fase 2: UI Components (Alta Prioridad)
- [ ] Actualizar `LiveTVContent` con tabs de categorías
- [ ] Actualizar `MoviesContent` con tabs de categorías
- [ ] Actualizar `SeriesContent` con tabs de categorías
- [ ] Crear componentes reutilizables (`CategoryTabs`, `ContentGrid`)

### Fase 3: Mejoras Visuales (Media Prioridad)
- [ ] Agregar dependencia Coil
- [ ] Implementar carga de imágenes en cards
- [ ] Agregar animaciones de transición entre categorías
- [ ] Implementar pull-to-refresh

### Fase 4: Funcionalidades Avanzadas (Baja Prioridad)
- [ ] Implementar búsqueda (con `distinctBy` para evitar duplicados)
- [ ] Agregar favoritos
- [ ] Implementar historial de reproducción
- [ ] Estadísticas en HomeContent

## 🎨 Diseño Recomendado

```
┌─────────────────────────────────────┐
│  PLAYXY                    [Profile]│
├─────────────────────────────────────┤
│ ┌─────┬─────┬─────┬─────┬─────┐    │
│ │ All │ HD  │Sport│News │More...   │ ← Categorías (ScrollableTabRow)
│ └─────┴─────┴─────┴─────┴─────┘    │
├─────────────────────────────────────┤
│  ┌───┐ ┌───┐ ┌───┐                 │
│  │   │ │   │ │   │  ← Grid 3 cols  │
│  └───┘ └───┘ └───┘                 │
│  ┌───┐ ┌───┐ ┌───┐                 │
│  │   │ │   │ │   │                 │
│  └───┘ └───┘ └───┘                 │
│  ...                                │
├─────────────────────────────────────┤
│ [Home] [TV] [Movies] [Series] [⚙️] │ ← BottomNavigation
└─────────────────────────────────────┘
```

## 🔍 Puntos Clave a Recordar

### 1. Usar Siempre los Métodos Por Categoría
```kotlin
// ✅ CORRECTO
repository.getLiveStreamsByCategory(categoryId)

// ❌ EVITAR (retorna duplicados)
repository.getLiveStreams().filter { it.categoryId == categoryId }
```

### 2. Keys en LazyGrid/LazyColumn
```kotlin
// ✅ CORRECTO - Combina streamId + categoryId
items(streams, key = { "${it.streamId}_${it.categoryId}" }) { ... }

// ❌ INCORRECTO - streamId se repite
items(streams, key = { it.streamId }) { ... }
```

### 3. Búsqueda Global
```kotlin
// Si implementas búsqueda, evita duplicados
val searchResults = repository.getLiveStreams()
    .filter { it.name.contains(query, ignoreCase = true) }
    .distinctBy { it.streamId }  // ← Importante
```

## 📊 Métricas de Éxito

Después de implementar estos cambios, deberías ver:

- ✅ Carga rápida de contenido por categoría
- ✅ Navegación fluida entre tabs de categorías
- ✅ Sin duplicados visibles en la UI
- ✅ Capacidad de mostrar el mismo contenido en múltiples categorías
- ✅ Base de datos con datos completos y precisos

## 🆘 Troubleshooting

### Problema: "No se muestran streams"
**Solución**: Verificar que la categoría seleccionada tenga contenido.
```kotlin
Log.d("Category", "Selected: $selectedCategory, Streams: ${streams.size}")
```

### Problema: "Veo streams duplicados"
**Solución**: Asegurarte de usar métodos `*ByCategory`, no `getAll*()`.

### Problema: "Error al cambiar de categoría"
**Solución**: Verificar que el `key` en LazyGrid incluya categoryId.

---

**Estado Actual**: ✅ Base de datos lista  
**Siguiente Paso**: Implementar ViewModels  
**Prioridad**: Alta  
**Dificultad Estimada**: Media  
**Tiempo Estimado**: 2-3 horas

