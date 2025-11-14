# Solución de Error KSP - Storage Already Registered

## Fecha: 13 de Noviembre, 2025

## Problema Encontrado
```
java.lang.IllegalStateException: Storage for [/root/StudioProjects/playxy/app/build/kspCaches/debug/symbolLookups/file-to-id.tab] is already registered
```

## Causa del Error
El error ocurre cuando los cachés de KSP (Kotlin Symbol Processing) se corrompen o quedan en un estado inconsistente. Esto puede suceder por:
- Interrupciones de compilación
- Múltiples procesos de Gradle intentando acceder al mismo caché
- Cambios significativos en el código que invalidan el caché incremental

## Solución Aplicada

### 1. Detener todos los daemons de Gradle
```bash
./gradlew --stop
```
Esto asegura que no hay procesos bloqueando los archivos de caché.

### 2. Eliminar cachés corruptos
```bash
rm -rf app/build/kspCaches app/.cxx .gradle/
```
Elimina:
- `app/build/kspCaches`: Cachés de KSP corruptos
- `app/.cxx`: Cachés de C/C++ (si aplica)
- `.gradle/`: Cachés globales de Gradle

### 3. Limpiar el proyecto
```bash
./gradlew clean --no-daemon
```
Limpia todos los artefactos de compilación.

### 4. Recompilar desde cero
```bash
./gradlew :app:assembleDebug --no-daemon
```
Compila el proyecto sin usar daemon para evitar conflictos.

## Resultado
✅ **Compilación exitosa**
✅ **Cachés regenerados correctamente**
✅ **APK debug generado**

## Prevención Futura

### Recomendaciones:
1. **No interrumpir compilaciones**: Dejar que las compilaciones terminen completamente
2. **Limpiar cachés periódicamente**: Ejecutar `./gradlew clean` cuando hay cambios grandes
3. **Usar un solo proceso**: Evitar múltiples compilaciones simultáneas

### Comandos útiles para limpiar:
```bash
# Limpieza rápida (solo build)
./gradlew clean

# Limpieza completa (build + cachés)
./gradlew clean
rm -rf app/build .gradle/

# Limpieza total (incluye KSP)
./gradlew --stop
rm -rf app/build app/.cxx .gradle/
./gradlew clean
```

## Notas Técnicas
- El error específico de KSP indica un problema con el almacenamiento persistente de símbolos
- El registro doble del storage ocurre cuando KSP intenta acceder a un caché ya abierto
- La limpieza de cachés no afecta el código fuente ni los cambios realizados

## Estado del Proyecto
- ✅ Código de reproductores con manejo de errores: **Funcional**
- ✅ Limpieza de frames al cambiar contenido: **Implementado**
- ✅ Sistema de compilación: **Restaurado**
- ✅ Cachés de KSP: **Regenerados**

