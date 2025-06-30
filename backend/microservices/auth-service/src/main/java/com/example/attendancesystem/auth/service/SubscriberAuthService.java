package com.example.attendancesystem.auth.service;

import com.example.attendancesystem.shared.dto.SubscriberLoginDto;
import com.example.attendancesystem.shared.dto.LoginResponse;
import com.example.attendancesystem.shared.dto.NewAccessTokenResponse;
import com.example.attendancesystem.auth.security.SubscriberJwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for subscriber authentication
 * Handles mobile app authentication with gRPC communication to subscriber service
 */
@Service
public class SubscriberAuthService {

    private static final Logger logger = LoggerFactory.getLogger(SubscriberAuthService.class);

    @Autowired
    private SubscriberJwtUtil subscriberJwtUtil;

    // In-memory token blacklist for logout functionality
    private final Map<String, Long> blacklistedTokens = new ConcurrentHashMap<>();

    /**
     * Authenticate subscriber via gRPC call to subscriber service
     */
    public LoginResponse authenticateSubscriber(SubscriberLoginDto loginDto) {
        try {
            logger.debug("Authenticating subscriber with mobile: {}", loginDto.getMobileNumber());

            // TODO: Replace with actual gRPC call to subscriber service
            // For now, implement basic authentication logic

            // Validate mobile number format
            if (!isValidMobileNumber(loginDto.getMobileNumber())) {
                throw new IllegalArgumentException("Invalid mobile number format");
            }

            // Validate PIN
            if (!isValidPin(loginDto.getPin())) {
                throw new IllegalArgumentException("Invalid PIN format");
            }

            // Simulate gRPC call to subscriber service
            Map<String, Object> subscriberInfo = authenticateViaGrpc(loginDto);

            if (subscriberInfo != null) {
                // Generate JWT tokens for subscriber
                String accessToken = subscriberJwtUtil.generateToken(loginDto.getMobileNumber(), subscriberInfo);
                String refreshToken = subscriberJwtUtil.generateRefreshToken(loginDto.getMobileNumber());

                logger.info("Subscriber authentication successful for mobile: {}", loginDto.getMobileNumber());
                return new LoginResponse(accessToken, refreshToken);
            } else {
                throw new IllegalArgumentException("Invalid mobile number or PIN");
            }

        } catch (IllegalArgumentException e) {
            logger.warn("Subscriber authentication failed: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Error during subscriber authentication", e);
            throw new RuntimeException("Authentication service error", e);
        }
    }

    /**
     * Refresh subscriber token
     */
    public NewAccessTokenResponse refreshSubscriberToken(String refreshToken) {
        try {
            logger.debug("Refreshing subscriber token");

            if (!subscriberJwtUtil.isValidRefreshToken(refreshToken)) {
                logger.warn("Invalid refresh token provided");
                return null;
            }

            String mobileNumber = subscriberJwtUtil.extractUsername(refreshToken);

            // TODO: Validate subscriber still exists via gRPC call
            Map<String, Object> subscriberInfo = getSubscriberInfoViaGrpc(mobileNumber);

            if (subscriberInfo != null) {
                String newAccessToken = subscriberJwtUtil.generateToken(mobileNumber, subscriberInfo);
                String newRefreshToken = subscriberJwtUtil.generateRefreshToken(mobileNumber);

                logger.debug("Subscriber token refresh successful");
                return new NewAccessTokenResponse(newAccessToken, newRefreshToken);
            } else {
                logger.warn("Subscriber not found during token refresh");
                return null;
            }

        } catch (Exception e) {
            logger.error("Error during subscriber token refresh", e);
            return null;
        }
    }

    /**
     * Logout subscriber by blacklisting token
     */
    public boolean logoutSubscriber(String token) {
        try {
            if (subscriberJwtUtil.isValidToken(token)) {
                // Add token to blacklist
                blacklistedTokens.put(token, System.currentTimeMillis());

                // Clean up old blacklisted tokens (older than 24 hours)
                cleanupBlacklistedTokens();

                logger.debug("Subscriber logout successful");
                return true;
            }
            return false;
        } catch (Exception e) {
            logger.error("Error during subscriber logout", e);
            return false;
        }
    }

    /**
     * Validate subscriber token
     */
    public boolean validateSubscriberToken(String token) {
        try {
            // Check if token is blacklisted
            if (blacklistedTokens.containsKey(token)) {
                logger.debug("Token is blacklisted");
                return false;
            }

            // Validate token structure and expiration
            return subscriberJwtUtil.isValidToken(token);

        } catch (Exception e) {
            logger.error("Error during subscriber token validation", e);
            return false;
        }
    }

