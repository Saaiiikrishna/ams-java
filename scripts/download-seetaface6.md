# SeetaFace6 Download and Setup Guide

This guide will help you download and set up SeetaFace6 native libraries and models for the attendance management system.

## Required Downloads

### 1. SeetaFace6 Development Packages

**Windows (x64):**
- Download: [Baidu Pan](https://pan.baidu.com/s/1_rFID6k6Istmu8QJkHpbFw) code: `iqjk`
- Extract to: `src/main/resources/native/lib/windows/x64/`
- Required files: `SeetaFace.dll`, `SeetaNet.dll`, and other dependencies

**Linux Ubuntu 16.04 (x64):**
- Download: [Baidu Pan](https://pan.baidu.com/s/1tOq12SdpUtuybe48cMuwag) code: `lc44`
- Extract to: `src/main/resources/native/lib/linux/x64/`
- Required files: `libSeetaFace.so`, `libSeetaNet.so`, and other dependencies

### 2. Model Files (Required for all platforms)

**Part I Models:**
- Download: [Baidu Pan](https://pan.baidu.com/s/1LlXe2-YsUxQMe-MLzhQ2Aw) code: `ngne`
- Extract to: `src/main/resources/native/models/`
- Required files:
  - `face_detector.csta` - Face detection model
  - `face_landmarker_pts68.csta` - 68-point facial landmark model
  - `face_recognizer.csta` - Face recognition model
  - `fas_first.csta` - Anti-spoofing model (first stage)
  - `fas_second.csta` - Anti-spoofing model (second stage)
  - `quality_lbn.csta` - Image quality assessment model

**Part II Models (Optional - Lightweight):**
- Download: [Baidu Pan](https://pan.baidu.com/s/1xjciq-lkzEBOZsTfVYAT9g) code: `t6j0`
- Extract to: `src/main/resources/native/models/`
- Files:
  - `face_recognizer_light.csta` - Lightweight face recognition model

## Directory Structure After Setup

```
src/main/resources/native/
├── lib/
│   ├── windows/
│   │   └── x64/
│   │       ├── SeetaFace.dll
│   │       ├── SeetaNet.dll
│   │       └── [other Windows dependencies]
│   └── linux/
│       └── x64/
│           ├── libSeetaFace.so
│           ├── libSeetaNet.so
│           └── [other Linux dependencies]
└── models/
    ├── face_detector.csta
    ├── face_landmarker_pts68.csta
    ├── face_recognizer.csta
    ├── fas_first.csta
    ├── fas_second.csta
    ├── quality_lbn.csta
    └── face_recognizer_light.csta
```

## Step-by-Step Setup Instructions

### Step 1: Download Files
1. Use the Baidu Pan links above to download the required packages
2. Extract each package to a temporary directory

### Step 2: Copy Native Libraries
1. From Windows package: Copy all `.dll` files to `src/main/resources/native/lib/windows/x64/`
2. From Linux package: Copy all `.so` files to `src/main/resources/native/lib/linux/x64/`

### Step 3: Copy Model Files
1. From Part I models: Copy all `.csta` files to `src/main/resources/native/models/`
2. From Part II models: Copy `face_recognizer_light.csta` to `src/main/resources/native/models/`

### Step 4: Verify Setup
Run the application and check logs for:
```
INFO  - SeetaFace6 JNI library loaded successfully
INFO  - SeetaFace6 engine initialized successfully with handle: [handle_id]
```

## Alternative Download Methods

If Baidu Pan is not accessible, you can:

1. **Build from Source:**
   - Clone: https://github.com/SeetaFace6Open/index
   - Follow build instructions for your platform
   - Copy built libraries to the appropriate directories

2. **Use Docker:**
   - Use pre-built Docker images with SeetaFace6
   - Extract libraries from the container

3. **Contact Support:**
   - For commercial support: bd@seetatech.com
   - For community support: Join SeetaFace developer community

## Troubleshooting

### Common Issues:

1. **Library not found:**
   - Ensure files are in correct directories
   - Check file permissions (Linux/macOS)
   - Verify architecture matches (x64)

2. **Model loading failed:**
   - Verify all required `.csta` files are present
   - Check file integrity (not corrupted)
   - Ensure sufficient disk space

3. **Performance issues:**
   - Use `face_recognizer_light.csta` for faster processing
   - Adjust confidence thresholds in settings
   - Monitor memory usage

## Next Steps

After successful setup:
1. Restart the application
2. Test face recognition endpoints
3. Verify performance metrics
4. Configure production settings

The system will automatically detect SeetaFace6 availability and switch from fallback to native implementation.
