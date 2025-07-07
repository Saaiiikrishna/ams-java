package com.example.attendancesystem.auth.controller;

import com.example.attendancesystem.auth.dto.SubscriberLoginDto;
import com.example.attendancesystem.auth.dto.LoginResponse;
import com.example.attendancesystem.auth.dto.RefreshTokenRequest;
import com.example.attendancesystem.auth.dto.NewAccessTokenResponse;
import com.example.attendancesystem.auth.dto.ErrorResponse;
import com.example.attendancesystem.auth.dto.SuccessResponse;
import com.example.attendancesystem.auth.service.SubscriberAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Map;

/**
 * Subscriber Authentication Controller
 * Handles authentication for mobile app users (subscribers)
 * Supports mobile number + PIN authentication
 */
@RestController
@RequestMapping("/api/subscriber/auth")
@CrossOrigin(origins = "*") // Allow mobile apps from any origin
public class SubscriberAuthController {

    private static final Logger logger = LoggerFactory.getLogger(SubscriberAuthController.class);

    @Autowired
    private SubscriberAuthService subscriberAuthService;

    /**
     * Subscriber login with mobile number and PIN
     * Default PIN is "0000" for new subscribers
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody SubscriberLoginDto loginRequest) {
        try {
            logger.info("Subscriber login attempt for mobile: {}", loginRequest.getMobileNumber());
            
            // Validate input
            if (loginRequest.getMobileNumber() == null || loginRequest.getMobileNumber().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ErrorResponse("INVALID_INPUT", "Mobile number is required", 400));
            }
            
            if (loginRequest.getPin() == null || loginRequest.getPin().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ErrorResponse("INVALID_INPUT", "PIN is required", 400));
            }

            // Authenticate subscriber
            LoginResponse response = subscriberAuthService.authenticateSubscriber(loginRequest);
            
            if (response != null) {
                logger.info("Subscriber login successful for mobile: {}", loginRequest.getMobileNumber());
                return ResponseEntity.ok(response);
            } else {
                logger.warn("Subscriber login failed for mobile: {}", loginRequest.getMobileNumber());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("AUTHENTICATION_FAILED", "Invalid mobile number or PIN", 401));
            }

        } catch (IllegalArgumentException e) {
            logger.warn("Subscriber login failed - invalid credentials: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("AUTHENTICATION_FAILED", e.getMessage(), 401));
        } catch (Exception e) {
            logger.error("Error during subscriber authentication", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("AUTHENTICATION_ERROR", "Authentication service temporarily unavailable", 500));
        }
    }

    /**
     * Refresh subscriber token
     */
    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        try {
            logger.debug("Subscriber token refresh request");
            
            if (request.getRefreshToken() == null || request.getRefreshToken().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ErrorResponse("INVALID_INPUT", "Refresh token is required", 400));
            }

            NewAccessTokenResponse response = subscriberAuthService.refreshSubscriberToken(request.getRefreshToken());
            
            if (response != null) {
                logger.debug("Subscriber token refresh successful");
                return ResponseEntity.ok(response);
            } else {
                logger.warn("Subscriber token refresh failed - invalid token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("TOKEN_INVALID", "Invalid or expired refresh token", 401));
            }

        } catch (Exception e) {
            logger.error("Error during subscriber token refresh", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("TOKEN_REFRESH_ERROR", "Token refresh service temporarily unavailable", 500));
        }
    }

    /**
     * Subscriber logout
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String authHeader) {
        try {
            logger.debug("Subscriber logout request");
            
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ErrorResponse("INVALID_HEADER", "Valid Authorization header required", 400));
            }

            String token = authHeader.substring(7);
            boolean success = subscriberAuthService.logoutSubscriber(token);
            
            if (success) {
                logger.debug("Subscriber logout successful");
                return ResponseEntity.ok(new SuccessResponse("Logout successful"));
            } else {
                logger.warn("Subscriber logout failed - invalid token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("LOGOUT_FAILED", "Invalid token", 401));
            }

        } catch (Exception e) {
            logger.error("Error during subscriber logout", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("LOGOUT_ERROR", "Logout service temporarily unavailable", 500));
        }
    }

    /**
     * Validate subscriber token
     */
    @PostMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String authHeader) {
        try {
            logger.debug("Subscriber token validation request");
            
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ErrorResponse("INVALID_HEADER", "Valid Authorization header required", 400));
            }

            String token = authHeader.substring(7);
            boolean isValid = subscriberAuthService.validateSubscriberToken(token);
            
            if (isValid) {
                return ResponseEntity.ok(new SuccessResponse("Token is valid", 
                    Map.of("valid", true, "timestamp", System.currentTimeMillis())));
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("TOKEN_INVALID", "Token is invalid or expired", 401));
            }

        } catch (Exception e) {
            logger.error("Error during subscriber token validation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("VALIDATION_ERROR", "Token validation service temporarily unavailable", 500));
        }
    }

    /**
     * Change subscriber PIN
     */
    @PostMapping("/change-pin")
    public ResponseEntity<?> changePin(@RequestHeader("Authorization") String authHeader,
                                      @RequestBody ChangePinRequest request) {
        try {
            logger.debug("Subscriber PIN change request");
            
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ErrorResponse("INVALID_HEADER", "Valid Authorization header required", 400));
            }

            String token = authHeader.substring(7);
            boolean success = subscriberAuthService.changeSubscriberPin(token, request.getCurrentPin(), request.getNewPin());
            
            if (success) {
                logger.info("Subscriber PIN changed successfully");
                return ResponseEntity.ok(new SuccessResponse("PIN changed successfully"));
            } else {
                logger.warn("Subscriber PIN change failed");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ErrorResponse("PIN_CHANGE_FAILED", "Current PIN is incorrect", 400));
            }

        } catch (Exception e) {
            logger.error("Error during subscriber PIN change", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("PIN_CHANGE_ERROR", "PIN change service temporarily unavailable", 500));
        }
    }

    /**
     * Get subscriber profile information
     */
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@RequestHeader("Authorization") String authHeader) {
        try {
            logger.debug("Subscriber profile request");
            
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ErrorResponse("INVALID_HEADER", "Valid Authorization header required", 400));
            }

            String token = authHeader.substring(7);
            Map<String, Object> profile = subscriberAuthService.getSubscriberProfile(token);
            
            if (profile != null) {
                return ResponseEntity.ok(new SuccessResponse("Profile retrieved successfully", profile));
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("PROFILE_ACCESS_DENIED", "Invalid token or access denied", 401));
            }

        } catch (Exception e) {
            logger.error("Error retrieving subscriber profile", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("PROFILE_ERROR", "Profile service temporarily unavailable", 500));
        }
    }

    /**
     * DTO for PIN change request
     */
    public static class ChangePinRequest {
        private String currentPin;
        private String newPin;

        public String getCurrentPin() { return currentPin; }
        public void setCurrentPin(String currentPin) { this.currentPin = currentPin; }

        public String getNewPin() { return newPin; }
        public void setNewPin(String newPin) { this.newPin = newPin; }
    }
}
