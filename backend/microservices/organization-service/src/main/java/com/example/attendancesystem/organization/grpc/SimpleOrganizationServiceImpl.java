package com.example.attendancesystem.organization.grpc;

import com.example.attendancesystem.grpc.organization.*;
import com.example.attendancesystem.organization.model.Organization;
import com.example.attendancesystem.organization.repository.OrganizationRepository;
import com.example.attendancesystem.organization.service.EntityIdService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@GrpcService
public class SimpleOrganizationServiceImpl extends OrganizationServiceGrpc.OrganizationServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(SimpleOrganizationServiceImpl.class);

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private EntityIdService entityIdService;

    @Override
    public void createOrganization(CreateOrganizationRequest request, StreamObserver<OrganizationResponse> responseObserver) {
        try {
            logger.info("Creating organization: {}", request.getName());

            // Check if organization name already exists
            if (organizationRepository.existsByName(request.getName())) {
                responseObserver.onError(Status.ALREADY_EXISTS
                        .withDescription("Organization with name '" + request.getName() + "' already exists")
                        .asRuntimeException());
                return;
            }

            Organization organization = new Organization();
            organization.setName(request.getName());
            // Note: Organization model doesn't have description field in current implementation
            organization.setAddress(request.getAddress());
            organization.setEmail(request.getContactEmail());
            organization.setContactPerson(request.getContactPhone());
            organization.setCreatedAt(LocalDateTime.now());
            organization.setUpdatedAt(LocalDateTime.now());
            organization.setIsActive(true);

            // Generate unique entity ID
            String entityId = entityIdService.generateUniqueEntityId();
            organization.setEntityId(entityId);

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
            logger.info("Listing organizations - Page: {}, Size: {}, Search: {}, ActiveOnly: {}",
                       request.getPage(), request.getSize(), request.getSearch(), request.getActiveOnly());

            // Set default pagination if not provided
            int page = request.getPage() > 0 ? request.getPage() - 1 : 0; // Convert to 0-based
            int size = request.getSize() > 0 ? request.getSize() : 10;

            Pageable pageable = PageRequest.of(page, size);
            Page<Organization> organizationPage;

            // Apply filters
            if (request.getActiveOnly()) {
                if (!request.getSearch().isEmpty()) {
                    organizationPage = organizationRepository.findByIsActiveTrueAndNameContainingIgnoreCase(
                            request.getSearch(), pageable);
                } else {
                    organizationPage = organizationRepository.findByIsActiveTrue(pageable);
                }
            } else {
                if (!request.getSearch().isEmpty()) {
                    organizationPage = organizationRepository.findByNameContainingIgnoreCase(
                            request.getSearch(), pageable);
                } else {
                    organizationPage = organizationRepository.findAll(pageable);
                }
            }

            List<com.example.attendancesystem.grpc.organization.Organization> grpcOrganizations =
                    organizationPage.getContent().stream()
                            .map(this::convertToGrpcOrganization)
                            .collect(Collectors.toList());

            ListOrganizationsResponse response = ListOrganizationsResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Organizations retrieved successfully")
                    .addAllOrganizations(grpcOrganizations)
                    .setTotalCount(organizationPage.getTotalElements())
                    .setPage(request.getPage())
                    .setSize(organizationPage.getSize())
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
