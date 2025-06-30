package com.example.attendancesystem.dto;

public class EntityAdminDto {
    private String username;
    private String password;
    private Long organizationId;

    // Default constructor
    public EntityAdminDto() {
    }

    // Constructor with fields
    public EntityAdminDto(String username, String password, Long organizationId) {
        this.username = username;
        this.password = password;
        this.organizationId = organizationId;
    }

    // Getters and Setters
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

    public Long getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }
}
