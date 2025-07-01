package com.example.attendancesystem.attendance.model;

/**
 * Enum representing different check-in methods available for attendance
 */
public enum CheckInMethod {
    NFC("NFC Card Scan"),
    QR("QR Code Scan"),
    BLUETOOTH("Bluetooth Proximity"),
    WIFI("WiFi Network"),
    MOBILE_NFC("Mobile NFC Scan"),
    FACE_RECOGNITION("Face Recognition"),
    MANUAL("Manual Check-out");

    private final String displayName;

    CheckInMethod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
