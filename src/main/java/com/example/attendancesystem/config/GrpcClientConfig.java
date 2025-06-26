package com.example.attendancesystem.config;

import com.example.attendancesystem.grpc.auth.AuthServiceGrpc;
import com.example.attendancesystem.grpc.attendance.AttendanceServiceGrpc;
import com.example.attendancesystem.grpc.menu.MenuServiceGrpc;
import com.example.attendancesystem.grpc.order.OrderServiceGrpc;
import com.example.attendancesystem.grpc.organization.OrganizationServiceGrpc;
import com.example.attendancesystem.grpc.subscriber.SubscriberServiceGrpc;
import com.example.attendancesystem.grpc.table.TableServiceGrpc;
import com.example.attendancesystem.service.GrpcClientFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

/**
 * Configuration for gRPC clients with service discovery integration
 * Provides beans for all gRPC service stubs using dynamic service discovery
 */
@Configuration
public class GrpcClientConfig {

    @Autowired
    private GrpcClientFactory grpcClientFactory;

    /**
     * Auth Service Client - for authentication and authorization
     */
    @Bean
    public AuthServiceGrpc.AuthServiceBlockingStub authServiceClient() {
        Optional<AuthServiceGrpc.AuthServiceBlockingStub> stub = grpcClientFactory.createStub(
                "auth-service", 
                AuthServiceGrpc::newBlockingStub
        );
        return stub.orElse(null); // Will be null if service not discovered
    }

    @Bean
    public AuthServiceGrpc.AuthServiceStub authServiceAsyncClient() {
        Optional<AuthServiceGrpc.AuthServiceStub> stub = grpcClientFactory.createStub(
                "auth-service", 
                AuthServiceGrpc::newStub
        );
        return stub.orElse(null);
    }

    /**
     * Organization Service Client - for organization management
     */
    @Bean
    public OrganizationServiceGrpc.OrganizationServiceBlockingStub organizationServiceClient() {
        Optional<OrganizationServiceGrpc.OrganizationServiceBlockingStub> stub = grpcClientFactory.createStub(
                "organization-service", 
                OrganizationServiceGrpc::newBlockingStub
        );
        return stub.orElse(null);
    }

    @Bean
    public OrganizationServiceGrpc.OrganizationServiceStub organizationServiceAsyncClient() {
        Optional<OrganizationServiceGrpc.OrganizationServiceStub> stub = grpcClientFactory.createStub(
                "organization-service", 
                OrganizationServiceGrpc::newStub
        );
        return stub.orElse(null);
    }

    /**
     * Subscriber Service Client - for subscriber management
     */
    @Bean
    public SubscriberServiceGrpc.SubscriberServiceBlockingStub subscriberServiceClient() {
        Optional<SubscriberServiceGrpc.SubscriberServiceBlockingStub> stub = grpcClientFactory.createStub(
                "subscriber-service", 
                SubscriberServiceGrpc::newBlockingStub
        );
        return stub.orElse(null);
    }

    @Bean
    public SubscriberServiceGrpc.SubscriberServiceStub subscriberServiceAsyncClient() {
        Optional<SubscriberServiceGrpc.SubscriberServiceStub> stub = grpcClientFactory.createStub(
                "subscriber-service", 
                SubscriberServiceGrpc::newStub
        );
        return stub.orElse(null);
    }

    /**
     * Attendance Service Client - for attendance management
     */
    @Bean
    public AttendanceServiceGrpc.AttendanceServiceBlockingStub attendanceServiceClient() {
        Optional<AttendanceServiceGrpc.AttendanceServiceBlockingStub> stub = grpcClientFactory.createStub(
                "attendance-service", 
                AttendanceServiceGrpc::newBlockingStub
        );
        return stub.orElse(null);
    }

