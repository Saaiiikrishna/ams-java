package com.example.attendancesystem.auth.model;

import jakarta.persistence.*;

/**
 * Organization entity for auth-service
 * Uses different entity name to avoid conflicts with shared library
 */
@Entity(name = "AuthOrganization")
@Table(name = "organizations")
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = true, unique = true, length = 8)
    private String entityId; // Custom 8-character ID with MSD prefix

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = true)
    private String address;

    // Optional metadata
    private Double latitude;
    private Double longitude;
    private String contactPerson;
    private String email;

    // Default constructor
    public Organization() {}

    // Constructor with essential fields
    public Organization(String name, String address) {
        this.name = name;
        this.address = address;
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

    @Override
    public String toString() {
        return "Organization{" +
                "id=" + id +
                ", entityId='" + entityId + '\'' +
                ", name='" + name + '\'' +
                ", address='" + address + '\'' +
                '}';
    }
}
