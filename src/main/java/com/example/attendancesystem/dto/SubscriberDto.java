package com.example.attendancesystem.dto;

public class SubscriberDto {
    private Long id; // For response and updates
    private String firstName;
    private String lastName;
    private String email;
    private String mobileNumber;
    private Long organizationId; // Needed for context, especially in responses if not creating
    private String nfcCardUid; // For associating/updating NFC card

    // Default constructor
    public SubscriberDto() {
    }

    // Constructor for creation (without id)
    public SubscriberDto(String firstName, String lastName, String email, String mobileNumber, String nfcCardUid) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.mobileNumber = mobileNumber;
        this.nfcCardUid = nfcCardUid;
    }

    // Constructor for response (with id and organizationId)
    public SubscriberDto(Long id, String firstName, String lastName, String email, String mobileNumber, Long organizationId, String nfcCardUid) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.mobileNumber = mobileNumber;
        this.organizationId = organizationId;
        this.nfcCardUid = nfcCardUid;
    }


    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public Long getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }

    public String getNfcCardUid() {
        return nfcCardUid;
    }

    public void setNfcCardUid(String nfcCardUid) {
        this.nfcCardUid = nfcCardUid;
    }
}
