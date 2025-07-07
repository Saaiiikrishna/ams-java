// src/main/java/com/example/attendancesystem/service/RefreshTokenService.java
package com.example.attendancesystem.auth.service;

import com.example.attendancesystem.auth.model.RefreshToken;
import com.example.attendancesystem.auth.repository.RefreshTokenRepository;
import com.example.attendancesystem.auth.repository.EntityAdminRepository;
import com.example.attendancesystem.auth.model.EntityAdmin;
import com.example.attendancesystem.auth.security.JwtUtil;
import com.example.attendancesystem.auth.security.SuperAdminJwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Date; // Required for JwtUtil.extractExpiration comparison
import java.util.Optional;

@Service
public class RefreshTokenService {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private EntityAdminRepository entityAdminRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private SuperAdminJwtUtil superAdminJwtUtil;

    @Transactional
    public RefreshToken createAndSaveRefreshToken(String username, String tokenString) {
        // Note: In microservices architecture, we don't store user data locally in Auth Service
        // Users are managed by the User Service. We only store refresh tokens here.

        // Determine which JWT utility to use based on token type
        Instant expiryDate;
        try {
            // Try Super Admin JWT utility first
            if (superAdminJwtUtil.isSuperAdminToken(tokenString)) {
                expiryDate = superAdminJwtUtil.extractExpiration(tokenString).toInstant();
            } else {
                // Fall back to Entity Admin JWT utility
                expiryDate = jwtUtil.extractExpiration(tokenString).toInstant();
            }
        } catch (Exception e) {
            // If both fail, try the other one as fallback
            try {
                expiryDate = jwtUtil.extractExpiration(tokenString).toInstant();
            } catch (Exception e2) {
                expiryDate = superAdminJwtUtil.extractExpiration(tokenString).toInstant();
            }
        }

        // Get Entity Admin ID for the refresh token
        Long adminId = null;
        try {
            Optional<EntityAdmin> entityAdmin = entityAdminRepository.findByUsername(username);
            if (entityAdmin.isPresent()) {
                adminId = entityAdmin.get().getId();
            }
        } catch (Exception e) {
            // If Entity Admin not found, this might be a Super Admin token
            // Super Admin tokens use a different table, so we can skip this
        }

        // Only create refresh token if we have a valid admin ID
        if (adminId != null) {
            RefreshToken refreshToken = new RefreshToken(tokenString, username, adminId, expiryDate);
            return refreshTokenRepository.save(refreshToken);
        } else {
            throw new RuntimeException("Cannot create refresh token: Entity Admin not found for username: " + username);
        }
    }

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    @Transactional
    public void deleteToken(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(refreshTokenRepository::delete);
    }

    @Transactional
    public void deleteUserTokens(String username) {
        refreshTokenRepository.deleteByUsername(username);
    }

    public boolean verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(token);
            return false;
        }
        // Redundant check if DB stores expiry accurately from token, but good as a safeguard.
        try {
            Date tokenExpiration;
            // Determine which JWT utility to use based on token type
            if (superAdminJwtUtil.isSuperAdminToken(token.getToken())) {
                tokenExpiration = superAdminJwtUtil.extractExpiration(token.getToken());
            } else {
                tokenExpiration = jwtUtil.extractExpiration(token.getToken());
            }

            if (tokenExpiration.before(new Date(System.currentTimeMillis()))) {
                refreshTokenRepository.delete(token);
                return false;
            }
        } catch (Exception e) {
            // If token parsing fails, consider it expired
            refreshTokenRepository.delete(token);
            return false;
        }
        return true;
    }

    // Example: @Scheduled(cron = "0 0 5 * * ?") // Run daily at 5 AM
    @Transactional
    public void purgeExpiredTokens() {
        // System.out.println("Purging expired refresh tokens at: " + Instant.now()); // For logging
        refreshTokenRepository.deleteByExpiryDateBefore(Instant.now());
        // Potentially log count of deleted tokens
    }

    /**
     * Check if refresh token is valid and exists in database
     */
    public boolean isRefreshTokenValid(String tokenString) {
        try {
            Optional<RefreshToken> tokenOpt = refreshTokenRepository.findByToken(tokenString);
            if (tokenOpt.isPresent()) {
                RefreshToken token = tokenOpt.get();
                return token.getExpiryDate().isAfter(Instant.now());
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Revoke all refresh tokens for a user
     */
    @Transactional
    public void revokeAllRefreshTokensForUser(String username) {
        try {
            refreshTokenRepository.deleteByUsername(username);
        } catch (Exception e) {
            // Log error but don't throw exception
            System.err.println("Failed to revoke refresh tokens for user: " + username + " - " + e.getMessage());
        }
    }
}
