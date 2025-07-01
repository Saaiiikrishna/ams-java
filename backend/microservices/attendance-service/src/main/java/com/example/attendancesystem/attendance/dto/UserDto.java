package com.example.attendancesystem.attendance.dto;

/**
 * DTO for User data from User Service
 * Contains only fields needed by Attendance Service
 */
public class UserDto {
    private Long id;
    private String username;
    private String email;
    private String mobileNumber;
    private String firstName;
    private String lastName;
    private String userType; // SUPERADMIN, ENTITY_ADMIN, MEMBER
    private Long organizationId;
    private Boolean active;

    // Face recognition fields (for attendance service compatibility)
    private byte[] faceEncoding;
    private String faceEncodingVersion;
    private java.time.LocalDateTime faceRegisteredAt;
    private java.time.LocalDateTime faceUpdatedAt;
    private String profilePhotoPath;
    
    // Constructors
    public UserDto() {}
    
    public UserDto(Long id, String username, String email, String mobileNumber, 
                   String firstName, String lastName, String userType, 
                   Long organizationId, Boolean active) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.mobileNumber = mobileNumber;
        this.firstName = firstName;
        this.lastName = lastName;
        this.userType = userType;
        this.organizationId = organizationId;
        this.active = active;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
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
    
    public String getUserType() {
        return userType;
    }
    
    public void setUserType(String userType) {
        this.userType = userType;
    }
    
    public Long getOrganizationId() {
        return organizationId;
    }
    
    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }
    
    public Boolean getActive() {
        return active;
    }
    
    public void setActive(Boolean active) {
        this.active = active;
    }
    
    // Utility methods
    public String getFullName() {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        } else if (firstName != null) {
            return firstName;
        } else if (lastName != null) {
            return lastName;
        }
        return username;
    }
    
    public boolean isMember() {
        return "MEMBER".equals(userType);
    }
    
    public boolean isEntityAdmin() {
        return "ENTITY_ADMIN".equals(userType);
    }
    
    public boolean isSuperAdmin() {
        return "SUPERADMIN".equals(userType);
    }

    // Face recognition getters and setters
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

    public java.time.LocalDateTime getFaceRegisteredAt() {
        return faceRegisteredAt;
    }

    public void setFaceRegisteredAt(java.time.LocalDateTime faceRegisteredAt) {
        this.faceRegisteredAt = faceRegisteredAt;
    }

    public java.time.LocalDateTime getFaceUpdatedAt() {
        return faceUpdatedAt;
    }

    public void setFaceUpdatedAt(java.time.LocalDateTime faceUpdatedAt) {
        this.faceUpdatedAt = faceUpdatedAt;
    }

    public String getProfilePhotoPath() {
        return profilePhotoPath;
    }

    public void setProfilePhotoPath(String profilePhotoPath) {
        this.profilePhotoPath = profilePhotoPath;
    }

    // Face recognition helper methods
    public boolean hasFaceRecognition() {
        return faceEncoding != null && faceEncoding.length > 0;
    }
    
    @Override
    public String toString() {
        return "UserDto{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", mobileNumber='" + mobileNumber + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", userType='" + userType + '\'' +
                ", organizationId=" + organizationId +
                ", active=" + active +
                '}';
    }
}
