package com.example.attendancesystem.attendance.dto;

import com.example.attendancesystem.attendance.model.CheckInMethod;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

public class ScheduledSessionDto {
    private Long id;
    private String name;
    private String description;
    
    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;
    
    private Integer durationMinutes;
    private Set<DayOfWeek> daysOfWeek;
    private Set<CheckInMethod> allowedCheckInMethods;
    private Boolean active;
    private String organizationEntityId; // For response context

    // Constructors
    public ScheduledSessionDto() {}

    public ScheduledSessionDto(String name, String description, LocalTime startTime, 
                              Integer durationMinutes, Set<DayOfWeek> daysOfWeek, 
                              Set<CheckInMethod> allowedCheckInMethods) {
        this.name = name;
        this.description = description;
        this.startTime = startTime;
        this.durationMinutes = durationMinutes;
        this.daysOfWeek = daysOfWeek;
        this.allowedCheckInMethods = allowedCheckInMethods;
        this.active = true;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public Set<DayOfWeek> getDaysOfWeek() {
        return daysOfWeek;
    }

    public void setDaysOfWeek(Set<DayOfWeek> daysOfWeek) {
        this.daysOfWeek = daysOfWeek;
    }

    public Set<CheckInMethod> getAllowedCheckInMethods() {
        return allowedCheckInMethods;
    }

    public void setAllowedCheckInMethods(Set<CheckInMethod> allowedCheckInMethods) {
        this.allowedCheckInMethods = allowedCheckInMethods;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getOrganizationEntityId() {
        return organizationEntityId;
    }

    public void setOrganizationEntityId(String organizationEntityId) {
        this.organizationEntityId = organizationEntityId;
    }
}
