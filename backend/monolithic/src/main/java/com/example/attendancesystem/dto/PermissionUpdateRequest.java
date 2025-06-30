package com.example.attendancesystem.dto;

import com.example.attendancesystem.model.FeaturePermission;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public class PermissionUpdateRequest {
    
    @NotNull(message = "Entity ID is required")
    private String entityId;
    
    @NotNull(message = "Permissions list is required")
    private List<FeaturePermission> permissions;
    
    private Boolean isEnabled = true;
    private LocalDateTime expiresAt;
    private String notes;
    
    // Constructors
    public PermissionUpdateRequest() {}
    
    public PermissionUpdateRequest(String entityId, List<FeaturePermission> permissions) {
        this.entityId = entityId;
        this.permissions = permissions;
    }
    
    // Getters and Setters
    public String getEntityId() {
        return entityId;
    }
    
    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }
    
    public List<FeaturePermission> getPermissions() {
        return permissions;
    }
    
    public void setPermissions(List<FeaturePermission> permissions) {
        this.permissions = permissions;
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
}

// Single permission update request
class SinglePermissionRequest {
    
    @NotNull(message = "Entity ID is required")
    private String entityId;
    
    @NotNull(message = "Feature permission is required")
    private FeaturePermission featurePermission;
    
    private Boolean isEnabled = true;
    private LocalDateTime expiresAt;
    private String notes;
    
    // Constructors
    public SinglePermissionRequest() {}
    
    public SinglePermissionRequest(String entityId, FeaturePermission featurePermission) {
        this.entityId = entityId;
        this.featurePermission = featurePermission;
    }
    
    // Getters and Setters
    public String getEntityId() {
        return entityId;
    }
    
    public void setEntityId(String entityId) {
        this.entityId = entityId;
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
}
