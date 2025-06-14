#!/bin/bash

# Build script for SeetaFace6 JNI on Linux

echo "Building SeetaFace6 JNI for Linux..."

# Set paths
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$SCRIPT_DIR/.."
CPP_DIR="$PROJECT_ROOT/src/main/cpp"
BUILD_DIR="$CPP_DIR/build"

# Create build directory
mkdir -p "$BUILD_DIR"

# Change to build directory
cd "$BUILD_DIR"

# Check for required tools
if ! command -v cmake &> /dev/null; then
    echo "CMake not found. Please install CMake."
    echo "Ubuntu/Debian: sudo apt-get install cmake"
    echo "CentOS/RHEL: sudo yum install cmake"
    exit 1
fi

if ! command -v g++ &> /dev/null; then
    echo "g++ not found. Please install build tools."
    echo "Ubuntu/Debian: sudo apt-get install build-essential"
    echo "CentOS/RHEL: sudo yum groupinstall 'Development Tools'"
    exit 1
fi

# Check for Java
if [ -z "$JAVA_HOME" ]; then
    echo "JAVA_HOME not set. Please set JAVA_HOME environment variable."
    echo "Example: export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64"
    exit 1
fi

# Configure with CMake
echo "Configuring with CMake..."
cmake .. -DCMAKE_BUILD_TYPE=Release
if [ $? -ne 0 ]; then
    echo "CMake configuration failed."
    exit 1
fi

# Build the project
echo "Building JNI library..."
make -j$(nproc)
if [ $? -ne 0 ]; then
    echo "Build failed."
    exit 1
fi

echo "Build completed successfully!"
echo "JNI library should be in: $PROJECT_ROOT/src/main/resources/native/lib/linux/x64/"

# Make the script executable
chmod +x "$0"
