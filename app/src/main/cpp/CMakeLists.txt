cmake_minimum_required(VERSION 3.22.1)
project(pawar_llama)

# --- llama.cpp -------------------------------------------------------------
# Two ways this gets satisfied, checked in this order:
#   1. A locally vendored copy at app/src/main/cpp/llama.cpp (e.g. added via
#      `git submodule add https://github.com/ggml-org/llama.cpp app/src/main/cpp/llama.cpp`
#      and pinned with `git checkout <tag>`). If present, it always wins --
#      useful for reproducible local builds or an offline dev machine.
#   2. If it's not there, CMake fetches it itself at configure time via
#      FetchContent, pinned to PAWAR_LLAMA_CPP_TAG below. This is what makes
#      a plain CI checkout (no submodule step) build successfully: the CI
#      runner has network access even when a given dev machine doesn't.
#
# Either way the version is pinned, not a moving branch -- if llama.cpp's
# public C API has changed since PAWAR_LLAMA_CPP_TAG, the compiler errors in
# pawar_llama_jni.cpp will point at exactly which function names moved.

set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_SERVER OFF CACHE BOOL "" FORCE)
set(LLAMA_CURL OFF CACHE BOOL "" FORCE)
# llama.cpp decoupled the `common` utility library from LLAMA_BUILD_EXAMPLES
# a while back -- it now has its own gate. pawar_llama_jni.cpp links against
# it, so this must be ON even though we don't build the examples. Note: the
# CMake *target* for this library has also been renamed over time (it was
# `common`, now `llama-common` as of v0.4.0) -- if a future bump of
# PAWAR_LLAMA_CPP_TAG fails at the link step with "unable to find library",
# check llama.cpp's common/CMakeLists.txt for the current target name.
set(LLAMA_BUILD_COMMON ON CACHE BOOL "" FORCE)

set(PAWAR_LLAMA_CPP_TAG "v0.4.0" CACHE STRING
    "llama.cpp tag/commit to fetch when no local copy is vendored")

if(EXISTS "${CMAKE_CURRENT_SOURCE_DIR}/llama.cpp/CMakeLists.txt")
    message(STATUS "pawar_llama: using vendored llama.cpp at ${CMAKE_CURRENT_SOURCE_DIR}/llama.cpp")
    add_subdirectory(llama.cpp)
else()
    message(STATUS "pawar_llama: no local llama.cpp found -- fetching ${PAWAR_LLAMA_CPP_TAG} from GitHub")
    include(FetchContent)
    FetchContent_Declare(
        llama_cpp
        GIT_REPOSITORY https://github.com/ggml-org/llama.cpp
        GIT_TAG        ${PAWAR_LLAMA_CPP_TAG}
        GIT_SHALLOW    TRUE
    )
    FetchContent_MakeAvailable(llama_cpp)
endif()

add_library(pawar_llama SHARED
    pawar_llama_jni.cpp
)

target_link_libraries(pawar_llama
    llama
    llama-common
    android
    log
)

target_compile_options(pawar_llama PRIVATE -O2)
