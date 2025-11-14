# Playxy - IPTV Android Application

Aplicación IPTV desarrollada en Kotlin para dispositivos móviles Android en formato vertical (Portrait).

## 🎯 Características

- **Gestión completa de contenido IPTV**: TV en vivo, VOD (Películas), y Series
- **🆕 Pantalla TV completa**: Categorías, favoritos y recientes
- **Caché local con Room**: Almacenamiento persistente para mejor rendimiento
- **Soporte para contenido en múltiples categorías**: Claves primarias compuestas (v3)
- **Favoritos y Recientes**: Persistencia de preferencias del usuario
- **Soporte HTTP**: Permite conexiones no seguras según requerimientos de proveedores IPTV
- **Interfaz moderna con Jetpack Compose**: UI declarativa y reactiva
- **Arquitectura MVVM**: Separación clara de responsabilidades
- **Manejo robusto de datos inconsistentes**: Adaptadores Moshi personalizados

## 📱 Flujo de la Aplicación

1. **SplashScreen**: Verificación de perfil guardado
2. **LoginScreen**: Autenticación con proveedor IPTV
3. **LoadingScreen**: Descarga y caché de contenido
4. **MainScreen**: Navegación con 5 pestañas (Inicio, TV, Películas, Series, Ajustes)

## 🔄 Actualización Importante: Base de Datos v3

### Cambios en el Schema

La base de datos ahora incluye **gestión de favoritos y recientes** además de las claves primarias compuestas:

```kotlin
// v3: Nuevas tablas para favoritos y recientes
@Entity(tableName = "favorite_channels")
data class FavoriteChannelEntity(
    @PrimaryKey val channelId: String,
    val timestamp: Long
)

@Entity(tableName = "recent_channels")
data class RecentChannelEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val channelId: String,
    val timestamp: Long
)
```

### 🆕 Pantalla TV

La pantalla de TV en vivo está completamente implementada con:
- **Categorías virtuales**: Todos, Favoritos, Recientes + categorías del proveedor
- **Gestión de favoritos**: Agregar/quitar canales favoritos con persistencia
- **Historial de recientes**: Últimos 50 canales reproducidos
- **Filtrado por categoría**: Scroll horizontal de categorías con FilterChips
- **Lista de canales**: Scroll vertical con logos e iconos de favorito

## 🏗️ Arquitectura

### Estructura de Paquetes

```
com.iptv.playxy/
├── ui/                     # Capa de presentación
│   ├── splash/            # Pantalla de inicio
│   ├── login/             # Pantalla de login
│   ├── loading/           # Pantalla de carga de contenido
│   ├── main/              # Pantalla principal con navegación
│   ├── tv/                # 🆕 Pantalla TV
│   │   ├── TVScreen.kt
│   │   └── components/    # Componentes de la pantalla TV
│   ├── components/        # Componentes reutilizables
│   └── theme/             # Tema de Material Design 3
├── data/                   # Capa de datos
│   ├── api/               # Interfaces Retrofit y modelos de respuesta
│   ├── db/                # Entidades Room y DAOs
│   └── repository/        # Repositorios (coordinación de fuentes)
├── domain/                 # Modelos de dominio
│   ├── LiveStream.kt
│   ├── Category.kt
│   ├── FavoriteChannel.kt # 🆕
│   └── RecentChannel.kt   # 🆕
└── di/                     # Módulos de Hilt para DI
```

## Dependencias Principales

- **Jetpack Compose**: UI moderna y declarativa
- **Hilt**: Inyección de dependencias
- **Retrofit + OkHttp**: Consumo de APIs
- **Moshi**: Serialización/Deserialización JSON con adaptadores personalizados
- **Room**: Base de datos local
- **Navigation Compose**: Navegación entre pantallas
- **Coroutines**: Programación asíncrona
- **Coil**: Carga de imágenes asíncrona (🆕)

## Flujo de la Aplicación

