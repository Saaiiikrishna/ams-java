package com.example.attendancesystem.subscriber.repository;

import com.example.attendancesystem.model.FaceRecognitionSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for FaceRecognitionSettings entity
 */
@Repository
public interface FaceRecognitionSettingsRepository extends JpaRepository<FaceRecognitionSettings, Long> {
    
    /**
     * Find settings by entity ID
     */
    Optional<FaceRecognitionSettings> findByEntityId(String entityId);
    
    /**
     * Check if settings exist for entity
     */
    boolean existsByEntityId(String entityId);
    
    /**
     * Delete settings by entity ID
     */
    void deleteByEntityId(String entityId);
    
    /**
     * Find settings with custom query for performance
     */
    @Query("SELECT frs FROM FaceRecognitionSettings frs WHERE frs.entityId = :entityId")
    Optional<FaceRecognitionSettings> findSettingsByEntityId(@Param("entityId") String entityId);
}
