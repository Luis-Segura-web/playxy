# Fix: URL Dinámica del Usuario en API Calls

## 🔴 Problema Identificado

La aplicación estaba usando `http://example.com/` en todas las llamadas API en lugar de la URL ingresada por el usuario.

**Log del problema**:
```
2025-11-07 01:37:29.027  8119-17451 okhttp.OkHttpClient     com.iptv.playxy                      I  --> GET http://example.com/player_api.php?username=FtvLuis&password=yQW8Qj7gdcT5&action=get_vod_streams
```

### Causa Raíz

El `AppModule` de Hilt estaba creando una instancia **Singleton** de `IptvApiService` con una URL base fija (`http://example.com/`):

```kotlin
@Provides
@Singleton
fun provideRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit {
    return Retrofit.Builder()
        .baseUrl("http://example.com/")  // ❌ URL fija
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
}
```

Como era un Singleton, esta URL nunca cambiaba, incluso cuando el usuario ingresaba su propia URL.

---

## ✅ Solución Implementada

### 1. Creación de `ApiServiceFactory`

Se creó una **fábrica** que genera instancias dinámicas de `IptvApiService` con la URL del usuario:

**Archivo nuevo**: `data/api/ApiServiceFactory.kt`

```kotlin
@Singleton
class ApiServiceFactory @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val gson: Gson
) {
    fun createService(baseUrl: String): IptvApiService {
        // Asegura que la URL termine con /
        val formattedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        
        val retrofit = Retrofit.Builder()
            .baseUrl(formattedBaseUrl)  // ✅ URL dinámica
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
        
        return retrofit.create(IptvApiService::class.java)
    }
}
```

### 2. Actualización del `IptvRepository`

El repositorio ahora:
- Inyecta `ApiServiceFactory` en lugar de `IptvApiService`
- Obtiene la URL del perfil del usuario desde la base de datos
- Crea una instancia del servicio API con esa URL

**Cambios en `IptvRepository.kt`**:

```kotlin
@Singleton
class IptvRepository @Inject constructor(
    private val apiServiceFactory: ApiServiceFactory,  // ✅ Factory en lugar de Service
    private val database: PlayxyDatabase
) {
    // ...
    
    suspend fun validateCredentials(username: String, password: String, baseUrl: String): Boolean {
        return try {
            // ✅ Crea servicio con URL proporcionada
            val apiService = apiServiceFactory.createService(baseUrl)
            val response = apiService.validateCredentials(username, password)
            response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    suspend fun loadAllContent(username: String, password: String): Result<Unit> {
        return try {
            // ✅ Obtiene el perfil para sacar la URL
            val profile = userProfileDao.getProfile()
            if (profile == null) {
                return Result.failure(Exception("No user profile found"))
            }
            
            // ✅ Crea servicio con URL del perfil
            val apiService = apiServiceFactory.createService(profile.url)
            
            // Carga el contenido
            loadLiveStreams(apiService, username, password)
            loadVodStreams(apiService, username, password)
            loadSeries(apiService, username, password)
            loadCategories(apiService, username, password)
            
            // ...
        }
    }
}
```

### 3. Actualización de `AppModule`

Se eliminaron las provisiones de `Retrofit` y `IptvApiService` ya que ahora se crean dinámicamente:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideGson(): Gson { /* ... */ }
    
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient { /* ... */ }
    
    // ❌ ELIMINADO: provideRetrofit()
    // ❌ ELIMINADO: provideIptvApiService()
    
    @Provides
    @Singleton
    fun provideDatabase(...): PlayxyDatabase { /* ... */ }
}
```

---

## 🔄 Flujo de Funcionamiento

### Antes (❌ Incorrecto)

```
Usuario ingresa URL: "http://mi-servidor.com/"
        ↓
Se guarda en Room
        ↓
IptvApiService (Singleton) siempre usa: "http://example.com/"
        ↓
❌ Llamadas van a la URL incorrecta
```

### Después (✅ Correcto)

```
Usuario ingresa URL: "http://mi-servidor.com/"
        ↓
Se guarda en Room
        ↓
Repository obtiene perfil de Room
        ↓
ApiServiceFactory.createService("http://mi-servidor.com/")
        ↓
✅ Llamadas van a la URL correcta
```

---

## 📋 Archivos Modificados

### Creados
1. ✅ `data/api/ApiServiceFactory.kt` - Fábrica para crear servicios dinámicos

### Modificados
1. ✅ `data/repository/IptvRepository.kt` - Usa factory en lugar de service
2. ✅ `di/AppModule.kt` - Eliminadas provisiones de Retrofit y ApiService

---

## 🧪 Verificación

### Antes del Fix
```
GET http://example.com/player_api.php?username=FtvLuis&password=...
```

### Después del Fix
```
GET http://[URL_DEL_USUARIO]/player_api.php?username=FtvLuis&password=...
```

---

## ✅ Pruebas Recomendadas

1. **Login con URL personalizada**:
   - Ingresar: `http://tu-servidor.com/`
   - Usuario: `test`
   - Contraseña: `test123`
   - Verificar en logs que la llamada use `http://tu-servidor.com/player_api.php`

2. **Carga de contenido**:
   - Después del login exitoso
   - Verificar en logs que todas las llamadas usen la URL del usuario
   - Ver logs de OkHttp: `GET http://[TU_URL]/player_api.php?action=get_live_streams`

3. **Cambio de servidor**:
   - Cerrar sesión
   - Ingresar con diferente URL
   - Verificar que use la nueva URL

---

## 🎯 Beneficios

1. ✅ **URLs Dinámicas**: Cada usuario puede conectarse a su propio servidor
2. ✅ **Sin Hardcoding**: No hay URLs fijas en el código
3. ✅ **Validación Correcta**: El login valida contra el servidor correcto
4. ✅ **Múltiples Proveedores**: Soporte para diferentes proveedores IPTV
5. ✅ **Mejor Debugging**: Los logs muestran la URL real siendo usada

---

## 📝 Notas Técnicas

### Por qué Factory Pattern

- **Singleton no sirve**: Retrofit con URL fija no puede cambiar dinámicamente
- **Factory crea instancias nuevas**: Cada llamada puede usar diferente URL
- **Performance**: OkHttpClient sigue siendo Singleton (compartido)
- **Flexibilidad**: Fácil agregar más configuraciones por URL

### Manejo de URLs

```kotlin
// Si el usuario ingresa: "http://servidor.com"
// Se formatea a: "http://servidor.com/"
val formattedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
```

Esto asegura que Retrofit combine correctamente:
- Base: `http://servidor.com/`
- Path: `player_api.php`
- Resultado: `http://servidor.com/player_api.php` ✅

---

## 🚨 Posibles Mejoras Futuras

1. **Cache de Services**: Si el usuario no cambia de URL, reutilizar la instancia
2. **Validación de URL**: Verificar formato antes de crear el servicio
3. **Timeout por servidor**: Diferentes timeouts para diferentes proveedores
4. **Retry Logic**: Reintentar con backoff en caso de fallo

---

**Fecha**: 2025-11-07  
**Estado**: ✅ IMPLEMENTADO  
**Verificado**: Compilación exitosa  
**Probado**: Pendiente (requiere dispositivo/emulador)

