package com.example.attendancesystem.user.dto;

import com.example.attendancesystem.user.model.UserType;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Generic User DTO
 * Used for all user types: SUPER_ADMIN, ENTITY_ADMIN, MEMBER
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDto {

    private Long id;

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
    private String password; // Only for creation/update, not returned in responses

    @Email(message = "Invalid email format")
    private String email;

    @Size(max = 50, message = "First name must not exceed 50 characters")
    private String firstName;

    @Size(max = 50, message = "Last name must not exceed 50 characters")
    private String lastName;

    @Size(max = 15, message = "Mobile number must not exceed 15 characters")
    private String mobileNumber;

    private UserType userType;

    private Long organizationId;
    private String organizationName;

    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private String createdByName;

    // Device information (for members)
    private String lastDeviceId;
    private String lastDeviceInfo;
    private LocalDateTime lastLoginTime;

    // Permissions
    private List<String> permissions;

    // Computed fields
    private String fullName;
    private String displayName;

    // Default constructor
    public UserDto() {}

    // Constructor for responses (without password)
    public UserDto(Long id, String username, String email, String firstName, String lastName, 
                   String mobileNumber, UserType userType, Long organizationId, String organizationName,
                   Boolean isActive, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.mobileNumber = mobileNumber;
        this.userType = userType;
        this.organizationId = organizationId;
        this.organizationName = organizationName;
        this.isActive = isActive;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.fullName = buildFullName();
        this.displayName = buildDisplayName();
    }

    // Helper methods
    private String buildFullName() {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        } else if (firstName != null) {
            return firstName;
        } else if (lastName != null) {
            return lastName;
        }
        return username;
    }

    private String buildDisplayName() {
        String name = buildFullName();
        if (userType != null) {
            return name + " (" + userType.getDisplayName() + ")";
        }
        return name;
    }

    // Validation methods
    public boolean isValidForUserType() {
        switch (userType) {
            case SUPER_ADMIN:
                return organizationId == null; // Super admins don't belong to organizations
            case ENTITY_ADMIN:
            case MEMBER:
                return organizationId != null; // Entity admins and members must belong to organizations
            default:
                return false;
        }
    }

    public boolean requiresMobileNumber() {
        return userType == UserType.MEMBER;
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
        this.fullName = buildFullName();
        this.displayName = buildDisplayName();
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
        this.fullName = buildFullName();
        this.displayName = buildDisplayName();
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public UserType getUserType() {
        return userType;
    }

    public void setUserType(UserType userType) {
        this.userType = userType;
        this.displayName = buildDisplayName();
    }

    public Long getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }

    public String getLastDeviceId() {
        return lastDeviceId;
    }

    public void setLastDeviceId(String lastDeviceId) {
        this.lastDeviceId = lastDeviceId;
    }

    public String getLastDeviceInfo() {
        return lastDeviceInfo;
    }

    public void setLastDeviceInfo(String lastDeviceInfo) {
        this.lastDeviceInfo = lastDeviceInfo;
    }

    public LocalDateTime getLastLoginTime() {
        return lastLoginTime;
    }

    public void setLastLoginTime(LocalDateTime lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}
