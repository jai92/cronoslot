# CronoSlot 4.0.1

Build fix for GitHub Actions.

The application source is Java-only. The previous build declared the Kotlin Android
plugin in `app/build.gradle.kts`, but GitHub's Gradle setup did not have a version
for that plugin in the module. This version removes the unnecessary Kotlin plugin
from both root and app Gradle configuration.

Build command:
`gradle assembleDebug --no-daemon`
