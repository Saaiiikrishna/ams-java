package com.example.attendancesystem.shared.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "attendance_sessions")
public class AttendanceSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    @ManyToOne
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<AttendanceLog> attendanceLogs;

    @Column
    private String description;

    @ElementCollection(targetClass = CheckInMethod.class)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "session_checkin_methods", joinColumns = @JoinColumn(name = "session_id"))
    @Column(name = "checkin_method")
    private Set<CheckInMethod> allowedCheckInMethods;

    @Column(name = "qr_code")
    private String qrCode; // Generated QR code for this session

    @Column(name = "qr_code_expiry")
    private LocalDateTime qrCodeExpiry; // QR code expiration time

    @ManyToOne
    @JoinColumn(name = "scheduled_session_id")
    private ScheduledSession scheduledSession; // Reference to scheduled session if auto-created

    // Constructors
    public AttendanceSession() {}

    public AttendanceSession(String name, LocalDateTime startTime, LocalDateTime endTime, Organization organization) {
        this.name = name;
        this.startTime = startTime;
        this.endTime = endTime;
        this.organization = organization;
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

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    public Set<AttendanceLog> getAttendanceLogs() {
        return attendanceLogs;
    }

    public void setAttendanceLogs(Set<AttendanceLog> attendanceLogs) {
        this.attendanceLogs = attendanceLogs;
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

    public String getQrCode() {
        return qrCode;
    }

    public void setQrCode(String qrCode) {
        this.qrCode = qrCode;
    }

    public LocalDateTime getQrCodeExpiry() {
        return qrCodeExpiry;
    }

    public void setQrCodeExpiry(LocalDateTime qrCodeExpiry) {
        this.qrCodeExpiry = qrCodeExpiry;
    }

    public ScheduledSession getScheduledSession() {
        return scheduledSession;
    }

    public void setScheduledSession(ScheduledSession scheduledSession) {
        this.scheduledSession = scheduledSession;
    }

    // Utility methods
    public boolean isActive() {
        LocalDateTime now = LocalDateTime.now();
        return startTime != null && startTime.isBefore(now) &&
               (endTime == null || endTime.isAfter(now));
    }

    public boolean isQrCodeValid() {
        return qrCode != null && qrCodeExpiry != null &&
               LocalDateTime.now().isBefore(qrCodeExpiry);
    }
}
