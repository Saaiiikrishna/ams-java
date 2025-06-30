package com.example.attendancesystem.order.controller;

import com.example.attendancesystem.shared.dto.OrderDto;
import com.example.attendancesystem.shared.dto.OrderItemDto;
import com.example.attendancesystem.shared.model.OrderStatus;
import com.example.attendancesystem.order.service.OrderService;
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
 * Simplified Order Controller for microservice
 * Handles order management operations without complex security
 */
@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderControllerSimple {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderControllerSimple.class);
    
    @Autowired
    private OrderService orderService;
    
    // Order Management Endpoints
    
    @GetMapping
    public ResponseEntity<?> getOrders(@RequestParam String entityId) {
        try {
            List<OrderDto> orders = orderService.getOrdersByOrganization(entityId);
            return ResponseEntity.ok(orders);

        } catch (Exception e) {
            logger.error("Failed to get orders: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get orders"));
        }
    }
    
    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrder(@RequestParam String entityId, @PathVariable Long orderId) {
        try {
            OrderDto order = orderService.getOrderById(entityId, orderId);
            return ResponseEntity.ok(order);

        } catch (Exception e) {
            logger.error("Failed to get order: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get order"));
        }
    }
    
    @GetMapping("/number/{orderNumber}")
    public ResponseEntity<?> getOrderByNumber(@RequestParam String entityId, @PathVariable String orderNumber) {
        try {
            OrderDto order = orderService.getOrderByNumber(orderNumber);
            return ResponseEntity.ok(order);

        } catch (Exception e) {
            logger.error("Failed to get order by number: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get order by number"));
        }
    }
    
    @PostMapping
    public ResponseEntity<?> createOrder(@RequestParam String entityId, @Valid @RequestBody OrderDto orderDto) {
        try {
            OrderDto createdOrder = orderService.createOrder(entityId, orderDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdOrder);

        } catch (Exception e) {
            logger.error("Failed to create order: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create order"));
        }
    }
    
    @PutMapping("/{orderId}")
    public ResponseEntity<?> updateOrder(@RequestParam String entityId, @PathVariable Long orderId, @Valid @RequestBody OrderDto orderDto) {
        try {
            OrderDto updatedOrder = orderService.updateOrder(entityId, orderId, orderDto);
            return ResponseEntity.ok(updatedOrder);
            
        } catch (Exception e) {
            logger.error("Failed to update order: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update order"));
        }
    }
    
    @PutMapping("/{orderId}/status")
    public ResponseEntity<?> updateOrderStatus(@RequestParam String entityId, @PathVariable Long orderId, @RequestParam OrderStatus status) {
        try {
            OrderDto updatedOrder = orderService.updateOrderStatus(entityId, orderId, status);
            return ResponseEntity.ok(updatedOrder);
            
        } catch (Exception e) {
            logger.error("Failed to update order status: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update order status"));
        }
    }
    
    @DeleteMapping("/{orderId}")
    public ResponseEntity<?> deleteOrder(@RequestParam String entityId, @PathVariable Long orderId) {
        try {
            orderService.deleteOrder(entityId, orderId);
            return ResponseEntity.ok(Map.of("message", "Order deleted successfully"));
            
        } catch (Exception e) {
            logger.error("Failed to delete order: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete order"));
        }
    }
    
    // Order Item Management
    
    @PostMapping("/{orderId}/items")
    public ResponseEntity<?> addOrderItem(@RequestParam String entityId, @PathVariable Long orderId, @Valid @RequestBody OrderItemDto orderItemDto) {
        try {
            OrderDto updatedOrder = orderService.addItemToOrder(entityId, orderId, orderItemDto);
            return ResponseEntity.ok(updatedOrder);
            
        } catch (Exception e) {
            logger.error("Failed to add order item: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to add order item"));
        }
    }
    
    @DeleteMapping("/{orderId}/items/{itemId}")
    public ResponseEntity<?> removeOrderItem(@RequestParam String entityId, @PathVariable Long orderId, @PathVariable Long itemId) {
        try {
            OrderDto updatedOrder = orderService.removeOrderItem(entityId, orderId, itemId);
            return ResponseEntity.ok(updatedOrder);
            
        } catch (Exception e) {
            logger.error("Failed to remove order item: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to remove order item"));
        }
    }
}
