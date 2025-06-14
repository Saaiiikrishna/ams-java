package com.example.attendancesystem.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing face recognition configuration per organization
 * Allows customization of recognition parameters for different entities
 */
@Entity
@Table(name = "face_recognition_settings")
public class FaceRecognitionSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entity_id", nullable = false, unique = true, length = 8)
    private String entityId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entity_id", referencedColumnName = "entityId", insertable = false, updatable = false)
    private Organization organization;

    @Column(name = "confidence_threshold", nullable = false, precision = 5, scale = 4)
    private BigDecimal confidenceThreshold = new BigDecimal("0.8000");

    @Column(name = "max_recognition_distance", nullable = false, precision = 8, scale = 6)
    private BigDecimal maxRecognitionDistance = new BigDecimal("0.600000");

    @Column(name = "enable_anti_spoofing", nullable = false)
    private Boolean enableAntiSpoofing = true;

    @Column(name = "enable_multiple_face_detection", nullable = false)
    private Boolean enableMultipleFaceDetection = false;

    @Column(name = "max_processing_time_ms", nullable = false)
    private Integer maxProcessingTimeMs = 5000;

    @Column(name = "photo_quality_threshold", nullable = false, precision = 5, scale = 4)
    private BigDecimal photoQualityThreshold = new BigDecimal("0.7000");

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    // Constructors
    public FaceRecognitionSettings() {}

    public FaceRecognitionSettings(String entityId) {
        this.entityId = entityId;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    public BigDecimal getConfidenceThreshold() {
        return confidenceThreshold;
    }

    public void setConfidenceThreshold(BigDecimal confidenceThreshold) {
        this.confidenceThreshold = confidenceThreshold;
    }

    public BigDecimal getMaxRecognitionDistance() {
        return maxRecognitionDistance;
    }

    public void setMaxRecognitionDistance(BigDecimal maxRecognitionDistance) {
        this.maxRecognitionDistance = maxRecognitionDistance;
    }

    public Boolean getEnableAntiSpoofing() {
        return enableAntiSpoofing;
    }

    public void setEnableAntiSpoofing(Boolean enableAntiSpoofing) {
        this.enableAntiSpoofing = enableAntiSpoofing;
    }

    public Boolean getEnableMultipleFaceDetection() {
        return enableMultipleFaceDetection;
    }

    public void setEnableMultipleFaceDetection(Boolean enableMultipleFaceDetection) {
        this.enableMultipleFaceDetection = enableMultipleFaceDetection;
    }

    public Integer getMaxProcessingTimeMs() {
        return maxProcessingTimeMs;
    }

    public void setMaxProcessingTimeMs(Integer maxProcessingTimeMs) {
        this.maxProcessingTimeMs = maxProcessingTimeMs;
    }

    public BigDecimal getPhotoQualityThreshold() {
        return photoQualityThreshold;
    }

    public void setPhotoQualityThreshold(BigDecimal photoQualityThreshold) {
        this.photoQualityThreshold = photoQualityThreshold;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
