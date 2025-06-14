package com.example.attendancesystem.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing face recognition audit logs
 * Tracks all face recognition attempts for security and debugging
 */
@Entity
@Table(name = "face_recognition_logs")
public class FaceRecognitionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscriber_id")
    private Subscriber subscriber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private AttendanceSession session;

    @Column(name = "recognition_timestamp", nullable = false)
    private LocalDateTime recognitionTimestamp = LocalDateTime.now();

    @Column(name = "confidence_score", precision = 5, scale = 4)
    private BigDecimal confidenceScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "recognition_status", nullable = false, length = 20)
    private RecognitionStatus recognitionStatus;

    @Column(name = "processing_time_ms")
    private Integer processingTimeMs;

    @Column(name = "image_path", length = 500)
    private String imagePath;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "device_info", columnDefinition = "TEXT")
    private String deviceInfo;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Constructors
    public FaceRecognitionLog() {}

    public FaceRecognitionLog(Subscriber subscriber, AttendanceSession session, 
                             RecognitionStatus status, BigDecimal confidenceScore) {
        this.subscriber = subscriber;
        this.session = session;
        this.recognitionStatus = status;
        this.confidenceScore = confidenceScore;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Subscriber getSubscriber() {
        return subscriber;
    }

    public void setSubscriber(Subscriber subscriber) {
        this.subscriber = subscriber;
    }

    public AttendanceSession getSession() {
        return session;
    }

    public void setSession(AttendanceSession session) {
        this.session = session;
    }

    public LocalDateTime getRecognitionTimestamp() {
        return recognitionTimestamp;
    }

    public void setRecognitionTimestamp(LocalDateTime recognitionTimestamp) {
        this.recognitionTimestamp = recognitionTimestamp;
    }

    public BigDecimal getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(BigDecimal confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public RecognitionStatus getRecognitionStatus() {
        return recognitionStatus;
    }

    public void setRecognitionStatus(RecognitionStatus recognitionStatus) {
        this.recognitionStatus = recognitionStatus;
    }

    public Integer getProcessingTimeMs() {
        return processingTimeMs;
    }

    public void setProcessingTimeMs(Integer processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getDeviceInfo() {
        return deviceInfo;
    }

    public void setDeviceInfo(String deviceInfo) {
        this.deviceInfo = deviceInfo;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Enum for face recognition status
     */
    public enum RecognitionStatus {
        SUCCESS("Recognition successful"),
        FAILED("Recognition failed"),
        LOW_CONFIDENCE("Low confidence score"),
        ERROR("Processing error"),
        MULTIPLE_FACES("Multiple faces detected"),
        NO_FACE_DETECTED("No face detected in image");

        private final String description;

        RecognitionStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
