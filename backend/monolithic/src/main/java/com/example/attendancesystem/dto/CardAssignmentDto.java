package com.example.attendancesystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CardAssignmentDto {
    
    @NotBlank(message = "Card UID is required")
    private String cardUid;
    
    @NotNull(message = "Subscriber ID is required")
    private Long subscriberId;

    // Default constructor
    public CardAssignmentDto() {
    }

    // Constructor
    public CardAssignmentDto(String cardUid, Long subscriberId) {
        this.cardUid = cardUid;
        this.subscriberId = subscriberId;
    }

    // Getters and Setters
    public String getCardUid() {
        return cardUid;
    }

    public void setCardUid(String cardUid) {
        this.cardUid = cardUid;
    }

    public Long getSubscriberId() {
        return subscriberId;
    }

    public void setSubscriberId(Long subscriberId) {
        this.subscriberId = subscriberId;
    }
}
