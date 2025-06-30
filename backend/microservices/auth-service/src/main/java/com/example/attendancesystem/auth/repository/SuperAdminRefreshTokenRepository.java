package com.example.attendancesystem.auth.repository;

import com.example.attendancesystem.auth.model.SuperAdminRefreshToken;
import com.example.attendancesystem.auth.model.SuperAdmin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SuperAdminRefreshTokenRepository extends JpaRepository<SuperAdminRefreshToken, Long> {
    
    Optional<SuperAdminRefreshToken> findByToken(String token);
    
    void deleteByToken(String token);
    
    void deleteByUser(SuperAdmin user);
    
    boolean existsByToken(String token);
}
