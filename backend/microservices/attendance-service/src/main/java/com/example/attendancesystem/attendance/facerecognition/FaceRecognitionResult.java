package com.example.attendancesystem.attendance.facerecognition;

/**
 * Result of face recognition/matching operation
 */
public class FaceRecognitionResult {
    
    private boolean success;
    private String errorMessage;
    private boolean matched;
    private float confidenceScore;
    private float distance;
    private Long matchedSubscriberId;
    private String matchedSubscriberName;
    private int processingTimeMs;
    private float qualityScore;
    private float livenessScore;
    private FaceRectangle detectedFace;
    
    public FaceRecognitionResult() {}
    
    public FaceRecognitionResult(boolean success, String errorMessage) {
        this.success = success;
        this.errorMessage = errorMessage;
    }
    
    public FaceRecognitionResult(boolean success, boolean matched, float confidenceScore, 
                               float distance, int processingTimeMs) {
        this.success = success;
        this.matched = matched;
        this.confidenceScore = confidenceScore;
        this.distance = distance;
        this.processingTimeMs = processingTimeMs;
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
    
    public boolean isMatched() {
        return matched;
    }
    
    public void setMatched(boolean matched) {
        this.matched = matched;
    }
    
    public float getConfidenceScore() {
        return confidenceScore;
    }
    
    public void setConfidenceScore(float confidenceScore) {
        this.confidenceScore = confidenceScore;
    }
    
    public float getDistance() {
        return distance;
    }
    
    public void setDistance(float distance) {
        this.distance = distance;
    }
    
    public Long getMatchedSubscriberId() {
        return matchedSubscriberId;
    }
    
    public void setMatchedSubscriberId(Long matchedSubscriberId) {
        this.matchedSubscriberId = matchedSubscriberId;
    }
    
    public String getMatchedSubscriberName() {
        return matchedSubscriberName;
    }
    
    public void setMatchedSubscriberName(String matchedSubscriberName) {
        this.matchedSubscriberName = matchedSubscriberName;
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
    
    public FaceRectangle getDetectedFace() {
        return detectedFace;
    }
    
    public void setDetectedFace(FaceRectangle detectedFace) {
        this.detectedFace = detectedFace;
    }
    
    public boolean isValidMatch() {
        return success && matched && matchedSubscriberId != null;
    }
}
