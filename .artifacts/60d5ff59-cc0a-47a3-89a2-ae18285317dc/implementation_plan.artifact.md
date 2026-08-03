# Plan de Corrección de Errores del Backend

He detectado dos problemas principales:
1. **Error de Construcción (Gradle):** Existe una incompatibilidad entre Gradle y el plugin de Shadow (usado por Ktor) debido a la eliminación de la propiedad `mainClassName` en versiones recientes de Gradle.
2. **Errores de Compilación (Kotlin):** En `ProductRoutes.kt` faltan importaciones para el manejo de archivos `multipart` (usado en la funcionalidad de visión por IA).

## Cambios Propuestos

### Componente: Infraestructura de Construcción

#### [MODIFY] [gradle-wrapper.properties](file:///C:/Users/Darkar/StudioProjects/backend/gradle/wrapper/gradle-wrapper.properties)
- Actualizar Gradle a la versión `8.10.2` para asegurar compatibilidad con Ktor 3.0.

#### [MODIFY] [build.gradle.kts](file:///C:/Users/Darkar/StudioProjects/backend/build.gradle.kts)
- Actualizar el plugin de Ktor a la versión `3.0.3`.
- Mantener el uso de `mainClass.set()`.
- **Solución al error `mainClassName`:** Añadiremos un bloque para forzar la propiedad `mainClassName` en las tareas de Shadow si el plugin no las detecta correctamente, o desactivaremos las tareas de distribución redundantes.

### Componente: Rutas y Lógica de Negocio

#### [MODIFY] [ProductRoutes.kt](file:///C:/Users/Darkar/StudioProjects/backend/src/main/kotlin/com/example/routes/ProductRoutes.kt)
- Añadir `import io.ktor.http.content.*` para habilitar el soporte de `MultiPartData` y `PartData`.
- Corregir el uso de `readBytes()` a `readRawBytes()` (o similar) si es necesario, y asegurar el contexto de corrutinas.

## Plan de Verificación

### Pruebas Automatizadas
- Ejecutar `./gradlew assemble` para verificar la construcción completa.
- Ejecutar `./gradlew buildFatJar` para confirmar la generación del ejecutable.

### Verificación Manual
- Iniciar el servidor y probar el endpoint de visión `/api/inventario/vision` para confirmar que el manejo de imágenes funciona correctamente.
