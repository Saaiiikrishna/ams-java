package com.example.attendancesystem.auth.repository;

import com.example.attendancesystem.auth.model.Subscriber;
import com.example.attendancesystem.auth.model.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Subscriber entity in auth-service
 * Only contains methods needed for authentication
 */
@Repository
public interface SubscriberRepository extends JpaRepository<Subscriber, Long> {
    
    /**
     * Find subscriber by mobile number and organization
     */
    Optional<Subscriber> findByMobileNumberAndOrganization(String mobileNumber, Organization organization);
    
    /**
     * Find subscriber by mobile number and organization entity ID
     */
    @Query("SELECT s FROM AuthSubscriber s WHERE s.mobileNumber = :mobileNumber AND s.organization.entityId = :entityId")
    Optional<Subscriber> findByMobileNumberAndOrganizationEntityId(@Param("mobileNumber") String mobileNumber, @Param("entityId") String entityId);
    
    /**
     * Check if subscriber exists by mobile number and organization entity ID
     */
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM AuthSubscriber s WHERE s.mobileNumber = :mobileNumber AND s.organization.entityId = :entityId")
    boolean existsByMobileNumberAndOrganizationEntityId(@Param("mobileNumber") String mobileNumber, @Param("entityId") String entityId);
    
    /**
     * Find subscriber by ID and organization
     */
    Optional<Subscriber> findByIdAndOrganization(Long id, Organization organization);
}
