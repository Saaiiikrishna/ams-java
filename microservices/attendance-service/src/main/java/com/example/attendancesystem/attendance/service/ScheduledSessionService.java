package com.example.attendancesystem.organization.service;

import com.example.attendancesystem.dto.ScheduledSessionDto;
import com.example.attendancesystem.organization.model.*;
import com.example.attendancesystem.organization.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class ScheduledSessionService {

    private static final Logger logger = LoggerFactory.getLogger(ScheduledSessionService.class);

    @Autowired
    private ScheduledSessionRepository scheduledSessionRepository;

    @Autowired
    private AttendanceSessionRepository attendanceSessionRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private QrCodeService qrCodeService;

    /**
     * Create a new scheduled session
     */
    @Transactional
    @CacheEvict(value = "scheduledSessions", key = "#entityId")
    public ScheduledSessionDto createScheduledSession(ScheduledSessionDto dto, String entityId) {
        logger.info("Creating scheduled session: {} for entity: {}", dto.getName(), entityId);

        Organization organization = organizationRepository.findByEntityId(entityId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found with entity ID: " + entityId));

        ScheduledSession scheduledSession = new ScheduledSession();
        scheduledSession.setName(dto.getName());
        scheduledSession.setDescription(dto.getDescription());
        scheduledSession.setStartTime(dto.getStartTime());
        scheduledSession.setDurationMinutes(dto.getDurationMinutes());
        scheduledSession.setDaysOfWeek(dto.getDaysOfWeek());
        scheduledSession.setAllowedCheckInMethods(dto.getAllowedCheckInMethods());
        scheduledSession.setOrganization(organization);
        scheduledSession.setActive(true);

        ScheduledSession saved = scheduledSessionRepository.save(scheduledSession);
        logger.info("Scheduled session created successfully with ID: {}", saved.getId());

        return convertToDto(saved);
    }

    /**
     * Get all scheduled sessions for an organization
     */
    @Cacheable(value = "scheduledSessions", key = "#entityId")
    public List<ScheduledSessionDto> getScheduledSessions(String entityId) {
        List<ScheduledSession> sessions = scheduledSessionRepository.findAllByOrganizationEntityId(entityId);
        return sessions.stream().map(this::convertToDto).toList();
    }

    /**
     * Get active scheduled sessions for an organization
     */
    public List<ScheduledSessionDto> getActiveScheduledSessions(String entityId) {
        List<ScheduledSession> sessions = scheduledSessionRepository.findAllByOrganizationEntityIdAndActiveTrue(entityId);
        return sessions.stream().map(this::convertToDto).toList();
    }

    /**
     * Get a single scheduled session by ID
     */
    public ScheduledSessionDto getScheduledSessionById(Long id, String entityId) {
        ScheduledSession session = scheduledSessionRepository.findByIdAndOrganizationEntityId(id, entityId)
                .orElseThrow(() -> new IllegalArgumentException("Scheduled session not found"));
        return convertToDto(session);
    }

    /**
     * Update a scheduled session
     */
    @Transactional
    @CacheEvict(value = "scheduledSessions", key = "#entityId")
    public ScheduledSessionDto updateScheduledSession(Long id, ScheduledSessionDto dto, String entityId) {
        ScheduledSession session = scheduledSessionRepository.findByIdAndOrganizationEntityId(id, entityId)
                .orElseThrow(() -> new IllegalArgumentException("Scheduled session not found"));

        session.setName(dto.getName());
        session.setDescription(dto.getDescription());
        session.setStartTime(dto.getStartTime());
        session.setDurationMinutes(dto.getDurationMinutes());
        session.setDaysOfWeek(dto.getDaysOfWeek());
        session.setAllowedCheckInMethods(dto.getAllowedCheckInMethods());
        session.setActive(dto.getActive());

        ScheduledSession saved = scheduledSessionRepository.save(session);
        logger.info("Scheduled session updated: {}", saved.getId());

        return convertToDto(saved);
    }

    /**
     * Delete a scheduled session
     */
    @Transactional
    @CacheEvict(value = "scheduledSessions", key = "#entityId")
    public void deleteScheduledSession(Long id, String entityId) {
        ScheduledSession session = scheduledSessionRepository.findByIdAndOrganizationEntityId(id, entityId)
                .orElseThrow(() -> new IllegalArgumentException("Scheduled session not found"));

        scheduledSessionRepository.delete(session);
        logger.info("Scheduled session deleted: {}", id);
    }

    /**
     * Cron job to automatically create sessions based on schedule
     * Runs every minute to check for sessions that should be started
     */
    @Scheduled(cron = "0 * * * * *") // Every minute
    @Transactional
    public void createScheduledSessions() {
        LocalDateTime now = LocalDateTime.now();
        DayOfWeek currentDay = now.getDayOfWeek();
        LocalTime currentTime = LocalTime.of(now.getHour(), now.getMinute()); // Ignore seconds

        logger.debug("Checking for scheduled sessions at {} on {}", currentTime, currentDay);

        List<ScheduledSession> sessionsToCreate = scheduledSessionRepository
                .findActiveSessionsForDayAndTime(currentDay, currentTime);

        for (ScheduledSession scheduledSession : sessionsToCreate) {
            try {
                createAttendanceSessionFromScheduled(scheduledSession, now);
            } catch (Exception e) {
                logger.error("Failed to create attendance session from scheduled session {}: {}",
                           scheduledSession.getId(), e.getMessage(), e);
            }
        }
    }

    /**
     * Cron job to automatically end sessions that have exceeded their duration
     * Runs every minute to check for sessions that should be ended
     */
    @Scheduled(cron = "0 * * * * *") // Every minute
    @Transactional
    public void endExpiredSessions() {
        LocalDateTime now = LocalDateTime.now();
        logger.debug("Checking for expired sessions at {}", now);

        // Find all active sessions that should have ended
        List<AttendanceSession> activeSessions = attendanceSessionRepository
                .findByEndTimeIsNull(); // Get all active sessions

        for (AttendanceSession session : activeSessions) {
            try {
                // Check if this session was created from a scheduled session
                if (session.getScheduledSession() != null) {
                    ScheduledSession scheduledSession = session.getScheduledSession();
                    LocalDateTime expectedEndTime = session.getStartTime()
                            .plusMinutes(scheduledSession.getDurationMinutes());

                    // If current time is past the expected end time, end the session
                    if (now.isAfter(expectedEndTime)) {
                        session.setEndTime(now);
                        attendanceSessionRepository.save(session);
                        logger.info("Automatically ended scheduled session {} (ID: {}) at {}",
                                  session.getName(), session.getId(), now);
                    }
                }
            } catch (Exception e) {
                logger.error("Failed to end expired session {}: {}",
                           session.getId(), e.getMessage(), e);
            }
        }
    }

    /**
     * Create an attendance session from a scheduled session
     */
    @Transactional
    public AttendanceSession createAttendanceSessionFromScheduled(ScheduledSession scheduledSession, LocalDateTime startTime) {
        // Check if session already exists for this time
        LocalDateTime endTime = startTime.plusMinutes(scheduledSession.getDurationMinutes());
        
        // Check for existing session in the same time window
        boolean sessionExists = attendanceSessionRepository
                .findByOrganizationAndEndTimeIsNullAndStartTimeBefore(
                    scheduledSession.getOrganization(), startTime.plusMinutes(5))
                .stream()
                .anyMatch(session -> session.getScheduledSession() != null && 
                         session.getScheduledSession().getId().equals(scheduledSession.getId()) &&
                         session.getStartTime().isAfter(startTime.minusMinutes(5)));

        if (sessionExists) {
            logger.debug("Session already exists for scheduled session {}", scheduledSession.getId());
            return null;
        }

        AttendanceSession session = new AttendanceSession();
        session.setName(scheduledSession.getName());
        session.setDescription(scheduledSession.getDescription());
        session.setStartTime(startTime);
        session.setOrganization(scheduledSession.getOrganization());
        session.setScheduledSession(scheduledSession);
        session.setAllowedCheckInMethods(new HashSet<>(scheduledSession.getAllowedCheckInMethods()));

        // Save session first to get an ID
        AttendanceSession saved = attendanceSessionRepository.save(session);

        // Generate QR code if QR is an allowed method
        if (scheduledSession.getAllowedCheckInMethods().contains(CheckInMethod.QR)) {
            String qrCode = qrCodeService.generateQrCodeForSession(saved);
            saved.setQrCode(qrCode);
            saved.setQrCodeExpiry(endTime); // QR code expires when session ends
            saved = attendanceSessionRepository.save(saved); // Save again with QR code
        }
        logger.info("Created attendance session {} from scheduled session {}", 
                   saved.getId(), scheduledSession.getId());

        return saved;
    }

    /**
     * Convert entity to DTO
     */
    private ScheduledSessionDto convertToDto(ScheduledSession session) {
        ScheduledSessionDto dto = new ScheduledSessionDto();
        dto.setId(session.getId());
        dto.setName(session.getName());
        dto.setDescription(session.getDescription());
        dto.setStartTime(session.getStartTime());
        dto.setDurationMinutes(session.getDurationMinutes());
        dto.setDaysOfWeek(session.getDaysOfWeek());
        dto.setAllowedCheckInMethods(session.getAllowedCheckInMethods());
        dto.setActive(session.getActive());
        dto.setOrganizationEntityId(session.getOrganization().getEntityId());
        return dto;
    }
}
