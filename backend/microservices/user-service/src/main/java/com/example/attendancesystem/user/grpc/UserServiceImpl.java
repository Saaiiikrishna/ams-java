package com.example.attendancesystem.user.grpc;

import com.example.attendancesystem.grpc.user.*;
import com.example.attendancesystem.user.model.User;
import com.example.attendancesystem.user.service.UserService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * gRPC Service Implementation for User Service
 * Handles user management operations via gRPC
 */
@GrpcService
public class UserServiceImpl extends UserServiceGrpc.UserServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Autowired
    private UserService userService;

    @Override
    public void createEntityAdmin(CreateEntityAdminRequest request, StreamObserver<UserResponse> responseObserver) {
        try {
            logger.info("Creating Entity Admin via gRPC: {}", request.getUsername());

            // Create Entity Admin user
            User user = userService.createEntityAdmin(
                    request.getUsername(),
                    request.getPassword(),
                    request.getEmail(),
                    request.getFirstName(),
                    request.getLastName(),
                    request.getMobileNumber(),
                    request.getOrganizationId(),
                    request.getCreatedByUserId()
            );

            // Convert to gRPC response
            com.example.attendancesystem.grpc.user.User grpcUser = convertToGrpcUser(user);
            
            UserResponse response = UserResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Entity Admin created successfully")
                    .setUser(grpcUser)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Error creating Entity Admin via gRPC: {}", request.getUsername(), e);
            
            UserResponse response = UserResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Failed to create Entity Admin: " + e.getMessage())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void createMember(CreateMemberRequest request, StreamObserver<UserResponse> responseObserver) {
        try {
            logger.info("Creating Member via gRPC: {}", request.getUsername());

            // Create Member user
            User user = userService.createMember(
                    request.getUsername(),
                    request.getPassword(),
                    request.getEmail(),
                    request.getFirstName(),
                    request.getLastName(),
                    request.getMobileNumber(),
                    request.getOrganizationId(),
                    request.getCreatedByUserId()
            );

            // Convert to gRPC response
            com.example.attendancesystem.grpc.user.User grpcUser = convertToGrpcUser(user);
            
            UserResponse response = UserResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Member created successfully")
                    .setUser(grpcUser)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Error creating Member via gRPC: {}", request.getUsername(), e);
            
            UserResponse response = UserResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Failed to create Member: " + e.getMessage())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getUserByUsername(GetUserByUsernameRequest request, StreamObserver<UserResponse> responseObserver) {
        try {
            logger.debug("Getting user by username via gRPC: {}", request.getUsername());

            User user = userService.findByUsername(request.getUsername());
            if (user == null) {
                UserResponse response = UserResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("User not found")
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            // Convert to gRPC response (NO PASSWORD - Auth Service handles authentication)
            com.example.attendancesystem.grpc.user.User grpcUser = convertToGrpcUser(user);

            UserResponse response = UserResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("User retrieved successfully")
                    .setUser(grpcUser)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Error getting user by username via gRPC: {}", request.getUsername(), e);

            UserResponse response = UserResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Failed to get user: " + e.getMessage())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    // REMOVED: validateCredentials method
    // Authentication should be handled by Auth Service, not User Service
    // User Service only manages user data (CRUD operations)

    /**
     * Convert User entity to gRPC User message
     */
    private com.example.attendancesystem.grpc.user.User convertToGrpcUser(User user) {
        com.example.attendancesystem.grpc.user.User.Builder builder = 
                com.example.attendancesystem.grpc.user.User.newBuilder()
                        .setId(user.getId())
                        .setUsername(user.getUsername())
                        .setEmail(user.getEmail() != null ? user.getEmail() : "")
                        .setFirstName(user.getFirstName() != null ? user.getFirstName() : "")
                        .setLastName(user.getLastName() != null ? user.getLastName() : "")
                        .setMobileNumber(user.getMobileNumber() != null ? user.getMobileNumber() : "")
                        .setUserType(user.getUserType().name())
                        .setIsActive(user.getIsActive());

        if (user.getOrganizationId() != null) {
            builder.setOrganizationId(user.getOrganizationId());
        }

        if (user.getCreatedAt() != null) {
            builder.setCreatedAt(user.getCreatedAt().format(DATE_FORMATTER));
        }

        if (user.getUpdatedAt() != null) {
            builder.setUpdatedAt(user.getUpdatedAt().format(DATE_FORMATTER));
        }

        return builder.build();
    }
}
