package com.example.attendancesystem.dto;

import java.time.LocalDateTime;

public class NfcCardDto {
    private Long id;
    private String cardUid;
    private boolean isActive;
    private Long subscriberId;
    private String subscriberName;
    private String subscriberEmail;
    private String subscriberMobileNumber;
    private String organizationName;
    private String entityId;
    private LocalDateTime assignedAt;

    // Default constructor
    public NfcCardDto() {
    }

    // Constructor for unassigned card
    public NfcCardDto(Long id, String cardUid, boolean isActive) {
        this.id = id;
        this.cardUid = cardUid;
        this.isActive = isActive;
    }

    // Constructor for assigned card
    public NfcCardDto(Long id, String cardUid, boolean isActive, Long subscriberId, 
                      String subscriberName, String subscriberEmail, String subscriberMobileNumber,
                      String organizationName, String entityId, LocalDateTime assignedAt) {
        this.id = id;
        this.cardUid = cardUid;
        this.isActive = isActive;
        this.subscriberId = subscriberId;
        this.subscriberName = subscriberName;
        this.subscriberEmail = subscriberEmail;
        this.subscriberMobileNumber = subscriberMobileNumber;
        this.organizationName = organizationName;
        this.entityId = entityId;
        this.assignedAt = assignedAt;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public Long getSubscriberId() {
        return subscriberId;
    }

    public void setSubscriberId(Long subscriberId) {
        this.subscriberId = subscriberId;
    }

    public String getSubscriberName() {
        return subscriberName;
    }

    public void setSubscriberName(String subscriberName) {
        this.subscriberName = subscriberName;
    }

    public String getSubscriberEmail() {
        return subscriberEmail;
    }

    public void setSubscriberEmail(String subscriberEmail) {
        this.subscriberEmail = subscriberEmail;
    }

    public String getSubscriberMobileNumber() {
        return subscriberMobileNumber;
    }

    public void setSubscriberMobileNumber(String subscriberMobileNumber) {
        this.subscriberMobileNumber = subscriberMobileNumber;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }

    // Helper methods
    public boolean isAssigned() {
        return subscriberId != null;
    }

    public String getDisplayName() {
        if (subscriberName != null) {
            return subscriberName;
        }
        return "Unassigned";
    }
}
