package com.example.attendancesystem.dto;

import com.example.attendancesystem.model.CheckInMethod;
import java.time.LocalDateTime;
import java.util.Set;

public class AttendanceSessionDto {
    private Long id; // For responses
    private String name;
    private String description;
    private LocalDateTime startTime; // Can be set by user or defaulted to now
    private LocalDateTime endTime;   // For ending a session
    private Long organizationId; // For context in responses
    private Set<CheckInMethod> allowedCheckInMethods;

    // Default constructor
    public AttendanceSessionDto() {
    }

    // Constructor for creating a session
    public AttendanceSessionDto(String name, LocalDateTime startTime) {
        this.name = name;
        this.startTime = startTime;
    }

    // Full constructor for responses
    public AttendanceSessionDto(Long id, String name, String description, LocalDateTime startTime, LocalDateTime endTime, Long organizationId, Set<CheckInMethod> allowedCheckInMethods) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
        this.organizationId = organizationId;
        this.allowedCheckInMethods = allowedCheckInMethods;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<CheckInMethod> getAllowedCheckInMethods() {
        return allowedCheckInMethods;
    }

    public void setAllowedCheckInMethods(Set<CheckInMethod> allowedCheckInMethods) {
        this.allowedCheckInMethods = allowedCheckInMethods;
    }
}