    @Bean
    public AttendanceServiceGrpc.AttendanceServiceStub attendanceServiceAsyncClient() {
        Optional<AttendanceServiceGrpc.AttendanceServiceStub> stub = grpcClientFactory.createStub(
                "attendance-service", 
                AttendanceServiceGrpc::newStub
        );
        return stub.orElse(null);
    }

    /**
     * Menu Service Client - for menu management
     */
    @Bean
    public MenuServiceGrpc.MenuServiceBlockingStub menuServiceClient() {
        Optional<MenuServiceGrpc.MenuServiceBlockingStub> stub = grpcClientFactory.createStub(
                "menu-service", 
                MenuServiceGrpc::newBlockingStub
        );
        return stub.orElse(null);
    }

    @Bean
    public MenuServiceGrpc.MenuServiceStub menuServiceAsyncClient() {
        Optional<MenuServiceGrpc.MenuServiceStub> stub = grpcClientFactory.createStub(
                "menu-service", 
                MenuServiceGrpc::newStub
        );
        return stub.orElse(null);
    }

    /**
     * Order Service Client - for order management
     */
    @Bean
    public OrderServiceGrpc.OrderServiceBlockingStub orderServiceClient() {
        Optional<OrderServiceGrpc.OrderServiceBlockingStub> stub = grpcClientFactory.createStub(
                "order-service", 
                OrderServiceGrpc::newBlockingStub
        );
        return stub.orElse(null);
    }

    @Bean
    public OrderServiceGrpc.OrderServiceStub orderServiceAsyncClient() {
        Optional<OrderServiceGrpc.OrderServiceStub> stub = grpcClientFactory.createStub(
                "order-service", 
                OrderServiceGrpc::newStub
        );
        return stub.orElse(null);
    }

    /**
     * Table Service Client - for table management
     */
    @Bean
    public TableServiceGrpc.TableServiceBlockingStub tableServiceClient() {
        Optional<TableServiceGrpc.TableServiceBlockingStub> stub = grpcClientFactory.createStub(
                "table-service", 
                TableServiceGrpc::newBlockingStub
        );
        return stub.orElse(null);
    }

    @Bean
    public TableServiceGrpc.TableServiceStub tableServiceAsyncClient() {
        Optional<TableServiceGrpc.TableServiceStub> stub = grpcClientFactory.createStub(
                "table-service", 
                TableServiceGrpc::newStub
        );
        return stub.orElse(null);
    }

    /**
     * Health check method to verify all services are available
     */
    public boolean areAllServicesAvailable() {
        return grpcClientFactory.isServiceAvailable("auth-service") &&
               grpcClientFactory.isServiceAvailable("organization-service") &&
               grpcClientFactory.isServiceAvailable("subscriber-service") &&
               grpcClientFactory.isServiceAvailable("attendance-service") &&
               grpcClientFactory.isServiceAvailable("menu-service") &&
               grpcClientFactory.isServiceAvailable("order-service") &&
               grpcClientFactory.isServiceAvailable("table-service");
    }

    /**
     * Get service availability status
     */
    public java.util.Map<String, Boolean> getServiceAvailability() {
        java.util.Map<String, Boolean> availability = new java.util.HashMap<>();
        availability.put("auth-service", grpcClientFactory.isServiceAvailable("auth-service"));
        availability.put("organization-service", grpcClientFactory.isServiceAvailable("organization-service"));
        availability.put("subscriber-service", grpcClientFactory.isServiceAvailable("subscriber-service"));
        availability.put("attendance-service", grpcClientFactory.isServiceAvailable("attendance-service"));
        availability.put("menu-service", grpcClientFactory.isServiceAvailable("menu-service"));
        availability.put("order-service", grpcClientFactory.isServiceAvailable("order-service"));
        availability.put("table-service", grpcClientFactory.isServiceAvailable("table-service"));
        return availability;
    }

    /**
     * Refresh all client connections
     */
    public void refreshAllConnections() {
        grpcClientFactory.refreshConnections();
    }
}
