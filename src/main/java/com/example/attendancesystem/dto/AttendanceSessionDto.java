package com.example.attendancesystem.dto;

import java.time.LocalDateTime;

public class AttendanceSessionDto {
    private Long id; // For responses
    private String name;
    private LocalDateTime startTime; // Can be set by user or defaulted to now
    private LocalDateTime endTime;   // For ending a session
    private Long organizationId; // For context in responses

    // Default constructor
    public AttendanceSessionDto() {
    }

    // Constructor for creating a session
    public AttendanceSessionDto(String name, LocalDateTime startTime) {
        this.name = name;
        this.startTime = startTime;
    }

    // Full constructor for responses
    public AttendanceSessionDto(Long id, String name, LocalDateTime startTime, LocalDateTime endTime, Long organizationId) {
        this.id = id;
        this.name = name;
        this.startTime = startTime;
        this.endTime = endTime;
        this.organizationId = organizationId;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public Long getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }
}
