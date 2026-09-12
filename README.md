# Pawar

Pawar is a local-first Android chat UI with a generic, result-driven agent orchestration layer. The orchestrator is not tied to one fixed workflow and can choose different actions from the current task state.

## What the current build can actually do

### 1. Understand requests
Pawar separates:
- task intent: create, modify, fix, inspect, explain, research, clarify, unknown
- output mode: normal response, code-only, options-only, project mutation, ZIP output

A request such as `Kotlin me code likho` is treated as a code-generation request, not as permission to create a project. Explicit `sirf code`/`code only` selects CODE_ONLY and disables project mutation.

### 2. Ask when the outcome is missing
ZIP/image/file-only requests can pause with contextual options instead of guessing. Options are clickable in the UI. Selecting an option resumes the same task context rather than starting an unrelated new task.

### 3. Inspect real local inputs
- Images: safely opened for real byte/dimension inspection.
- ZIPs: safely copied and inspected with entry/size/path limits.
- ZIP projects: can be opened into a sandboxed `ProjectWorkspace` for file listing, reading, searching, writing, deletion and packaging.

Archive contents are never executed by the workspace.

### 4. Create starter projects locally
The current local generator can create deterministic starter scaffolds for **8 project types**:

1. Android / Jetpack Compose
2. Web HTML/CSS/JS
3. Python
4. C/C++ native
5. Kotlin/JVM
6. Rust
7. Go
8. Generic

These are real files, not just a list of filenames. They can also be packaged into a real ZIP when ZIP output is requested.

The scaffold is intentionally a starter. Arbitrary application logic still requires a connected reasoning/code-generation runtime.

### 5. Handle existing project changes safely
For `fix`, `modify`, or similar ZIP tasks, Pawar can open and inspect the real project workspace. The workspace is genuinely writable, but the current build does not pretend that a model-generated patch exists. Without a connected reasoning/code-generation capability, Pawar stops at the edit boundary and reports the blocker.

This boundary is deliberate: no fake “fixed” result is emitted.

### 6. Dynamic action stream
The UI receives actual orchestration events. A typical run may look like:

`Understanding request`
→ `Inspecting context`
→ `Inspecting provided inputs`
→ `Planning next action`
→ `Reading project structure`
→ `Re-evaluating plan`
→ `Verifying output`

A different request may skip most of those actions. There is no mandatory fixed sequence.

For real tool operations, the activity row remains `Working…` while the operation is executing and changes to `Done`, `Failed`, or `Blocked` from the actual result.

### 7. ZIP artifact sharing
Generated ZIP artifacts are stored in the app cache under a dedicated artifact directory and exposed through the app's `FileProvider`. The activity card provides a Share action so the generated ZIP can be sent to another app.

## Capability boundaries

The codebase exposes replaceable interfaces for:
- model-driven reasoning/action selection
- code generation
- semantic image analysis
- live/current-information research
- project workspaces
- project generation

The orchestrator only reports a capability as successful when an actual implementation returned a result.

## Local model status

`ModelManager` safely imports, validates, hashes, memory-maps and unloads a GGUF file. A memory-mapped GGUF is **not** treated as inference. Native Qwen/llama.cpp inference must be connected through a real code-generation/reasoning capability before Pawar can autonomously generate and patch arbitrary code.

## No automatic build/device execution

The agent pipeline does not automatically build an APK, install an APK, launch an emulator, or run device tests. Static/state verification is separate from build/device execution.

The existing GitHub Actions workflow from the project is preserved; it is not part of the runtime agent loop.

## Build configuration audited

- Android Gradle Plugin: 9.4.0
- Required Gradle: 9.6.0
- JDK: 17
- compileSdk / targetSdk: 37
- Kotlin Compose compiler plugin: 2.3.21
- Compose BOM: 2026.08.00
- Activity Compose: 1.13.0
- Core KTX: 1.19.0
- JUnit: 4.13.2

See `DEPENDENCY_AUDIT.md` and `AUDIT_REPORT.md` for the full static audit.