### 1. Splash Screen
- Muestra el logo de la aplicación
- Verifica si existe un perfil válido en la base de datos
- Navega a Loading si hay perfil válido, o a Login si no

### 2. Login Screen
- Campos: Nombre de Perfil, Usuario, Contraseña, URL
- Validación de formato de URL (http:// o https://)
- Validación de credenciales con el proveedor IPTV
- Almacenamiento seguro del perfil en Room

### 3. Loading Screen
- **Estrategia de caché**: Primero verifica caché local
- Si la caché es válida (< 24 horas), carga desde Room
- Si no, descarga contenido del proveedor:
  - Canales de TV en vivo
  - Películas (VOD)
  - Series
  - Categorías
- Muestra progreso visual (0-100%) y estado actual

### 4. Main Screen
- Navegación inferior con 5 pestañas:
  1. **Inicio**: Estadísticas de contenido disponible
  2. **TV**: 🆕 Pantalla completa con mini-reproductor, categorías y favoritos
  3. **Películas**: En construcción
  2. **TV**: 🆕 Pantalla completa con categorías y favoritos
  5. **Ajustes**: Cerrar sesión y forzar recarga

#### Características de la Pantalla TV

La pestaña TV incluye una implementación completa con:

**Layout de 4 secciones**:
1. **Mini-Reproductor** (16:9) - Solo visible cuando hay reproducción activa
**Layout de 2 secciones**:
1. **Barra de Categorías** - Scroll horizontal con FilterChips
2. **Lista de Canales** - Scroll vertical con logos y botón de favorito
- **Todos**: Todos los canales (sin duplicados)
- **Favoritos**: Canales marcados como favoritos
- **Recientes**: Últimos 50 canales reproducidos
- **[Categorías del Proveedor]**: Ordenadas alfabéticamente

**Funcionalidades**:
- Marcar/desmarcar favoritos (persiste en Room)
- Historial de reproducción automático
- Navegación entre canales (anterior/siguiente)
- Filtrado por categoría

## Manejo de Datos

### Adaptadores Moshi Personalizados

El sistema incluye adaptadores para manejar datos inconsistentes de la API:

1. **StringToBooleanAdapter**: Convierte "0"/"1" y strings a Boolean
2. **SafeFloatAdapter**: Convierte String/Int/null a Float con valor por defecto
3. **SafeIntAdapter**: Convierte String/null a Int con valor por defecto
4. **SafeStringAdapter**: Convierte null/"null" a String vacío

### Mappers

- **ResponseMapper**: Convierte respuestas de API a modelos de dominio
- **EntityMapper**: Convierte entre entidades Room y modelos de dominio

## Configuración

### AndroidManifest.xml

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

<application
    android:name=".PlayxyApp"
    android:usesCleartextTraffic="true"
    ...>
```

### Build Configuration

- **minSdk**: 24 (Android 7.0)
- **targetSdk**: 35 (Android 15)
- **compileSdk**: 35
- **Kotlin**: 1.9.22
- **Compose Compiler**: 1.5.10

## Seguridad

⚠️ **Nota Importante**: Esta aplicación permite tráfico HTTP sin cifrar (`usesCleartextTraffic="true"`) 
ya que muchos proveedores IPTV requieren conexiones HTTP. En producción, considere:
- Encriptar credenciales almacenadas en Room
- Usar HTTPS cuando el proveedor lo soporte
- Implementar validación adicional de certificados

## Funcionalidades Futuras

### Reproductor VLC
Un componente `VLCPlayer` está preparado para integración futura:
- Reproducción de streams en vivo
- Reproducción de VOD
- Controles de reproducción
- Integración con las pantallas de TV, Películas y Series


### Ejecutar Tests

```bash
./gradlew test
```

### Instalar en Dispositivo

```bash
./gradlew installDebug
```

## Licencia

Este proyecto es de código abierto y está disponible bajo los términos especificados en el archivo LICENSE.

## Contacto

Para más información, consulte la documentación del proyecto o contacte al equipo de desarrollo.
