package com.example.attendancesystem.table.controller;

import com.example.attendancesystem.shared.dto.RestaurantTableDto;
import com.example.attendancesystem.table.service.TableService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Simplified Table Controller for microservice
 */
@RestController
@RequestMapping("/api/tables")
@CrossOrigin(origins = "*")
public class TableController {
    
    private static final Logger logger = LoggerFactory.getLogger(TableController.class);
    
    @Autowired
    private TableService tableService;
    
    @GetMapping
    public ResponseEntity<?> getTables(@RequestParam String entityId) {
        try {
            List<RestaurantTableDto> tables = tableService.getTablesByOrganization(entityId);
            return ResponseEntity.ok(tables);

        } catch (Exception e) {
            logger.error("Failed to get tables: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get tables"));
        }
    }
    
    @GetMapping("/{tableId}")
    public ResponseEntity<?> getTable(@RequestParam String entityId, @PathVariable Long tableId) {
        try {
            RestaurantTableDto table = tableService.getTableById(entityId, tableId);
            return ResponseEntity.ok(table);

        } catch (Exception e) {
            logger.error("Failed to get table: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get table"));
        }
    }
    
    @GetMapping("/number/{tableNumber}")
    public ResponseEntity<?> getTableByNumber(@RequestParam String entityId, @PathVariable Integer tableNumber) {
        try {
            RestaurantTableDto table = tableService.getTableByNumber(entityId, tableNumber);
            return ResponseEntity.ok(table);

        } catch (Exception e) {
            logger.error("Failed to get table by number: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get table by number"));
        }
    }
    
    @GetMapping("/qr/{qrCode}")
    public ResponseEntity<?> getTableByQrCode(@PathVariable String qrCode) {
        try {
            RestaurantTableDto table = tableService.getTableByQrCode(qrCode);
            return ResponseEntity.ok(table);

        } catch (Exception e) {
            logger.error("Failed to get table by QR code: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get table by QR code"));
        }
    }
    
    @PostMapping
    public ResponseEntity<?> createTable(@RequestParam String entityId, @Valid @RequestBody RestaurantTableDto tableDto) {
        try {
            RestaurantTableDto createdTable = tableService.createTable(entityId, tableDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdTable);

        } catch (Exception e) {
            logger.error("Failed to create table: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create table"));
        }
    }
    
    @PutMapping("/{tableId}")
    public ResponseEntity<?> updateTable(@RequestParam String entityId, @PathVariable Long tableId, @Valid @RequestBody RestaurantTableDto tableDto) {
        try {
            RestaurantTableDto updatedTable = tableService.updateTable(entityId, tableId, tableDto);
            return ResponseEntity.ok(updatedTable);
            
        } catch (Exception e) {
            logger.error("Failed to update table: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update table"));
        }
    }
    
    @DeleteMapping("/{tableId}")
    public ResponseEntity<?> deleteTable(@RequestParam String entityId, @PathVariable Long tableId) {
        try {
            tableService.deleteTable(entityId, tableId);
            return ResponseEntity.ok(Map.of("message", "Table deleted successfully"));
            
        } catch (Exception e) {
            logger.error("Failed to delete table: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete table"));
        }
    }
}
