package com.example.attendancesystem.repository;

import com.example.attendancesystem.model.NfcCard;
import com.example.attendancesystem.model.Organization;
import com.example.attendancesystem.model.Subscriber;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NfcCardRepository extends JpaRepository<NfcCard, Long> {
    Optional<NfcCard> findByCardUid(String cardUid);
    Optional<NfcCard> findBySubscriber(Subscriber subscriber);
    boolean existsByCardUid(String cardUid);

    // Find all unassigned cards for a specific organization
    @Query("SELECT c FROM NfcCard c WHERE c.subscriber IS NULL AND c.organization.entityId = :entityId")
    List<NfcCard> findBySubscriberIsNullAndOrganizationEntityId(@Param("entityId") String entityId);

    // Find all assigned cards for a specific organization
    @Query("SELECT c FROM NfcCard c WHERE c.subscriber IS NOT NULL AND c.organization.entityId = :entityId")
    List<NfcCard> findBySubscriberIsNotNullAndOrganizationEntityId(@Param("entityId") String entityId);

    // Find cards by organization
    List<NfcCard> findByOrganization(Organization organization);

    // Find cards by organization entity ID
    List<NfcCard> findByOrganizationEntityId(String entityId);

    // Find unassigned cards for a specific organization
    @Query("SELECT c FROM NfcCard c WHERE c.subscriber IS NULL AND c.organization.entityId = :entityId AND c.isActive = true")
    List<NfcCard> findUnassignedActiveCardsByEntityId(@Param("entityId") String entityId);

    // Find all unassigned cards (global - for super admin)
    List<NfcCard> findBySubscriberIsNull();

    // Find all assigned cards (global - for super admin)
    List<NfcCard> findBySubscriberIsNotNull();

    // Count cards by assignment status for specific organization
    long countBySubscriberIsNullAndOrganizationEntityId(String entityId);
    long countBySubscriberIsNotNullAndOrganizationEntityId(String entityId);

    // Count cards by organization
    long countByOrganizationEntityId(String entityId);

    // Global counts (for super admin)
    long countBySubscriberIsNull();
    long countBySubscriberIsNotNull();
}
