cmake_minimum_required(VERSION 3.10)
project(seetaface_jni)

# Set C++ standard
set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Find required packages
find_package(JNI REQUIRED)

# Platform-specific settings
if(WIN32)
    set(SEETAFACE_LIB_DIR "${CMAKE_SOURCE_DIR}/../resources/native/lib/windows/x64")
    set(LIBRARY_SUFFIX ".dll")
elseif(UNIX AND NOT APPLE)
    set(SEETAFACE_LIB_DIR "${CMAKE_SOURCE_DIR}/../resources/native/lib/linux/x64")
    set(LIBRARY_SUFFIX ".so")
elseif(APPLE)
    set(SEETAFACE_LIB_DIR "${CMAKE_SOURCE_DIR}/../resources/native/lib/macos")
    set(LIBRARY_SUFFIX ".dylib")
endif()

# SeetaFace6 include directories
set(SEETAFACE_INCLUDE_DIR "${CMAKE_SOURCE_DIR}/../resources/native/include")

# Include directories
include_directories(${JNI_INCLUDE_DIRS})
include_directories(${SEETAFACE_INCLUDE_DIR})

# Source files
set(SOURCES
    seetaface_jni.cpp
)

# Create shared library
add_library(seetaface_jni SHARED ${SOURCES})

# Link libraries
if(WIN32)
    target_link_libraries(seetaface_jni
        ${SEETAFACE_LIB_DIR}/SeetaFace${LIBRARY_SUFFIX}
        ${SEETAFACE_LIB_DIR}/SeetaNet${LIBRARY_SUFFIX}
    )
elseif(UNIX)
    target_link_libraries(seetaface_jni
        ${SEETAFACE_LIB_DIR}/libSeetaFace${LIBRARY_SUFFIX}
        ${SEETAFACE_LIB_DIR}/libSeetaNet${LIBRARY_SUFFIX}
    )
endif()

# Set output directory
set_target_properties(seetaface_jni PROPERTIES
    LIBRARY_OUTPUT_DIRECTORY ${SEETAFACE_LIB_DIR}
    RUNTIME_OUTPUT_DIRECTORY ${SEETAFACE_LIB_DIR}
)

# Platform-specific compiler flags
if(WIN32)
    target_compile_definitions(seetaface_jni PRIVATE WIN32_LEAN_AND_MEAN)
    set_target_properties(seetaface_jni PROPERTIES
        PREFIX ""
        SUFFIX ".dll"
    )
elseif(UNIX)
    set_target_properties(seetaface_jni PROPERTIES
        PREFIX "lib"
        SUFFIX ".so"
    )
endif()

# Copy to resources directory after build
add_custom_command(TARGET seetaface_jni POST_BUILD
    COMMAND ${CMAKE_COMMAND} -E copy
    $<TARGET_FILE:seetaface_jni>
    ${CMAKE_SOURCE_DIR}/../resources/native/lib/$<IF:$<PLATFORM_ID:Windows>,windows,$<IF:$<PLATFORM_ID:Linux>,linux,macos>>/x64/
    COMMENT "Copying JNI library to resources directory"
)
