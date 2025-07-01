package com.example.attendancesystem.attendance.dto;

/**
 * DTO for Organization data from Organization Service
 * Contains only fields needed by Attendance Service
 */
public class OrganizationDto {
    private Long id;
    private String name;
    private String description;
    private String entityId;
    private String address;
    private String contactEmail;
    private String contactPhone;
    private Boolean active;
    
    // Constructors
    public OrganizationDto() {}
    
    public OrganizationDto(Long id, String name, String description, String entityId,
                          String address, String contactEmail, String contactPhone, Boolean active) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.entityId = entityId;
        this.address = address;
        this.contactEmail = contactEmail;
        this.contactPhone = contactPhone;
        this.active = active;
    }
    
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
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getEntityId() {
        return entityId;
    }
    
    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
    
    public String getContactEmail() {
        return contactEmail;
    }
    
    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }
    
    public String getContactPhone() {
        return contactPhone;
    }
    
    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }
    
    public Boolean getActive() {
        return active;
    }
    
    public void setActive(Boolean active) {
        this.active = active;
    }
    
    @Override
    public String toString() {
        return "OrganizationDto{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", entityId='" + entityId + '\'' +
                ", address='" + address + '\'' +
                ", contactEmail='" + contactEmail + '\'' +
                ", contactPhone='" + contactPhone + '\'' +
                ", active=" + active +
                '}';
    }
}
