# Pawar UI design notes

The supplied reference image remains the visual source of truth. Existing UI is not intentionally redesigned.

## This iteration

Only the requested top-menu/settings/module-manager behavior was added.

### Header
- Removed the circular `P` profile control.
- Added a clean three-line menu icon in the same header position.
- Tapping it opens Settings.
- Settings has a clear back arrow.
- Android system back also returns to chat.

### Settings
- Light/white surface consistent with the existing visual language.
- One focused section: Local module.
- No unrelated settings or decorative controls.
- Module card clearly shows name, file name, size/quantization, status and actions.

### Module lifecycle
- `Not imported` → Import is available.
- `Ready` → Load and Delete are available.
- `Loading` → all module actions are locked.
- `Loaded` → Unload is available; Import and Delete are locked.
- After Unload → Ready, then Delete becomes available.
- A failed operation leaves the previous safe state and displays the error.

### Actual file behavior
- Import uses Android's document picker and copies the selected GGUF into private app storage.
- The file is checked for the `GGUF` magic header before it is accepted.
- Load uses a read-only memory mapping and keeps the file handles open until Unload.
- Unload releases those resources.
- Delete is refused while loaded.
- SHA-256 is calculated for integrity visibility.

## Runtime boundary

This step does not pretend that a memory-mapped GGUF is an inference engine. Actual Qwen inference through llama.cpp/native code is intentionally deferred until requested.
