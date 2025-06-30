package com.example.attendancesystem.facerecognition;

import java.util.List;

/**
 * Result of face detection operation
 */
public class FaceDetectionResult {
    
    private boolean success;
    private String errorMessage;
    private List<FaceRectangle> faces;
    private int processingTimeMs;
    private float imageQualityScore;
    
    public FaceDetectionResult() {}
    
    public FaceDetectionResult(boolean success, String errorMessage) {
        this.success = success;
        this.errorMessage = errorMessage;
    }
    
    public FaceDetectionResult(boolean success, List<FaceRectangle> faces, int processingTimeMs, float imageQualityScore) {
        this.success = success;
        this.faces = faces;
        this.processingTimeMs = processingTimeMs;
        this.imageQualityScore = imageQualityScore;
    }
    
    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public List<FaceRectangle> getFaces() {
        return faces;
    }
    
    public void setFaces(List<FaceRectangle> faces) {
        this.faces = faces;
    }
    
    public int getProcessingTimeMs() {
        return processingTimeMs;
    }
    
    public void setProcessingTimeMs(int processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }
    
    public float getImageQualityScore() {
        return imageQualityScore;
    }
    
    public void setImageQualityScore(float imageQualityScore) {
        this.imageQualityScore = imageQualityScore;
    }
    
    public boolean hasFaces() {
        return faces != null && !faces.isEmpty();
    }
    
    public int getFaceCount() {
        return faces != null ? faces.size() : 0;
    }
}
