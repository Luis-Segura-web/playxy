# Cambios en el Schema de la Base de Datos

## Problema Identificado

Los `streamId` y `seriesId` se repetían en varias categorías, causando conflictos en la base de datos ya que estaban definidos como claves primarias únicas.

### Ejemplo del Problema:
```
Stream ID: "12345" aparece en:
- Categoría "Deportes" (categoryId: "10")
- Categoría "HD Channels" (categoryId: "20")
```

Con el schema anterior, solo se podía guardar una de estas entradas, perdiendo información importante sobre la relación contenido-categoría.

## Solución Implementada

Se ha cambiado el diseño de las tablas para usar **claves primarias compuestas** que combinan el ID del contenido con el ID de la categoría.

### Cambios en las Entidades

#### 1. LiveStreamEntity
**Antes:**
```kotlin
@Entity(tableName = "live_streams")
data class LiveStreamEntity(
    @PrimaryKey val streamId: String,
    val categoryId: String,
    // ... otros campos
)
```

**Después:**
```kotlin
@Entity(
    tableName = "live_streams",
    primaryKeys = ["streamId", "categoryId"]
)
data class LiveStreamEntity(
    val streamId: String,
    val categoryId: String,
    // ... otros campos
)
```

#### 2. VodStreamEntity
**Antes:**
```kotlin
@Entity(tableName = "vod_streams")
data class VodStreamEntity(
    @PrimaryKey val streamId: String,
    val categoryId: String,
    // ... otros campos
)
```

**Después:**
```kotlin
@Entity(
    tableName = "vod_streams",
    primaryKeys = ["streamId", "categoryId"]
)
data class VodStreamEntity(
    val streamId: String,
    val categoryId: String,
    // ... otros campos
)
```

#### 3. SeriesEntity
**Antes:**
```kotlin
@Entity(tableName = "series")
data class SeriesEntity(
    @PrimaryKey val seriesId: String,
    val categoryId: String,
    // ... otros campos
)
```

**Después:**
```kotlin
@Entity(
    tableName = "series",
    primaryKeys = ["seriesId", "categoryId"]
)
data class SeriesEntity(
    val seriesId: String,
    val categoryId: String,
    // ... otros campos
)
```

## Mejoras en los DAOs

Se han agregado nuevos métodos de consulta para mayor flexibilidad:

### LiveStreamDao
```kotlin
// Obtener todos los streams con un streamId específico (todas las categorías)
suspend fun getLiveStreamsByStreamId(streamId: String): List<LiveStreamEntity>

// Obtener un stream específico de una categoría específica
suspend fun getLiveStream(streamId: String, categoryId: String): LiveStreamEntity?
```

### VodStreamDao
```kotlin
// Obtener todos los VOD con un streamId específico (todas las categorías)
suspend fun getVodStreamsByStreamId(streamId: String): List<VodStreamEntity>

// Obtener un VOD específico de una categoría específica
suspend fun getVodStream(streamId: String, categoryId: String): VodStreamEntity?
```

### SeriesDao
```kotlin
// Obtener todas las series con un seriesId específico (todas las categorías)
suspend fun getSeriesBySeriesId(seriesId: String): List<SeriesEntity>

// Obtener una serie específica de una categoría específica
suspend fun getSeries(seriesId: String, categoryId: String): SeriesEntity?
```

## Versión de Base de Datos

La versión de la base de datos se ha incrementado de `1` a `2`:
```kotlin
@Database(
    entities = [...],
    version = 2,  // Incrementado desde 1
    exportSchema = false
)
```

## Impacto en la Aplicación

### ✅ Ventajas:
1. **Datos Completos**: Ahora se pueden guardar todos los streams/series aunque aparezcan en múltiples categorías
2. **Relaciones Correctas**: Se mantiene la relación exacta entre contenido y categorías
3. **Sin Conflictos**: No hay errores al insertar el mismo contenido en diferentes categorías
4. **Consultas Flexibles**: Se pueden consultar streams por ID específico o por combinación ID+categoría

### ⚠️ Consideraciones:
1. **Duplicación de Datos**: El mismo stream puede estar guardado múltiples veces (una por categoría)
   - Esto es intencional y refleja la estructura real de la API
   - Permite filtrado correcto por categorías
   
2. **Migración Automática**: La base de datos se recreará automáticamente en la próxima ejecución
   - Configurado con `.fallbackToDestructiveMigration()`
   - Los datos antiguos se perderán (se recargarán desde la API)

## Uso Recomendado

### Para Mostrar Contenido por Categoría:
```kotlin
val streams = liveStreamDao.getLiveStreamsByCategory("10")
```

### Para Buscar Contenido Específico:
```kotlin
val stream = liveStreamDao.getLiveStream("12345", "10")
```

### Para Ver Todas las Categorías de un Stream:
```kotlin
val allInstances = liveStreamDao.getLiveStreamsByStreamId("12345")
// Retorna el mismo stream en todas sus categorías
```

## Archivos Modificados

1. `data/db/Entities.kt` - Cambio de claves primarias
2. `data/db/Daos.kt` - Nuevos métodos de consulta
3. `data/db/PlayxyDatabase.kt` - Incremento de versión
4. `util/EntityMapper.kt` - Sin cambios (ya estaba correcto)
5. `data/repository/IptvRepository.kt` - Sin cambios (ya estaba correcto)

