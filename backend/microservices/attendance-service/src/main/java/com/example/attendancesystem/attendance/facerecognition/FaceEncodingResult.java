package com.example.attendancesystem.attendance.facerecognition;

/**
 * Result of face encoding extraction operation
 */
public class FaceEncodingResult {
    
    private boolean success;
    private String errorMessage;
    private float[] encoding;
    private FaceRectangle faceRectangle;
    private int processingTimeMs;
    private float qualityScore;
    private float livenessScore;
    
    public FaceEncodingResult() {}
    
    public FaceEncodingResult(boolean success, String errorMessage) {
        this.success = success;
        this.errorMessage = errorMessage;
    }
    
    public FaceEncodingResult(boolean success, float[] encoding, FaceRectangle faceRectangle, 
                            int processingTimeMs, float qualityScore, float livenessScore) {
        this.success = success;
        this.encoding = encoding;
        this.faceRectangle = faceRectangle;
        this.processingTimeMs = processingTimeMs;
        this.qualityScore = qualityScore;
        this.livenessScore = livenessScore;
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
    
    public float[] getEncoding() {
        return encoding;
    }
    
    public void setEncoding(float[] encoding) {
        this.encoding = encoding;
    }
    
    public FaceRectangle getFaceRectangle() {
        return faceRectangle;
    }
    
    public void setFaceRectangle(FaceRectangle faceRectangle) {
        this.faceRectangle = faceRectangle;
    }
    
    public int getProcessingTimeMs() {
        return processingTimeMs;
    }
    
    public void setProcessingTimeMs(int processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }
    
    public float getQualityScore() {
        return qualityScore;
    }
    
    public void setQualityScore(float qualityScore) {
        this.qualityScore = qualityScore;
    }
    
    public float getLivenessScore() {
        return livenessScore;
    }
    
    public void setLivenessScore(float livenessScore) {
        this.livenessScore = livenessScore;
    }
    
    public boolean hasValidEncoding() {
        return encoding != null && encoding.length > 0;
    }
    
    public int getEncodingDimensions() {
        return encoding != null ? encoding.length : 0;
    }
}
