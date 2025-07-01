package com.example.attendancesystem.attendance.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "attendance_logs",
       uniqueConstraints = @UniqueConstraint(
           columnNames = {"user_id", "session_id"},
           name = "uk_user_session"
       ))
public class AttendanceLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId; // Reference to user from user-service (can be MEMBER, ENTITY_ADMIN, or SUPER_ADMIN)

    @Column(name = "user_name")
    private String userName; // Cached for display - get from user-service via gRPC

    @Column(name = "user_mobile")
    private String userMobile; // Cached for display - get from user-service via gRPC

    @ManyToOne
    @JoinColumn(name = "session_id", nullable = false)
    private AttendanceSession session;

    @Column(nullable = false)
    private LocalDateTime checkInTime;

    private LocalDateTime checkOutTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "checkin_method", nullable = false)
    private CheckInMethod checkInMethod = CheckInMethod.NFC; // Default to NFC for backward compatibility

    @Enumerated(EnumType.STRING)
    @Column(name = "checkout_method")
    private CheckInMethod checkOutMethod; // Track check-out method separately

    @Column(name = "device_info")
    private String deviceInfo; // Device information for check-in

    @Column(name = "location_info")
    private String locationInfo; // Location information for check-in

    @Column(name = "notes")
    private String notes; // Additional notes

    // Constructors
    public AttendanceLog() {}

    public AttendanceLog(Long userId, String userName, String userMobile, AttendanceSession session, LocalDateTime checkInTime, CheckInMethod checkInMethod) {
        this.userId = userId;
        this.userName = userName;
        this.userMobile = userMobile;
        this.session = session;
        this.checkInTime = checkInTime;
        this.checkInMethod = checkInMethod;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    // Additional helper methods for compatibility
    public String getUserMobileNumber() {
        return userMobile;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserMobile() {
        return userMobile;
    }

    public void setUserMobile(String userMobile) {
        this.userMobile = userMobile;
    }

    // Removed deprecated Subscriber methods - use gRPC UserServiceClient instead

    public AttendanceSession getSession() {
        return session;
    }

    public void setSession(AttendanceSession session) {
        this.session = session;
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

    public CheckInMethod getCheckInMethod() {
        return checkInMethod;
    }

    public void setCheckInMethod(CheckInMethod checkInMethod) {
        this.checkInMethod = checkInMethod;
    }

    public CheckInMethod getCheckOutMethod() {
        return checkOutMethod;
    }

    public void setCheckOutMethod(CheckInMethod checkOutMethod) {
        this.checkOutMethod = checkOutMethod;
    }

    public String getDeviceInfo() {
        return deviceInfo;
    }

    public void setDeviceInfo(String deviceInfo) {
        this.deviceInfo = deviceInfo;
    }

    public String getLocationInfo() {
        return locationInfo;
    }

    public void setLocationInfo(String locationInfo) {
        this.locationInfo = locationInfo;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    // Utility methods
    public boolean isCheckedOut() {
        return checkOutTime != null;
    }

    public long getDurationMinutes() {
        if (checkOutTime == null) {
            return 0;
        }
        return java.time.Duration.between(checkInTime, checkOutTime).toMinutes();
    }
}
