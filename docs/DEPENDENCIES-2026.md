# VOJS dependencies — November 2026

### Stable dependencies adopted
- AGP **9.4.0** and Gradle **9.6.0** (Google official minimum for AGP 9.4). AGP includes Kotlin support: do not apply \`org.jetbrains.kotlin.android\` together with AGP9.
- Kotlin Compose compiler plugin **2.4.21**, aligned with the existing Kotlin build.
- Jetpack Compose BOM **2026.09.00** (stable BOM from Android Developers).
- AndroidX Core **1.19.1** (stable). Activity **1.13.0** is the stable channel; 1.14.0 is alpha and is intentionally not used for Play releases.
- Core SplashScreen **1.2.0**, the stable version.
- \`compileSdk=36.1\`, \`targetSdk=36\`, \`minSdk=24\` for existing device support and current Google Play phone rules.

Compose 2026.09.00 requires compile API 37 or newer via AndroidX AAR metadata; this does not change runtime targetSdk 36. The GitHub CI installs `platforms;android-37.0` to make resolution deterministic.

The source code contains **no direct Firebase, advertising analytics or paid SDK dependencies**. All version changes must pass Gradle release APK/AAB, Android Lint, 16 KiB native ELF/ZIP checks, 3 device profiles and exact original splash artwork QA. Moving to a new major build tool does **not** itself prove readiness for Google Play.

Sources:
- https://developer.android.com/build/releases/about-agp
- https://developer.android.com/build/migrate-to-built-in-kotlin
- https://developer.android.com/develop/ui/compose/setup-compose-dependencies-and-compiler
- https://developer.android.com/jetpack/androidx/releases/core
- https://developer.android.com/jetpack/androidx/releases/activity
