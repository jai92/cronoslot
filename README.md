# CronoSlot 1.0.6

Corrección de compilación:
- Añadido el import de `androidx.appcompat.app.AlertDialog` en `CameraActivity.kt`.
- Mantiene Java/Kotlin en JVM 17.
- Mantiene AndroidX.
- GitHub Actions usa Gradle 8.9 directamente.

El error corregido era:
`Unresolved reference 'AlertDialog'`
