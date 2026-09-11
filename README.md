# Pawar

Pawar is a local-first Android UI project. The supplied reference remains the visual source of truth; new UI is added only when explicitly requested.

## Current scope

### Chat UI
- Reference Pawar chat UI.
- `+` attachment flow for Camera, Photos and ZIP.
- Keyboard-safe composer and bottom-anchored chat scrolling.

### Settings / module manager
- The top-left profile `P` has been replaced with a three-line menu button.
- Tapping the menu opens the light Settings screen.
- A visible back button returns to the chat; Android system back also closes Settings.
- The module card is for the requested local model:
  - `qwen2.5-coder-7b-q4_k_m.gguf`
  - `1.04 GB · Q4_K_M · GGUF`
- Real file operations currently implemented:
  - Import: copies a selected GGUF into private app storage after validating the GGUF magic header.
  - Load: opens the stored GGUF read-only and memory-maps it, keeping the mapping open while loaded.
  - Unload: releases the file mapping/handles.
  - Delete: deletes only when the module is not loaded and no operation is running.
- While an import/load/unload/delete operation is running, module actions are locked.
- Import is disabled while the module is loaded.
- Delete is disabled while the module is loaded.
- A SHA-256 fingerprint is calculated after import/load for integrity visibility.

## Important runtime boundary

This iteration deliberately does **not** claim to run Qwen inference. Loading here means safely validating and loading/mapping the GGUF file in the app. A llama.cpp/native GGUF inference runtime is a separate step and will be added only when requested.

## Security notes

The app does not execute the imported GGUF as native code. It validates the GGUF header before storing/reading it and keeps the model in private app storage. The SHA-256 value helps detect corruption or unexpected changes; it is not an antivirus scan. Android's security guidance recommends validating data read from external storage and checking integrity before use.

## Build

Open the project in an Android Studio version supporting:
- Android Gradle Plugin 9.4.0
- Gradle 9.6.0
- JDK 17
- compileSdk 37

The GitHub Actions workflow builds **debug only** and uploads the debug APK as an artifact. It does not create a GitHub Release, release tag, or publish an APK/App Bundle.
