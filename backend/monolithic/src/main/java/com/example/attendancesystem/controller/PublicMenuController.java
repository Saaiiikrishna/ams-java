package com.example.attendancesystem.subscriber.controller;

import com.example.attendancesystem.dto.CategoryDto;
import com.example.attendancesystem.dto.OrderDto;
import com.example.attendancesystem.dto.RestaurantTableDto;
import com.example.attendancesystem.dto.OrderItemDto;
import com.example.attendancesystem.model.Order;
import com.example.attendancesystem.service.MenuService;
import com.example.attendancesystem.service.OrderService;
import com.example.attendancesystem.service.TableService;
import com.example.attendancesystem.repository.RestaurantTableRepository;
import com.example.attendancesystem.model.RestaurantTable;
import org.springframework.ui.Model;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import java.nio.charset.StandardCharsets;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.springframework.web.bind.annotation.ModelAttribute;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/public/menu")
@CrossOrigin(origins = "*")
public class PublicMenuController {

    private static final Logger logger = LoggerFactory.getLogger(PublicMenuController.class);

    // Log all requests to this controller
    @ModelAttribute
    public void logRequest(HttpServletRequest request) {
        logger.info("=== INCOMING REQUEST TO PUBLIC CONTROLLER ===");
        logger.info("Method: {}", request.getMethod());
        logger.info("URI: {}", request.getRequestURI());
        logger.info("Query String: {}", request.getQueryString());
        logger.info("Remote Address: {}", request.getRemoteAddr());
    }
    
    @Autowired
    private MenuService menuService;
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private TableService tableService;

    @Autowired
    private RestaurantTableRepository restaurantTableRepository;

    /**
     * Get menu for a specific organization (public access)
     */
    @GetMapping("/{entityId}")
    public ResponseEntity<?> getMenu(@PathVariable String entityId,
                                   @RequestParam(required = false) Integer table,
                                   @RequestParam(required = false) String qr) {
        try {
            logger.info("Getting menu for entity: {}, table: {}, qr: {}", entityId, table, qr);
            
            // Validate table and QR code if provided (optional - don't block menu loading)
            if (table != null && qr != null) {
                try {
                    RestaurantTableDto tableDto = tableService.getTableByQrCode(qr);
                    if (!tableDto.getTableNumber().equals(table)) {
                        logger.warn("QR code validation failed: table mismatch. Expected: {}, Got: {}", table, tableDto.getTableNumber());
                        // Don't return error - continue with menu loading
                    } else {
                        logger.info("QR code validation successful for table: {}", table);
                    }
                } catch (Exception e) {
                    logger.warn("QR code validation failed: {}, but continuing with menu load", e.getMessage());
                    // Don't return error - continue with menu loading
                }
            }
            
            List<CategoryDto> menu = menuService.getCategoriesWithItemsByOrganization(entityId);
            
            Map<String, Object> response = Map.of(
                    "entityId", entityId,
                    "tableNumber", table != null ? table : 0,
                    "menu", menu,
                    "timestamp", System.currentTimeMillis()
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to get menu: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Menu not found"));
        }
    }
    
    /**
     * Place an order (public access)
     */
    @PostMapping("/{entityId}/order")
    public ResponseEntity<?> placeOrder(@PathVariable String entityId,
                                      @RequestParam(required = false) Integer table,
                                      @RequestParam(required = false) String qr,
                                      @Valid @RequestBody OrderDto orderDto) {
        try {
            logger.info("Placing order for entity: {}, table: {}", entityId, table);
            
            // Validate table and QR code if provided
            if (table != null && qr != null) {
                try {
                    RestaurantTableDto tableDto = tableService.getTableByQrCode(qr);
                    if (!tableDto.getTableNumber().equals(table)) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(Map.of("error", "Invalid table or QR code"));
                    }
                } catch (Exception e) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("error", "Invalid QR code"));
                }
            }
            
            // Set table number from URL parameter
            if (table != null) {
                orderDto.setTableNumber(table);
            }
            
            // Validate order has items
            if (orderDto.getOrderItems() == null || orderDto.getOrderItems().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Order must contain at least one item"));
            }
            
            OrderDto createdOrder = orderService.createOrder(entityId, orderDto);
            
