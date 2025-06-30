package com.example.attendancesystem.dto;

import java.time.LocalDateTime;

public class AttendanceLogDto {
    private Long id;
    private Long subscriberId;
    private String subscriberFirstName;
    private String subscriberLastName;
    private String subscriberEmail;
    private Long sessionId;
    private String sessionName;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private String checkinMethod;
    private String checkoutMethod;

    // Default constructor
    public AttendanceLogDto() {
    }

    // Full constructor
    public AttendanceLogDto(Long id, Long subscriberId, String subscriberFirstName, String subscriberLastName, String subscriberEmail,
                            Long sessionId, String sessionName, LocalDateTime checkInTime, LocalDateTime checkOutTime, String checkinMethod, String checkoutMethod) {
        this.id = id;
        this.subscriberId = subscriberId;
        this.subscriberFirstName = subscriberFirstName;
        this.subscriberLastName = subscriberLastName;
        this.subscriberEmail = subscriberEmail;
        this.sessionId = sessionId;
        this.sessionName = sessionName;
        this.checkInTime = checkInTime;
        this.checkOutTime = checkOutTime;
        this.checkinMethod = checkinMethod;
        this.checkoutMethod = checkoutMethod;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSubscriberId() {
        return subscriberId;
    }

    public void setSubscriberId(Long subscriberId) {
        this.subscriberId = subscriberId;
    }

    public String getSubscriberFirstName() {
        return subscriberFirstName;
    }

    public void setSubscriberFirstName(String subscriberFirstName) {
        this.subscriberFirstName = subscriberFirstName;
    }

    public String getSubscriberLastName() {
        return subscriberLastName;
    }

    public void setSubscriberLastName(String subscriberLastName) {
        this.subscriberLastName = subscriberLastName;
    }

    public String getSubscriberEmail() {
        return subscriberEmail;
    }

    public void setSubscriberEmail(String subscriberEmail) {
        this.subscriberEmail = subscriberEmail;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public String getSessionName() {
        return sessionName;
    }

    public void setSessionName(String sessionName) {
        this.sessionName = sessionName;
    }

    public LocalDateTime getCheckInTime() {
        return checkInTime;
    }

    public void setCheckInTime(LocalDateTime checkInTime) {
        this.checkInTime = checkInTime;
    }

    public LocalDateTime getCheckOutTime() {
        return checkOutTime;
    }

    public void setCheckOutTime(LocalDateTime checkOutTime) {
        this.checkOutTime = checkOutTime;
    }

    public String getCheckinMethod() {
        return checkinMethod;
    }

    public void setCheckinMethod(String checkinMethod) {
        this.checkinMethod = checkinMethod;
    }

    public String getCheckoutMethod() {
        return checkoutMethod;
    }

    public void setCheckoutMethod(String checkoutMethod) {
        this.checkoutMethod = checkoutMethod;
    }
}
