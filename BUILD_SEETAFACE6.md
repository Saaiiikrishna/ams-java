# Building SeetaFace6 from Source

## Prerequisites

### Windows:
- Visual Studio 2019 or later (with C++ tools)
- CMake 3.15+
- Git

### Linux:
- GCC 7+ or Clang 6+
- CMake 3.15+
- Git
- Build essentials: `sudo apt-get install build-essential cmake git`

### macOS:
- Xcode Command Line Tools
- CMake 3.15+
- Git

## Step-by-Step Build Process

### 1. Clone Repositories
```bash
# Run our automated script
python scripts/build-seetaface6-from-source.py

# Or manually clone:
git clone https://github.com/SeetaFace6Open/index.git
git clone https://github.com/SeetaFace6Open/SeetaFaceEngine.git
git clone https://github.com/SeetaFace6Open/FaceDetector.git
git clone https://github.com/SeetaFace6Open/FaceLandmarker.git
git clone https://github.com/SeetaFace6Open/FaceRecognizer.git
```

### 2. Build SeetaFaceEngine (Core)
```bash
cd SeetaFaceEngine
mkdir build && cd build
cmake .. -DCMAKE_BUILD_TYPE=Release
cmake --build . --config Release
```

### 3. Build Individual Components
```bash
# Face Detector
cd FaceDetector
mkdir build && cd build
cmake .. -DCMAKE_BUILD_TYPE=Release -DSeetaFace_DIR=../SeetaFaceEngine/build
cmake --build . --config Release

# Repeat for other components...
```

### 4. Copy Built Libraries
Copy the built libraries to:
- Windows: `src\main\resources\native/lib/windows/x64/`
- Linux: `src\main\resources\native/lib/linux/x64/`

Required files:
- SeetaFace.dll/.so
- SeetaNet.dll/.so
- Component-specific libraries

### 5. Download Models
Models are typically available in the main index repository or need to be trained.

## Alternative: Use Docker

```dockerfile
FROM ubuntu:20.04
RUN apt-get update && apt-get install -y build-essential cmake git
# Build SeetaFace6 in container
# Extract libraries
```

## Alternative: Pre-built Packages

Check these sources for pre-built packages:
1. GitHub Releases: https://github.com/SeetaFace6Open/index/releases
2. Package managers (vcpkg, conan)
3. Academic mirrors
4. Community builds

## Troubleshooting

### Common Issues:
1. **CMake errors**: Ensure CMake 3.15+
2. **Compiler errors**: Use compatible compiler versions
3. **Dependency issues**: Install required development packages
4. **Model files**: May need separate download or training

### Getting Help:
- SeetaFace6 Issues: https://github.com/SeetaFace6Open/index/issues
- Community Forums: Search for SeetaFace6 discussions
- Academic Papers: Original SeetaFace publications

## Current Status

Platform: windows
Build Directory: build_seetaface6
Target Directory: src\main\resources\native

The application will continue to work with fallback implementation until
real SeetaFace6 components are built and installed.
