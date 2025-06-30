package com.example.attendancesystem.table.service;

import com.example.attendancesystem.shared.dto.RestaurantTableDto;
import com.example.attendancesystem.shared.model.Organization;
import com.example.attendancesystem.shared.repository.OrganizationRepository;
import com.example.attendancesystem.table.model.RestaurantTable;
import com.example.attendancesystem.table.repository.RestaurantTableRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Simplified Table Service for microservice
 */
@Service
public class TableService {
    
    private static final Logger logger = LoggerFactory.getLogger(TableService.class);
    
    @Autowired
    private RestaurantTableRepository tableRepository;
    
    @Autowired
    private OrganizationRepository organizationRepository;
    
    public List<RestaurantTableDto> getTablesByOrganization(String entityId) {
        logger.info("Getting tables for organization: {}", entityId);
        
        Organization organization = organizationRepository.findByEntityId(entityId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));
        
        List<RestaurantTable> tables = tableRepository.findByOrganizationOrderByTableNumberAsc(organization);
        return tables.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public RestaurantTableDto getTableById(String entityId, Long tableId) {
        logger.info("Getting table {} for organization: {}", tableId, entityId);
        
        Organization organization = organizationRepository.findByEntityId(entityId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));
        
        RestaurantTable table = tableRepository.findByIdAndOrganization(tableId, organization)
                .orElseThrow(() -> new RuntimeException("Table not found"));
        
        return convertToDto(table);
    }
    
    public RestaurantTableDto getTableByNumber(String entityId, Integer tableNumber) {
        logger.info("Getting table {} for organization: {}", tableNumber, entityId);
        
        Organization organization = organizationRepository.findByEntityId(entityId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));
        
        RestaurantTable table = tableRepository.findByTableNumberAndOrganization(tableNumber, organization)
                .orElseThrow(() -> new RuntimeException("Table not found"));
        
        return convertToDto(table);
    }
    
    public RestaurantTableDto getTableByQrCode(String qrCode) {
        logger.info("Getting table by QR code: {}", qrCode);
        
        RestaurantTable table = tableRepository.findActiveByQrCode(qrCode)
                .orElseThrow(() -> new RuntimeException("Invalid QR code or table not active"));
        
        return convertToDto(table);
    }
    
    public RestaurantTableDto createTable(String entityId, RestaurantTableDto tableDto) {
        logger.info("Creating table for organization: {}", entityId);
        
        Organization organization = organizationRepository.findByEntityId(entityId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));
        
        // Check if table number already exists
        if (tableRepository.findByTableNumberAndOrganization(tableDto.getTableNumber(), organization).isPresent()) {
            throw new RuntimeException("Table number " + tableDto.getTableNumber() + " already exists");
        }
        
        RestaurantTable table = new RestaurantTable();
        table.setTableNumber(tableDto.getTableNumber());
        table.setCapacity(tableDto.getCapacity());
        table.setLocationDescription(tableDto.getLocationDescription());
        table.setIsActive(true);
        table.setOrganization(organization);
        table.setCreatedAt(LocalDateTime.now());
        
        // Generate QR code data (simplified)
        table.setQrCode("TABLE-" + tableDto.getTableNumber());
        table.setQrCodeUrl("https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=TABLE-" + tableDto.getTableNumber());
        
        RestaurantTable savedTable = tableRepository.save(table);
        logger.info("Table created with ID: {}", savedTable.getId());
        
        return convertToDto(savedTable);
    }
    
    public RestaurantTableDto updateTable(String entityId, Long tableId, RestaurantTableDto tableDto) {
        logger.info("Updating table {} for organization: {}", tableId, entityId);
        
        Organization organization = organizationRepository.findByEntityId(entityId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));
        
        RestaurantTable table = tableRepository.findByIdAndOrganization(tableId, organization)
                .orElseThrow(() -> new RuntimeException("Table not found"));
        
        // Update fields
        if (tableDto.getCapacity() != null) {
            table.setCapacity(tableDto.getCapacity());
        }
        if (tableDto.getLocationDescription() != null) {
            table.setLocationDescription(tableDto.getLocationDescription());
        }
        if (tableDto.getIsActive() != null) {
            table.setIsActive(tableDto.getIsActive());
        }
        
        table.setUpdatedAt(LocalDateTime.now());
        
        RestaurantTable savedTable = tableRepository.save(table);
        return convertToDto(savedTable);
    }
    
    public void deleteTable(String entityId, Long tableId) {
        logger.info("Deleting table {} for organization: {}", tableId, entityId);
        
        Organization organization = organizationRepository.findByEntityId(entityId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));
        
        RestaurantTable table = tableRepository.findByIdAndOrganization(tableId, organization)
                .orElseThrow(() -> new RuntimeException("Table not found"));
        
        tableRepository.delete(table);
        logger.info("Table {} deleted", tableId);
    }
    
    private RestaurantTableDto convertToDto(RestaurantTable table) {
        RestaurantTableDto dto = new RestaurantTableDto();
        dto.setId(table.getId());
        dto.setTableNumber(table.getTableNumber());
        dto.setCapacity(table.getCapacity());
        dto.setLocationDescription(table.getLocationDescription());
        dto.setIsActive(table.getIsActive());
        dto.setQrCode(table.getQrCode());
        dto.setQrCodeUrl(table.getQrCodeUrl());
        dto.setOrganizationEntityId(table.getOrganization().getEntityId());
        dto.setCreatedAt(table.getCreatedAt());
        dto.setUpdatedAt(table.getUpdatedAt());
        return dto;
    }
}
