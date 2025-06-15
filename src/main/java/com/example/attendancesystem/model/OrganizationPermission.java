package com.example.attendancesystem.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "organization_permissions", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"organization_id", "feature_permission"}))
public class OrganizationPermission {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "feature_permission", nullable = false)
    private FeaturePermission featurePermission;
    
    @Column(name = "is_enabled", nullable = false)
    private Boolean isEnabled = true;
    
    @Column(name = "granted_by")
    private String grantedBy; // Super admin who granted the permission
    
    @Column(name = "granted_at")
    private LocalDateTime grantedAt;
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt; // Optional expiration date
    
    @Column(name = "notes")
    private String notes; // Optional notes about the permission
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructors
    public OrganizationPermission() {}
    
    public OrganizationPermission(Organization organization, FeaturePermission featurePermission, String grantedBy) {
        this.organization = organization;
        this.featurePermission = featurePermission;
        this.grantedBy = grantedBy;
        this.isEnabled = true;
        this.grantedAt = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    // Lifecycle callbacks
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (grantedAt == null) {
            grantedAt = LocalDateTime.now();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Helper methods
    public boolean isActive() {
        if (!isEnabled) {
            return false;
        }
        if (expiresAt != null && expiresAt.isBefore(LocalDateTime.now())) {
            return false;
        }
        return true;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Organization getOrganization() {
        return organization;
    }
    
    public void setOrganization(Organization organization) {
        this.organization = organization;
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
    
    @Override
    public String toString() {
        return "OrganizationPermission{" +
                "id=" + id +
                ", featurePermission=" + featurePermission +
                ", isEnabled=" + isEnabled +
                ", grantedBy='" + grantedBy + '\'' +
                ", grantedAt=" + grantedAt +
                '}';
    }
}
