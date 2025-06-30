package com.example.attendancesystem.dto;

import com.example.attendancesystem.model.FeaturePermission;

import java.time.LocalDateTime;

public class PermissionDto {
    
    private Long id;
    private String entityId;
    private String organizationName;
    private FeaturePermission featurePermission;
    private String permissionName;
    private String permissionDescription;
    private Boolean isEnabled;
    private Boolean isActive;
    private String grantedBy;
    private LocalDateTime grantedAt;
    private LocalDateTime expiresAt;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Constructors
    public PermissionDto() {}
    
    public PermissionDto(FeaturePermission featurePermission, Boolean isEnabled) {
        this.featurePermission = featurePermission;
        this.permissionName = featurePermission.getDisplayName();
        this.permissionDescription = featurePermission.getDescription();
        this.isEnabled = isEnabled;
        this.isActive = isEnabled;
    }
    
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
        if (featurePermission != null) {
            this.permissionName = featurePermission.getDisplayName();
            this.permissionDescription = featurePermission.getDescription();
        }
    }
    
    public String getPermissionName() {
        return permissionName;
    }
    
    public void setPermissionName(String permissionName) {
        this.permissionName = permissionName;
    }
    
    public String getPermissionDescription() {
        return permissionDescription;
    }
    
    public void setPermissionDescription(String permissionDescription) {
        this.permissionDescription = permissionDescription;
    }
    
    public Boolean getIsEnabled() {
        return isEnabled;
    }
    
    public void setIsEnabled(Boolean isEnabled) {
        this.isEnabled = isEnabled;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
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
}
