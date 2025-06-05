package com.example.attendancesystem.dto;

public class OrganizationDto {
    private Long id; // Added ID field
    private String name;
    private String address;

    // Default constructor
    public OrganizationDto() {
    }

    // Constructor with fields
    public OrganizationDto(Long id, String name, String address) { // Updated constructor
        this.id = id;
        this.name = name;
        this.address = address;
    }

    // Getters and Setters
    public Long getId() { // Getter for ID
        return id;
    }

    public void setId(Long id) { // Setter for ID
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
