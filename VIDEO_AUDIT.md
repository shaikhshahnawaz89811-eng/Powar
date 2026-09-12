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

## Second module-load audit — 2026-09-12

The supplied 7.56s recording was inspected at 26.59 FPS (201 frames).
Observed state sequence:
- Settings opens with the imported module in Ready state.
- Load is tapped.
- The card transitions through Loading and reaches **Loaded**.
- The **Unload** action becomes enabled while Load/Delete are disabled.
- The app returns to the chat screen; the header remains green/loaded in the recorded frames.
- There is no evidence in this recording that an explicit unload occurred.

A source-level lifecycle bug was nevertheless found: `ModuleSettingsScreen` initialized/validated an existing model as `READY` without checking `manager.loaded`. This could make an already mapped module appear unloaded after the settings composition was recreated. `ModelManager` was also previously created with `remember`, so an Activity recreation could replace the manager instance and lose the in-process mapped state.

Fixes applied:
1. `ModelManager` is now process-scoped via `getInstance(applicationContext)` so Activity recreation does not create a second manager and silently drop the active mapping.
2. Settings initializes and revalidates using `manager.loaded`; a live mapping remains visibly `Loaded`.
3. Model state-changing operations (`import`, `load`, `unload`, `delete`) are serialized with a coroutine `Mutex` to prevent races.
4. Repeated `load()` while loaded is idempotent; repeated `unload()` is safe.

Model-state harness result: `MODEL_STATE_TEST_PASS` (load -> loaded -> repeated load -> unload -> repeated unload).

Important runtime distinction: process death/OS kill necessarily destroys an in-memory mapping. On a fresh process the module is correctly shown as `Ready`, not falsely `Loaded`; the user can Load it again. The current code still maps/validates the GGUF but does not contain a native inference engine.

## Third audit — CREATE-branch routing bug found and fixed, 2026-09-12

A 6.7s recording was reviewed (frame extraction; no network/whisper in this
sandbox, so audio was not transcribed). Three requests all produced the
identical generic blocked response even with a module showing **Loaded** in
Settings: `create 2 line python code`, `create web app for shipping Market`,
and `calculator bannao` followed by tapping the "Write code" clarification
option.

**Root cause (found by source trace, not by running the app — no
Gradle/NDK/device here either):** none of the three ever reached the
`OutputMode.CODE_ONLY` branch that the previous native-inference pass wired
to real generation. `TaskPlanner` only sets `CODE_ONLY` for an explicit
`sirf/only/just/bas ... code` phrase; a plain "create/banao" request instead
became `TaskIntent.CREATE` + `OutputMode.NORMAL_RESPONSE`, and that branch's
non-project `else` case in `AgentPipeline.kt` unconditionally reported "No
connected code-generation runtime is available" — it never checked
`effectiveRequest.modelLoaded` and never called `toolRegistry.generateCode(...)`.
Selecting "Write code" doesn't help either: it re-enters the classifier with
a `[CODE_REQUEST]` marker that sets `intent = CREATE` but still not
`outputMode = CODE_ONLY`, so it lands in the same dead-end. The generic
"...capability boundary..." verification/final-response text is a shared
`AgentStatus.BLOCKED` fallback, which is why every phrasing looked identical
and gave no clue the real problem was "this branch never tries" rather than
"the module isn't loaded."

**Fix applied:** the generation attempt that only lived inside the
`CODE_ONLY` check was pulled into a local `attemptLocalCodeGeneration()`
function in `AgentPipeline.kt`, now also called from the CREATE branch's
non-project `else` case whenever `effectiveRequest.modelLoaded` is true. A
loaded module is now actually tried before either branch reports a
boundary, and a genuine generation failure surfaces
`LlamaCodeGenerationCapability`'s real error instead of the generic one.

**What this does not establish:** checked by hand-tracing and a
brace/paren-balance script only — still no kotlinc/NDK/Gradle/device
available to compile or run this. Whether these phrasings now return real
text depends entirely on whether `pawar_llama` actually built for the
running APK, which nothing in this pass could verify either way. If it did,
these three phrasings should now generate for real; if it didn't, they'll
now show the specific "Native inference library is not built yet..."
reason instead of the misleading generic one. `TaskPlanner`'s phrase list
was deliberately left unchanged — narrowing which exact wordings count as
"just code" is a separate design question from the bug actually shown here,
which was that the working generation path was simply unreachable from a
whole branch.
