# Guía de Uso: Consultas con Claves Primarias Compuestas

## Contexto

Después de los cambios en el schema de la base de datos, ahora cada stream/serie puede existir en múltiples categorías. Esta guía explica cómo aprovechar esta funcionalidad.

## Escenarios de Uso

### 1. Mostrar Contenido en la Pantalla Principal (Todas las Categorías)

Cuando necesites mostrar todo el contenido disponible sin filtrar por categoría:

```kotlin
// En tu ViewModel
class MainViewModel @Inject constructor(
    private val repository: IptvRepository
) : ViewModel() {
    
    fun loadAllContent() {
        viewModelScope.launch {
            val liveStreams = repository.getLiveStreams()
            val vodStreams = repository.getVodStreams()
            val series = repository.getSeries()
            
            // Nota: Estos métodos retornan TODAS las entradas,
            // incluyendo duplicados del mismo contenido en diferentes categorías
        }
    }
}
```

⚠️ **Importante**: `getAllLiveStreams()` retornará duplicados si el mismo stream está en múltiples categorías. Para mostrar contenido único, necesitarás filtrar por `streamId`:

```kotlin
val uniqueStreams = liveStreams.distinctBy { it.streamId }
```

### 2. Mostrar Contenido Filtrado por Categoría Seleccionada

Este es el **caso de uso recomendado** para la mayoría de pantallas:

```kotlin
// En tu ViewModel
class LiveTVViewModel @Inject constructor(
    private val repository: IptvRepository
) : ViewModel() {
    
    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory
    
    private val _streams = MutableStateFlow<List<LiveStream>>(emptyList())
    val streams: StateFlow<List<LiveStream>> = _streams
    
    fun selectCategory(categoryId: String) {
        viewModelScope.launch {
            _selectedCategory.value = categoryId
            _streams.value = repository.getLiveStreamsByCategory(categoryId)
        }
    }
    
    fun loadCategories() {
        viewModelScope.launch {
            val categories = repository.getCategories("live")
            // Mostrar categorías en la UI
            // Seleccionar la primera por defecto
            if (categories.isNotEmpty()) {
                selectCategory(categories.first().categoryId)
            }
        }
    }
}
```

### 3. Buscar un Stream Específico en una Categoría Específica

Útil para verificar si un stream específico pertenece a una categoría:

```kotlin
suspend fun checkStreamInCategory(streamId: String, categoryId: String): LiveStream? {
    return liveStreamDao.getLiveStream(streamId, categoryId)
        ?.let { EntityMapper.liveStreamToDomain(it) }
}
```

### 4. Ver Todas las Categorías de un Stream

Útil para mostrar al usuario en qué categorías aparece un contenido:

```kotlin
suspend fun getStreamCategories(streamId: String): List<String> {
    val streamInstances = liveStreamDao.getLiveStreamsByStreamId(streamId)
    return streamInstances.map { it.categoryId }
}

// Uso en UI
viewModelScope.launch {
    val categories = getStreamCategories("12345")
    // ["Deportes", "HD Channels", "Internacionales"]
}
```

## Ejemplos de Composables

### Composable para Mostrar Streams por Categoría

```kotlin
@Composable
fun LiveTVScreen(
    viewModel: LiveTVViewModel = hiltViewModel()
) {
    val categories by viewModel.categories.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val streams by viewModel.streams.collectAsState()
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Tabs de categorías
        ScrollableTabRow(
            selectedTabIndex = categories.indexOfFirst { it.categoryId == selectedCategory }
        ) {
            categories.forEachIndexed { index, category ->
                Tab(
                    selected = category.categoryId == selectedCategory,
                    onClick = { viewModel.selectCategory(category.categoryId) },
                    text = { Text(category.categoryName) }
                )
            }
        }
        
        // Grid de streams
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(streams) { stream ->
                StreamCard(
                    stream = stream,
                    onClick = { /* Reproducir */ }
                )
            }
        }
    }
}
```

