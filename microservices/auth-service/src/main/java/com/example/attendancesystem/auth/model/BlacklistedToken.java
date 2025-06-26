package com.example.attendancesystem.auth.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "blacklisted_tokens")
public class BlacklistedToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 1024)
    private String tokenHash; // Store hash of token for security

    @Column(nullable = false)
    private String username; // For tracking which user's token was blacklisted

    @Column(nullable = false)
    private Instant blacklistedAt;

    @Column(nullable = false)
    private Instant expiresAt; // When the original token would expire

    @Column(length = 100)
    private String reason; // Why the token was blacklisted

    // Constructors
    public BlacklistedToken() {}

    public BlacklistedToken(String tokenHash, String username, Instant expiresAt, String reason) {
        this.tokenHash = tokenHash;
        this.username = username;
        this.blacklistedAt = Instant.now();
        this.expiresAt = expiresAt;
        this.reason = reason;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Instant getBlacklistedAt() {
        return blacklistedAt;
    }

    public void setBlacklistedAt(Instant blacklistedAt) {
        this.blacklistedAt = blacklistedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
