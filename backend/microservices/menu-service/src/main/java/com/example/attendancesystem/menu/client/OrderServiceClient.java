package com.example.attendancesystem.menu.client;

import com.example.attendancesystem.grpc.order.*;
import com.example.attendancesystem.shared.dto.OrderDto;
import com.example.attendancesystem.shared.dto.OrderItemDto;
import com.example.attendancesystem.shared.model.OrderStatus;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * gRPC client for Order Service
 */
@Service
public class OrderServiceClient {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderServiceClient.class);
    
    @GrpcClient("order-service")
    private OrderServiceGrpc.OrderServiceBlockingStub orderServiceStub;
    
    /**
     * Create a new order via gRPC (with entity ID)
     */
    public OrderDto createOrder(String entityId, OrderDto orderDto) {
        // For now, we'll use a simplified approach
        // In a real implementation, we'd need to resolve entityId to organizationId
        return createOrder(0L, 0L, orderDto.getCustomerName(), orderDto.getCustomerPhone(),
                          orderDto.getOrderItems(), orderDto.getNotes(), 0.0);
    }

    /**
     * Create a new order via gRPC
     */
    public OrderDto createOrder(Long tableId, Long organizationId, String customerName,
                               String customerPhone, List<OrderItemDto> orderItems,
                               String specialInstructions, Double discountAmount) {
        try {
            CreateOrderRequest.Builder requestBuilder = CreateOrderRequest.newBuilder()
                    .setTableId(tableId)
                    .setOrganizationId(organizationId)
                    .setCustomerName(customerName != null ? customerName : "")
                    .setCustomerPhone(customerPhone != null ? customerPhone : "")
                    .setSpecialInstructions(specialInstructions != null ? specialInstructions : "")
                    .setDiscountAmount(discountAmount != null ? discountAmount : 0.0);
            
            // Add order items
            for (OrderItemDto item : orderItems) {
                CreateOrderItemRequest orderItem = CreateOrderItemRequest.newBuilder()
                        .setItemId(item.getItemId())
                        .setQuantity(item.getQuantity())
                        .setSpecialInstructions(item.getSpecialInstructions() != null ? item.getSpecialInstructions() : "")
                        .build();
                requestBuilder.addOrderItems(orderItem);
            }
            
            OrderResponse response = orderServiceStub.createOrder(requestBuilder.build());
            
            if (response.getSuccess()) {
                return convertToOrderDto(response.getOrder());
            } else {
                throw new RuntimeException("Failed to create order: " + response.getMessage());
            }
        } catch (Exception e) {
            logger.error("Error creating order via gRPC", e);
            throw new RuntimeException("Failed to create order: " + e.getMessage());
        }
    }
    
    /**
     * Get order by ID via gRPC
     */
    public OrderDto getOrderById(Long orderId) {
        try {
            GetOrderRequest request = GetOrderRequest.newBuilder()
                    .setId(orderId)
                    .build();

            OrderResponse response = orderServiceStub.getOrder(request);

            if (response.getSuccess()) {
                return convertToOrderDto(response.getOrder());
            } else {
                throw new RuntimeException("Order not found: " + response.getMessage());
            }
        } catch (Exception e) {
            logger.error("Error getting order by ID via gRPC", e);
            throw new RuntimeException("Failed to get order: " + e.getMessage());
        }
    }

    /**
     * Get order by order number via gRPC
     */
    public OrderDto getOrderByNumber(String orderNumber) {
        try {
            // Note: This is a simplified implementation
            // In a real scenario, we'd need a specific gRPC method for getting by order number
            // For now, we'll throw an exception to indicate this needs proper implementation
            throw new RuntimeException("getOrderByNumber not yet implemented in gRPC service");
        } catch (Exception e) {
            logger.error("Error getting order by number via gRPC", e);
            throw new RuntimeException("Failed to get order by number: " + e.getMessage());
        }
    }
    
    /**
     * Convert gRPC Order to OrderDto
     */
    private OrderDto convertToOrderDto(com.example.attendancesystem.grpc.order.Order grpcOrder) {
        OrderDto dto = new OrderDto();
        dto.setId(grpcOrder.getId());
        dto.setOrderNumber(grpcOrder.getOrderNumber());
        dto.setTableNumber(Integer.parseInt(grpcOrder.getTable().getTableNumber()));
        dto.setCustomerName(grpcOrder.getCustomerName());
        dto.setCustomerPhone(grpcOrder.getCustomerPhone());
        dto.setStatus(OrderStatus.valueOf(grpcOrder.getStatus()));
        dto.setTotalAmount(BigDecimal.valueOf(grpcOrder.getTotalAmount()));
        dto.setNotes(grpcOrder.getSpecialInstructions());
        
        // Parse timestamps
        if (!grpcOrder.getCreatedAt().isEmpty()) {
            dto.setCreatedAt(LocalDateTime.parse(grpcOrder.getCreatedAt(), DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
        if (!grpcOrder.getUpdatedAt().isEmpty()) {
            dto.setUpdatedAt(LocalDateTime.parse(grpcOrder.getUpdatedAt(), DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
        
        // Convert order items
        List<OrderItemDto> orderItems = grpcOrder.getOrderItemsList().stream()
                .map(this::convertToOrderItemDto)
                .collect(Collectors.toList());
        dto.setOrderItems(orderItems);
        
        return dto;
    }
    
    /**
     * Convert gRPC OrderItem to OrderItemDto
     */
    private OrderItemDto convertToOrderItemDto(com.example.attendancesystem.grpc.order.OrderItem grpcItem) {
        OrderItemDto dto = new OrderItemDto();
        dto.setId(grpcItem.getId());
        dto.setItemId(grpcItem.getItemId());
        dto.setItemName(grpcItem.getItem().getName());
        dto.setQuantity(grpcItem.getQuantity());
        dto.setPrice(BigDecimal.valueOf(grpcItem.getUnitPrice()));
        dto.setSubtotal(BigDecimal.valueOf(grpcItem.getTotalPrice()));
        dto.setSpecialInstructions(grpcItem.getSpecialInstructions());
        return dto;
    }
}