    /**
     * Change subscriber PIN
     */
    public boolean changeSubscriberPin(String token, String currentPin, String newPin) {
        try {
            if (!validateSubscriberToken(token)) {
                return false;
            }

            String mobileNumber = subscriberJwtUtil.extractUsername(token);

            // TODO: Implement via gRPC call to subscriber service
            return changeSubscriberPinViaGrpc(mobileNumber, currentPin, newPin);

        } catch (Exception e) {
            logger.error("Error during subscriber PIN change", e);
            return false;
        }
    }

    /**
     * Get subscriber profile information
     */
    public Map<String, Object> getSubscriberProfile(String token) {
        try {
            if (!validateSubscriberToken(token)) {
                return null;
            }

            String mobileNumber = subscriberJwtUtil.extractUsername(token);

            // TODO: Get profile via gRPC call to subscriber service
            return getSubscriberInfoViaGrpc(mobileNumber);

        } catch (Exception e) {
            logger.error("Error retrieving subscriber profile", e);
            return null;
        }
    }

    // Helper methods

    private boolean isValidMobileNumber(String mobileNumber) {
        return mobileNumber != null &&
               mobileNumber.matches("^[+]?[0-9]{10,15}$");
    }

    private boolean isValidPin(String pin) {
        return pin != null &&
               pin.matches("^[0-9]{4,6}$");
    }

    private void cleanupBlacklistedTokens() {
        long cutoffTime = System.currentTimeMillis() - (24 * 60 * 60 * 1000); // 24 hours
        blacklistedTokens.entrySet().removeIf(entry -> entry.getValue() < cutoffTime);
    }

    // TODO: Replace these placeholder methods with actual gRPC calls

    /**
     * Authenticate subscriber via gRPC call to subscriber service
     * This is a placeholder - replace with actual gRPC implementation
     */
    private Map<String, Object> authenticateViaGrpc(SubscriberLoginDto loginDto) {
        // Placeholder implementation - replace with actual gRPC call
        // For now, accept default PIN "0000" for any mobile number
        if ("0000".equals(loginDto.getPin())) {
            Map<String, Object> subscriberInfo = new HashMap<>();
            subscriberInfo.put("mobileNumber", loginDto.getMobileNumber());
            subscriberInfo.put("subscriberId", "SUB_" + loginDto.getMobileNumber());
            subscriberInfo.put("name", "Subscriber " + loginDto.getMobileNumber());
            subscriberInfo.put("status", "ACTIVE");
            subscriberInfo.put("organizationId", "ORG_001");
            return subscriberInfo;
        }
        return null;
    }

    /**
     * Get subscriber information via gRPC call to subscriber service
     * This is a placeholder - replace with actual gRPC implementation
     */
    private Map<String, Object> getSubscriberInfoViaGrpc(String mobileNumber) {
        // Placeholder implementation - replace with actual gRPC call
        Map<String, Object> subscriberInfo = new HashMap<>();
        subscriberInfo.put("mobileNumber", mobileNumber);
        subscriberInfo.put("subscriberId", "SUB_" + mobileNumber);
        subscriberInfo.put("name", "Subscriber " + mobileNumber);
        subscriberInfo.put("status", "ACTIVE");
        subscriberInfo.put("organizationId", "ORG_001");
        return subscriberInfo;
    }

    /**
     * Change subscriber PIN via gRPC call to subscriber service
     * This is a placeholder - replace with actual gRPC implementation
     */
    private boolean changeSubscriberPinViaGrpc(String mobileNumber, String currentPin, String newPin) {
        // Placeholder implementation - replace with actual gRPC call
        // For now, always return true if current PIN is "0000"
        return "0000".equals(currentPin);
    }

    /**
     * Legacy method for backward compatibility
     */
    public java.util.Map<String, Object> loginWithPinSimple(SubscriberLoginDto loginDto) {
        try {
            LoginResponse response = authenticateSubscriber(loginDto);
            Map<String, Object> result = new HashMap<>();
            result.put("accessToken", response.getJwt());
            result.put("refreshToken", response.getRefreshToken());
            result.put("success", true);
            return result;
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", e.getMessage());
            return result;
        }
    }

    /**
     * Get subscriber details from token
     */
    public Object getSubscriberFromToken(String token) {
        try {
            if (!validateSubscriberToken(token)) {
                return null;
            }

            String mobileNumber = subscriberJwtUtil.extractUsername(token);
            return getSubscriberInfoViaGrpc(mobileNumber);

        } catch (Exception e) {
            logger.error("Error getting subscriber from token", e);
            return null;
        }
    }
}
