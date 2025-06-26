package com.example.attendancesystem.subscriber.service;

import com.example.attendancesystem.dto.RestaurantTableDto;
import com.example.attendancesystem.model.Organization;
import com.example.attendancesystem.model.RestaurantTable;
import com.example.attendancesystem.repository.OrganizationRepository;
import com.example.attendancesystem.repository.RestaurantTableRepository;
import com.example.attendancesystem.service.QrCodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class TableService {
    
    private static final Logger logger = LoggerFactory.getLogger(TableService.class);
    
    @Autowired
    private RestaurantTableRepository restaurantTableRepository;
    
    @Autowired
    private OrganizationRepository organizationRepository;
    
    @Autowired
    private TableQrCodeService tableQrCodeService;
    
    public List<RestaurantTableDto> getTablesByOrganization(String entityId) {
        logger.debug("Getting tables for organization: {}", entityId);
        List<RestaurantTable> tables = restaurantTableRepository.findActiveByOrganizationEntityId(entityId);
        return tables.stream().map(this::convertToTableDto).collect(Collectors.toList());
    }
    
    public RestaurantTableDto getTableById(String entityId, Long tableId) {
        logger.debug("Getting table {} for organization: {}", tableId, entityId);
        Organization organization = organizationRepository.findByEntityId(entityId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));
        
        RestaurantTable table = restaurantTableRepository.findByIdAndOrganization(tableId, organization)
                .orElseThrow(() -> new RuntimeException("Table not found"));
        
        return convertToTableDto(table);
    }
    
    public RestaurantTableDto getTableByNumber(String entityId, Integer tableNumber) {
        logger.debug("Getting table {} for organization: {}", tableNumber, entityId);
        Organization organization = organizationRepository.findByEntityId(entityId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));
        
        RestaurantTable table = restaurantTableRepository.findByTableNumberAndOrganization(tableNumber, organization)
                .orElseThrow(() -> new RuntimeException("Table not found"));
        
        return convertToTableDto(table);
    }
    
    public List<RestaurantTableDto> createTables(String entityId, Integer numberOfTables) {
        logger.info("Creating {} tables for organization: {}", numberOfTables, entityId);
        
        Organization organization = organizationRepository.findByEntityId(entityId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));
        
        if (numberOfTables <= 0 || numberOfTables > 100) {
            throw new RuntimeException("Number of tables must be between 1 and 100");
        }
        
        // Soft delete existing active tables instead of hard delete
        List<RestaurantTable> existingTables = restaurantTableRepository.findActiveByOrganizationEntityId(entityId);
        for (RestaurantTable existingTable : existingTables) {
            existingTable.markAsDeleted();
            restaurantTableRepository.save(existingTable);
        }

        List<RestaurantTable> tables = new ArrayList<>();
        
        for (int i = 1; i <= numberOfTables; i++) {
            RestaurantTable table = new RestaurantTable();
            table.setTableNumber(i);
            table.setOrganization(organization);
            table.setIsActive(true);

            // Generate QR code identifier first
            String qrCodeId = "TABLE-" + System.currentTimeMillis() + "-" + i;
            table.setQrCode(qrCodeId);

            // Generate QR code data for the table using dedicated table QR service
            String qrCodeData = tableQrCodeService.generateMenuUrl(organization.getEntityId(), i, qrCodeId);
            table.setQrCodeUrl(tableQrCodeService.generateTableQrCodeImageUrl(qrCodeData));

            tables.add(table);
        }
        
        List<RestaurantTable> savedTables = restaurantTableRepository.saveAll(tables);
        logger.info("Created {} tables for organization: {}", savedTables.size(), entityId);
        
        return savedTables.stream().map(this::convertToTableDto).collect(Collectors.toList());
    }
    
    public RestaurantTableDto createSingleTable(String entityId, RestaurantTableDto tableDto) {
        logger.info("Creating table {} for organization: {}", tableDto.getTableNumber(), entityId);
        
        Organization organization = organizationRepository.findByEntityId(entityId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));
        
        // Check if table number already exists
        if (restaurantTableRepository.existsByTableNumberAndOrganization(tableDto.getTableNumber(), organization)) {
            throw new RuntimeException("Table number " + tableDto.getTableNumber() + " already exists");
        }
        
        RestaurantTable table = new RestaurantTable();
        table.setTableNumber(tableDto.getTableNumber());
        table.setOrganization(organization);
        table.setCapacity(tableDto.getCapacity());
        table.setLocationDescription(tableDto.getLocationDescription());
        table.setIsActive(tableDto.getIsActive() != null ? tableDto.getIsActive() : true);

        // Save table first to get the ID
        RestaurantTable savedTable = restaurantTableRepository.save(table);

        // Generate QR code identifier using table ID for consistency
        String qrCodeId = "TABLE-" + savedTable.getId() + "-" + System.currentTimeMillis();
        savedTable.setQrCode(qrCodeId);

        // Generate QR code data for the table using table ID for proper routing
        String qrCodeData = tableQrCodeService.generateTableMenuUrlById(savedTable.getId());
        savedTable.setQrCodeUrl(tableQrCodeService.generateTableQrCodeImageUrl(qrCodeData));

        // Save the table again with QR code information
        RestaurantTable finalTable = restaurantTableRepository.save(savedTable);
        logger.info("Table {} created successfully with QR code: {}", finalTable.getTableNumber(), finalTable.getQrCode());

        return convertToTableDto(finalTable);
    }
    
    public RestaurantTableDto updateTable(String entityId, Long tableId, RestaurantTableDto tableDto) {
        logger.info("Updating table {} for organization: {}", tableId, entityId);

        Organization organization = organizationRepository.findByEntityId(entityId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));

        RestaurantTable table = restaurantTableRepository.findByIdAndOrganization(tableId, organization)
                .orElseThrow(() -> new RuntimeException("Table not found"));

        // Store original table number to check if it changed
        Integer originalTableNumber = table.getTableNumber();

        // Check if new table number conflicts with existing tables
        if (!table.getTableNumber().equals(tableDto.getTableNumber()) &&
            restaurantTableRepository.existsByTableNumberAndOrganizationAndIdNot(
                tableDto.getTableNumber(), organization, tableId)) {
            throw new RuntimeException("Table number " + tableDto.getTableNumber() + " already exists");
        }

        // Update table properties
        table.setTableNumber(tableDto.getTableNumber());
        table.setCapacity(tableDto.getCapacity());
        table.setLocationDescription(tableDto.getLocationDescription());
        table.setIsActive(tableDto.getIsActive() != null ? tableDto.getIsActive() : true);

        // Regenerate QR code if table number changed or if QR code is missing
        if (!originalTableNumber.equals(tableDto.getTableNumber()) ||
            table.getQrCode() == null || table.getQrCodeUrl() == null) {
            table.setQrCode("TABLE-" + table.getId() + "-" + System.currentTimeMillis());
            // CRITICAL FIX: Use proper URL format with entityId, table number, and QR code
            String qrCodeData = tableQrCodeService.generateMenuUrl(
                table.getOrganization().getEntityId(),
                table.getTableNumber(),
                table.getQrCode()
            );
            table.setQrCodeUrl(tableQrCodeService.generateTableQrCodeImageUrl(qrCodeData));
            logger.info("QR code regenerated for table {}", table.getTableNumber());
        }

        RestaurantTable savedTable = restaurantTableRepository.save(table);
        logger.info("Table {} updated successfully", savedTable.getTableNumber());

        return convertToTableDto(savedTable);
    }
    
    public void deleteTable(String entityId, Long tableId) {
        logger.info("Soft deleting table {} for organization: {}", tableId, entityId);

        Organization organization = organizationRepository.findByEntityId(entityId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));

        RestaurantTable table = restaurantTableRepository.findByIdAndOrganization(tableId, organization)
                .orElseThrow(() -> new RuntimeException("Table not found"));

        // Soft delete the table
        table.markAsDeleted();
        restaurantTableRepository.save(table);
        logger.info("Table {} soft deleted", table.getTableNumber());
    }
    
    public String regenerateQrCode(String entityId, Long tableId) {
        logger.info("Regenerating QR code for table {} in organization: {}", tableId, entityId);
        
        Organization organization = organizationRepository.findByEntityId(entityId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));
        
        RestaurantTable table = restaurantTableRepository.findByIdAndOrganization(tableId, organization)
                .orElseThrow(() -> new RuntimeException("Table not found"));
        
        // Generate new QR code using table ID for consistency
        table.setQrCode("TABLE-" + table.getId() + "-" + System.currentTimeMillis());
        // CRITICAL FIX: Use proper URL format with entityId, table number, and QR code
        String qrCodeData = tableQrCodeService.generateMenuUrl(
            table.getOrganization().getEntityId(),
            table.getTableNumber(),
            table.getQrCode()
        );
        table.setQrCodeUrl(tableQrCodeService.generateTableQrCodeImageUrl(qrCodeData));
        
        RestaurantTable savedTable = restaurantTableRepository.save(table);
        logger.info("QR code regenerated for table {}", savedTable.getTableNumber());
        
        return savedTable.getQrCodeUrl();
    }
    
    /**
     * Get table by QR code for public access
     */
    public RestaurantTableDto getTableByQrCode(String qrCode) {
        logger.debug("Getting table by QR code: {}", qrCode);

        RestaurantTable table = restaurantTableRepository.findActiveByQrCode(qrCode)
                .orElseThrow(() -> new RuntimeException("Table not found or inactive"));

        return convertToTableDto(table);
    }

    /**
     * Get table by ID for public access (no entity validation)
     */
    public RestaurantTableDto getTableById(Long tableId) {
        logger.debug("Getting table by ID: {}", tableId);

        RestaurantTable table = restaurantTableRepository.findById(tableId)
                .orElseThrow(() -> new RuntimeException("Table not found"));

        if (!table.getIsActive()) {
            throw new RuntimeException("Table is not active");
        }

        return convertToTableDto(table);
    }

    /**
     * Regenerate QR codes for all tables in an organization
     */
    public List<RestaurantTableDto> regenerateAllQrCodes(String entityId) {
        logger.info("Regenerating QR codes for all tables in organization: {}", entityId);

        Organization organization = organizationRepository.findByEntityId(entityId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));

        List<RestaurantTable> tables = restaurantTableRepository.findByOrganizationOrderByTableNumberAsc(organization);
        List<RestaurantTableDto> updatedTables = new ArrayList<>();

        for (RestaurantTable table : tables) {
            // Generate new QR code using table ID for consistency
            table.setQrCode("TABLE-" + table.getId() + "-" + System.currentTimeMillis());
            // CRITICAL FIX: Use proper URL format with entityId, table number, and QR code
            String qrCodeData = tableQrCodeService.generateMenuUrl(
                table.getOrganization().getEntityId(),
                table.getTableNumber(),
                table.getQrCode()
            );
            table.setQrCodeUrl(tableQrCodeService.generateTableQrCodeImageUrl(qrCodeData));

            RestaurantTable savedTable = restaurantTableRepository.save(table);
            updatedTables.add(convertToTableDto(savedTable));
            logger.info("QR code regenerated for table {} (ID: {})", savedTable.getTableNumber(), savedTable.getId());
        }

        logger.info("Successfully regenerated QR codes for {} tables", updatedTables.size());
        return updatedTables;
    }


    private RestaurantTableDto convertToTableDto(RestaurantTable table) {
        RestaurantTableDto dto = new RestaurantTableDto();
        dto.setId(table.getId());
        dto.setTableNumber(table.getTableNumber());
        dto.setQrCode(table.getQrCode());
        dto.setQrCodeUrl(table.getQrCodeUrl());
        dto.setIsActive(table.getIsActive());
        dto.setCapacity(table.getCapacity());
        dto.setLocationDescription(table.getLocationDescription());
        dto.setMenuUrl(table.getMenuUrl());
        dto.setOrganizationEntityId(table.getOrganization() != null ? table.getOrganization().getEntityId() : null);
        dto.setCreatedAt(table.getCreatedAt());
        dto.setUpdatedAt(table.getUpdatedAt());
        return dto;
    }
}
