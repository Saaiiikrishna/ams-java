package com.example.attendancesystem.attendance.client;

import com.example.attendancesystem.attendance.dto.UserDto;
import com.example.attendancesystem.grpc.user.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * gRPC client for communicating with User Service
 * Replaces direct database access to user/subscriber data
 */
@Component
public class UserServiceGrpcClient {
    
    private static final Logger logger = LoggerFactory.getLogger(UserServiceGrpcClient.class);
    
    @Value("${grpc.client.user-service.host:localhost}")
    private String userServiceHost;
    
    @Value("${grpc.client.user-service.port:9093}")
    private int userServicePort;
    
    private ManagedChannel channel;
    private UserServiceGrpc.UserServiceBlockingStub userServiceStub;
    
    @PostConstruct
    public void init() {
        try {
            channel = ManagedChannelBuilder.forAddress(userServiceHost, userServicePort)
                    .usePlaintext()
                    .build();
            userServiceStub = UserServiceGrpc.newBlockingStub(channel);
            logger.info("User Service gRPC client initialized: {}:{}", userServiceHost, userServicePort);
        } catch (Exception e) {
            logger.error("Failed to initialize User Service gRPC client", e);
        }
    }
    
    @PreDestroy
    public void shutdown() {
        if (channel != null && !channel.isShutdown()) {
            channel.shutdown();
            logger.info("User Service gRPC client shutdown");
        }
    }
    
    /**
     * Get user by ID
     */
    public Optional<UserDto> getUserById(Long userId) {
        try {
            GetUserRequest request = GetUserRequest.newBuilder()
                    .setUserId(userId)
                    .build();

            GetUserResponse response = userServiceStub.getUser(request);

            if (response.hasUser()) {
                return Optional.of(convertToUserDto(response.getUser()));
            }
            return Optional.empty();

        } catch (Exception e) {
            logger.error("Error getting user by ID: {}", userId, e);
            return Optional.empty();
        }
    }
    
    /**
     * Get user by username (for subscriber lookup)
     */
    public Optional<UserDto> getUserByUsername(String username) {
        try {
            GetUserByUsernameRequest request = GetUserByUsernameRequest.newBuilder()
                    .setUsername(username)
                    .build();

            UserResponse response = userServiceStub.getUserByUsername(request);

            if (response.hasUser()) {
                return Optional.of(convertToUserDto(response.getUser()));
            }
            return Optional.empty();

        } catch (Exception e) {
            logger.error("Error getting user by username: {}", username, e);
            return Optional.empty();
        }
    }
    
    /**
     * Get users by organization ID
     */
    public List<UserDto> getUsersByOrganizationId(Long organizationId) {
        try {
            GetUsersByOrganizationRequest request = GetUsersByOrganizationRequest.newBuilder()
                    .setOrganizationId(organizationId)
                    .build();

            ListUsersResponse response = userServiceStub.getUsersByOrganization(request);

            List<UserDto> users = new ArrayList<>();
            for (User user : response.getUsersList()) {
                users.add(convertToUserDto(user));
            }
            return users;

        } catch (Exception e) {
            logger.error("Error getting users by organization ID: {}", organizationId, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Get user by mobile number (for mobile-based lookup)
     */
    public Optional<UserDto> getUserByMobileNumber(String mobileNumber) {
        try {
            GetUserByMobileRequest request = GetUserByMobileRequest.newBuilder()
                    .setMobileNumber(mobileNumber)
                    .build();

            GetUserResponse response = userServiceStub.getUserByMobile(request);

            if (response.hasUser()) {
                return Optional.of(convertToUserDto(response.getUser()));
            }
            return Optional.empty();

        } catch (Exception e) {
            logger.error("Error getting user by mobile number: {}", mobileNumber, e);
            return Optional.empty();
        }
    }
    
    /**
     * Convert gRPC User to UserDto
     */
    private UserDto convertToUserDto(User grpcUser) {
        UserDto userDto = new UserDto();
        userDto.setId(grpcUser.getId());
        userDto.setUsername(grpcUser.getUsername());
        userDto.setEmail(grpcUser.getEmail());
        userDto.setMobileNumber(grpcUser.getMobileNumber());
        userDto.setFirstName(grpcUser.getFirstName());
        userDto.setLastName(grpcUser.getLastName());
        userDto.setUserType(grpcUser.getUserType());
        userDto.setOrganizationId(grpcUser.getOrganizationId());
        userDto.setActive(grpcUser.getIsActive());
        return userDto;
    }
    
    /**
     * Check if user exists by ID
     */
    public boolean userExists(Long userId) {
        return getUserById(userId).isPresent();
    }
    
    /**
     * Get user's organization ID
     */
    public Optional<Long> getUserOrganizationId(Long userId) {
        Optional<UserDto> user = getUserById(userId);
        return user.map(UserDto::getOrganizationId);
    }
}
