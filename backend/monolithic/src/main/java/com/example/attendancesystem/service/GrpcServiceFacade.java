package com.example.attendancesystem.subscriber.service;

import com.example.attendancesystem.grpc.auth.AuthServiceGrpc;
import com.example.attendancesystem.grpc.attendance.AttendanceServiceGrpc;
import com.example.attendancesystem.grpc.menu.MenuServiceGrpc;
import com.example.attendancesystem.grpc.order.OrderServiceGrpc;
import com.example.attendancesystem.grpc.organization.OrganizationServiceGrpc;
import com.example.attendancesystem.grpc.subscriber.SubscriberServiceGrpc;
import com.example.attendancesystem.grpc.table.TableServiceGrpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Facade service that provides a unified interface to all gRPC services
 * This service abstracts the complexity of gRPC communication and provides
 * a clean interface for REST controllers and other components
 */
@Service
public class GrpcServiceFacade {

    private static final Logger logger = LoggerFactory.getLogger(GrpcServiceFacade.class);

    // gRPC Service Clients (can be null if services are not available)
    @Autowired(required = false)
    private AuthServiceGrpc.AuthServiceBlockingStub authServiceClient;

    @Autowired(required = false)
    private OrganizationServiceGrpc.OrganizationServiceBlockingStub organizationServiceClient;

    @Autowired(required = false)
    private SubscriberServiceGrpc.SubscriberServiceBlockingStub subscriberServiceClient;

    @Autowired(required = false)
    private AttendanceServiceGrpc.AttendanceServiceBlockingStub attendanceServiceClient;

    @Autowired(required = false)
    private MenuServiceGrpc.MenuServiceBlockingStub menuServiceClient;

    @Autowired(required = false)
    private OrderServiceGrpc.OrderServiceBlockingStub orderServiceClient;

    @Autowired(required = false)
    private TableServiceGrpc.TableServiceBlockingStub tableServiceClient;

    /**
     * Check if a specific service is available
     */
    public boolean isServiceAvailable(String serviceName) {
        switch (serviceName.toLowerCase()) {
            case "auth":
            case "auth-service":
                return authServiceClient != null;
            case "organization":
            case "organization-service":
                return organizationServiceClient != null;
            case "subscriber":
            case "subscriber-service":
                return subscriberServiceClient != null;
            case "attendance":
            case "attendance-service":
                return attendanceServiceClient != null;
            case "menu":
            case "menu-service":
                return menuServiceClient != null;
            case "order":
            case "order-service":
                return orderServiceClient != null;
            case "table":
            case "table-service":
                return tableServiceClient != null;
            default:
                return false;
        }
    }

    /**
     * Get Auth Service Client
     */
    public AuthServiceGrpc.AuthServiceBlockingStub getAuthService() {
        if (authServiceClient == null) {
            logger.warn("Auth service is not available");
            throw new RuntimeException("Auth service is not available");
        }
        return authServiceClient;
    }

    /**
     * Get Organization Service Client
     */
    public OrganizationServiceGrpc.OrganizationServiceBlockingStub getOrganizationService() {
        if (organizationServiceClient == null) {
            logger.warn("Organization service is not available");
            throw new RuntimeException("Organization service is not available");
        }
        return organizationServiceClient;
    }

    /**
     * Get Subscriber Service Client
     */
    public SubscriberServiceGrpc.SubscriberServiceBlockingStub getSubscriberService() {
        if (subscriberServiceClient == null) {
            logger.warn("Subscriber service is not available");
            throw new RuntimeException("Subscriber service is not available");
        }
        return subscriberServiceClient;
    }

    /**
     * Get Attendance Service Client
     */
    public AttendanceServiceGrpc.AttendanceServiceBlockingStub getAttendanceService() {
        if (attendanceServiceClient == null) {
            logger.warn("Attendance service is not available");
            throw new RuntimeException("Attendance service is not available");
        }
        return attendanceServiceClient;
    }

    /**
     * Get Menu Service Client
     */
    public MenuServiceGrpc.MenuServiceBlockingStub getMenuService() {
        if (menuServiceClient == null) {
            logger.warn("Menu service is not available");
            throw new RuntimeException("Menu service is not available");
        }
        return menuServiceClient;
    }

    /**
     * Get Order Service Client
     */
    public OrderServiceGrpc.OrderServiceBlockingStub getOrderService() {
        if (orderServiceClient == null) {
            logger.warn("Order service is not available");
            throw new RuntimeException("Order service is not available");
        }
        return orderServiceClient;
    }

    /**
     * Get Table Service Client
     */
    public TableServiceGrpc.TableServiceBlockingStub getTableService() {
        if (tableServiceClient == null) {
            logger.warn("Table service is not available");
            throw new RuntimeException("Table service is not available");
        }
        return tableServiceClient;
    }

    /**
     * Get service health status
     */
    public java.util.Map<String, Object> getServiceHealth() {
        java.util.Map<String, Object> health = new java.util.HashMap<>();
        
        health.put("authService", isServiceAvailable("auth"));
        health.put("organizationService", isServiceAvailable("organization"));
        health.put("subscriberService", isServiceAvailable("subscriber"));
        health.put("attendanceService", isServiceAvailable("attendance"));
        health.put("menuService", isServiceAvailable("menu"));
        health.put("orderService", isServiceAvailable("order"));
        health.put("tableService", isServiceAvailable("table"));
        
        long availableServices = health.values().stream()
                .mapToLong(v -> (Boolean) v ? 1 : 0)
                .sum();
        
        health.put("totalServices", health.size());
        health.put("availableServices", availableServices);
        health.put("healthPercentage", (availableServices * 100.0) / health.size());
        
        return health;
    }

    /**
     * Execute a gRPC call with error handling and fallback
     */
    public <T> T executeWithFallback(java.util.function.Supplier<T> grpcCall, T fallbackValue, String operationName) {
        try {
            return grpcCall.get();
        } catch (Exception e) {
            logger.error("gRPC call failed for operation: {}", operationName, e);
            return fallbackValue;
        }
    }

    /**
     * Execute a gRPC call with error handling and exception throwing
     */
    public <T> T executeWithException(java.util.function.Supplier<T> grpcCall, String operationName) {
        try {
            return grpcCall.get();
        } catch (Exception e) {
            logger.error("gRPC call failed for operation: {}", operationName, e);
            throw new RuntimeException("Service call failed: " + operationName, e);
        }
    }

    /**
     * Check if all critical services are available
     */
    public boolean areAllCriticalServicesAvailable() {
        return isServiceAvailable("auth") && 
               isServiceAvailable("organization") && 
               isServiceAvailable("subscriber");
    }

    /**
     * Get list of unavailable services
     */
    public java.util.List<String> getUnavailableServices() {
        java.util.List<String> unavailable = new java.util.ArrayList<>();
        
        if (!isServiceAvailable("auth")) unavailable.add("auth-service");
        if (!isServiceAvailable("organization")) unavailable.add("organization-service");
        if (!isServiceAvailable("subscriber")) unavailable.add("subscriber-service");
        if (!isServiceAvailable("attendance")) unavailable.add("attendance-service");
        if (!isServiceAvailable("menu")) unavailable.add("menu-service");
        if (!isServiceAvailable("order")) unavailable.add("order-service");
        if (!isServiceAvailable("table")) unavailable.add("table-service");
        
        return unavailable;
    }
}
