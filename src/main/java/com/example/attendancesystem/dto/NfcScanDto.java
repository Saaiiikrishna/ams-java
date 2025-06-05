package com.example.attendancesystem.dto;

public class NfcScanDto {
    private String cardUid;

    // Default constructor
    public NfcScanDto() {
    }

    // Constructor with fields
    public NfcScanDto(String cardUid) {
        this.cardUid = cardUid;
    }

    // Getter and Setter
    public String getCardUid() {
        return cardUid;
    }

    public void setCardUid(String cardUid) {
        this.cardUid = cardUid;
    }
}
