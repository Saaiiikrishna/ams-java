package com.example.attendancesystem.organization.service;

import com.example.attendancesystem.dto.*;
import com.example.attendancesystem.organization.model.*;
import com.example.attendancesystem.organization.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrderService {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private ItemRepository itemRepository;
    
    @Autowired
    private OrganizationRepository organizationRepository;
    
    @Autowired
    private RestaurantTableRepository restaurantTableRepository;
    
    public List<OrderDto> getOrdersByOrganization(String entityId) {
        logger.debug("Getting orders for organization: {}", entityId);
        List<Order> orders = orderRepository.findByOrganizationEntityId(entityId);
        return orders.stream().map(this::convertToOrderDto).collect(Collectors.toList());
    }
    
    public List<OrderDto> getPendingOrdersByOrganization(String entityId) {
        logger.debug("Getting pending orders for organization: {}", entityId);
        Organization organization = organizationRepository.findByEntityId(entityId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));
        
        List<Order> orders = orderRepository.findPendingOrdersByOrganization(organization);
        return orders.stream().map(this::convertToOrderDto).collect(Collectors.toList());
    }
    
    public List<OrderDto> getTodaysOrdersByOrganization(String entityId) {
        logger.debug("Getting today's orders for organization: {}", entityId);
        Organization organization = organizationRepository.findByEntityId(entityId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));
        
        List<Order> orders = orderRepository.findTodaysOrdersByOrganization(organization);
        return orders.stream().map(this::convertToOrderDto).collect(Collectors.toList());
    }
    
    public OrderDto getOrderById(String entityId, Long orderId) {
        logger.debug("Getting order {} for organization: {}", orderId, entityId);
        Organization organization = organizationRepository.findByEntityId(entityId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));
        
        Order order = orderRepository.findByIdAndOrganization(orderId, organization)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        return convertToOrderDto(order);
    }
    
    public OrderDto createOrder(String entityId, OrderDto orderDto) {
        String requestId = java.util.UUID.randomUUID().toString().substring(0, 8);
        logger.info("🚀 [{}] Creating order for table {} in organization: {}", requestId, orderDto.getTableNumber(), entityId);

        Organization organization = organizationRepository.findByEntityId(entityId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));

        RestaurantTable table = null;
        // Validate table number if provided and link to table
        if (orderDto.getTableNumber() != null) {
            table = restaurantTableRepository.findByTableNumberAndOrganization(orderDto.getTableNumber(), organization)
                    .orElseThrow(() -> new RuntimeException("Table " + orderDto.getTableNumber() + " not found"));

            if (!table.getIsActive()) {
                throw new RuntimeException("Table " + orderDto.getTableNumber() + " is not active");
            }
        }
        
        Order order = new Order();
        order.setOrganization(organization);
        order.setTableNumber(orderDto.getTableNumber());
        order.setTable(table); // Link to the actual table entity
        order.setCustomerName(orderDto.getCustomerName());
        order.setCustomerPhone(orderDto.getCustomerPhone());
        order.setNotes(orderDto.getNotes());
        order.setStatus(Order.OrderStatus.PENDING);
        
        // Add order items
        if (orderDto.getOrderItems() != null && !orderDto.getOrderItems().isEmpty()) {
            for (OrderItemDto itemDto : orderDto.getOrderItems()) {
                Item item = itemRepository.findByIdAndOrganization(itemDto.getItemId(), organization)
                        .orElseThrow(() -> new RuntimeException("Item not found: " + itemDto.getItemId()));
                
                if (!item.getIsActive() || !item.getIsAvailable()) {
                    throw new RuntimeException("Item '" + item.getName() + "' is not available");
                }
                
                OrderItem orderItem = new OrderItem();
                orderItem.setItem(item);
                orderItem.setQuantity(itemDto.getQuantity());
                orderItem.setPrice(item.getPrice()); // Use current item price
                orderItem.setSpecialInstructions(itemDto.getSpecialInstructions());
                
                order.addOrderItem(orderItem);
            }
        }
        
        order.calculateTotalAmount();
        Order savedOrder = orderRepository.save(order);

        logger.info("✅ [{}] Order {} created with total amount: {}", requestId, savedOrder.getOrderNumber(), savedOrder.getTotalAmount());
        return convertToOrderDto(savedOrder);
    }

    /**
     * Create order by QR code for public access
     */
    public OrderDto createOrderByQrCode(String qrCode, OrderDto orderDto) {
        String requestId = java.util.UUID.randomUUID().toString().substring(0, 8);
        logger.info("🚀 [{}] Creating order by QR code: {}", requestId, qrCode);

        // Find table by QR code
        logger.info("🔍 [{}] Looking up table by QR code: {}", requestId, qrCode);
        RestaurantTable table = restaurantTableRepository.findActiveByQrCode(qrCode)
                .orElseThrow(() -> new RuntimeException("Invalid QR code or table not active"));

        logger.info("🔍 [{}] Found table: {} for QR code: {}", requestId, table.getTableNumber(), qrCode);

        // Set table information in order
        orderDto.setTableNumber(table.getTableNumber());

        // Create order using the table's organization
        logger.info("🔍 [{}] Creating order for entity: {}", requestId, table.getOrganization().getEntityId());
        return createOrder(table.getOrganization().getEntityId(), orderDto);
    }

    /**
     * Get order by order number for public access
     */
    public OrderDto getOrderByNumber(String orderNumber) {
        logger.debug("Getting order by number: {}", orderNumber);

        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        return convertToOrderDto(order);
    }
    
    public OrderDto updateOrderStatus(String entityId, Long orderId, Order.OrderStatus newStatus) {
        logger.info("Updating order {} status to {} for organization: {}", orderId, newStatus, entityId);
        
        Organization organization = organizationRepository.findByEntityId(entityId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));
        
        Order order = orderRepository.findByIdAndOrganization(orderId, organization)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        // Validate status transition
        if (!isValidStatusTransition(order.getStatus(), newStatus)) {
            throw new RuntimeException("Invalid status transition from " + order.getStatus() + " to " + newStatus);
        }
        
        order.setStatus(newStatus);
        if (newStatus == Order.OrderStatus.COMPLETED) {
            order.setCompletedAt(LocalDateTime.now());
        }
        
        Order savedOrder = orderRepository.save(order);
        logger.info("Order {} status updated to {}", savedOrder.getOrderNumber(), savedOrder.getStatus());
        
        return convertToOrderDto(savedOrder);
    }
    
    public OrderDto addItemToOrder(String entityId, Long orderId, OrderItemDto itemDto) {
        logger.info("Adding item {} to order {} for organization: {}", itemDto.getItemId(), orderId, entityId);
        
        Organization organization = organizationRepository.findByEntityId(entityId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));
        
        Order order = orderRepository.findByIdAndOrganization(orderId, organization)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        if (order.getStatus() != Order.OrderStatus.PENDING) {
            throw new RuntimeException("Cannot modify order that is not in PENDING status");
        }
        
        Item item = itemRepository.findByIdAndOrganization(itemDto.getItemId(), organization)
                .orElseThrow(() -> new RuntimeException("Item not found"));
        
        if (!item.getIsActive() || !item.getIsAvailable()) {
            throw new RuntimeException("Item '" + item.getName() + "' is not available");
        }
        
        OrderItem orderItem = new OrderItem();
        orderItem.setItem(item);
        orderItem.setQuantity(itemDto.getQuantity());
        orderItem.setPrice(item.getPrice());
        orderItem.setSpecialInstructions(itemDto.getSpecialInstructions());
        
        order.addOrderItem(orderItem);
        order.calculateTotalAmount();
        
        Order savedOrder = orderRepository.save(order);
        logger.info("Item added to order {}, new total: {}", savedOrder.getOrderNumber(), savedOrder.getTotalAmount());
        
        return convertToOrderDto(savedOrder);
    }
    
    public void cancelOrder(String entityId, Long orderId) {
        logger.info("Cancelling order {} for organization: {}", orderId, entityId);
        
        Organization organization = organizationRepository.findByEntityId(entityId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));
        
        Order order = orderRepository.findByIdAndOrganization(orderId, organization)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        if (order.getStatus() == Order.OrderStatus.COMPLETED) {
            throw new RuntimeException("Cannot cancel completed order");
        }
        
        order.setStatus(Order.OrderStatus.CANCELLED);
        orderRepository.save(order);
        
        logger.info("Order {} cancelled", order.getOrderNumber());
    }
    
    private boolean isValidStatusTransition(Order.OrderStatus currentStatus, Order.OrderStatus newStatus) {
        switch (currentStatus) {
            case PENDING:
                return newStatus == Order.OrderStatus.CONFIRMED || newStatus == Order.OrderStatus.CANCELLED;
            case CONFIRMED:
                return newStatus == Order.OrderStatus.PREPARING || newStatus == Order.OrderStatus.CANCELLED;
            case PREPARING:
                return newStatus == Order.OrderStatus.READY || newStatus == Order.OrderStatus.CANCELLED;
            case READY:
                return newStatus == Order.OrderStatus.COMPLETED;
            case COMPLETED:
            case CANCELLED:
                return false; // No transitions allowed from final states
            default:
                return false;
        }
    }
    
    private OrderDto convertToOrderDto(Order order) {
        OrderDto dto = new OrderDto();
        dto.setId(order.getId());
        dto.setOrderNumber(order.getOrderNumber());
        dto.setTableNumber(order.getTableNumber());
        dto.setCustomerName(order.getCustomerName());
        dto.setCustomerPhone(order.getCustomerPhone());
        dto.setStatus(order.getStatus());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setNotes(order.getNotes());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setUpdatedAt(order.getUpdatedAt());
        dto.setCompletedAt(order.getCompletedAt());
        
        if (order.getOrderItems() != null) {
            List<OrderItemDto> orderItems = order.getOrderItems().stream()
                    .map(this::convertToOrderItemDto)
                    .collect(Collectors.toList());
            dto.setOrderItems(orderItems);
        }
        
        return dto;
    }
    
    private OrderItemDto convertToOrderItemDto(OrderItem orderItem) {
        OrderItemDto dto = new OrderItemDto();
        dto.setId(orderItem.getId());
        dto.setItemId(orderItem.getItem().getId());
        dto.setItemName(orderItem.getItem().getName());
        dto.setQuantity(orderItem.getQuantity());
        dto.setPrice(orderItem.getPrice());
        dto.setSpecialInstructions(orderItem.getSpecialInstructions());
        dto.setSubtotal(orderItem.getSubtotal());
        return dto;
    }
}
