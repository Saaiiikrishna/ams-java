package com.example.attendancesystem.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
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

    @OneToOne(mappedBy = "subscriber", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private NfcCard nfcCard;

    @Column(name = "profile_photo_path", length = 500)
    private String profilePhotoPath;

    @Column(name = "face_encoding")
    private byte[] faceEncoding;

    @Column(name = "face_encoding_version", length = 20)
    private String faceEncodingVersion = "1.0";

    @Column(name = "face_registered_at")
    private LocalDateTime faceRegisteredAt;

    @Column(name = "face_updated_at")
    private LocalDateTime faceUpdatedAt;

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

    public String getProfilePhotoPath() {
        return profilePhotoPath;
    }

    public void setProfilePhotoPath(String profilePhotoPath) {
        this.profilePhotoPath = profilePhotoPath;
    }

    public byte[] getFaceEncoding() {
        return faceEncoding;
    }

    public void setFaceEncoding(byte[] faceEncoding) {
        this.faceEncoding = faceEncoding;
    }

    public String getFaceEncodingVersion() {
        return faceEncodingVersion;
    }

    public void setFaceEncodingVersion(String faceEncodingVersion) {
        this.faceEncodingVersion = faceEncodingVersion;
    }

    public LocalDateTime getFaceRegisteredAt() {
        return faceRegisteredAt;
    }

    public void setFaceRegisteredAt(LocalDateTime faceRegisteredAt) {
        this.faceRegisteredAt = faceRegisteredAt;
    }

    public LocalDateTime getFaceUpdatedAt() {
        return faceUpdatedAt;
    }

    public void setFaceUpdatedAt(LocalDateTime faceUpdatedAt) {
        this.faceUpdatedAt = faceUpdatedAt;
    }

    /**
     * Check if subscriber has face recognition enabled
     */
    public boolean hasFaceRecognition() {
        return faceEncoding != null && faceEncoding.length > 0;
    }
}
