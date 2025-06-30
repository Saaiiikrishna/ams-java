package com.example.attendancesystem.organization.dto;

import com.example.attendancesystem.organization.model.FeaturePermission;
import java.time.LocalDateTime;

/**
 * DTO for Organization Permission data transfer
 */
public class PermissionDto {
    
    private Long id;
    private Long organizationId;
    private String organizationName;
    private FeaturePermission featurePermission;
    private Boolean isEnabled;
    private String grantedBy;
    private LocalDateTime grantedAt;
    private LocalDateTime expiresAt;
    private String notes;

    // Default constructor
    public PermissionDto() {}

    // Constructor with essential fields
    public PermissionDto(Long organizationId, FeaturePermission featurePermission, Boolean isEnabled) {
        this.organizationId = organizationId;
        this.featurePermission = featurePermission;
        this.isEnabled = isEnabled;
    }

    // Constructor for PermissionController compatibility
    public PermissionDto(FeaturePermission featurePermission, Boolean isEnabled) {
        this.featurePermission = featurePermission;
        this.isEnabled = isEnabled;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getGrantedBy() {
        return grantedBy;
    }

    public void setGrantedBy(String grantedBy) {
        this.grantedBy = grantedBy;
    }

    public LocalDateTime getGrantedAt() {
        return grantedAt;
    }

    public void setGrantedAt(LocalDateTime grantedAt) {
        this.grantedAt = grantedAt;
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

    // Additional methods for compatibility
    public void setEntityId(String entityId) {
        // This is for compatibility - organization entity ID is not stored in permission
    }

    public void setIsActive(boolean isActive) {
        // This is for compatibility - permissions don't have isActive field
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.grantedAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        // This is for compatibility - permissions don't have updatedAt field
    }
}
