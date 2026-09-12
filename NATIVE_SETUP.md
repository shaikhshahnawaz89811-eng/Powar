# Wiring real local code generation — what's done and what's left

## What changed in this pass

- `ModelManager` → llama.cpp is now actually *called* when the module is
  loaded, instead of `modelLoaded` being a dead boolean.
- New files: `app/src/main/cpp/CMakeLists.txt`, `app/src/main/cpp/pawar_llama_jni.cpp`,
  `LlamaBridge.kt`, `LlamaCodeGenerationCapability.kt`.
- `AgentPipeline.kt` CODE_ONLY branch now attempts real generation when
  `modelLoaded == true`, and only falls back to the old "no runtime
  connected" message when it's actually not loaded or the native lib isn't
  built. It never returns fabricated code on failure — a genuine error
  message is shown instead.
- `ModuleSettingsScreen.kt` no longer shows a hardcoded "1.04 GB" — it now
  shows the real imported file's size.

## What I could NOT do here (and why)

This sandbox has no network access and no Android SDK/NDK/CMake installed.
Concretely, that means I could not:
1. Fetch llama.cpp source (`git clone` is blocked — no network egress).
2. Compile the new C++ against the actual llama.cpp headers.
3. Run a Gradle/NDK build to confirm the JNI signatures link.
4. Run this on a device/emulator to confirm real generation works end to end.

The Kotlin side was checked with the project's own delimiter-balance
validator and by manual review, but **none of this native code has been
compiled**. Treat it as a real, honest first draft — not a verified build —
the same way this project's own audits should have, and previously didn't
for the model-load harness.

## Steps to actually finish this yourself

1. **Vendor llama.cpp** (pin a specific tag, don't track a moving branch):
   ```
   git submodule add https://github.com/ggml-org/llama.cpp app/src/main/cpp/llama.cpp
   cd app/src/main/cpp/llama.cpp && git checkout <a tag you've tested> && cd -
   ```
2. **Install the NDK + CMake** via Android Studio → SDK Manager → SDK Tools.
3. **Build**: `gradle :app:assembleDebug`. If llama.cpp's public API has
   moved since this was written, the compiler errors will point at exactly
   which function names in `pawar_llama_jni.cpp` need updating — this is
   normal for a fast-moving upstream project and is expected to need at
   least minor fixes.
4. **Use a model that actually fits on a phone.** `ModelManager` currently
   hardcodes `EXPECTED_FILE_NAME = "qwen2.5-coder-7b-q4_k_m.gguf"` and the UI
   labels everything "Qwen2.5-Coder" regardless of what you actually import.
   A real Qwen2.5-Coder-7B Q4_K_M is roughly **4.5–4.8 GB of weights**, plus
   roughly another 1–1.5 GB for the KV cache at a 4096-token context — 6+ GB
   for one app is unrealistic on most phones and will be very slow (CPU-only,
   likely well under 1 token/sec on mid-range hardware) or get OOM-killed.
   For something that actually runs acceptably, use a **1.5B or 3B**
   Qwen2.5-Coder-Instruct GGUF instead, and update `EXPECTED_FILE_NAME`
   and the UI label to match reality rather than always saying "Qwen2.5-Coder".
5. **Test the lifecycle for real**: import → load → ask a CODE_ONLY prompt →
   confirm real generated text appears → unload → confirm the next
   CODE_ONLY prompt correctly falls back to the "not loaded" message instead
   of reusing a stale native session.

None of the MD files in this repo should claim step 5 passed until it has
actually been run on a device — that's the exact gap this whole exercise
started from.
