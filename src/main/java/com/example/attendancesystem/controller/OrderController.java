package com.example.attendancesystem.controller;

import com.example.attendancesystem.dto.OrderDto;
import com.example.attendancesystem.dto.OrderItemDto;
import com.example.attendancesystem.model.FeaturePermission;
import com.example.attendancesystem.model.Order;
import com.example.attendancesystem.service.OrderService;
import com.example.attendancesystem.service.PermissionService;
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
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);
    
    @Autowired
    private OrderService orderService;

    @Autowired
    private PermissionService permissionService;

    /**
     * Helper method to check order management permission
     */
    private ResponseEntity<?> checkOrderPermission(String entityId) {
        if (!permissionService.hasPermission(entityId, FeaturePermission.ORDER_MANAGEMENT)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied. Order management permission required."));
        }
        return null;
    }
    
    @GetMapping
    @PreAuthorize("hasRole('ENTITY_ADMIN')")
    public ResponseEntity<?> getOrders() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String entityId = ((com.example.attendancesystem.security.CustomUserDetails) auth.getPrincipal()).getEntityAdmin().getOrganization().getEntityId();

            ResponseEntity<?> permissionCheck = checkOrderPermission(entityId);
            if (permissionCheck != null) return permissionCheck;

            List<OrderDto> orders = orderService.getOrdersByOrganization(entityId);
            return ResponseEntity.ok(orders);
            
        } catch (Exception e) {
            logger.error("Failed to get orders: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/pending")
    @PreAuthorize("hasRole('ENTITY_ADMIN')")
    public ResponseEntity<?> getPendingOrders() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String entityId = ((com.example.attendancesystem.security.CustomUserDetails) auth.getPrincipal()).getEntityAdmin().getOrganization().getEntityId();

            ResponseEntity<?> permissionCheck = checkOrderPermission(entityId);
            if (permissionCheck != null) return permissionCheck;

            List<OrderDto> orders = orderService.getPendingOrdersByOrganization(entityId);
            return ResponseEntity.ok(orders);
            
        } catch (Exception e) {
            logger.error("Failed to get pending orders: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/today")
    @PreAuthorize("hasRole('ENTITY_ADMIN')")
    public ResponseEntity<?> getTodaysOrders() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String entityId = ((com.example.attendancesystem.security.CustomUserDetails) auth.getPrincipal()).getEntityAdmin().getOrganization().getEntityId();

            ResponseEntity<?> permissionCheck = checkOrderPermission(entityId);
            if (permissionCheck != null) return permissionCheck;

            List<OrderDto> orders = orderService.getTodaysOrdersByOrganization(entityId);
            return ResponseEntity.ok(orders);
            
        } catch (Exception e) {
            logger.error("Failed to get today's orders: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/{orderId}")
    @PreAuthorize("hasRole('ENTITY_ADMIN')")
    public ResponseEntity<?> getOrder(@PathVariable Long orderId) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String entityId = ((com.example.attendancesystem.security.CustomUserDetails) auth.getPrincipal()).getEntityAdmin().getOrganization().getEntityId();

            ResponseEntity<?> permissionCheck = checkOrderPermission(entityId);
            if (permissionCheck != null) return permissionCheck;

            OrderDto order = orderService.getOrderById(entityId, orderId);
            return ResponseEntity.ok(order);
            
        } catch (Exception e) {
            logger.error("Failed to get order: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/number/{orderNumber}")
    public ResponseEntity<?> getOrderByNumber(@PathVariable String orderNumber) {
        try {
            OrderDto order = orderService.getOrderByNumber(orderNumber);
            return ResponseEntity.ok(order);
            
        } catch (Exception e) {
            logger.error("Failed to get order by number: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping
    public ResponseEntity<?> createOrder(@Valid @RequestBody OrderDto orderDto) {
        try {
            // For public orders, we need to get entityId from the request or table info
            // For now, we'll assume it's provided in the request
            String entityId = orderDto.getTableNumber() != null ? 
                extractEntityIdFromTable(orderDto.getTableNumber()) : 
                getCurrentEntityId();
            
            OrderDto createdOrder = orderService.createOrder(entityId, orderDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdOrder);
            
        } catch (Exception e) {
            logger.error("Failed to create order: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    @PutMapping("/{orderId}/status")
    @PreAuthorize("hasRole('ENTITY_ADMIN')")
    public ResponseEntity<?> updateOrderStatus(@PathVariable Long orderId, @RequestBody Map<String, String> statusUpdate) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String entityId = ((com.example.attendancesystem.security.CustomUserDetails) auth.getPrincipal()).getEntityAdmin().getOrganization().getEntityId();
            
            String statusStr = statusUpdate.get("status");
            if (statusStr == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Status is required"));
            }
            
            Order.OrderStatus newStatus = Order.OrderStatus.valueOf(statusStr.toUpperCase());
            OrderDto updatedOrder = orderService.updateOrderStatus(entityId, orderId, newStatus);
            return ResponseEntity.ok(updatedOrder);
            
        } catch (IllegalArgumentException e) {
            logger.error("Invalid status: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Invalid status"));
        } catch (Exception e) {
            logger.error("Failed to update order status: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/{orderId}/items")
    @PreAuthorize("hasRole('ENTITY_ADMIN')")
    public ResponseEntity<?> addItemToOrder(@PathVariable Long orderId, @Valid @RequestBody OrderItemDto itemDto) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String entityId = ((com.example.attendancesystem.security.CustomUserDetails) auth.getPrincipal()).getEntityAdmin().getOrganization().getEntityId();
            
            OrderDto updatedOrder = orderService.addItemToOrder(entityId, orderId, itemDto);
            return ResponseEntity.ok(updatedOrder);
            
        } catch (Exception e) {
            logger.error("Failed to add item to order: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    @DeleteMapping("/{orderId}")
    @PreAuthorize("hasRole('ENTITY_ADMIN')")
    public ResponseEntity<?> cancelOrder(@PathVariable Long orderId) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String entityId = ((com.example.attendancesystem.security.CustomUserDetails) auth.getPrincipal()).getEntityAdmin().getOrganization().getEntityId();
            
            orderService.cancelOrder(entityId, orderId);
            return ResponseEntity.ok(Map.of("message", "Order cancelled successfully"));
            
        } catch (Exception e) {
            logger.error("Failed to cancel order: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    private String extractEntityIdFromTable(Integer tableNumber) {
        // This would need to be implemented to extract entityId from table context
        // For now, return a placeholder
        return "default-entity";
    }
    
    private String getCurrentEntityId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            return ((com.example.attendancesystem.security.CustomUserDetails) auth.getPrincipal()).getEntityAdmin().getOrganization().getEntityId();
        } catch (Exception e) {
            return "default-entity";
        }
    }
}
