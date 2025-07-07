package com.example.attendancesystem.auth.grpc.client;

import com.example.attendancesystem.grpc.user.*;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * gRPC Client for User Service
 * Handles authentication-related user operations via gRPC
 */
@Service
public class UserServiceGrpcClient {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceGrpcClient.class);

    @GrpcClient("user-service")
    private UserServiceGrpc.UserServiceBlockingStub userServiceStub;

    /**
     * Get user by username for authentication
     * This method includes password data for authentication purposes
     */
    public UserResponse getUserByUsernameForAuth(String username) {
        try {
            logger.debug("Getting user by username for auth: {}", username);
            
            GetUserByUsernameRequest request = GetUserByUsernameRequest.newBuilder()
                    .setUsername(username)
                    .setIncludePassword(true) // Request password for authentication
                    .build();
            
            UserResponse response = userServiceStub.getUserByUsername(request);
            
            if (response.getSuccess()) {
                logger.debug("Successfully retrieved user: {}", username);
            } else {
                logger.warn("Failed to retrieve user: {} - {}", username, response.getMessage());
            }
            
            return response;
            
        } catch (Exception e) {
            logger.error("Error getting user by username for auth: {}", username, e);
            return UserResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Failed to get user: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Validate user credentials
     */
    public boolean validateUserCredentials(String username, String password) {
        try {
            logger.debug("Validating credentials for user: {}", username);
            
            ValidateCredentialsRequest request = ValidateCredentialsRequest.newBuilder()
                    .setUsername(username)
                    .setPassword(password)
                    .build();
            
            ValidateCredentialsResponse response = userServiceStub.validateCredentials(request);
            
            logger.debug("Credential validation result for {}: {}", username, response.getValid());
            return response.getValid();
            
        } catch (Exception e) {
            logger.error("Error validating credentials for user: {}", username, e);
            return false;
        }
    }

    /**
     * Get user by ID
     */
    public UserResponse getUserById(Long userId) {
        try {
            logger.debug("Getting user by ID: {}", userId);
            
            GetUserByIdRequest request = GetUserByIdRequest.newBuilder()
                    .setUserId(userId)
                    .build();
            
            UserResponse response = userServiceStub.getUserById(request);
            
            if (response.getSuccess()) {
                logger.debug("Successfully retrieved user by ID: {}", userId);
            } else {
                logger.warn("Failed to retrieve user by ID: {} - {}", userId, response.getMessage());
            }
            
            return response;
            
        } catch (Exception e) {
            logger.error("Error getting user by ID: {}", userId, e);
            return UserResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Failed to get user: " + e.getMessage())
                    .build();
        }
    }
}
