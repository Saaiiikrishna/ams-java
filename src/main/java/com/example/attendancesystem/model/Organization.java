package com.example.attendancesystem.model;

import jakarta.persistence.*;
import java.util.Set;

@Entity
@Table(name = "organizations")
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String address;

    @OneToMany(mappedBy = "organization")
    private Set<EntityAdmin> admins;

    @OneToMany(mappedBy = "organization")
    private Set<Subscriber> subscribers;

    @OneToMany(mappedBy = "organization")
    private Set<AttendanceSession> attendanceSessions;

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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
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
