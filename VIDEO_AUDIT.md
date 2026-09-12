# Pawar video + source re-audit — 2026-09-12

## Recording reviewed

- Screen recording: 8.62 seconds, 432 frames at ~50.1 FPS.
- The recording was inspected frame-by-frame, including the two visible test flows.

## Bugs confirmed from the recording

1. `kuch nahin bas python code likho` could fall into clarification/UNKNOWN in the recorded build.
2. `create calculator kotlin code` was routed through a project-oriented branch instead of being treated as a code-generation request boundary.
3. `error find karo` was ambiguous between inspect and fix; it must be read-only when the user says find/check rather than fix.
4. ZIP + error finding needed to inspect the actual workspace before deciding whether an edit is possible.
5. Clarification option `1` could be sent as a fresh request when its original clarification run was no longer the newest conversation turn. The UI callback did not identify its originating run.
6. Completed activity was too expanded/noisy compared with a compact agent activity summary.
7. ZIP workspace input needed a compressed-size limit in addition to extraction limits.
8. Diagnostic findings could incorrectly make the diagnostic tool appear failed; finding a source error is evidence, not a failed inspection operation.
9. Verification could report failure merely because a task was blocked by an unavailable capability; safety/state verification and task completion are separate concepts.

## Fixes now present in the audited source

- Broader language-specific code-only detection (`sirf/only/just/bas <language> code`).
- Numeric and title-based clarification continuation.
- Option clicks are bound directly to the `PipelineRun` they came from.
- Fallback continuation searches the newest waiting run if no explicit run is supplied.
- Static diagnostics are read-only and their operation remains successful even when findings contain errors.
- Verification can pass the checks while the task status remains `BLOCKED` at a genuine capability boundary.
- ZIP archive normalization now catches aliases such as `./src/a.txt` and `src//a.txt` as the same path before extraction.
- Android starter `settings.gradle.kts` includes the required `RepositoriesMode` import.
- QA now checks the Android template import and clarification wiring.

## Capability boundary intentionally retained

The recording shows a Qwen model label, but the current `ModelManager` validates/maps GGUF files and does not implement model inference. Therefore code generation and arbitrary AI patching are not falsely claimed. A real reasoning/code-generation runtime must be connected for those operations.

## Tests performed after the second audit

- 23 Kotlin files: delimiter balance PASS.
- Planner compiled with `kotlinc`: PASS.
- Video prompt harness: PASS.
- `sirf python code likho` => CREATE / PYTHON / CODE_ONLY / no write: PASS.
- `create calculator kotlin code` => CREATE / JVM / NORMAL_RESPONSE / no project mutation: PASS.
- `error find karo` => INSPECT / no write: PASS.
- Pipeline continuation harness with numeric and title selection: PASS.
- Static diagnostics fixture with unclosed delimiter + merge conflict: PASS.
- ZIP normalized-duplicate safety fixture: PASS.
- Existing 8 project blueprint coverage: PASS.
- No Gradle/APK/device execution was performed.
