package com.example.attendancesystem.menu.client;

import com.example.attendancesystem.grpc.table.*;
import com.example.attendancesystem.shared.dto.RestaurantTableDto;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * gRPC client for Table Service
 */
@Service
public class TableServiceClient {
    
    private static final Logger logger = LoggerFactory.getLogger(TableServiceClient.class);
    
    @GrpcClient("table-service")
    private TableServiceGrpc.TableServiceBlockingStub tableServiceStub;
    
    /**
     * Get table by ID via gRPC
     */
    public RestaurantTableDto getTableById(Long tableId) {
        try {
            GetTableRequest request = GetTableRequest.newBuilder()
                    .setId(tableId)
                    .build();
            
            TableResponse response = tableServiceStub.getTable(request);
            
            if (response.getSuccess()) {
                return convertToTableDto(response.getTable());
            } else {
                throw new RuntimeException("Table not found: " + response.getMessage());
            }
        } catch (Exception e) {
            logger.error("Error getting table by ID via gRPC", e);
            throw new RuntimeException("Failed to get table: " + e.getMessage());
        }
    }
    
    /**
     * Get table by QR code via gRPC
     */
    public RestaurantTableDto getTableByQrCode(String qrCode) {
        try {
            ValidateTableQrCodeRequest request = ValidateTableQrCodeRequest.newBuilder()
                    .setQrCodeData(qrCode)
                    .build();

            QrCodeValidationResponse response = tableServiceStub.validateTableQrCode(request);

            if (response.getValid()) {
                // Get the full table details
                return getTableById(response.getTableId());
            } else {
                throw new RuntimeException("Invalid QR code: " + response.getMessage());
            }
        } catch (Exception e) {
            logger.error("Error getting table by QR code via gRPC", e);
            throw new RuntimeException("Failed to get table by QR code: " + e.getMessage());
        }
    }

    /**
     * Get table by number and organization via gRPC
     */
    public RestaurantTableDto getTableByNumber(String entityId, Integer tableNumber) {
        try {
            // For now, we'll need to list tables and find by number
            // This is a limitation of the current gRPC API
            ListTablesRequest request = ListTablesRequest.newBuilder()
                    .setOrganizationId(0) // We'll need to get org ID from entityId
                    .setPage(1)
                    .setSize(100)
                    .build();

            ListTablesResponse response = tableServiceStub.listTables(request);

            if (response.getSuccess()) {
                for (com.example.attendancesystem.grpc.table.RestaurantTable table : response.getTablesList()) {
                    if (table.getTableNumber().equals(tableNumber.toString())) {
                        return convertToTableDto(table);
                    }
                }
                throw new RuntimeException("Table not found with number: " + tableNumber);
            } else {
                throw new RuntimeException("Failed to list tables: " + response.getMessage());
            }
        } catch (Exception e) {
            logger.error("Error getting table by number via gRPC", e);
            throw new RuntimeException("Failed to get table by number: " + e.getMessage());
        }
    }
    
    /**
     * Convert gRPC RestaurantTable to RestaurantTableDto
     */
    private RestaurantTableDto convertToTableDto(com.example.attendancesystem.grpc.table.RestaurantTable grpcTable) {
        RestaurantTableDto dto = new RestaurantTableDto();
        dto.setId(grpcTable.getId());
        dto.setTableNumber(Integer.parseInt(grpcTable.getTableNumber()));
        dto.setCapacity(grpcTable.getCapacity());
        dto.setLocationDescription(grpcTable.getLocation());
        dto.setIsActive(grpcTable.getActive());
        dto.setQrCode(grpcTable.getQrCodeData());
        dto.setQrCodeUrl(grpcTable.getQrCodeUrl());
        
        // Parse timestamps
        if (!grpcTable.getCreatedAt().isEmpty()) {
            dto.setCreatedAt(LocalDateTime.parse(grpcTable.getCreatedAt(), DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
        if (!grpcTable.getUpdatedAt().isEmpty()) {
            dto.setUpdatedAt(LocalDateTime.parse(grpcTable.getUpdatedAt(), DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
        
        return dto;
    }
}
