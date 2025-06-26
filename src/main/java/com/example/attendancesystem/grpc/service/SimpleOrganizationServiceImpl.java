package com.example.attendancesystem.grpc.service;

import com.example.attendancesystem.grpc.organization.*;
import com.example.attendancesystem.model.Organization;
import com.example.attendancesystem.repository.OrganizationRepository;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

@GrpcService
public class SimpleOrganizationServiceImpl extends OrganizationServiceGrpc.OrganizationServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(SimpleOrganizationServiceImpl.class);

    @Autowired
    private OrganizationRepository organizationRepository;

    @Override
    public void createOrganization(CreateOrganizationRequest request, StreamObserver<OrganizationResponse> responseObserver) {
        try {
            logger.info("Creating organization: {}", request.getName());

            Organization organization = new Organization();
            organization.setName(request.getName());
            organization.setAddress(request.getAddress());
            organization.setEmail(request.getContactEmail());

            Organization savedOrganization = organizationRepository.save(organization);

            com.example.attendancesystem.grpc.organization.Organization grpcOrganization = convertToGrpcOrganization(savedOrganization);

            OrganizationResponse response = OrganizationResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Organization created successfully")
                    .setOrganization(grpcOrganization)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
            logger.info("Organization created successfully with ID: {}", savedOrganization.getId());

        } catch (Exception e) {
            logger.error("Error creating organization: {}", request.getName(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to create organization: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void getOrganization(GetOrganizationRequest request, StreamObserver<OrganizationResponse> responseObserver) {
        try {
            Optional<Organization> organizationOpt = organizationRepository.findById(request.getId());

            if (organizationOpt.isEmpty()) {
                OrganizationResponse response = OrganizationResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Organization not found")
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            com.example.attendancesystem.grpc.organization.Organization grpcOrganization = convertToGrpcOrganization(organizationOpt.get());

            OrganizationResponse response = OrganizationResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Organization retrieved successfully")
                    .setOrganization(grpcOrganization)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Error retrieving organization with ID: {}", request.getId(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to retrieve organization: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void updateOrganization(UpdateOrganizationRequest request, StreamObserver<OrganizationResponse> responseObserver) {
        try {
            Optional<Organization> organizationOpt = organizationRepository.findById(request.getId());

            if (organizationOpt.isEmpty()) {
                OrganizationResponse response = OrganizationResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Organization not found")
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            Organization organization = organizationOpt.get();
            organization.setName(request.getName());
            organization.setAddress(request.getAddress());
            organization.setEmail(request.getContactEmail());

            Organization savedOrganization = organizationRepository.save(organization);

            com.example.attendancesystem.grpc.organization.Organization grpcOrganization = convertToGrpcOrganization(savedOrganization);

            OrganizationResponse response = OrganizationResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Organization updated successfully")
                    .setOrganization(grpcOrganization)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Error updating organization with ID: {}", request.getId(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to update organization: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void deleteOrganization(DeleteOrganizationRequest request, StreamObserver<DeleteResponse> responseObserver) {
        try {
            Optional<Organization> organizationOpt = organizationRepository.findById(request.getId());

            if (organizationOpt.isEmpty()) {
                DeleteResponse response = DeleteResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Organization not found")
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            // For now, just mark as deleted (you can implement actual deletion logic)
            organizationRepository.deleteById(request.getId());

            DeleteResponse response = DeleteResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Organization deleted successfully")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Error deleting organization with ID: {}", request.getId(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to delete organization: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void listOrganizations(ListOrganizationsRequest request, StreamObserver<ListOrganizationsResponse> responseObserver) {
        try {
            // Simple implementation - get all organizations
            var organizations = organizationRepository.findAll();

            var grpcOrganizations = organizations.stream()
                    .map(this::convertToGrpcOrganization)
                    .toList();

            ListOrganizationsResponse response = ListOrganizationsResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Organizations retrieved successfully")
                    .addAllOrganizations(grpcOrganizations)
                    .setTotalCount(organizations.size())
                    .setPage(0)
                    .setSize(organizations.size())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Error listing organizations", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to list organizations: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    private com.example.attendancesystem.grpc.organization.Organization convertToGrpcOrganization(Organization organization) {
        return com.example.attendancesystem.grpc.organization.Organization.newBuilder()
                .setId(organization.getId())
                .setName(organization.getName())
                .setDescription("") // Not available in current model
                .setContactEmail(organization.getEmail() != null ? organization.getEmail() : "")
                .setContactPhone("") // Not available in current model
                .setAddress(organization.getAddress() != null ? organization.getAddress() : "")
                .setActive(true) // Default to true for now
                .setCreatedAt("") // Not available in current model
                .setUpdatedAt("") // Not available in current model
                .build();
    }

    // Placeholder implementations for other methods
    @Override
    public void getOrganizationPermissions(GetPermissionsRequest request, StreamObserver<PermissionsResponse> responseObserver) {
        PermissionsResponse response = PermissionsResponse.newBuilder()
                .setSuccess(false)
                .setMessage("Not implemented yet")
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void updateOrganizationPermissions(UpdatePermissionsRequest request, StreamObserver<PermissionsResponse> responseObserver) {
        PermissionsResponse response = PermissionsResponse.newBuilder()
                .setSuccess(false)
                .setMessage("Not implemented yet")
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getFeaturePermissions(GetFeaturePermissionsRequest request, StreamObserver<FeaturePermissionsResponse> responseObserver) {
        FeaturePermissionsResponse response = FeaturePermissionsResponse.newBuilder()
                .setSuccess(false)
                .setMessage("Not implemented yet")
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void createEntityAdmin(CreateEntityAdminRequest request, StreamObserver<EntityAdminResponse> responseObserver) {
        EntityAdminResponse response = EntityAdminResponse.newBuilder()
                .setSuccess(false)
                .setMessage("Not implemented yet")
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getEntityAdmin(GetEntityAdminRequest request, StreamObserver<EntityAdminResponse> responseObserver) {
        EntityAdminResponse response = EntityAdminResponse.newBuilder()
                .setSuccess(false)
                .setMessage("Not implemented yet")
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void updateEntityAdmin(UpdateEntityAdminRequest request, StreamObserver<EntityAdminResponse> responseObserver) {
        EntityAdminResponse response = EntityAdminResponse.newBuilder()
                .setSuccess(false)
                .setMessage("Not implemented yet")
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void deleteEntityAdmin(DeleteEntityAdminRequest request, StreamObserver<DeleteResponse> responseObserver) {
        DeleteResponse response = DeleteResponse.newBuilder()
                .setSuccess(false)
                .setMessage("Not implemented yet")
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void listEntityAdmins(ListEntityAdminsRequest request, StreamObserver<ListEntityAdminsResponse> responseObserver) {
        ListEntityAdminsResponse response = ListEntityAdminsResponse.newBuilder()
                .setSuccess(false)
                .setMessage("Not implemented yet")
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
