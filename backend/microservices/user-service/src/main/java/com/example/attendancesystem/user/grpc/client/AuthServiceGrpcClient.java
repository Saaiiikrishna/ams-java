package com.example.attendancesystem.user.grpc.client;

import com.example.attendancesystem.grpc.auth.*;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * gRPC Client for Auth Service
 * Handles authentication-related operations from User Service
 */
@Service
public class AuthServiceGrpcClient {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceGrpcClient.class);

    @GrpcClient("auth-service")
    private AuthServiceGrpc.AuthServiceBlockingStub authServiceStub;

    /**
     * Create Entity Admin in Auth Service for authentication
     * This ensures EntityAdmins can login after being created in User Service
     */
    public boolean createEntityAdminForAuth(String username, String password, Long organizationId) {
        try {
            logger.info("Creating Entity Admin in Auth Service: {} for organization: {}", username, organizationId);
            
            // Create request for auth service
            CreateEntityAdminForAuthRequest request = CreateEntityAdminForAuthRequest.newBuilder()
                    .setUsername(username)
                    .setPassword(password)
                    .setOrganizationId(organizationId)
                    .build();
            
            CreateEntityAdminForAuthResponse response = authServiceStub.createEntityAdminForAuth(request);
            
            if (response.getSuccess()) {
                logger.info("Successfully created Entity Admin in Auth Service: {} (ID: {})", 
                           username, response.getEntityAdminId());
                return true;
            } else {
                logger.warn("Failed to create Entity Admin in Auth Service: {} - {}", username, response.getMessage());
                return false;
            }
            
        } catch (Exception e) {
            logger.error("Error creating Entity Admin in Auth Service: {} - {}", username, e.getMessage());
            return false;
        }
    }



    /**
     * Hash password using Auth Service gRPC
     * This ensures consistent password hashing across services
     */
    public String hashPassword(String originalPassword) {
        try {
            logger.info("Requesting password hash from Auth Service via gRPC");

            // Create request for password hashing
            HashPasswordRequest request = HashPasswordRequest.newBuilder()
                    .setPassword(originalPassword)
                    .build();

            HashPasswordResponse response = authServiceStub.hashPassword(request);

            if (response.getSuccess()) {
                logger.info("Successfully hashed password via Auth Service gRPC");
                return response.getHashedPassword();
            } else {
                logger.warn("Failed to hash password via Auth Service gRPC: {}", response.getMessage());
                return null;
            }

        } catch (Exception e) {
            logger.error("Error hashing password via Auth Service gRPC: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Validate if Auth Service is available
     */
    public boolean isAuthServiceAvailable() {
        try {
            // Simple ping to check if auth service is available
            // We can implement a health check method in auth service if needed
            return true;
        } catch (Exception e) {
            logger.error("Auth Service is not available: {}", e.getMessage());
            return false;
        }
    }
}
