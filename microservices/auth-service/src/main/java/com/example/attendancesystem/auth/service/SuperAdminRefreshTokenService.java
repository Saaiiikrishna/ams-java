package com.example.attendancesystem.auth.service;

import com.example.attendancesystem.auth.model.SuperAdmin;
import com.example.attendancesystem.auth.model.SuperAdminRefreshToken;
import com.example.attendancesystem.auth.repository.SuperAdminRefreshTokenRepository;
import com.example.attendancesystem.auth.repository.SuperAdminRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
public class SuperAdminRefreshTokenService {

    private static final Logger logger = LoggerFactory.getLogger(SuperAdminRefreshTokenService.class);

    @Autowired
    private SuperAdminRefreshTokenRepository refreshTokenRepository;

    @Autowired
    private SuperAdminRepository superAdminRepository;

    // Refresh token expiry: 7 days
    private static final long REFRESH_TOKEN_EXPIRY_DAYS = 7;

    @Transactional
    public void createAndSaveRefreshToken(String username, String refreshToken) {
        try {
            logger.debug("Creating refresh token for SuperAdmin: {}", username);

            SuperAdmin superAdmin = superAdminRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("SuperAdmin not found: " + username));

            // Delete any existing refresh tokens for this user
            refreshTokenRepository.deleteByUser(superAdmin);

            // Create new refresh token
            Instant expiryDate = Instant.now().plus(REFRESH_TOKEN_EXPIRY_DAYS, ChronoUnit.DAYS);
            SuperAdminRefreshToken refreshTokenEntity = new SuperAdminRefreshToken(
                    refreshToken,
                    superAdmin,
                    expiryDate
            );

            refreshTokenRepository.save(refreshTokenEntity);
            logger.info("Refresh token created successfully for SuperAdmin: {}", username);
        } catch (Exception e) {
            logger.error("Failed to create refresh token for SuperAdmin: {}", username, e);
            throw new RuntimeException("Failed to create refresh token", e);
        }
    }

    public Optional<SuperAdminRefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    public boolean verifyExpiration(SuperAdminRefreshToken token) {
        if (token.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(token);
            return false;
        }
        return true;
    }

    @Transactional
    public void deleteToken(String token) {
        refreshTokenRepository.deleteByToken(token);
    }

    @Transactional
    public void deleteByUser(SuperAdmin user) {
        refreshTokenRepository.deleteByUser(user);
    }
}
