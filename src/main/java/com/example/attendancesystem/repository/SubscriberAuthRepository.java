package com.example.attendancesystem.subscriber.repository;

import com.example.attendancesystem.model.Organization;
import com.example.attendancesystem.model.Subscriber;
import com.example.attendancesystem.model.SubscriberAuth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriberAuthRepository extends JpaRepository<SubscriberAuth, Long> {
    
    // Find by subscriber
    Optional<SubscriberAuth> findBySubscriber(Subscriber subscriber);
    
    // Find by subscriber ID
    Optional<SubscriberAuth> findBySubscriberId(Long subscriberId);
    
    // Find by subscriber mobile number (for login)
    @Query("SELECT sa FROM SubscriberAuth sa WHERE sa.subscriber.mobileNumber = :mobileNumber AND sa.isActive = true")
    Optional<SubscriberAuth> findBySubscriberMobileNumberAndIsActiveTrue(@Param("mobileNumber") String mobileNumber);

    // Find by subscriber mobile number only (simplified login) - returns list to handle multiple organizations
    @Query("SELECT sa FROM SubscriberAuth sa WHERE sa.subscriber.mobileNumber = :mobileNumber AND sa.isActive = true ORDER BY sa.lastLoginTime DESC NULLS LAST, sa.id ASC")
    List<SubscriberAuth> findBySubscriberMobileNumber(@Param("mobileNumber") String mobileNumber);
    

    
    // Find active subscriber auth records
    List<SubscriberAuth> findAllByIsActiveTrue();
    
    // Find by organization
    @Query("SELECT sa FROM SubscriberAuth sa WHERE sa.subscriber.organization.entityId = :entityId")
    List<SubscriberAuth> findAllByOrganizationEntityId(@Param("entityId") String entityId);
    
    // Find expired OTP records for cleanup
    @Query("SELECT sa FROM SubscriberAuth sa WHERE sa.otpExpiryTime IS NOT NULL AND sa.otpExpiryTime < :currentTime")
    List<SubscriberAuth> findExpiredOtpRecords(@Param("currentTime") LocalDateTime currentTime);
    
    // Check if subscriber has active auth
    @Query("SELECT COUNT(sa) > 0 FROM SubscriberAuth sa WHERE sa.subscriber.id = :subscriberId AND sa.isActive = true")
    boolean existsBySubscriberIdAndIsActiveTrue(@Param("subscriberId") Long subscriberId);
    
    // Find by device ID for security tracking
    List<SubscriberAuth> findByLastDeviceId(String deviceId);
    
    // Count active auth records for organization
    @Query("SELECT COUNT(sa) FROM SubscriberAuth sa WHERE sa.subscriber.organization.entityId = :entityId AND sa.isActive = true")
    long countActiveByOrganizationEntityId(@Param("entityId") String entityId);

    // Count and delete methods for organization cleanup
    @Query("SELECT COUNT(sa) FROM SubscriberAuth sa WHERE sa.subscriber.organization = :organization")
    long countByOrganization(@Param("organization") Organization organization);

    @Modifying
    @Query("DELETE FROM SubscriberAuth sa WHERE sa.subscriber.organization = :organization")
    void deleteByOrganization(@Param("organization") Organization organization);

    // Delete by subscriber for cascade cleanup
    void deleteBySubscriber(Subscriber subscriber);
}
