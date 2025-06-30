package com.example.attendancesystem.subscriber.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "attendance_logs",
       uniqueConstraints = @UniqueConstraint(
           columnNames = {"subscriber_id", "session_id"},
           name = "uk_subscriber_session"
       ))
public class AttendanceLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "subscriber_id", nullable = false)
    private Subscriber subscriber;

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
    private String deviceInfo; // Store device information for mobile check-ins

    @Column(name = "location_info")
    private String locationInfo; // Store location/network info for WiFi/Bluetooth check-ins

    // Constructors
    public AttendanceLog() {}

    public AttendanceLog(Subscriber subscriber, AttendanceSession session, LocalDateTime checkInTime, CheckInMethod checkInMethod) {
        this.subscriber = subscriber;
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

    public Subscriber getSubscriber() {
        return subscriber;
    }

    public void setSubscriber(Subscriber subscriber) {
        this.subscriber = subscriber;
    }

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
}
