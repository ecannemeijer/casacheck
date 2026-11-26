# On-device speech recognition for CasaCheck: whisper.cpp (MIT, vendored in ./whisper.cpp, v1.9.6) with the ggml CPU backend only.
cmake_minimum_required(VERSION 3.22.1)
project(casawhisper C CXX)

set(CMAKE_CXX_STANDARD 17)
set(WHISPER_LIB_DIR ${CMAKE_CURRENT_SOURCE_DIR}/whisper.cpp)

# ggml: static, CPU only, no OpenMP (ggml's own thread pool), portable instruction set.
set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
set(GGML_NATIVE OFF CACHE BOOL "" FORCE)
set(GGML_OPENMP OFF CACHE BOOL "" FORCE)
set(GGML_BACKEND_DL OFF CACHE BOOL "" FORCE)
set(GGML_LLAMAFILE OFF CACHE BOOL "" FORCE)
set(GGML_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(GGML_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
if (ANDROID_ABI STREQUAL "x86_64")
    # emulator / rare x86 devices: SSE4.2 baseline only, no AVX assumptions
    set(GGML_AVX OFF CACHE BOOL "" FORCE)
    set(GGML_AVX2 OFF CACHE BOOL "" FORCE)
    set(GGML_FMA OFF CACHE BOOL "" FORCE)
    set(GGML_F16C OFF CACHE BOOL "" FORCE)
    set(GGML_BMI2 OFF CACHE BOOL "" FORCE)
    set(GGML_AVX512 OFF CACHE BOOL "" FORCE)
endif ()
add_subdirectory(${WHISPER_LIB_DIR}/ggml ggml)

add_library(casawhisper SHARED
    ${WHISPER_LIB_DIR}/src/whisper.cpp
    ${WHISPER_LIB_DIR}/src/aneforge/whisper-aneforge.cpp
    whisper_jni.c)

target_include_directories(casawhisper PRIVATE
    ${WHISPER_LIB_DIR}/include ${WHISPER_LIB_DIR}/src ${WHISPER_LIB_DIR}/ggml/include)
target_compile_definitions(casawhisper PRIVATE GGML_USE_CPU WHISPER_VERSION="1.9.6")
target_compile_options(casawhisper PRIVATE -O3 -fvisibility=hidden -fvisibility-inlines-hidden -ffunction-sections -fdata-sections)
target_link_options(casawhisper PRIVATE -Wl,--gc-sections -Wl,--exclude-libs,ALL "-Wl,-z,max-page-size=16384")

find_library(LOG_LIB log)
target_link_libraries(casawhisper ggml ${LOG_LIB} android)