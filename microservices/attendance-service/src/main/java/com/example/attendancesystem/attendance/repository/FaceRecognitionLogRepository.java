package com.example.attendancesystem.organization.repository;

import com.example.attendancesystem.organization.model.FaceRecognitionLog;
import com.example.attendancesystem.organization.model.Subscriber;
import com.example.attendancesystem.organization.model.AttendanceSession;
import com.example.attendancesystem.organization.model.FaceRecognitionLog.RecognitionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for FaceRecognitionLog entity
 */
@Repository
public interface FaceRecognitionLogRepository extends JpaRepository<FaceRecognitionLog, Long> {
    
    /**
     * Find logs by subscriber
     */
    List<FaceRecognitionLog> findBySubscriberOrderByRecognitionTimestampDesc(Subscriber subscriber);
    
    /**
     * Find logs by session
     */
    List<FaceRecognitionLog> findBySessionOrderByRecognitionTimestampDesc(AttendanceSession session);
    
    /**
     * Find logs by recognition status
     */
    List<FaceRecognitionLog> findByRecognitionStatusOrderByRecognitionTimestampDesc(RecognitionStatus status);
    
    /**
     * Find logs within date range
     */
    @Query("SELECT frl FROM FaceRecognitionLog frl WHERE frl.recognitionTimestamp BETWEEN :startDate AND :endDate ORDER BY frl.recognitionTimestamp DESC")
    List<FaceRecognitionLog> findByDateRange(@Param("startDate") LocalDateTime startDate, 
                                           @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find logs by subscriber and date range
     */
    @Query("SELECT frl FROM FaceRecognitionLog frl WHERE frl.subscriber = :subscriber AND frl.recognitionTimestamp BETWEEN :startDate AND :endDate ORDER BY frl.recognitionTimestamp DESC")
    List<FaceRecognitionLog> findBySubscriberAndDateRange(@Param("subscriber") Subscriber subscriber,
                                                         @Param("startDate") LocalDateTime startDate,
                                                         @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find logs by session and status
     */
    List<FaceRecognitionLog> findBySessionAndRecognitionStatus(AttendanceSession session, RecognitionStatus status);
    
    /**
     * Count logs by status for a session
     */
    @Query("SELECT COUNT(frl) FROM FaceRecognitionLog frl WHERE frl.session = :session AND frl.recognitionStatus = :status")
    long countBySessionAndStatus(@Param("session") AttendanceSession session, @Param("status") RecognitionStatus status);
    
    /**
     * Find recent logs with pagination
     */
    Page<FaceRecognitionLog> findAllByOrderByRecognitionTimestampDesc(Pageable pageable);
    
    /**
     * Find logs by entity (through session relationship)
     */
    @Query("SELECT frl FROM FaceRecognitionLog frl JOIN frl.session s JOIN s.organization o WHERE o.entityId = :entityId ORDER BY frl.recognitionTimestamp DESC")
    List<FaceRecognitionLog> findByEntityId(@Param("entityId") String entityId);
    
    /**
     * Find logs by entity with pagination
     */
    @Query("SELECT frl FROM FaceRecognitionLog frl JOIN frl.session s JOIN s.organization o WHERE o.entityId = :entityId ORDER BY frl.recognitionTimestamp DESC")
    Page<FaceRecognitionLog> findByEntityId(@Param("entityId") String entityId, Pageable pageable);
    
    /**
     * Get recognition statistics for an entity
     */
    @Query("SELECT frl.recognitionStatus, COUNT(frl) FROM FaceRecognitionLog frl JOIN frl.session s JOIN s.organization o WHERE o.entityId = :entityId GROUP BY frl.recognitionStatus")
    List<Object[]> getRecognitionStatsByEntity(@Param("entityId") String entityId);
    
    /**
     * Find failed recognition attempts for debugging
     */
    @Query("SELECT frl FROM FaceRecognitionLog frl WHERE frl.recognitionStatus IN ('FAILED', 'ERROR') AND frl.recognitionTimestamp >= :since ORDER BY frl.recognitionTimestamp DESC")
    List<FaceRecognitionLog> findRecentFailures(@Param("since") LocalDateTime since);
}
