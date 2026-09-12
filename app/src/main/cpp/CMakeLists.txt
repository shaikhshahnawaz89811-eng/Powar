cmake_minimum_required(VERSION 3.22.1)
project(pawar_llama)

# --- llama.cpp is NOT vendored in this repo. -------------------------------
# This build expects the user to add llama.cpp as a subdirectory here, e.g.:
#   git submodule add https://github.com/ggml-org/llama.cpp app/src/main/cpp/llama.cpp
# Pin it to a known-good tag/commit rather than a moving branch, since this
# JNI file targets the llama.cpp public C API and will need small updates
# if that API changes upstream.
if(NOT EXISTS "${CMAKE_CURRENT_SOURCE_DIR}/llama.cpp/CMakeLists.txt")
    message(FATAL_ERROR
        "llama.cpp is missing at app/src/main/cpp/llama.cpp. "
        "Run: git submodule add https://github.com/ggml-org/llama.cpp app/src/main/cpp/llama.cpp"
    )
endif()

set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_SERVER OFF CACHE BOOL "" FORCE)
set(LLAMA_CURL OFF CACHE BOOL "" FORCE)
add_subdirectory(llama.cpp)

add_library(pawar_llama SHARED
    pawar_llama_jni.cpp
)

target_link_libraries(pawar_llama
    llama
    common
    android
    log
)

target_compile_options(pawar_llama PRIVATE -O2)
