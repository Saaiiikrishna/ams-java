package com.example.attendancesystem.repository;

import com.example.attendancesystem.model.Organization;
import com.example.attendancesystem.model.Subscriber;
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
}
