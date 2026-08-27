# CronoSlot 1.0.3

Proyecto Android para GitHub Actions.

Corrección de esta versión:
- Java y Kotlin compilan ambos con JVM 17, evitando el error de compatibilidad 1.8/17.
- `gradle.properties` habilita AndroidX.
- GitHub Actions instala Gradle 8.9 directamente, sin necesitar `gradlew`.

Workflow:
`.github/workflows/build-apk.yml`

Comando:
`gradle assembleDebug --no-daemon`

Después del build correcto, descarga el artefacto:
`CronoSlot-debug-apk`

La detección de cámara es una primera versión basada en movimiento y está pensada para probarla en la pista real.
