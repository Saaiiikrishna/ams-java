package com.example.attendancesystem.shared.model;

import jakarta.persistence.*;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

@Entity
@Table(name = "scheduled_sessions")
public class ScheduledSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private Integer durationMinutes;

    @ElementCollection(targetClass = DayOfWeek.class)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "scheduled_session_days", joinColumns = @JoinColumn(name = "scheduled_session_id"))
    @Column(name = "day_of_week")
    private Set<DayOfWeek> daysOfWeek;

    @ElementCollection(targetClass = CheckInMethod.class)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "scheduled_session_checkin_methods", joinColumns = @JoinColumn(name = "scheduled_session_id"))
    @Column(name = "checkin_method")
    private Set<CheckInMethod> allowedCheckInMethods;

    @ManyToOne
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false)
    private Boolean active = true;

    // Constructors
    public ScheduledSession() {}

    public ScheduledSession(String name, String description, LocalTime startTime, 
                           Integer durationMinutes, Set<DayOfWeek> daysOfWeek, 
                           Set<CheckInMethod> allowedCheckInMethods, Organization organization) {
        this.name = name;
        this.description = description;
        this.startTime = startTime;
        this.durationMinutes = durationMinutes;
        this.daysOfWeek = daysOfWeek;
        this.allowedCheckInMethods = allowedCheckInMethods;
        this.organization = organization;
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

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    // Utility methods
    public boolean isActiveForDay(DayOfWeek dayOfWeek) {
        return active && daysOfWeek != null && daysOfWeek.contains(dayOfWeek);
    }

    public LocalTime getEndTime() {
        return startTime.plusMinutes(durationMinutes);
    }
}
