package com.example.attendancesystem.organization.dto;

import com.example.attendancesystem.organization.model.FeaturePermission;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * DTO for updating organization permissions
 */
public class PermissionUpdateRequest {
    
    @NotNull(message = "Organization ID is required")
    private Long organizationId;
    
    @NotNull(message = "Feature permission is required")
    private FeaturePermission featurePermission;
    
    @NotNull(message = "Enabled status is required")
    private Boolean isEnabled;
    
    private LocalDateTime expiresAt;
    private String notes;

    // Default constructor
    public PermissionUpdateRequest() {}

    // Constructor
    public PermissionUpdateRequest(Long organizationId, FeaturePermission featurePermission, Boolean isEnabled) {
        this.organizationId = organizationId;
        this.featurePermission = featurePermission;
        this.isEnabled = isEnabled;
    }

    // Getters and Setters
    public Long getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }

    public FeaturePermission getFeaturePermission() {
        return featurePermission;
    }

    public void setFeaturePermission(FeaturePermission featurePermission) {
        this.featurePermission = featurePermission;
    }

    public Boolean getIsEnabled() {
        return isEnabled;
    }

    public void setIsEnabled(Boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    // Additional method for compatibility
    public java.util.List<FeaturePermission> getPermissions() {
        // Return single permission as list for compatibility
        return java.util.Arrays.asList(this.featurePermission);
    }
}
