package com.example.attendancesystem.organization.grpc.client;

import com.example.attendancesystem.grpc.user.*;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * gRPC Client for User Service
 * Handles user-related operations from Organization Service
 */
@Service
public class UserServiceGrpcClient {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceGrpcClient.class);

    @GrpcClient("user-service")
    private UserServiceGrpc.UserServiceBlockingStub userServiceStub;

    /**
     * Create Entity Admin for an organization
     */
    public UserResponse createEntityAdmin(String username, String password, String email, 
                                        String firstName, String lastName, String mobileNumber, 
                                        Long organizationId, Long createdByUserId) {
        try {
            logger.info("Creating Entity Admin: {} for organization: {}", username, organizationId);
            
            CreateEntityAdminRequest request = CreateEntityAdminRequest.newBuilder()
                    .setUsername(username)
                    .setPassword(password)
                    .setEmail(email)
                    .setFirstName(firstName)
                    .setLastName(lastName)
                    .setMobileNumber(mobileNumber)
                    .setOrganizationId(organizationId)
                    .setCreatedByUserId(createdByUserId)
                    .build();
            
            UserResponse response = userServiceStub.createEntityAdmin(request);
            
            if (response.getSuccess()) {
                logger.info("Successfully created Entity Admin: {}", username);
            } else {
                logger.warn("Failed to create Entity Admin: {} - {}", username, response.getMessage());
            }
            
            return response;
            
        } catch (Exception e) {
            logger.error("Error creating Entity Admin: {}", username, e);
            return UserResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Failed to create Entity Admin: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Create Member for an organization
     */
    public UserResponse createMember(String username, String password, String email, 
                                   String firstName, String lastName, String mobileNumber, 
                                   Long organizationId, Long createdByUserId) {
        try {
            logger.info("Creating Member: {} for organization: {}", username, organizationId);
            
            CreateMemberRequest request = CreateMemberRequest.newBuilder()
                    .setUsername(username)
                    .setPassword(password)
                    .setEmail(email)
                    .setFirstName(firstName)
                    .setLastName(lastName)
                    .setMobileNumber(mobileNumber)
                    .setOrganizationId(organizationId)
                    .setCreatedByUserId(createdByUserId)
                    .build();
            
            UserResponse response = userServiceStub.createMember(request);
            
            if (response.getSuccess()) {
                logger.info("Successfully created Member: {}", username);
            } else {
                logger.warn("Failed to create Member: {} - {}", username, response.getMessage());
            }
            
            return response;
            
        } catch (Exception e) {
            logger.error("Error creating Member: {}", username, e);
            return UserResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Failed to create Member: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Get users by organization
     */
    public ListUsersResponse getUsersByOrganization(Long organizationId, int page, int size, 
                                                  String userType, boolean activeOnly) {
        try {
            logger.debug("Getting users for organization: {}", organizationId);
            
            GetUsersByOrganizationRequest request = GetUsersByOrganizationRequest.newBuilder()
                    .setOrganizationId(organizationId)
                    .setPage(page)
                    .setSize(size)
                    .setUserType(userType)
                    .setActiveOnly(activeOnly)
                    .build();
            
            ListUsersResponse response = userServiceStub.getUsersByOrganization(request);
            
            if (response.getSuccess()) {
                logger.debug("Successfully retrieved {} users for organization: {}", 
                           response.getUsersCount(), organizationId);
            } else {
                logger.warn("Failed to get users for organization: {} - {}", 
                          organizationId, response.getMessage());
            }
            
            return response;
            
        } catch (Exception e) {
            logger.error("Error getting users for organization: {}", organizationId, e);
            return ListUsersResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Failed to get users: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Count users by organization
     */
    public CountResponse countUsersByOrganization(Long organizationId, String userType, boolean activeOnly) {
        try {
            logger.debug("Counting users for organization: {}", organizationId);
            
            CountUsersByOrganizationRequest request = CountUsersByOrganizationRequest.newBuilder()
                    .setOrganizationId(organizationId)
                    .setUserType(userType)
                    .setActiveOnly(activeOnly)
                    .build();
            
            CountResponse response = userServiceStub.countUsersByOrganization(request);
            
            if (response.getSuccess()) {
                logger.debug("Successfully counted {} users for organization: {}", 
                           response.getCount(), organizationId);
            } else {
                logger.warn("Failed to count users for organization: {} - {}", 
                          organizationId, response.getMessage());
            }
            
            return response;
            
        } catch (Exception e) {
            logger.error("Error counting users for organization: {}", organizationId, e);
            return CountResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Failed to count users: " + e.getMessage())
                    .build();
        }
    }
}
