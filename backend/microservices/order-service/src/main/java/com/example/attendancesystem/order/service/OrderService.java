package com.example.attendancesystem.order.service;

import com.example.attendancesystem.shared.dto.OrderDto;
import com.example.attendancesystem.shared.dto.OrderItemDto;
import com.example.attendancesystem.order.model.Order;
import com.example.attendancesystem.order.model.OrderItem;
import com.example.attendancesystem.order.repository.OrderRepository;
import com.example.attendancesystem.shared.model.Organization;
import com.example.attendancesystem.shared.repository.OrganizationRepository;
import com.example.attendancesystem.shared.model.OrderStatus;
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
    private OrganizationRepository organizationRepository;

    // TODO: Replace with gRPC clients for menu-service and table-service
    // @Autowired
    // private MenuServiceClient menuServiceClient;
    // @Autowired
    // private TableServiceClient tableServiceClient;
    
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

        // TODO: Validate table via gRPC call to table-service
        // For now, we'll just store the table number without validation
        
        Order order = new Order();
        order.setOrganization(organization);
        order.setTableNumber(orderDto.getTableNumber());
        order.setCustomerName(orderDto.getCustomerName());
        order.setCustomerPhone(orderDto.getCustomerPhone());
        order.setNotes(orderDto.getNotes());
        order.setStatus(OrderStatus.PENDING);
        order.setTableNumber(orderDto.getTableNumber());
        
        // Add order items
        if (orderDto.getOrderItems() != null && !orderDto.getOrderItems().isEmpty()) {
            for (OrderItemDto itemDto : orderDto.getOrderItems()) {
                // TODO: Validate item via gRPC call to menu-service
                // For now, we'll create order items without validation

                OrderItem orderItem = new OrderItem();
                orderItem.setItemId(itemDto.getItemId());
                orderItem.setItemName(itemDto.getItemName());
                orderItem.setQuantity(itemDto.getQuantity());
                orderItem.setPrice(itemDto.getPrice()); // Use provided price
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
     * TODO: Implement via gRPC call to table-service
     */
    public OrderDto createOrderByQrCode(String qrCode, OrderDto orderDto) {
        String requestId = java.util.UUID.randomUUID().toString().substring(0, 8);
        logger.info("🚀 [{}] Creating order by QR code: {}", requestId, qrCode);

        // TODO: Replace with gRPC call to table-service to validate QR code and get table info
        throw new RuntimeException("createOrderByQrCode not yet implemented in microservice version - use table-service gRPC");
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
    
    public OrderDto updateOrderStatus(String entityId, Long orderId, OrderStatus newStatus) {
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
        if (newStatus == OrderStatus.SERVED) {
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
        
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Cannot modify order that is not in PENDING status");
        }
        
        // TODO: Validate item via gRPC call to menu-service
        // For now, we'll create order items without validation

        OrderItem orderItem = new OrderItem();
        orderItem.setItemId(itemDto.getItemId());
        orderItem.setItemName(itemDto.getItemName());
        orderItem.setQuantity(itemDto.getQuantity());
        orderItem.setPrice(itemDto.getPrice());
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
        
        if (order.getStatus() == OrderStatus.SERVED) {
            throw new RuntimeException("Cannot cancel completed order");
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        
        logger.info("Order {} cancelled", order.getOrderNumber());
    }
    
    private boolean isValidStatusTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        switch (currentStatus) {
            case PENDING:
                return newStatus == OrderStatus.CONFIRMED || newStatus == OrderStatus.CANCELLED;
            case CONFIRMED:
                return newStatus == OrderStatus.PREPARING || newStatus == OrderStatus.CANCELLED;
            case PREPARING:
                return newStatus == OrderStatus.READY || newStatus == OrderStatus.CANCELLED;
            case READY:
                return newStatus == OrderStatus.SERVED;
            case SERVED:
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
        dto.setItemId(orderItem.getItemId());
        dto.setItemName(orderItem.getItemName());
        dto.setQuantity(orderItem.getQuantity());
        dto.setPrice(orderItem.getPrice());
        dto.setSpecialInstructions(orderItem.getSpecialInstructions());
        dto.setSubtotal(orderItem.getSubtotal());
        return dto;
    }

    // Additional methods needed by the controller

    public OrderDto updateOrder(String entityId, Long orderId, OrderDto orderDto) {
        logger.info("Updating order {} for organization: {}", orderId, entityId);

        Organization organization = organizationRepository.findByEntityId(entityId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));

        Order order = orderRepository.findByIdAndOrganization(orderId, organization)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Update basic fields
        if (orderDto.getCustomerName() != null) {
            order.setCustomerName(orderDto.getCustomerName());
        }
        if (orderDto.getCustomerPhone() != null) {
            order.setCustomerPhone(orderDto.getCustomerPhone());
        }
        if (orderDto.getNotes() != null) {
            order.setNotes(orderDto.getNotes());
        }

        Order savedOrder = orderRepository.save(order);
        return convertToOrderDto(savedOrder);
    }

    public void deleteOrder(String entityId, Long orderId) {
        logger.info("Deleting order {} for organization: {}", orderId, entityId);

        Organization organization = organizationRepository.findByEntityId(entityId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));

        Order order = orderRepository.findByIdAndOrganization(orderId, organization)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Can only delete orders in PENDING status");
        }

        orderRepository.delete(order);
        logger.info("Order {} deleted", orderId);
    }

    public OrderDto removeOrderItem(String entityId, Long orderId, Long itemId) {
        logger.info("Removing item {} from order {} for organization: {}", itemId, orderId, entityId);

        Organization organization = organizationRepository.findByEntityId(entityId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));

        Order order = orderRepository.findByIdAndOrganization(orderId, organization)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Cannot modify order that is not in PENDING status");
        }

        // Remove the order item
        order.getOrderItems().removeIf(item -> item.getId().equals(itemId));
        order.calculateTotalAmount();

        Order savedOrder = orderRepository.save(order);
        logger.info("Item removed from order {}, new total: {}", savedOrder.getOrderNumber(), savedOrder.getTotalAmount());

        return convertToOrderDto(savedOrder);
    }
}
