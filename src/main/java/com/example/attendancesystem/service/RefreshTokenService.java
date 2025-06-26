// src/main/java/com/example/attendancesystem/service/RefreshTokenService.java
package com.example.attendancesystem.subscriber.service;

import com.example.attendancesystem.model.EntityAdmin;
import com.example.attendancesystem.model.RefreshToken;
import com.example.attendancesystem.repository.EntityAdminRepository;
import com.example.attendancesystem.repository.RefreshTokenRepository;
import com.example.attendancesystem.security.JwtUtil;
import com.example.attendancesystem.security.SuperAdminJwtUtil;
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
    private EntityAdminRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private SuperAdminJwtUtil superAdminJwtUtil;

    @Transactional
    public RefreshToken createAndSaveRefreshToken(String username, String tokenString) {
        EntityAdmin user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username + " for refresh token creation"));

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

        RefreshToken refreshToken = new RefreshToken(tokenString, user, expiryDate);
        return refreshTokenRepository.save(refreshToken);
    }

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    @Transactional
    public void deleteToken(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(refreshTokenRepository::delete);
    }

    @Transactional
    public void deleteUserTokens(EntityAdmin user) {
        refreshTokenRepository.deleteByUser(user);
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
}
