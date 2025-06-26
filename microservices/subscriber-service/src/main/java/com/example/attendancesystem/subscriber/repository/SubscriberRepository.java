package com.example.attendancesystem.organization.repository;

import com.example.attendancesystem.organization.model.Organization;
import com.example.attendancesystem.organization.model.Subscriber;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriberRepository extends JpaRepository<Subscriber, Long> {
    Optional<Subscriber> findByEmailAndOrganization(String email, Organization organization);
    Optional<Subscriber> findByIdAndOrganization(Long id, Organization organization);
    List<Subscriber> findAllByOrganization(Organization organization);
    boolean existsByEmailAndOrganization(String email, Organization organization);
    boolean existsByMobileNumberAndOrganization(String mobileNumber, Organization organization);
    long countByOrganization(Organization organization);
    void deleteByOrganization(Organization organization);

    // Entity ID based methods
    @Query("SELECT s FROM Subscriber s WHERE s.organization.entityId = :entityId")
    List<Subscriber> findAllByOrganizationEntityId(@Param("entityId") String entityId);

    @Query("SELECT s FROM Subscriber s WHERE s.mobileNumber = :mobileNumber AND s.organization.entityId = :entityId")
    Optional<Subscriber> findByMobileNumberAndOrganizationEntityId(@Param("mobileNumber") String mobileNumber, @Param("entityId") String entityId);

    @Query("SELECT s FROM Subscriber s WHERE s.id = :id AND s.organization.entityId = :entityId")
    Optional<Subscriber> findByIdAndOrganizationEntityId(@Param("id") Long id, @Param("entityId") String entityId);

    @Query("SELECT s FROM Subscriber s WHERE s.email = :email AND s.organization.entityId = :entityId")
    Optional<Subscriber> findByEmailAndOrganizationEntityId(@Param("email") String email, @Param("entityId") String entityId);

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Subscriber s WHERE s.email = :email AND s.organization.entityId = :entityId")
    boolean existsByEmailAndOrganizationEntityId(@Param("email") String email, @Param("entityId") String entityId);

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Subscriber s WHERE s.mobileNumber = :mobileNumber AND s.organization.entityId = :entityId")
    boolean existsByMobileNumberAndOrganizationEntityId(@Param("mobileNumber") String mobileNumber, @Param("entityId") String entityId);

    @Query("SELECT COUNT(s) FROM Subscriber s WHERE s.organization.entityId = :entityId")
    long countByOrganizationEntityId(@Param("entityId") String entityId);

    // Face recognition related methods

    /**
     * Find subscribers with face encoding registered for an organization
     */
    List<Subscriber> findByOrganizationAndFaceEncodingIsNotNull(Organization organization);

    /**
     * Find subscribers with face encoding by entity ID
     */
    @Query("SELECT s FROM Subscriber s WHERE s.organization.entityId = :entityId AND s.faceEncoding IS NOT NULL")
    List<Subscriber> findByOrganizationEntityIdAndFaceEncodingIsNotNull(@Param("entityId") String entityId);

    /**
     * Count subscribers with face recognition enabled for an organization
     */
    @Query("SELECT COUNT(s) FROM Subscriber s WHERE s.organization.entityId = :entityId AND s.faceEncoding IS NOT NULL")
    long countByOrganizationEntityIdAndFaceEncodingIsNotNull(@Param("entityId") String entityId);

    /**
     * Find subscribers without face encoding for an organization
     */
    List<Subscriber> findByOrganizationAndFaceEncodingIsNull(Organization organization);

    // Pagination support
    Page<Subscriber> findByOrganization(Organization organization, Pageable pageable);
    Page<Subscriber> findByOrganizationAndFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
            Organization organization, String firstName, String lastName, Pageable pageable);

    /**
     * Check if subscriber has face recognition enabled
     */
    @Query("SELECT CASE WHEN s.faceEncoding IS NOT NULL THEN true ELSE false END FROM Subscriber s WHERE s.id = :subscriberId")
    boolean hasFaceRecognitionEnabled(@Param("subscriberId") Long subscriberId);
}
