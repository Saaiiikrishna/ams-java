package com.example.attendancesystem.auth.controller;

import com.example.attendancesystem.shared.dto.LoginRequest;
import com.example.attendancesystem.shared.dto.RefreshTokenRequest;
import com.example.attendancesystem.auth.service.AuthenticationService;
import com.example.attendancesystem.auth.service.RefreshTokenService;
import com.example.attendancesystem.auth.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Map;
import java.util.HashMap;

/**
 * Modern Authentication Controller
 * Integrates with User Service for authentication
 * Provides JWT-based authentication endpoints
 */
@RestController
@RequestMapping("/api/v2/auth")
@CrossOrigin(origins = "*")
public class ModernAuthController {

    private static final Logger logger = LoggerFactory.getLogger(ModernAuthController.class);

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * Authenticate user and return JWT tokens
     * Integrates with User Service for user validation
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest loginRequest) {
        logger.info("Login attempt for user: {}", loginRequest.getUsername());
        
        try {
            Map<String, Object> authResult = authenticationService.authenticate(loginRequest);
            
            if ((Boolean) authResult.get("success")) {
                logger.info("Login successful for user: {}", loginRequest.getUsername());
                return ResponseEntity.ok(authResult);
            } else {
                Integer statusCode = (Integer) authResult.get("statusCode");
                HttpStatus status = HttpStatus.valueOf(statusCode != null ? statusCode : 401);
                return ResponseEntity.status(status).body(authResult);
            }
            
        } catch (Exception e) {
            logger.error("Login failed for user: {}", loginRequest.getUsername(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("errorCode", "LOGIN_ERROR");
            errorResponse.put("message", "Login failed due to server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Refresh access token using refresh token
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshToken(@Valid @RequestBody RefreshTokenRequest refreshRequest) {
        logger.info("Token refresh attempt");
        
        try {
            String refreshToken = refreshRequest.getRefreshToken();
            
            // Validate refresh token
            if (!jwtUtil.isTokenValid(refreshToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(createErrorResponse(
                    "INVALID_REFRESH_TOKEN", "Invalid or expired refresh token", 401));
            }
            
            // Extract user information from refresh token
            String username = jwtUtil.extractUsername(refreshToken);
            String userType = jwtUtil.extractUserType(refreshToken);
            Long userId = jwtUtil.extractUserId(refreshToken);
            String tokenType = jwtUtil.extractTokenType(refreshToken);
            
            // Verify it's a refresh token
            if (!"REFRESH".equals(tokenType)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(createErrorResponse(
                    "INVALID_TOKEN_TYPE", "Token is not a refresh token", 401));
            }
            
            // Verify refresh token exists in database
            if (!refreshTokenService.isRefreshTokenValid(refreshToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(createErrorResponse(
                    "REFRESH_TOKEN_NOT_FOUND", "Refresh token not found or revoked", 401));
            }
            
            // Generate new access token
            String newAccessToken = jwtUtil.generateTokenForUser(username, userType, userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("accessToken", newAccessToken);
            response.put("userType", userType);
            response.put("userId", userId);
            response.put("username", username);
            
            logger.info("Token refresh successful for user: {}", username);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Token refresh failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createErrorResponse(
                "REFRESH_ERROR", "Token refresh failed due to server error", 500));
        }
    }

    /**
     * Logout user and invalidate tokens
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(@RequestHeader("Authorization") String authHeader) {
        logger.info("Logout attempt");
        
        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                String username = jwtUtil.extractUsername(token);
                
                // Invalidate all refresh tokens for the user
                refreshTokenService.revokeAllRefreshTokensForUser(username);
                
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Logout successful");
                
                logger.info("Logout successful for user: {}", username);
                return ResponseEntity.ok(response);
            }
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(createErrorResponse(
                "INVALID_TOKEN", "Invalid authorization header", 400));
            
        } catch (Exception e) {
            logger.error("Logout failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createErrorResponse(
                "LOGOUT_ERROR", "Logout failed due to server error", 500));
        }
    }

    /**
     * Validate token endpoint
     */
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateToken(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                
                if (jwtUtil.isTokenValid(token)) {
                    String username = jwtUtil.extractUsername(token);
                    String userType = jwtUtil.extractUserType(token);
                    Long userId = jwtUtil.extractUserId(token);
                    
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("valid", true);
                    response.put("username", username);
                    response.put("userType", userType);
                    response.put("userId", userId);
                    
                    return ResponseEntity.ok(response);
                }
            }
            
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(createErrorResponse(
                "INVALID_TOKEN", "Token is invalid or expired", 401));
            
        } catch (Exception e) {
            logger.error("Token validation failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createErrorResponse(
                "VALIDATION_ERROR", "Token validation failed", 500));
        }
    }

    /**
     * Get current user info from token
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                
                if (jwtUtil.isTokenValid(token)) {
                    String username = jwtUtil.extractUsername(token);
                    String userType = jwtUtil.extractUserType(token);
                    Long userId = jwtUtil.extractUserId(token);
                    
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("username", username);
                    response.put("userType", userType);
                    response.put("userId", userId);
                    
                    return ResponseEntity.ok(response);
                }
            }
            
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(createErrorResponse(
                "INVALID_TOKEN", "Token is invalid or expired", 401));
            
        } catch (Exception e) {
            logger.error("Get current user failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createErrorResponse(
                "USER_INFO_ERROR", "Failed to get user information", 500));
        }
    }

    /**
     * Hash password endpoint (for User Service to call)
     */
    @PostMapping("/hash-password")
    public ResponseEntity<Map<String, Object>> hashPassword(@RequestBody Map<String, String> request) {
        try {
            String plainPassword = request.get("password");
            if (plainPassword == null || plainPassword.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(createErrorResponse(
                    "INVALID_PASSWORD", "Password cannot be empty", 400));
            }
            
            String hashedPassword = authenticationService.hashPassword(plainPassword);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("hashedPassword", hashedPassword);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Password hashing failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createErrorResponse(
                "HASH_ERROR", "Password hashing failed", 500));
        }
    }

    /**
     * Create error response
     */
    private Map<String, Object> createErrorResponse(String errorCode, String message, int statusCode) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("errorCode", errorCode);
        response.put("message", message);
        response.put("statusCode", statusCode);
        return response;
    }
}