### Composable para Mostrar Contenido Destacado (Sin Duplicados)

```kotlin
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val allStreams by viewModel.allStreams.collectAsState()
    
    // Filtrar para mostrar solo streams únicos
    val uniqueStreams = remember(allStreams) {
        allStreams.distinctBy { it.streamId }
    }
    
    LazyColumn {
        // Sección de canales destacados
        item {
            Text(
                "Canales Destacados",
                style = MaterialTheme.typography.headlineSmall
            )
        }
        
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(uniqueStreams.take(10)) { stream ->
                    StreamCard(stream = stream)
                }
            }
        }
    }
}
```

## Optimización de Consultas

### Evitar Consultas Innecesarias

En lugar de consultar todas las categorías y luego filtrar en memoria:

❌ **NO Recomendado:**
```kotlin
val allStreams = repository.getLiveStreams()
val sportsStreams = allStreams.filter { it.categoryId == "10" }
```

✅ **Recomendado:**
```kotlin
val sportsStreams = repository.getLiveStreamsByCategory("10")
```

### Cache en ViewModel

Almacena las categorías en el ViewModel para evitar consultas repetidas:

```kotlin
class ContentViewModel @Inject constructor(
    private val repository: IptvRepository
) : ViewModel() {
    
    // Cargar categorías solo una vez
    private val _liveCategories = MutableStateFlow<List<Category>>(emptyList())
    val liveCategories: StateFlow<List<Category>> = _liveCategories
    
    init {
        loadCategories()
    }
    
    private fun loadCategories() {
        viewModelScope.launch {
            _liveCategories.value = repository.getCategories("live")
        }
    }
}
```

## Consideraciones de Rendimiento

### Tamaño de la Base de Datos

Con claves primarias compuestas, si un stream aparece en 5 categorías, se guardará 5 veces. Esto es intencional pero debes tener en cuenta:

- **Ventaja**: Consultas por categoría son extremadamente rápidas (índice directo)
- **Desventaja**: Mayor uso de espacio en disco
- **Mitigación**: La diferencia es mínima (texto duplicado en campos como `name`, `streamIcon`, etc.)

### Índices Automáticos

Room crea automáticamente índices para las claves primarias compuestas, por lo que las consultas son eficientes:

```sql
-- Estas consultas usan el índice de clave primaria
SELECT * FROM live_streams WHERE categoryId = '10'
SELECT * FROM live_streams WHERE streamId = '12345' AND categoryId = '10'
```

## Migración de Datos Existentes

Cuando la aplicación se actualice con estos cambios:

1. La base de datos se recreará automáticamente (`.fallbackToDestructiveMigration()`)
2. Todos los datos se recargarán desde la API en la `LoadingScreen`
3. El usuario verá un proceso de carga inicial

No se requiere ninguna acción manual del usuario.

## Preguntas Frecuentes

### ¿Por qué no usar una tabla de relación Many-to-Many?

**Respuesta**: Aunque técnicamente más "normalizado", para una app IPTV es más eficiente tener los datos duplicados porque:
- Las consultas son más simples (un solo SELECT vs JOIN)
- Room carga los datos más rápido
- El espacio extra es negligible en dispositivos modernos
- La API ya envía los datos de esta forma (cada respuesta incluye `category_id`)

### ¿Qué pasa si actualizo un stream?

**Respuesta**: Si el stream aparece en múltiples categorías, necesitarás actualizar todas las instancias. Esto se maneja automáticamente cuando recargas desde la API, ya que se borra todo (`deleteAll()`) y se vuelve a insertar.

### ¿Cómo evito mostrar duplicados al usuario?

**Respuesta**: Depende del contexto:
- **En listados por categoría**: No hay duplicados (cada consulta es por `categoryId`)
- **En búsqueda global**: Usa `distinctBy { it.streamId }`
- **En favoritos/historial**: Guarda solo el par `(streamId, categoryId)` preferido del usuario

