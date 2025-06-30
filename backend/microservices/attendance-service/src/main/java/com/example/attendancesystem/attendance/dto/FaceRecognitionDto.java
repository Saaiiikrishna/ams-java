package com.example.attendancesystem.attendance.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for face recognition operations and responses
 */
public class FaceRecognitionDto {

    // For face registration requests
    private Long subscriberId;
    private String base64Image;
    private String deviceInfo;

    // For face recognition responses
    private boolean success;
    private String message;
    private BigDecimal confidenceScore;
    private String recognitionStatus;
    private Integer processingTimeMs;
    private LocalDateTime timestamp;

    // For face recognition check-in requests
    private Long sessionId;
    private String entityId;

    // For subscriber face info
    private boolean hasFaceRecognition;
    private String profilePhotoPath;
    private LocalDateTime faceRegisteredAt;
    private LocalDateTime faceUpdatedAt;

    // Constructors
    public FaceRecognitionDto() {}

    public FaceRecognitionDto(boolean success, String message) {
        this.success = success;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    public FaceRecognitionDto(boolean success, String message, BigDecimal confidenceScore) {
        this.success = success;
        this.message = message;
        this.confidenceScore = confidenceScore;
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getSubscriberId() {
        return subscriberId;
    }

    public void setSubscriberId(Long subscriberId) {
        this.subscriberId = subscriberId;
    }

    public String getBase64Image() {
        return base64Image;
    }

    public void setBase64Image(String base64Image) {
        this.base64Image = base64Image;
    }

    public String getDeviceInfo() {
        return deviceInfo;
    }

    public void setDeviceInfo(String deviceInfo) {
        this.deviceInfo = deviceInfo;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public BigDecimal getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(BigDecimal confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public String getRecognitionStatus() {
        return recognitionStatus;
    }

    public void setRecognitionStatus(String recognitionStatus) {
        this.recognitionStatus = recognitionStatus;
    }

    public Integer getProcessingTimeMs() {
        return processingTimeMs;
    }

    public void setProcessingTimeMs(Integer processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public boolean isHasFaceRecognition() {
        return hasFaceRecognition;
    }

    public void setHasFaceRecognition(boolean hasFaceRecognition) {
        this.hasFaceRecognition = hasFaceRecognition;
    }

    public String getProfilePhotoPath() {
        return profilePhotoPath;
    }

    public void setProfilePhotoPath(String profilePhotoPath) {
        this.profilePhotoPath = profilePhotoPath;
    }

    public LocalDateTime getFaceRegisteredAt() {
        return faceRegisteredAt;
    }

    public void setFaceRegisteredAt(LocalDateTime faceRegisteredAt) {
        this.faceRegisteredAt = faceRegisteredAt;
    }

    public LocalDateTime getFaceUpdatedAt() {
        return faceUpdatedAt;
    }

    public void setFaceUpdatedAt(LocalDateTime faceUpdatedAt) {
        this.faceUpdatedAt = faceUpdatedAt;
    }
}
