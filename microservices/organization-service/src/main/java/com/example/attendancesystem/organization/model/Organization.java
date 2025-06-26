package com.example.attendancesystem.organization.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.Set;

@Entity
@Table(name = "organizations")
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = true, unique = true, length = 8)
    private String entityId; // Custom 8-character ID with MSD prefix

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = true) // Changed to true
    private String address;

    // Optional metadata
    private Double latitude;
    private Double longitude;
    private String contactPerson;
    private String email;

    @OneToMany(mappedBy = "organization")
    @JsonIgnore // Prevent circular reference during serialization
    private Set<EntityAdmin> admins;

    @OneToMany(mappedBy = "organization")
    @JsonIgnore // Prevent circular reference during serialization
    private Set<Subscriber> subscribers;

    @OneToMany(mappedBy = "organization")
    @JsonIgnore // Prevent circular reference during serialization
    private Set<AttendanceSession> attendanceSessions;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Set<EntityAdmin> getAdmins() {
        return admins;
    }

    public void setAdmins(Set<EntityAdmin> admins) {
        this.admins = admins;
    }

    public Set<Subscriber> getSubscribers() {
        return subscribers;
    }

    public void setSubscribers(Set<Subscriber> subscribers) {
        this.subscribers = subscribers;
    }

    public Set<AttendanceSession> getAttendanceSessions() {
        return attendanceSessions;
    }

    public void setAttendanceSessions(Set<AttendanceSession> attendanceSessions) {
        this.attendanceSessions = attendanceSessions;
    }
}
