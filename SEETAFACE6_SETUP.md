# SeetaFace6 Manual Setup Guide

## Required Downloads

### 1. Platform-Specific Libraries

**For Windows (x64):**
- Download: [Baidu Pan](https://pan.baidu.com/s/1_rFID6k6Istmu8QJkHpbFw) code: `iqjk`
- Extract and copy these files to: `src\main\resources\native\lib\windows\x64/`
  - SeetaFace.dll
  - SeetaNet.dll
  - opencv_world*.dll (if included)
  - Any other .dll dependencies

**For Linux (x64):**
- Download: [Baidu Pan](https://pan.baidu.com/s/1tOq12SdpUtuybe48cMuwag) code: `lc44`
- Extract and copy these files to: `src\main\resources\native\lib\windows\x64/`
  - libSeetaFace.so
  - libSeetaNet.so
  - Any other .so dependencies

### 2. Model Files

**Core Models (Required):**
- Download: [Baidu Pan](https://pan.baidu.com/s/1LlXe2-YsUxQMe-MLzhQ2Aw) code: `ngne`
- Extract and copy these files to: `src\main\resources\native\models/`
  - face_detector.csta
  - face_landmarker_pts68.csta
  - face_recognizer.csta
  - fas_first.csta
  - fas_second.csta
  - quality_lbn.csta

**Additional Models (Optional):**
- Download: [Baidu Pan](https://pan.baidu.com/s/1xjciq-lkzEBOZsTfVYAT9g) code: `t6j0`
- Extract and copy to: `src\main\resources\native\models/`
  - face_recognizer_light.csta

## Verification

After copying files, run:
```bash
python scripts/verify-seetaface6-setup.py
```

## Building JNI Bridge

After setting up libraries and models:
```bash
# Windows
scripts/build-jni-windows.bat

# Linux
scripts/build-jni-linux.sh
```

## Directory Structure

Your final structure should look like:
```
src\main\resources\native/
├── lib/
│   └── windows/
│       └── x64/
│           ├── SeetaFace.dll (Windows) or libSeetaFace.so (Linux)
│           ├── SeetaNet.dll (Windows) or libSeetaNet.so (Linux)
│           └── [other dependencies]
└── models/
    ├── face_detector.csta
    ├── face_landmarker_pts68.csta
    ├── face_recognizer.csta
    ├── fas_first.csta
    ├── fas_second.csta
    └── quality_lbn.csta
```

## Troubleshooting

1. **Library not found errors:**
   - Ensure all .dll/.so files are in the correct directory
   - Check file permissions (Linux/macOS)
   - Verify architecture matches (x64)

2. **Model loading errors:**
   - Verify all .csta files are present and not corrupted
   - Check file sizes match expected values
   - Ensure sufficient disk space

3. **JNI compilation errors:**
   - Verify JAVA_HOME is set correctly
   - Install required build tools (Visual Studio/GCC)
   - Check CMake is installed and accessible

## Support

For issues with SeetaFace6:
- Official repository: https://github.com/SeetaFace6Open/index
- Commercial support: bd@seetatech.com
