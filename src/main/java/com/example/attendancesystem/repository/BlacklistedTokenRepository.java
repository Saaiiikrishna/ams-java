package com.example.attendancesystem.repository;

import com.example.attendancesystem.model.BlacklistedToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface BlacklistedTokenRepository extends JpaRepository<BlacklistedToken, Long> {
    
    /**
     * Check if a token hash is blacklisted
     */
    boolean existsByTokenHash(String tokenHash);
    
    /**
     * Find a blacklisted token by its hash
     */
    Optional<BlacklistedToken> findByTokenHash(String tokenHash);
    
    /**
     * Delete all blacklisted tokens for a specific username
     */
    void deleteByUsername(String username);
    
    /**
     * Clean up expired blacklisted tokens (tokens that would have expired anyway)
     */
    @Modifying
    @Query("DELETE FROM BlacklistedToken bt WHERE bt.expiresAt < :now")
    void deleteExpiredTokens(Instant now);
    
    /**
     * Count blacklisted tokens for a user
     */
    long countByUsername(String username);
}
