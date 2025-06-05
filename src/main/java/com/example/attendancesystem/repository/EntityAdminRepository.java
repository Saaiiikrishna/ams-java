package com.example.attendancesystem.repository;

import com.example.attendancesystem.model.EntityAdmin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EntityAdminRepository extends JpaRepository<EntityAdmin, Long> {
    Optional<EntityAdmin> findByUsername(String username);
}
