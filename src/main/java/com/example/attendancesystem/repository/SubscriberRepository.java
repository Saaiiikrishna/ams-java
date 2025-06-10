package com.example.attendancesystem.repository;

import com.example.attendancesystem.model.Organization;
import com.example.attendancesystem.model.Subscriber;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
