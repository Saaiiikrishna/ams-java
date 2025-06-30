package com.example.attendancesystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CardRegistrationDto {
    
    @NotBlank(message = "Card UID is required")
    @Size(min = 4, max = 50, message = "Card UID must be between 4 and 50 characters")
    private String cardUid;
    
    private boolean isActive = true; // Default to active

    // Default constructor
    public CardRegistrationDto() {
    }

    // Constructor
    public CardRegistrationDto(String cardUid, boolean isActive) {
        this.cardUid = cardUid;
        this.isActive = isActive;
    }

    // Getters and Setters
    public String getCardUid() {
        return cardUid;
    }

    public void setCardUid(String cardUid) {
        this.cardUid = cardUid;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}
