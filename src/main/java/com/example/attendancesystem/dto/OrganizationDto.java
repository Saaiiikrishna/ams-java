package com.example.attendancesystem.dto;

public class OrganizationDto {
    private Long id; // Added ID field
    private String name;
    private String address;

    private Double latitude;
    private Double longitude;
    private String contactPerson;
    private String email;

    // Default constructor
    public OrganizationDto() {
    }

    // Constructor with fields
    public OrganizationDto(Long id, String name, String address) {
        this.id = id;
        this.name = name;
        this.address = address;
    }

    public OrganizationDto(String name, String address) {
        this.name = name;
        this.address = address;
    }

    public OrganizationDto(String name, String address, Double latitude, Double longitude,
                            String contactPerson, String email) {
        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.contactPerson = contactPerson;
        this.email = email;
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

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
