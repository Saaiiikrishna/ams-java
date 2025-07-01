package com.example.attendancesystem.attendance.repository;

import com.example.attendancesystem.attendance.model.FaceRecognitionLog;
// TODO: Replace with gRPC calls to User Service
// import com.example.attendancesystem.shared.model.Subscriber;
import com.example.attendancesystem.attendance.model.AttendanceSession;
import com.example.attendancesystem.attendance.model.FaceRecognitionLog.RecognitionStatus;
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
     * Find logs by user ID
     */
    List<FaceRecognitionLog> findByUserIdOrderByRecognitionTimestampDesc(Long userId);
    
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
     * Find logs by user ID and date range
     */
    @Query("SELECT frl FROM FaceRecognitionLog frl WHERE frl.userId = :userId AND frl.recognitionTimestamp BETWEEN :startDate AND :endDate ORDER BY frl.recognitionTimestamp DESC")
    List<FaceRecognitionLog> findByUserIdAndDateRange(@Param("userId") Long userId,
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
     * Find logs by organization ID (microservices approach)
     */
    @Query("SELECT frl FROM FaceRecognitionLog frl JOIN frl.session s WHERE s.organizationId = :organizationId ORDER BY frl.recognitionTimestamp DESC")
    List<FaceRecognitionLog> findByOrganizationId(@Param("organizationId") Long organizationId);

    /**
     * Find logs by organization ID with pagination
     */
    @Query("SELECT frl FROM FaceRecognitionLog frl JOIN frl.session s WHERE s.organizationId = :organizationId ORDER BY frl.recognitionTimestamp DESC")
    Page<FaceRecognitionLog> findByOrganizationId(@Param("organizationId") Long organizationId, Pageable pageable);

    /**
     * Get recognition statistics for an organization
     */
    @Query("SELECT frl.recognitionStatus, COUNT(frl) FROM FaceRecognitionLog frl JOIN frl.session s WHERE s.organizationId = :organizationId GROUP BY frl.recognitionStatus")
    List<Object[]> getRecognitionStatsByOrganizationId(@Param("organizationId") Long organizationId);
    
    /**
     * Find failed recognition attempts for debugging
     */
    @Query("SELECT frl FROM FaceRecognitionLog frl WHERE frl.recognitionStatus IN ('FAILED', 'ERROR') AND frl.recognitionTimestamp >= :since ORDER BY frl.recognitionTimestamp DESC")
    List<FaceRecognitionLog> findRecentFailures(@Param("since") LocalDateTime since);
}
