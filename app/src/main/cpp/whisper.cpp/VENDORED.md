# Vendored whisper.cpp

Source: https://github.com/ggml-org/whisper.cpp release v1.9.6 (MIT licence, see LICENSE).

Only what CasaCheck needs is kept: `include/`, `src/whisper.cpp` (+ headers, `aneforge/`), and `ggml/` with the CPU backend
(`ggml/src/ggml-cpu`). GPU/NPU backends, examples, bindings, models and tests were removed. No source changes.
The JNI bridge lives in `../whisper_jni.c`, the build in `../CMakeLists.txt`.