# SeetaFace6 Native Library Setup

This directory should contain the SeetaFace6 native libraries and model files for face recognition functionality.

## Directory Structure

```
src/main/resources/
├── native/
│   ├── lib/
│   │   ├── windows/
│   │   │   ├── x64/
│   │   │   │   └── seetaface2_jni.dll
│   │   │   └── x86/
│   │   │       └── seetaface2_jni.dll
│   │   ├── linux/
│   │   │   ├── x64/
│   │   │   │   └── libseetaface2_jni.so
│   │   │   └── x86/
│   │   │       └── libseetaface2_jni.so
│   │   └── macos/
│   │       └── libseetaface2_jni.dylib
│   └── models/
│       ├── face_detector.csta
│       ├── face_landmarker_pts68.csta
│       ├── face_recognizer.csta
│       └── fas_first.csta
└── README.md (this file)
```

## Required Files

### Native Libraries
- **Windows x64**: `seetaface2_jni.dll` (compiled for Windows 64-bit)
- **Windows x86**: `seetaface2_jni.dll` (compiled for Windows 32-bit)
- **Linux x64**: `libseetaface2_jni.so` (compiled for Linux 64-bit)
- **Linux x86**: `libseetaface2_jni.so` (compiled for Linux 32-bit)
- **macOS**: `libseetaface2_jni.dylib` (compiled for macOS)

### Model Files
- **face_detector.csta**: Face detection model
- **face_landmarker_pts68.csta**: 68-point facial landmark detection model
- **face_recognizer.csta**: Face recognition/encoding model
- **fas_first.csta**: Anti-spoofing model (optional but recommended)

## Installation Instructions

### 1. Download SeetaFace2
```bash
git clone https://github.com/seetaface/SeetaFace2.git
cd SeetaFace2
```

### 2. Build Native Libraries
Follow the SeetaFace2 documentation to build the native libraries for your target platforms.

### 3. Create JNI Wrapper
Create a JNI wrapper that implements the methods declared in `SeetaFaceJNI.java`:
- `initializeEngine`
- `releaseEngine`
- `detectFaces`
- `extractFaceEncoding`
- `compareFaceEncodings`
- `detectLiveness`
- `assessImageQuality`
- `getLastError`

### 4. Download Model Files
Download the pre-trained model files from SeetaFace2 releases or train your own models.

### 5. Place Files
Copy the compiled libraries and model files to the appropriate directories as shown in the structure above.

## Fallback Implementation

If SeetaFace2 native libraries are not available, the system will automatically fall back to a basic implementation using OpenCV and simple algorithms. This ensures the application continues to function even without the native libraries.

## Configuration

Update `application.properties` to point to the correct model file paths:

```properties
face.recognition.models.detector=models/face_detector.csta
face.recognition.models.landmark=models/face_landmarker_pts68.csta
face.recognition.models.recognizer=models/face_recognizer.csta
face.recognition.models.antispoofing=models/fas_first.csta
face.recognition.fallback.enabled=true
```

## Testing

The system will automatically detect if the native libraries are loaded successfully. Check the application logs for:

```
INFO  - SeetaFace2 JNI library loaded successfully
INFO  - SeetaFace2 engine initialized successfully with handle: [handle_id]
```

If you see warnings about fallback implementation, it means the native libraries are not available.

## Production Deployment

For production deployment:

1. Ensure all required native libraries are included in the deployment package
2. Set appropriate file permissions for the native libraries
3. Configure the model file paths correctly for your deployment environment
4. Test face recognition functionality thoroughly before going live

## Troubleshooting

### Common Issues

1. **Library not found**: Ensure the native library is in the correct directory and has proper permissions
2. **Model files missing**: Verify all model files are present and accessible
3. **Architecture mismatch**: Ensure the native library matches your system architecture (x64/x86)
4. **Memory issues**: SeetaFace2 requires sufficient memory for model loading

### Debug Mode

Enable debug logging to troubleshoot issues:

```properties
logging.level.com.example.attendancesystem.facerecognition=DEBUG
```

This will provide detailed information about library loading and face recognition operations.
