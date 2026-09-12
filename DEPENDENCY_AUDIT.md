# Pawar Dependency & Build Audit

Audit date: 2026-09-12

## Android build stack

| Component | Version in project | Audit result |
|---|---:|---|
| Android Gradle Plugin | 9.4.0 | Matches current stable AGP; requires Gradle 9.6.0 and JDK 17. |
| compileSdk / targetSdk | 37 | Supported by AGP 9.4. |
| minSdk | 24 | Valid Android application minimum. |
| Kotlin Compose compiler plugin | 2.3.21 | Matches the Kotlin 2.3.21 Compose compiler plugin setup. |
| Compose BOM | 2026.08.00 | Current stable BOM used by the official Compose setup at audit time. |
| Compose Material3 | BOM-managed | Resolved through Compose BOM. |
| Compose UI/Foundation | BOM-managed | Resolved through Compose BOM. |
| Activity Compose | 1.13.0 | Current stable release at audit time. |
| Core KTX | 1.19.0 | Current stable release at audit time. |
| JUnit | 4.13.2 | Existing local unit-test dependency. |

## Removed unused dependency

`androidx.compose.ui:ui-text-google-fonts` was removed because no Google Fonts API is referenced by the source tree.

## Build-system notes

AGP 9.0+ provides built-in Kotlin support. Pawar therefore does not apply the legacy `org.jetbrains.kotlin.android` plugin. The Compose compiler plugin remains explicitly applied because Compose requires it.

The repository keeps the existing GitHub Actions workflow that installs Gradle 9.6.0 and runs the existing CI build/test job. This audit did not execute that workflow, Gradle, an APK build, an emulator, or a physical device.

## Official references checked

- Android Gradle Plugin 9.4.0 compatibility: https://developer.android.com/build/releases/agp-9-4-0-release-notes
- AGP compatibility and Gradle/JDK requirements: https://developer.android.com/build/releases/about-agp
- Built-in Kotlin in AGP 9: https://developer.android.com/build/releases/agp-9-0-0-release-notes
- Compose BOM setup: https://developer.android.com/develop/ui/compose/bom
- Compose compiler plugin setup: https://developer.android.com/develop/ui/compose/setup-compose-dependencies-and-compiler
- Activity 1.13.0 release: https://developer.android.com/jetpack/androidx/releases/activity
- Core 1.19.0 release: https://developer.android.com/jetpack/androidx/releases/core
