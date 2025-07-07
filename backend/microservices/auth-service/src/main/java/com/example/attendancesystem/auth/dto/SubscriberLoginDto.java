package com.example.attendancesystem.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Subscriber login DTO for mobile authentication
 */
public class SubscriberLoginDto {

    @JsonProperty("mobileNumber")
    private String mobileNumber;

    @JsonProperty("pin")
    private String pin;

    @JsonProperty("otpCode")
    private String otpCode;

    @JsonProperty("deviceId")
    private String deviceId;

    @JsonProperty("deviceInfo")
    private String deviceInfo;

    @JsonProperty("entityId")
    private String entityId; // Organization entity ID

    // Constructors
    public SubscriberLoginDto() {}

    public SubscriberLoginDto(String mobileNumber, String pin) {
        this.mobileNumber = mobileNumber;
        this.pin = pin;
    }

    // Getters and Setters
    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    public String getOtpCode() {
        return otpCode;
    }

    public void setOtpCode(String otpCode) {
        this.otpCode = otpCode;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceInfo() {
        return deviceInfo;
    }

    public void setDeviceInfo(String deviceInfo) {
        this.deviceInfo = deviceInfo;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }
}
