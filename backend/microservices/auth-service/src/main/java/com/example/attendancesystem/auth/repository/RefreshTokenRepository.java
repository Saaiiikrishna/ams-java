// src/main/java/com/example/attendancesystem/repository/RefreshTokenRepository.java
package com.example.attendancesystem.auth.repository;

import com.example.attendancesystem.auth.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);

    void deleteByUsername(String username);

    void deleteByExpiryDateBefore(Instant now);
}
