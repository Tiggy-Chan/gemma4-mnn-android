cmake_minimum_required(VERSION 3.22.1)
project("mnn_llm_jni")

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# MNN paths — relative to the new location outside the MNN submodule
set(MNN_SOURCE_ROOT "${CMAKE_CURRENT_SOURCE_DIR}/../../../MNN")
set(MNN_INSTALL_ROOT "${MNN_SOURCE_ROOT}/project/android/build_64")
set(LIB_PATH "${MNN_INSTALL_ROOT}/lib")

message(STATUS "MNN_SOURCE_ROOT: ${MNN_SOURCE_ROOT}")
message(STATUS "MNN_INSTALL_ROOT: ${MNN_INSTALL_ROOT}")

# Include directories
include_directories("${MNN_SOURCE_ROOT}/include/")
include_directories("${MNN_SOURCE_ROOT}/transformers/llm/engine/include")
include_directories("${MNN_SOURCE_ROOT}/transformers/diffusion/engine/include/")
include_directories("${MNN_SOURCE_ROOT}/3rd_party/")

# Native library
add_library(mnn_llm_jni SHARED mnn_jni.cpp)

# Import MNN shared libraries
add_library(MNN SHARED IMPORTED)
set_target_properties(MNN PROPERTIES IMPORTED_LOCATION "${LIB_PATH}/libMNN.so")

add_library(MNN_llm SHARED IMPORTED)
set_target_properties(MNN_llm PROPERTIES IMPORTED_LOCATION "${LIB_PATH}/libllm.so")

add_library(MNN_Express SHARED IMPORTED)
set_target_properties(MNN_Express PROPERTIES IMPORTED_LOCATION "${LIB_PATH}/libMNN_Express.so")

# Find required Android libraries
find_library(log-lib log)
find_library(android-lib android)

# Add 16KB page size support (required for Android 15+)
target_link_options(mnn_llm_jni PRIVATE "-Wl,-z,max-page-size=16384")

target_link_libraries(
    mnn_llm_jni
    MNN
    MNN_llm
    MNN_Express
    ${log-lib}
    ${android-lib}
)
