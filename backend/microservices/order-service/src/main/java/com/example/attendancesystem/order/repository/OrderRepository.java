package com.example.attendancesystem.order.repository;

import com.example.attendancesystem.order.model.Order;
import com.example.attendancesystem.shared.model.Organization;
import com.example.attendancesystem.shared.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    /**
     * Find all orders by organization
     */
    List<Order> findByOrganizationOrderByCreatedAtDesc(Organization organization);
    
    /**
     * Find orders by organization and status
     */
    List<Order> findByOrganizationAndStatusOrderByCreatedAtDesc(Organization organization, OrderStatus status);
    
    /**
     * Find orders by organization and status in
     */
    List<Order> findByOrganizationAndStatusInOrderByCreatedAtDesc(Organization organization, List<OrderStatus> statuses);
    
    /**
     * Find order by ID and organization
     */
    Optional<Order> findByIdAndOrganization(Long id, Organization organization);
    
    /**
     * Find order by order number
     */
    Optional<Order> findByOrderNumber(String orderNumber);
    
    /**
     * Find order by order number and organization
     */
    Optional<Order> findByOrderNumberAndOrganization(String orderNumber, Organization organization);
    
    /**
     * Find orders by table number and organization
     */
    List<Order> findByTableNumberAndOrganizationOrderByCreatedAtDesc(Integer tableNumber, Organization organization);
    
    /**
     * Find active orders by table number and organization
     */
    @Query("SELECT o FROM Order o WHERE o.tableNumber = :tableNumber AND o.organization = :organization AND o.status NOT IN ('COMPLETED', 'CANCELLED') ORDER BY o.createdAt DESC")
    List<Order> findActiveOrdersByTableAndOrganization(@Param("tableNumber") Integer tableNumber, @Param("organization") Organization organization);
    
    /**
     * Find orders by date range and organization
     */
    @Query("SELECT o FROM Order o WHERE o.organization = :organization AND o.createdAt BETWEEN :startDate AND :endDate ORDER BY o.createdAt DESC")
    List<Order> findByOrganizationAndDateRange(@Param("organization") Organization organization, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    /**
     * Count orders by organization
     */
    long countByOrganization(Organization organization);

    /**
     * Delete all orders by organization
     */
    void deleteByOrganization(Organization organization);
    
    /**
     * Count orders by organization and status
     */
    long countByOrganizationAndStatus(Organization organization, OrderStatus status);
    
    /**
     * Count orders by organization and date range
     */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.organization = :organization AND o.createdAt BETWEEN :startDate AND :endDate")
    long countByOrganizationAndDateRange(@Param("organization") Organization organization, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find orders by organization entity ID
     */
    @Query("SELECT o FROM Order o WHERE o.organization.entityId = :entityId ORDER BY o.createdAt DESC")
    List<Order> findByOrganizationEntityId(@Param("entityId") String entityId);
    
    /**
     * Find pending orders by organization
     */
    @Query("SELECT o FROM Order o WHERE o.organization = :organization AND o.status IN ('PENDING', 'CONFIRMED', 'PREPARING') ORDER BY o.createdAt ASC")
    List<Order> findPendingOrdersByOrganization(@Param("organization") Organization organization);
    
    /**
     * Find today's orders by organization
     */
    @Query("SELECT o FROM Order o WHERE o.organization = :organization AND o.createdAt >= CURRENT_DATE ORDER BY o.createdAt DESC")
    List<Order> findTodaysOrdersByOrganization(@Param("organization") Organization organization);
}
