# CronoSlot — ZIP listo para GitHub

Esta versión corrige el problema de `gradlew`: el workflow NO necesita `gradlew`.
GitHub Actions instala Gradle 8.9 con `gradle/actions/setup-gradle` y ejecuta `gradle assembleDebug`.

Sube TODO el contenido a la rama `cronoslot`.
Después abre Actions y ejecuta **Build CronoSlot APK**.
El APK aparecerá como artefacto **CronoSlot-debug-apk**.
