// src/main/java/com/example/attendancesystem/model/RefreshToken.java
package com.example.attendancesystem.auth.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true, length = 1024) // Ensure length is sufficient for JWT
    private String token;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String username; // Store username instead of foreign key for microservices architecture

    @NotNull
    @Column(name = "admin_id", nullable = false)
    private Long adminId; // Foreign key to entity_admins table

    @NotNull
    @Column(nullable = false)
    private Instant expiryDate;

    public RefreshToken() {}

    public RefreshToken(String token, String username, Long adminId, Instant expiryDate) {
        this.token = token;
        this.username = username;
        this.adminId = adminId;
        this.expiryDate = expiryDate;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public Long getAdminId() { return adminId; }
    public void setAdminId(Long adminId) { this.adminId = adminId; }

    public Instant getExpiryDate() { return expiryDate; }
    public void setExpiryDate(Instant expiryDate) { this.expiryDate = expiryDate; }
}
