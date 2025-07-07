package com.example.attendancesystem.auth.service;

import com.example.attendancesystem.auth.dto.LoginRequest;
import com.example.attendancesystem.auth.dto.LoginResponse;
import com.example.attendancesystem.auth.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.HashMap;

/**
 * Authentication Service
 * Integrates with User Service for authentication
 * Handles JWT token generation and validation
 */
@Service
public class AuthenticationService {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RefreshTokenService refreshTokenService;

    private final RestTemplate restTemplate = new RestTemplate();
    private final String userServiceUrl = "http://ams-user-service:8083/user/api/users";

    /**
     * Authenticate user and generate JWT tokens
     */
    public Map<String, Object> authenticate(LoginRequest loginRequest) {
        try {
            // Step 1: Get user from User Service by username
            Map<String, Object> userResponse = getUserByUsername(loginRequest.getUsername());
            
            if (userResponse == null || !(Boolean) userResponse.get("success")) {
                return createErrorResponse("USER_NOT_FOUND", "User not found", 404);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> user = (Map<String, Object>) userResponse.get("user");
            
            // Step 2: Verify password
            String storedPassword = (String) user.get("password");
            if (!passwordEncoder.matches(loginRequest.getPassword(), storedPassword)) {
                return createErrorResponse("INVALID_CREDENTIALS", "Invalid username or password", 401);
            }

            // Step 3: Check if user is active
            Boolean isActive = (Boolean) user.get("isActive");
            if (isActive == null || !isActive) {
                return createErrorResponse("ACCOUNT_DISABLED", "Account is disabled", 403);
            }

            // Step 4: Generate JWT tokens
            String username = (String) user.get("username");
            String userType = (String) user.get("userType");
            Long userId = ((Number) user.get("id")).longValue();

            String accessToken = jwtUtil.generateTokenForUser(username, userType, userId);
            String refreshToken = jwtUtil.generateRefreshTokenForUser(username, userType, userId);

            // Step 5: Save refresh token
            refreshTokenService.createAndSaveRefreshToken(username, refreshToken);

            // Step 6: Return success response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("accessToken", accessToken);
            response.put("refreshToken", refreshToken);
            response.put("userType", userType);
            response.put("userId", userId);
            response.put("username", username);

            logger.info("User authenticated successfully: {}", username);
            return response;

        } catch (Exception e) {
            logger.error("Authentication failed for user: {}", loginRequest.getUsername(), e);
            return createErrorResponse("AUTHENTICATION_ERROR", "Authentication failed", 500);
        }
    }

    /**
     * Validate user credentials without generating tokens
     */
    public Map<String, Object> validateCredentials(String username, String password) {
        try {
            Map<String, Object> userResponse = getUserByUsername(username);
            
            if (userResponse == null || !(Boolean) userResponse.get("success")) {
                return createErrorResponse("USER_NOT_FOUND", "User not found", 404);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> user = (Map<String, Object>) userResponse.get("user");
            
            String storedPassword = (String) user.get("password");
            if (!passwordEncoder.matches(password, storedPassword)) {
                return createErrorResponse("INVALID_CREDENTIALS", "Invalid credentials", 401);
            }

            Boolean isActive = (Boolean) user.get("isActive");
            if (isActive == null || !isActive) {
                return createErrorResponse("ACCOUNT_DISABLED", "Account is disabled", 403);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("user", user);
            return response;

        } catch (Exception e) {
            logger.error("Credential validation failed for user: {}", username, e);
            return createErrorResponse("VALIDATION_ERROR", "Credential validation failed", 500);
        }
    }

    /**
     * Get user by username from User Service
     */
    private Map<String, Object> getUserByUsername(String username) {
        try {
            String url = userServiceUrl + "/auth/username/" + username;
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            }

            return null;
        } catch (Exception e) {
            logger.error("Failed to get user from User Service: {}", username, e);
            return null;
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

    /**
     * Hash password for user creation
     */
    public String hashPassword(String plainPassword) {
        return passwordEncoder.encode(plainPassword);
    }

    /**
     * Verify if a user has specific permission
     */
    public boolean hasPermission(Long userId, String permission) {
        try {
            String url = userServiceUrl + "/" + userId + "/permissions/" + permission + "/check";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                Map<String, Object> body = response.getBody();
                return body != null && (Boolean) body.getOrDefault("hasPermission", false);
            }
            
            return false;
        } catch (Exception e) {
            logger.error("Failed to check permission for user: {} permission: {}", userId, permission, e);
            return false;
        }
    }
}
