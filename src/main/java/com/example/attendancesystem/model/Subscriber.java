package com.example.attendancesystem.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.Set;

@Entity
@Table(name = "subscribers")
public class Subscriber {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = true) // Email is now optional
    private String email;

    @Column(nullable = false)
    private String mobileNumber;

    @ManyToOne
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @OneToMany(mappedBy = "subscriber", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore // Prevent circular reference during serialization
    private Set<AttendanceLog> attendanceLogs;

    @OneToOne(mappedBy = "subscriber", cascade = CascadeType.ALL)
    private NfcCard nfcCard;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
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

    public NfcCard getNfcCard() {
        return nfcCard;
    }

    public void setNfcCard(NfcCard nfcCard) {
        this.nfcCard = nfcCard;
    }
}
