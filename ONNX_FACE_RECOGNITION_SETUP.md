# ONNX Runtime Face Recognition Setup Guide

## Why ONNX Runtime?
- State-of-the-art performance
- Cross-platform optimization
- Wide model support
- Industry standard

## Step 1: Add ONNX Runtime Dependencies

```xml
<dependency>
    <groupId>com.microsoft.onnxruntime</groupId>
    <artifactId>onnxruntime</artifactId>
    <version>1.16.3</version>
</dependency>
```

## Step 2: Download ONNX Models

```bash
# RetinaFace for detection
wget https://github.com/onnx/models/raw/main/vision/body_analysis/retinaface/model/retinaface.onnx

# ArcFace for recognition  
wget https://github.com/onnx/models/raw/main/vision/body_analysis/arcface/model/arcface.onnx
```

## Step 3: Implement ONNX Service

```java
@Service
public class ONNXFaceRecognitionService {
    
    private OrtEnvironment env;
    private OrtSession detectionSession;
    private OrtSession recognitionSession;
    
    @PostConstruct
    public void initialize() {
        try {
            env = OrtEnvironment.getEnvironment();
            
            // Load detection model
            detectionSession = env.createSession("models/retinaface.onnx");
            
            // Load recognition model
            recognitionSession = env.createSession("models/arcface.onnx");
            
        } catch (Exception e) {
            logger.error("Failed to load ONNX models", e);
        }
    }
}
```

## Performance Benefits

- 2-3x faster than fallback implementation
- GPU acceleration support
- Optimized for production workloads
- Memory efficient

## Model Sources

1. ONNX Model Zoo: https://github.com/onnx/models
2. Hugging Face: https://huggingface.co/models?library=onnx
3. Convert from PyTorch/TensorFlow
