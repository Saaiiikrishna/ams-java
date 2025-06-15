package com.example.attendancesystem.controller;

import com.example.attendancesystem.dto.RestaurantTableDto;
import com.example.attendancesystem.model.FeaturePermission;
import com.example.attendancesystem.service.PermissionService;
import com.example.attendancesystem.service.TableService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tables")
@CrossOrigin(origins = "*")
public class TableController {
    
    private static final Logger logger = LoggerFactory.getLogger(TableController.class);
    
    @Autowired
    private TableService tableService;

    @Autowired
    private PermissionService permissionService;

    /**
     * Helper method to check table management permission
     */
    private ResponseEntity<?> checkTablePermission(String entityId) {
        if (!permissionService.hasPermission(entityId, FeaturePermission.TABLE_MANAGEMENT)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied. Table management permission required."));
        }
        return null;
    }
    
    @GetMapping
    @PreAuthorize("hasRole('ENTITY_ADMIN')")
    public ResponseEntity<?> getTables() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String entityId = ((com.example.attendancesystem.security.CustomUserDetails) auth.getPrincipal()).getEntityAdmin().getOrganization().getEntityId();

            ResponseEntity<?> permissionCheck = checkTablePermission(entityId);
            if (permissionCheck != null) return permissionCheck;

            List<RestaurantTableDto> tables = tableService.getTablesByOrganization(entityId);
            return ResponseEntity.ok(tables);
            
        } catch (Exception e) {
            logger.error("Failed to get tables: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/{tableId}")
    @PreAuthorize("hasRole('ENTITY_ADMIN')")
    public ResponseEntity<?> getTable(@PathVariable Long tableId) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String entityId = ((com.example.attendancesystem.security.CustomUserDetails) auth.getPrincipal()).getEntityAdmin().getOrganization().getEntityId();
            
            RestaurantTableDto table = tableService.getTableById(entityId, tableId);
            return ResponseEntity.ok(table);
            
        } catch (Exception e) {
            logger.error("Failed to get table: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/number/{tableNumber}")
    @PreAuthorize("hasRole('ENTITY_ADMIN')")
    public ResponseEntity<?> getTableByNumber(@PathVariable Integer tableNumber) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String entityId = ((com.example.attendancesystem.security.CustomUserDetails) auth.getPrincipal()).getEntityAdmin().getOrganization().getEntityId();
            
            RestaurantTableDto table = tableService.getTableByNumber(entityId, tableNumber);
            return ResponseEntity.ok(table);
            
        } catch (Exception e) {
            logger.error("Failed to get table by number: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/qr/{qrCode}")
    public ResponseEntity<?> getTableByQrCode(@PathVariable String qrCode) {
        try {
            RestaurantTableDto table = tableService.getTableByQrCode(qrCode);
            return ResponseEntity.ok(table);
            
        } catch (Exception e) {
            logger.error("Failed to get table by QR code: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/generate")
    @PreAuthorize("hasRole('ENTITY_ADMIN')")
    public ResponseEntity<?> generateTables(@RequestBody Map<String, Integer> request) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String entityId = ((com.example.attendancesystem.security.CustomUserDetails) auth.getPrincipal()).getEntityAdmin().getOrganization().getEntityId();
            
            Integer numberOfTables = request.get("numberOfTables");
            if (numberOfTables == null || numberOfTables <= 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Number of tables must be greater than 0"));
            }
            
            List<RestaurantTableDto> tables = tableService.createTables(entityId, numberOfTables);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "message", "Tables created successfully",
                    "tables", tables,
                    "count", tables.size()
            ));
            
        } catch (Exception e) {
            logger.error("Failed to generate tables: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping
    @PreAuthorize("hasRole('ENTITY_ADMIN')")
    public ResponseEntity<?> createTable(@Valid @RequestBody RestaurantTableDto tableDto) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String entityId = ((com.example.attendancesystem.security.CustomUserDetails) auth.getPrincipal()).getEntityAdmin().getOrganization().getEntityId();
            
            RestaurantTableDto createdTable = tableService.createSingleTable(entityId, tableDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdTable);
            
        } catch (Exception e) {
            logger.error("Failed to create table: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    @PutMapping("/{tableId}")
    @PreAuthorize("hasRole('ENTITY_ADMIN')")
    public ResponseEntity<?> updateTable(@PathVariable Long tableId, @Valid @RequestBody RestaurantTableDto tableDto) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String entityId = ((com.example.attendancesystem.security.CustomUserDetails) auth.getPrincipal()).getEntityAdmin().getOrganization().getEntityId();
            
            RestaurantTableDto updatedTable = tableService.updateTable(entityId, tableId, tableDto);
            return ResponseEntity.ok(updatedTable);
            
        } catch (Exception e) {
            logger.error("Failed to update table: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    @DeleteMapping("/{tableId}")
    @PreAuthorize("hasRole('ENTITY_ADMIN')")
    public ResponseEntity<?> deleteTable(@PathVariable Long tableId) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String entityId = ((com.example.attendancesystem.security.CustomUserDetails) auth.getPrincipal()).getEntityAdmin().getOrganization().getEntityId();

            ResponseEntity<?> permissionCheck = checkTablePermission(entityId);
            if (permissionCheck != null) return permissionCheck;

            tableService.deleteTable(entityId, tableId);
            return ResponseEntity.ok(Map.of("message", "Table deleted successfully"));

        } catch (Exception e) {
            logger.error("Failed to delete table: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{tableId}/regenerate-qr")
    @PreAuthorize("hasRole('ENTITY_ADMIN')")
    public ResponseEntity<?> regenerateQrCode(@PathVariable Long tableId) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String entityId = ((com.example.attendancesystem.security.CustomUserDetails) auth.getPrincipal()).getEntityAdmin().getOrganization().getEntityId();

            ResponseEntity<?> permissionCheck = checkTablePermission(entityId);
            if (permissionCheck != null) return permissionCheck;

            String qrCodeUrl = tableService.regenerateQrCode(entityId, tableId);
            return ResponseEntity.ok(Map.of(
                "message", "QR code regenerated successfully",
                "qrCodeUrl", qrCodeUrl
            ));
        } catch (Exception e) {
            logger.error("Failed to regenerate QR code for table {}: {}", tableId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/regenerate-all-qr")
    @PreAuthorize("hasRole('ENTITY_ADMIN')")
    public ResponseEntity<?> regenerateAllQrCodes() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String entityId = ((com.example.attendancesystem.security.CustomUserDetails) auth.getPrincipal()).getEntityAdmin().getOrganization().getEntityId();

            ResponseEntity<?> permissionCheck = checkTablePermission(entityId);
            if (permissionCheck != null) return permissionCheck;

            List<RestaurantTableDto> tables = tableService.regenerateAllQrCodes(entityId);
            return ResponseEntity.ok(Map.of(
                "message", "All QR codes regenerated successfully",
                "tablesUpdated", tables.size(),
                "tables", tables
            ));

        } catch (Exception e) {
            logger.error("Failed to regenerate QR code: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
