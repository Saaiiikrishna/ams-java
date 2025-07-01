package com.example.attendancesystem.attendance.client;

import com.example.attendancesystem.attendance.dto.OrganizationDto;
import com.example.attendancesystem.grpc.organization.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Optional;

/**
 * gRPC client for communicating with Organization Service
 * Replaces direct database access to organization data
 */
@Component
public class OrganizationServiceGrpcClient {
    
    private static final Logger logger = LoggerFactory.getLogger(OrganizationServiceGrpcClient.class);
    
    @Value("${grpc.client.organization-service.host:localhost}")
    private String organizationServiceHost;
    
    @Value("${grpc.client.organization-service.port:9092}")
    private int organizationServicePort;
    
    private ManagedChannel channel;
    private OrganizationServiceGrpc.OrganizationServiceBlockingStub organizationServiceStub;
    
    @PostConstruct
    public void init() {
        try {
            channel = ManagedChannelBuilder.forAddress(organizationServiceHost, organizationServicePort)
                    .usePlaintext()
                    .build();
            organizationServiceStub = OrganizationServiceGrpc.newBlockingStub(channel);
            logger.info("Organization Service gRPC client initialized: {}:{}", organizationServiceHost, organizationServicePort);
        } catch (Exception e) {
            logger.error("Failed to initialize Organization Service gRPC client", e);
        }
    }
    
    @PreDestroy
    public void shutdown() {
        if (channel != null && !channel.isShutdown()) {
            channel.shutdown();
            logger.info("Organization Service gRPC client shutdown");
        }
    }
    
    /**
     * Get organization by ID
     */
    public Optional<OrganizationDto> getOrganizationById(Long organizationId) {
        try {
            GetOrganizationRequest request = GetOrganizationRequest.newBuilder()
                    .setId(organizationId)
                    .build();

            OrganizationResponse response = organizationServiceStub.getOrganization(request);

            if (response.hasOrganization()) {
                return Optional.of(convertToOrganizationDto(response.getOrganization()));
            }
            return Optional.empty();

        } catch (Exception e) {
            logger.error("Error getting organization by ID: {}", organizationId, e);
            return Optional.empty();
        }
    }
    
    /**
     * Get organization by entity ID (for legacy compatibility)
     */
    public Optional<OrganizationDto> getOrganizationByEntityId(String entityId) {
        try {
            GetOrganizationByEntityIdRequest request = GetOrganizationByEntityIdRequest.newBuilder()
                    .setEntityId(entityId)
                    .build();

            OrganizationResponse response = organizationServiceStub.getOrganizationByEntityId(request);

            if (response.hasOrganization()) {
                return Optional.of(convertToOrganizationDto(response.getOrganization()));
            }
            return Optional.empty();

        } catch (Exception e) {
            logger.error("Error getting organization by entity ID: {}", entityId, e);
            return Optional.empty();
        }
    }
    
    /**
     * Check if organization exists by ID
     */
    public boolean organizationExists(Long organizationId) {
        return getOrganizationById(organizationId).isPresent();
    }
    
    /**
     * Get organization name by ID
     */
    public Optional<String> getOrganizationName(Long organizationId) {
        Optional<OrganizationDto> org = getOrganizationById(organizationId);
        return org.map(OrganizationDto::getName);
    }
    
    /**
     * Get organization entity ID by ID
     */
    public Optional<String> getOrganizationEntityId(Long organizationId) {
        Optional<OrganizationDto> org = getOrganizationById(organizationId);
        return org.map(OrganizationDto::getEntityId);
    }
    
    /**
     * Convert gRPC Organization to OrganizationDto
     */
    private OrganizationDto convertToOrganizationDto(Organization grpcOrg) {
        OrganizationDto orgDto = new OrganizationDto();
        orgDto.setId(grpcOrg.getId());
        orgDto.setName(grpcOrg.getName());
        orgDto.setDescription(grpcOrg.getDescription());
        orgDto.setEntityId(grpcOrg.getEntityId());
        orgDto.setAddress(grpcOrg.getAddress());
        orgDto.setContactEmail(grpcOrg.getContactEmail());
        orgDto.setContactPhone(grpcOrg.getContactPhone());
        orgDto.setActive(grpcOrg.getActive());
        return orgDto;
    }
}