            Map<String, Object> response = Map.of(
                    "message", "Order placed successfully",
                    "order", createdOrder,
                    "orderNumber", createdOrder.getOrderNumber(),
                    "estimatedTime", "15-20 minutes"
            );
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            logger.error("Failed to place order: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Get order status (public access)
     */
    @GetMapping("/order/{orderNumber}")
    public ResponseEntity<?> getOrderStatus(@PathVariable String orderNumber) {
        try {
            logger.info("Getting order status for: {}", orderNumber);
            
            OrderDto order = orderService.getOrderByNumber(orderNumber);
            
            Map<String, Object> response = Map.of(
                    "orderNumber", order.getOrderNumber(),
                    "status", order.getStatus(),
                    "tableNumber", order.getTableNumber() != null ? order.getTableNumber() : 0,
                    "totalAmount", order.getTotalAmount(),
                    "createdAt", order.getCreatedAt(),
                    "estimatedTime", getEstimatedTime(order.getStatus())
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to get order status: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Order not found"));
        }
    }
    
    /**
     * Get menu for a specific table by table ID (public access)
     */
    @GetMapping("/tables/{tableId}/menu")
    public ResponseEntity<?> getTableMenu(@PathVariable Long tableId) {
        try {
            logger.info("Getting menu for table ID: {}", tableId);

            // Get table information
            RestaurantTableDto table = tableService.getTableById(tableId);
            String entityId = table.getOrganizationEntityId();

            // Get menu for the organization
            List<CategoryDto> menu = menuService.getCategoriesWithItemsByOrganization(entityId);

            Map<String, Object> response = Map.of(
                    "tableId", tableId,
                    "tableNumber", table.getTableNumber(),
                    "entityId", entityId,
                    "menu", menu,
                    "timestamp", System.currentTimeMillis()
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to get table menu: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Table menu not found"));
        }
    }

    /**
     * Get menu for a specific table by table number (public access)
     * This is the endpoint that QR codes should point to
     */
    @GetMapping("/tables/number/{tableNumber}/menu")
    public ResponseEntity<?> getTableMenuByNumber(@PathVariable Integer tableNumber,
                                                  @RequestParam(required = false) String entityId) {
        try {
            logger.info("Getting menu for table number: {} in entity: {}", tableNumber, entityId);

            // If entityId is not provided, try to find the table by number across all entities
            // For now, we'll assume there's only one active entity or require entityId
            if (entityId == null) {
                // Try to find the table by number - this assumes table numbers are unique across the system
                // In a multi-tenant system, you might need to pass entityId as a parameter
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Entity ID is required"));
            }

            // Get table information by number
            RestaurantTableDto table = tableService.getTableByNumber(entityId, tableNumber);

            // Get menu for the organization
            List<CategoryDto> menu = menuService.getCategoriesWithItemsByOrganization(entityId);

            Map<String, Object> response = Map.of(
                    "tableId", table.getId(),
                    "tableNumber", table.getTableNumber(),
                    "entityId", entityId,
                    "menu", menu,
                    "timestamp", System.currentTimeMillis()
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to get table menu by number: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Table menu not found"));
        }
    }

    /**
     * Get table information by QR code (public access)
     */
    @GetMapping("/table/qr/{qrCode}")
    public ResponseEntity<?> getTableByQr(@PathVariable String qrCode) {
        try {
            logger.info("Getting table info for QR: {}", qrCode);
            
            RestaurantTableDto table = tableService.getTableByQrCode(qrCode);
            
            Map<String, Object> response = Map.of(
                    "tableNumber", table.getTableNumber(),
                    "capacity", table.getCapacity() != null ? table.getCapacity() : 4,
                    "location", table.getLocationDescription() != null ? table.getLocationDescription() : "",
                    "menuUrl", table.getMenuUrl()
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to get table by QR: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Table not found"));
        }
    }

    /**
     * Create order from table (public access)
     */
    @PostMapping("/orders")
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> orderRequest) {
        String requestId = java.util.UUID.randomUUID().toString().substring(0, 8);
        try {
            logger.info("🚀 [{}] Creating order: {}", requestId, orderRequest);

            Long tableId = Long.valueOf(orderRequest.get("tableId").toString());
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) orderRequest.get("items");

            logger.info("🔍 [{}] Parsed tableId: {}, items count: {}", requestId, tableId, items.size());

            // Get table information
            logger.info("🔍 [{}] Getting table by ID: {}", requestId, tableId);
            RestaurantTableDto table = tableService.getTableById(tableId);
            String entityId = table.getOrganizationEntityId();
            logger.info("🔍 [{}] Found table: {} for entity: {}", requestId, table.getTableNumber(), entityId);

            // Create order DTO
            OrderDto orderDto = new OrderDto();
            orderDto.setTableNumber(table.getTableNumber());
            orderDto.setCustomerName((String) orderRequest.getOrDefault("customerName", "Customer"));
            orderDto.setCustomerPhone((String) orderRequest.get("customerPhone"));
            orderDto.setNotes((String) orderRequest.get("notes"));
            orderDto.setStatus(Order.OrderStatus.PENDING);

            // Convert items
            List<OrderItemDto> orderItems = items.stream().map(item -> {
                OrderItemDto orderItem = new OrderItemDto();
                orderItem.setItemId(Long.valueOf(item.get("id").toString()));
                orderItem.setQuantity(Integer.valueOf(item.get("qty").toString()));
                return orderItem;
            }).collect(java.util.stream.Collectors.toList());

            orderDto.setOrderItems(orderItems);

            // Create the order
            logger.info("🔍 [{}] Creating order with {} items for entity: {}", requestId, orderDto.getOrderItems().size(), entityId);
            OrderDto createdOrder = orderService.createOrder(entityId, orderDto);

            Map<String, Object> response = Map.of(
                    "orderNumber", createdOrder.getOrderNumber(),
                    "tableNumber", table.getTableNumber(),
                    "status", "PENDING",
                    "message", "Order placed successfully"
            );

            logger.info("✅ [{}] Order created successfully: {}", requestId, createdOrder.getOrderNumber());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            logger.error("❌ [{}] Failed to create order: {}", requestId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Fix QR codes for all tables (temporary public endpoint)
     */
    @PostMapping("/fix-qr-codes")
    public ResponseEntity<?> fixQrCodes() {
        try {
            logger.info("Fixing QR codes for all tables (public endpoint)");

            // This is a temporary fix endpoint - in production, this should be secured
            List<RestaurantTable> allTables = restaurantTableRepository.findAll();
            int updatedCount = 0;

            for (RestaurantTable table : allTables) {
                if (table.getId() != null && table.getIsActive()) {
                    // Generate new QR code using table ID for consistency
                    table.setQrCode("TABLE-" + table.getId() + "-" + System.currentTimeMillis());
                    // Use mDNS hostname for better WiFi network compatibility
                    String qrCodeData = "http://restaurant.local:8080/menu.html?entityId=" + table.getOrganization().getEntityId() + "&table=" + table.getTableNumber() + "&qr=" + table.getQrCode();
                    try {
                        table.setQrCodeUrl("https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=" +
                                          java.net.URLEncoder.encode(qrCodeData, "UTF-8"));
                    } catch (Exception e) {
                        table.setQrCodeUrl("https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=" + qrCodeData);
                    }

                    restaurantTableRepository.save(table);
                    updatedCount++;
                    logger.info("Fixed QR code for table {} (ID: {})", table.getTableNumber(), table.getId());
                }
            }

            return ResponseEntity.ok(Map.of(
                "message", "QR codes fixed successfully",
                "tablesUpdated", updatedCount
            ));

        } catch (Exception e) {
            logger.error("Failed to fix QR codes: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Check for duplicate QR codes (debugging endpoint)
     */
    @GetMapping("/debug/duplicate-qr-codes")
    public ResponseEntity<?> checkDuplicateQrCodes() {
        try {
            logger.info("Checking for duplicate QR codes...");

            List<RestaurantTable> allTables = restaurantTableRepository.findAll();
            Map<String, List<RestaurantTable>> qrCodeGroups = allTables.stream()
                .filter(table -> table.getQrCode() != null)
                .collect(Collectors.groupingBy(RestaurantTable::getQrCode));

            Map<String, List<Map<String, Object>>> duplicates = new HashMap<>();
            int totalDuplicates = 0;

            for (Map.Entry<String, List<RestaurantTable>> entry : qrCodeGroups.entrySet()) {
                if (entry.getValue().size() > 1) {
                    List<Map<String, Object>> tableInfo = entry.getValue().stream()
                        .map(table -> {
                            Map<String, Object> info = new HashMap<>();
                            info.put("id", table.getId());
                            info.put("tableNumber", table.getTableNumber());
                            info.put("entityId", table.getOrganization().getEntityId());
                            info.put("isActive", table.getIsActive());
                            info.put("createdAt", table.getCreatedAt());
                            return info;
                        })
                        .collect(Collectors.toList());
                    duplicates.put(entry.getKey(), tableInfo);
                    totalDuplicates += entry.getValue().size();
                }
            }

            logger.info("Found {} duplicate QR codes affecting {} tables", duplicates.size(), totalDuplicates);

            return ResponseEntity.ok(Map.of(
                "totalTables", allTables.size(),
                "duplicateQrCodes", duplicates.size(),
                "affectedTables", totalDuplicates,
                "duplicates", duplicates
            ));

        } catch (Exception e) {
            logger.error("Failed to check duplicate QR codes: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to check duplicates: " + e.getMessage()));
        }
    }

    /**
     * Serve HTML menu page directly - FIXED VERSION
     */
    @GetMapping("/page/{entityId}")
    public ResponseEntity<String> getMenuPage(@PathVariable String entityId,
                                            @RequestParam(required = false) String table,
                                            @RequestParam(required = false) String qr) {
        logger.info("=== MENU PAGE REQUEST RECEIVED ===");
        logger.info("Entity ID: {}", entityId);
        logger.info("Table: {}", table);
        logger.info("QR Code: {}", qr);
        logger.info("Request URL: /api/public/page/{}", entityId);

        try {
            // Read the HTML file and inject parameters
            ClassPathResource resource = new ClassPathResource("static/menu.html");
            String htmlContent = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            // Inject parameters into the HTML
            String modifiedHtml = htmlContent.replace(
                "const entityId = urlParams.get('entityId');",
                String.format("const entityId = '%s';", entityId)
            );

            if (table != null) {
                modifiedHtml = modifiedHtml.replace(
                    "const tableNumber = urlParams.get('table');",
                    String.format("const tableNumber = '%s';", table)
                );
            }

            if (qr != null) {
                modifiedHtml = modifiedHtml.replace(
                    "const qrCode = urlParams.get('qr');",
                    String.format("const qrCode = '%s';", qr)
                );
            }

            logger.info("=== HTML CONTENT SERVED SUCCESSFULLY ===");
            logger.info("Content length: {} characters", modifiedHtml.length());

            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(modifiedHtml);

        } catch (Exception e) {
            logger.error("=== ERROR SERVING HTML CONTENT ===", e);
            String errorHtml = String.format(
                "<html><body><h1>Restaurant Menu</h1><p>Loading menu for entity: %s, table: %s</p><script>console.log('Error: %s'); setTimeout(() => window.location.reload(), 2000);</script></body></html>",
                entityId, table, e.getMessage()
            );
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(errorHtml);
        }
    }

    /**
     * Connectivity test endpoint
     */
    @GetMapping("/test-connectivity")
    public ResponseEntity<Map<String, Object>> testConnectivity(HttpServletRequest request) {
        logger.info("=== CONNECTIVITY TEST REQUEST ===");
        logger.info("Remote Address: {}", request.getRemoteAddr());
        logger.info("User Agent: {}", request.getHeader("User-Agent"));
        logger.info("Timestamp: {}", System.currentTimeMillis());

        Map<String, Object> response = new HashMap<>();
        response.put("status", "OK");
        response.put("timestamp", System.currentTimeMillis());
        response.put("server", "Spring Boot Backend");
        response.put("remoteAddress", request.getRemoteAddr());

        return ResponseEntity.ok(response);
    }

    /**
     * Catch-all endpoint for debugging
     */
    @GetMapping("/**")
    public ResponseEntity<String> catchAll(HttpServletRequest request) {
        logger.info("=== CATCH-ALL ENDPOINT HIT ===");
        logger.info("Full URI: {}", request.getRequestURI());
        logger.info("Method: {}", request.getMethod());
        logger.info("Query String: {}", request.getQueryString());

        String debugInfo = String.format(
            "<html><body><h1>Debug Info</h1><p>URI: %s</p><p>Method: %s</p><p>Query: %s</p></body></html>",
            request.getRequestURI(), request.getMethod(), request.getQueryString()
        );

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(debugInfo);
    }

    private String getEstimatedTime(com.example.attendancesystem.model.Order.OrderStatus status) {
        switch (status) {
            case PENDING:
                return "Order received, waiting for confirmation";
            case CONFIRMED:
                return "Order confirmed, preparing in 5-10 minutes";
            case PREPARING:
                return "Your order is being prepared, 10-15 minutes";
            case READY:
                return "Your order is ready for pickup!";
            case COMPLETED:
                return "Order completed";
            case CANCELLED:
                return "Order cancelled";
            default:
                return "Unknown status";
        }
    }
}
