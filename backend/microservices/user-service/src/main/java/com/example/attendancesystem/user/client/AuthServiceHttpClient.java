package com.example.attendancesystem.user.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * HTTP Client for Auth Service
 * Handles authentication-related operations via REST API
 */
@Service
public class AuthServiceHttpClient {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceHttpClient.class);

    @Value("${auth.service.url:http://localhost:8081}")
    private String authServiceUrl;

    private final RestTemplate restTemplate;

    public AuthServiceHttpClient() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Create Entity Admin in Auth Service for authentication
     * This ensures EntityAdmins can login after being created in User Service
     */
    public boolean createEntityAdminForAuth(String username, String password, Long organizationId, String superAdminToken) {
        try {
            logger.info("Creating Entity Admin in Auth Service: {} for organization: {}", username, organizationId);
            
            // Prepare request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("username", username);
            requestBody.put("password", password);
            requestBody.put("organizationId", organizationId);

            // Prepare headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(superAdminToken);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            // Call Auth Service endpoint
            String url = authServiceUrl + "/auth/super/entity-admins";
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, 
                    HttpMethod.POST, 
                    request, 
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.CREATED) {
                logger.info("Successfully created Entity Admin in Auth Service: {}", username);
                return true;
            } else {
                logger.warn("Failed to create Entity Admin in Auth Service: {} - Status: {}", 
                           username, response.getStatusCode());
                return false;
            }
            
        } catch (Exception e) {
            logger.error("Error creating Entity Admin in Auth Service: {} - {}", username, e.getMessage());
            return false;
        }
    }

    /**
     * Check if Auth Service is available
     */
    public boolean isAuthServiceAvailable() {
        try {
            String url = authServiceUrl + "/auth/actuator/health";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            logger.error("Auth Service is not available: {}", e.getMessage());
            return false;
        }
    }
}
