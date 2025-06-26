package com.example.attendancesystem.subscriber.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

@Entity
@Table(name = "super_admin_refresh_tokens")
public class SuperAdminRefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true, length = 1024)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "super_admin_id", nullable = false)
    @NotNull
    private SuperAdmin user;

    @NotNull
    @Column(nullable = false)
    private Instant expiryDate;

    public SuperAdminRefreshToken() {}

    public SuperAdminRefreshToken(String token, SuperAdmin user, Instant expiryDate) {
        this.token = token;
        this.user = user;
        this.expiryDate = expiryDate;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public SuperAdmin getUser() { return user; }
    public void setUser(SuperAdmin user) { this.user = user; }

    public Instant getExpiryDate() { return expiryDate; }
    public void setExpiryDate(Instant expiryDate) { this.expiryDate = expiryDate; }
}
