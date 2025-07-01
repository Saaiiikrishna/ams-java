package com.example.attendancesystem.attendance.dto;

import com.example.attendancesystem.attendance.model.CheckInMethod;

public class CheckInRequestDto {
    private Long sessionId;
    private CheckInMethod checkInMethod;
    private String qrCode; // For QR code check-in
    private String deviceId; // For Bluetooth/WiFi check-in
    private String deviceInfo; // Device information
    private String locationInfo; // WiFi network name, Bluetooth beacon ID, etc.
    private String nfcData; // For mobile NFC check-in
    private Double latitude; // Optional location data
    private Double longitude; // Optional location data

    // Constructors
    public CheckInRequestDto() {}

    public CheckInRequestDto(Long sessionId, CheckInMethod checkInMethod) {
        this.sessionId = sessionId;
        this.checkInMethod = checkInMethod;
    }

    // Getters and Setters
    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public CheckInMethod getCheckInMethod() {
        return checkInMethod;
    }

    public void setCheckInMethod(CheckInMethod checkInMethod) {
        this.checkInMethod = checkInMethod;
    }

    public String getQrCode() {
        return qrCode;
    }

    public void setQrCode(String qrCode) {
        this.qrCode = qrCode;
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

    public String getLocationInfo() {
        return locationInfo;
    }

    public void setLocationInfo(String locationInfo) {
        this.locationInfo = locationInfo;
    }

    public String getNfcData() {
        return nfcData;
    }

    public void setNfcData(String nfcData) {
        this.nfcData = nfcData;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }
}
