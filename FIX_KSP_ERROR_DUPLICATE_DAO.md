# Fix: Error KSP - Conflicto de DAOs Duplicados

## Fecha: 12 de Noviembre de 2025

---

## Error Encontrado

```
> Task :app:kspDebugKotlin FAILED

e: [ksp] /root/StudioProjects/playxy/app/src/main/java/com/iptv/playxy/data/db/PlayxyDatabase.kt:33: Dao function has conflicts.
e: [ksp] /root/StudioProjects/playxy/app/src/main/java/com/iptv/playxy/data/db/PlayxyDatabase.kt:34: Dao function has conflicts.
e: [ksp] /root/StudioProjects/playxy/app/src/main/java/com/iptv/playxy/data/db/PlayxyDatabase.kt:28: All of these functions [categoryDao, cacheMetadataDao] return the same DAO class [com.iptv.playxy.data.db.CacheMetadataDao]. A database can use a DAO only once so you should remove 1 of these conflicting DAO functions.

KSP failed with exit code: PROCESSING_ERROR
```

---

## Causa del Error

En el archivo `PlayxyDatabase.kt`, la función `categoryDao()` estaba devolviendo el tipo incorrecto:

```kotlin
// ❌ INCORRECTO (causaba el error)
abstract fun categoryDao(): CacheMetadataDao  // Línea 33
abstract fun cacheMetadataDao(): CacheMetadataDao  // Línea 34
```

**Problema**: Ambas funciones devolvían `CacheMetadataDao`, violando la regla de Room que establece que **cada DAO solo puede ser usado una vez en una base de datos**.

---

## Solución Aplicada

Se corrigió el tipo de retorno de `categoryDao()` para que devuelva `CategoryDao`:

```kotlin
// ✅ CORRECTO
abstract fun categoryDao(): CategoryDao  // Línea 33
abstract fun cacheMetadataDao(): CacheMetadataDao  // Línea 34
```

### Código Completo Corregido

```kotlin
@Database(
    entities = [
        UserProfileEntity::class,
        LiveStreamEntity::class,
        VodStreamEntity::class,
        SeriesEntity::class,
        CategoryEntity::class,
        CacheMetadata::class,
        FavoriteChannelEntity::class,
        RecentChannelEntity::class,
        FavoriteVodEntity::class,
        RecentVodEntity::class,
        FavoriteSeriesEntity::class,
        RecentSeriesEntity::class,
        MovieProgressEntity::class,
        SeriesProgressEntity::class
    ],
    version = 6,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class PlayxyDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun liveStreamDao(): LiveStreamDao
    abstract fun vodStreamDao(): VodStreamDao
    abstract fun seriesDao(): SeriesDao
    abstract fun categoryDao(): CategoryDao  // ✅ CORREGIDO
    abstract fun cacheMetadataDao(): CacheMetadataDao
    abstract fun favoriteChannelDao(): FavoriteChannelDao
    abstract fun recentChannelDao(): RecentChannelDao
    abstract fun favoriteVodDao(): FavoriteVodDao
    abstract fun recentVodDao(): RecentVodDao
    abstract fun favoriteSeriesDao(): FavoriteSeriesDao
    abstract fun recentSeriesDao(): RecentSeriesDao
    abstract fun movieProgressDao(): MovieProgressDao
    abstract fun seriesProgressDao(): SeriesProgressDao
}
```

---

## Archivo Modificado

**Archivo**: `/root/StudioProjects/playxy/app/src/main/java/com/iptv/playxy/data/db/PlayxyDatabase.kt`

**Línea**: 33

**Cambio**:
```diff
- abstract fun categoryDao(): CacheMetadataDao
+ abstract fun categoryDao(): CategoryDao
```

---

## Contexto del Error

Este error ocurrió durante la implementación de:
1. **Progreso de reproducción** para películas y series
2. **Eliminación de duplicados** en categorías

Al agregar los nuevos DAOs (`MovieProgressDao` y `SeriesProgressDao`), se cometió un error tipográfico al definir `categoryDao()`, escribiendo `CacheMetadataDao` en lugar de `CategoryDao`.

---

## Validación de la Solución

### Antes de la corrección:
```
> Task :app:kspDebugKotlin FAILED
KSP failed with exit code: PROCESSING_ERROR
```

### Después de la corrección:
```
✅ Sin errores de compilación
✅ KSP procesa correctamente todos los DAOs
✅ Room genera correctamente el código de base de datos
```

---

## Por Qué Room Genera Este Error

Room Database tiene una restricción arquitectónica importante:

**Regla**: Cada DAO solo puede ser usado una vez en la base de datos.

**Razón**: 
- Room genera implementaciones concretas de cada DAO
- Si dos funciones devuelven el mismo DAO, Room no sabe cuál es la implementación correcta
- Esto podría causar conflictos en tiempo de ejecución

### Ejemplo de Error:
```kotlin
abstract class MyDatabase : RoomDatabase() {
    abstract fun dao1(): UserDao  // ✅ OK
    abstract fun dao2(): UserDao  // ❌ ERROR: UserDao ya usado
}
```

### Solución Correcta:
```kotlin
abstract class MyDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao  // ✅ OK
    abstract fun profileDao(): ProfileDao  // ✅ OK (DAO diferente)
}
```

---

## Prevención de Errores Similares

### Checklist al agregar nuevos DAOs:

1. ✅ Verificar que cada función `abstract fun` devuelve un DAO único
2. ✅ Usar nombres descriptivos que coincidan con el tipo de retorno
   - `categoryDao()` → devuelve `CategoryDao`
   - `cacheMetadataDao()` → devuelve `CacheMetadataDao`
3. ✅ Ejecutar compilación después de cada cambio en `PlayxyDatabase.kt`
4. ✅ Revisar errores de KSP antes de continuar

### Patrón Recomendado:
```kotlin
abstract fun [nombreDescriptivo]Dao(): [NombreDescriptivo]Dao

// Ejemplos:
abstract fun userDao(): UserDao
abstract fun movieProgressDao(): MovieProgressDao
abstract fun seriesProgressDao(): SeriesProgressDao
```

---

## Impacto del Error

### Severidad: **CRÍTICO** 🔴
- ❌ Bloquea completamente la compilación
- ❌ KSP no puede procesar las anotaciones de Room
- ❌ Imposible generar APK

### Alcance:
- 🔴 Afecta toda la compilación del módulo `app`
- 🔴 Impide testing y deployment

### Tiempo de Resolución:
- ⏱️ **Detección**: Inmediata (error de compilación)
- ⏱️ **Corrección**: < 1 minuto (cambio de 1 línea)
- ⏱️ **Verificación**: ~2 minutos (recompilación)

---

## Lecciones Aprendidas

1. **Copy-Paste con Cuidado**: Al duplicar líneas de código, verificar que todos los tipos sean correctos
2. **Nombres Consistentes**: Usar nombres que reflejen claramente el tipo de retorno
3. **Compilación Incremental**: Compilar frecuentemente para detectar errores temprano
4. **Revisión de Código**: Prestar especial atención a cambios en archivos de configuración de base de datos

---

## Estado Final

✅ **Error corregido**  
✅ **Compilación exitosa**  
✅ **Base de datos versión 6 funcionando correctamente**  
✅ **Todos los DAOs disponibles para uso**

---

**Versión de la app**: 1.0.0  
**Database version**: 6  
**Fecha de corrección**: 12 de Noviembre de 2025  
**Estado**: ✅ **RESUELTO Y VERIFICADO**

