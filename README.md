# CronoSlot 1.0.4

Corrección de compatibilidad JVM:
- Java toolchain 17 explícito.
- Kotlin JVM toolchain 17 explícito.
- compileOptions Java 17.
- AndroidX habilitado.
- GitHub Actions instala Gradle 8.9 directamente.

Workflow:
`.github/workflows/build-apk.yml`

Compilación:
`gradle assembleDebug --no-daemon`
