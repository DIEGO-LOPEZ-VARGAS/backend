# Plan de Corrección del Backend

El proyecto presenta errores de compilación y posibles fallos de configuración que impiden su ejecución. He identificado un error crítico en el nombre del objeto encargado del hashing de contraseñas y falta de importaciones.

## Cambios Propuestos

### Componente: Seguridad y Rutas

He detectado una inconsistencia entre la definición del `PasswordHasher` y su uso en las rutas y repositorios.

#### [MODIFY] [PasswordHasher.kt](file:///C:/Users/Darkar/StudioProjects/backend/src/main/kotlin/com/example/security/PasswordHasher.kt)
- Corregir el nombre del objeto de `gitPasswordHasher` a `PasswordHasher`.

#### [MODIFY] [AuthRoutes.kt](file:///C:/Users/Darkar/StudioProjects/backend/src/main/kotlin/com/example/routes/AuthRoutes.kt)
- Añadir la importación faltante: `import com.example.security.PasswordHasher`.

### Componente: Configuración de Base de Datos

#### [MODIFY] [DatabaseFactory.kt](file:///C:/Users/Darkar/StudioProjects/backend/src/main/kotlin/com/example/config/DatabaseFactory.kt)
- Mejorar la robustez del parseo de `DATABASE_URL` para evitar fallos comunes si el formato varía ligeramente.

## Plan de Verificación

### Pruebas Automatizadas
- Ejecutar `./gradlew build` para confirmar que los errores de compilación se han resuelto.

### Verificación Manual
- Iniciar la aplicación y verificar que el servidor Netty arranca en el puerto 8080 (o el puerto configurado).
- Comprobar que la base de datos se inicializa correctamente (ya sea Postgres o H2).
