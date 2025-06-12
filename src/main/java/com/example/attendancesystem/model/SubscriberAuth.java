package com.example.attendancesystem.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "subscriber_auth")
public class SubscriberAuth {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "subscriber_id", nullable = false, unique = true)
    private Subscriber subscriber;

    @Column(nullable = false, length = 255)
    private String pin; // Encoded PIN for mobile app access

    @Column(length = 6)
    private String otpCode; // Temporary OTP for verification

    @Column
    private LocalDateTime otpExpiryTime;

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column
    private LocalDateTime lastLoginTime;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column
    private LocalDateTime updatedAt = LocalDateTime.now();

    // Device information for security
    @Column
    private String lastDeviceId;

    @Column
    private String lastDeviceInfo;

    // Constructors
    public SubscriberAuth() {}

    public SubscriberAuth(Subscriber subscriber, String pin) {
        this.subscriber = subscriber;
        this.pin = pin;
        this.isActive = true;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
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

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
        this.updatedAt = LocalDateTime.now();
    }

    public String getOtpCode() {
        return otpCode;
    }

    public void setOtpCode(String otpCode) {
        this.otpCode = otpCode;
        this.otpExpiryTime = LocalDateTime.now().plusMinutes(5); // OTP valid for 5 minutes
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getOtpExpiryTime() {
        return otpExpiryTime;
    }

    public void setOtpExpiryTime(LocalDateTime otpExpiryTime) {
        this.otpExpiryTime = otpExpiryTime;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getLastLoginTime() {
        return lastLoginTime;
    }

    public void setLastLoginTime(LocalDateTime lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
        this.updatedAt = LocalDateTime.now();
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

    public String getLastDeviceId() {
        return lastDeviceId;
    }

    public void setLastDeviceId(String lastDeviceId) {
        this.lastDeviceId = lastDeviceId;
        this.updatedAt = LocalDateTime.now();
    }

    public String getLastDeviceInfo() {
        return lastDeviceInfo;
    }

    public void setLastDeviceInfo(String lastDeviceInfo) {
        this.lastDeviceInfo = lastDeviceInfo;
        this.updatedAt = LocalDateTime.now();
    }

    // Utility methods
    public boolean isOtpValid() {
        return otpCode != null && otpExpiryTime != null && 
               LocalDateTime.now().isBefore(otpExpiryTime);
    }

    public void clearOtp() {
        this.otpCode = null;
        this.otpExpiryTime = null;
        this.updatedAt = LocalDateTime.now();
    }
}
