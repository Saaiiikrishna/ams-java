package com.example.attendancesystem.controller;

import com.example.attendancesystem.dto.NfcCardDto;
import com.example.attendancesystem.dto.NfcScanDto;
import com.example.attendancesystem.model.*;
import com.example.attendancesystem.repository.*;
import com.example.attendancesystem.service.NfcCardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/super")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SuperAdminNfcController {

    private static final Logger logger = LoggerFactory.getLogger(SuperAdminNfcController.class);

    @Autowired
    private NfcCardRepository nfcCardRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private AttendanceSessionRepository attendanceSessionRepository;

    @Autowired
    private AttendanceLogRepository attendanceLogRepository;

    @Autowired
    private SubscriberRepository subscriberRepository;

    /**
     * Get all cards for a specific entity
     */
    @GetMapping("/cards/entity/{entityId}")
    public ResponseEntity<List<NfcCardDto>> getCardsByEntity(@PathVariable String entityId) {
        try {
            logger.info("Fetching cards for entity: {}", entityId);
            
            List<NfcCard> cards = nfcCardRepository.findByOrganizationEntityId(entityId);
            List<NfcCardDto> cardDtos = cards.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());
            
            logger.info("Found {} cards for entity: {}", cardDtos.size(), entityId);
            return ResponseEntity.ok(cardDtos);
        } catch (Exception e) {
            logger.error("Failed to fetch cards for entity: {}", entityId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get all cards globally (for super admin overview)
     */
    @GetMapping("/cards/all")
    public ResponseEntity<List<NfcCardDto>> getAllCards() {
        try {
            logger.info("Fetching all cards globally");
            
            List<NfcCard> cards = nfcCardRepository.findAll();
            List<NfcCardDto> cardDtos = cards.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());
            
            logger.info("Found {} cards globally", cardDtos.size());
            return ResponseEntity.ok(cardDtos);
        } catch (Exception e) {
            logger.error("Failed to fetch all cards", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Delete all NFC cards from the system
     */
    @DeleteMapping("/cards/all")
    @Transactional
    public ResponseEntity<?> deleteAllCards() {
        try {
            logger.warn("Deleting ALL NFC cards from the system");
            
            long cardCount = nfcCardRepository.count();
            nfcCardRepository.deleteAll();
            
            logger.warn("Successfully deleted {} NFC cards", cardCount);
            return ResponseEntity.ok(Map.of(
                "message", "All NFC cards deleted successfully",
                "deletedCount", cardCount
            ));
        } catch (Exception e) {
            logger.error("Failed to delete all cards", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Failed to delete all cards: " + e.getMessage()
            ));
        }
    }

    /**
     * Get active sessions for a specific entity
     */
    @GetMapping("/sessions/entity/{entityId}/active")
    public ResponseEntity<List<AttendanceSession>> getActiveSessionsByEntity(@PathVariable String entityId) {
        try {
            logger.info("Fetching active sessions for entity: {}", entityId);
            
            Organization organization = organizationRepository.findByEntityId(entityId)
                    .orElseThrow(() -> new IllegalArgumentException("Organization not found"));
            
            List<AttendanceSession> activeSessions = attendanceSessionRepository
                    .findByOrganizationAndEndTimeIsNullAndStartTimeBefore(organization, LocalDateTime.now());
            
            logger.info("Found {} active sessions for entity: {}", activeSessions.size(), entityId);
            return ResponseEntity.ok(activeSessions);
        } catch (Exception e) {
            logger.error("Failed to fetch active sessions for entity: {}", entityId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Simulate NFC card swipe for testing purposes
     */
    @PostMapping("/simulate-swipe")
    @Transactional
    public ResponseEntity<?> simulateSwipe(@RequestBody Map<String, Object> swipeData) {
        try {
            String cardUid = (String) swipeData.get("cardUid");
            Integer sessionId = (Integer) swipeData.get("sessionId");
            
            logger.info("Simulating NFC swipe - Card: {}, Session: {}", cardUid, sessionId);
            
            // Find the card
            NfcCard nfcCard = nfcCardRepository.findByCardUid(cardUid)
                    .orElseThrow(() -> new IllegalArgumentException("NFC card not found with UID: " + cardUid));
            
            if (!nfcCard.isActive() || nfcCard.getSubscriber() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "error", "NFC card is inactive or not assigned to a subscriber",
                    "code", "CARD_NOT_READY"
                ));
            }
            
            // Find the session
            AttendanceSession session = attendanceSessionRepository.findById(sessionId.longValue())
                    .orElseThrow(() -> new IllegalArgumentException("Session not found"));
            
            if (session.getEndTime() != null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "error", "Session has already ended",
                    "code", "SESSION_ENDED"
                ));
            }
            
            Subscriber subscriber = nfcCard.getSubscriber();
            
            // Check if subscriber belongs to the same organization as the session
            if (!subscriber.getOrganization().getId().equals(session.getOrganization().getId())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "error", "Subscriber does not belong to the session's organization",
                    "code", "ORGANIZATION_MISMATCH"
                ));
            }
            
            // Check for existing attendance log
            Optional<AttendanceLog> existingLogOpt = attendanceLogRepository
                    .findBySubscriberAndSession(subscriber, session);
            
            String action;
            AttendanceLog attendanceLog;
            
            if (existingLogOpt.isPresent()) {
                attendanceLog = existingLogOpt.get();
                if (attendanceLog.getCheckOutTime() == null) {
                    // Check out
                    attendanceLog.setCheckOutTime(LocalDateTime.now());
                    action = "CHECK_OUT";
                } else {
                    // Already checked in and out
                    return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                        "error", "Already checked in and out for this session",
                        "code", "ALREADY_COMPLETED"
                    ));
                }
            } else {
                // Check in
                attendanceLog = new AttendanceLog();
                attendanceLog.setSubscriber(subscriber);
                attendanceLog.setSession(session);
                attendanceLog.setCheckInTime(LocalDateTime.now());
                action = "CHECK_IN";
            }
            
            AttendanceLog savedLog = attendanceLogRepository.save(attendanceLog);
            
            logger.info("Swipe simulation successful - Action: {}, Subscriber: {} {}, Session: {}", 
                       action, subscriber.getFirstName(), subscriber.getLastName(), session.getName());
            
            return ResponseEntity.ok(Map.of(
                "message", "Swipe simulation successful",
                "action", action,
                "subscriber", subscriber.getFirstName() + " " + subscriber.getLastName(),
                "session", session.getName(),
                "time", action.equals("CHECK_IN") ? savedLog.getCheckInTime() : savedLog.getCheckOutTime()
            ));
            
        } catch (IllegalArgumentException e) {
            logger.warn("Swipe simulation failed - validation error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error", e.getMessage(),
                "code", "VALIDATION_ERROR"
            ));
        } catch (Exception e) {
            logger.error("Failed to simulate swipe", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Failed to simulate swipe: " + e.getMessage(),
                "code", "INTERNAL_ERROR"
            ));
        }
    }

    /**
     * Get global card statistics
     */
    @GetMapping("/cards/statistics")
    public ResponseEntity<Map<String, Object>> getGlobalCardStatistics() {
        try {
            logger.info("Fetching global card statistics");
            
            long totalCards = nfcCardRepository.count();
            long assignedCards = nfcCardRepository.countBySubscriberIsNotNull();
            long unassignedCards = nfcCardRepository.countBySubscriberIsNull();
            long totalEntities = organizationRepository.count();
            
            Map<String, Object> stats = Map.of(
                "totalCards", totalCards,
                "assignedCards", assignedCards,
                "unassignedCards", unassignedCards,
                "totalEntities", totalEntities
            );
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("Failed to fetch global card statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Convert NfcCard entity to DTO
     */
    private NfcCardDto convertToDto(NfcCard card) {
        if (card.getSubscriber() == null) {
            // Unassigned card
            NfcCardDto dto = new NfcCardDto(card.getId(), card.getCardUid(), card.isActive());
            if (card.getOrganization() != null) {
                dto.setOrganizationName(card.getOrganization().getName());
                dto.setEntityId(card.getOrganization().getEntityId());
            }
            return dto;
        } else {
            // Assigned card
            Subscriber subscriber = card.getSubscriber();
            String subscriberName = subscriber.getFirstName() + " " + subscriber.getLastName();
            
            return new NfcCardDto(
                card.getId(),
                card.getCardUid(),
                card.isActive(),
                subscriber.getId(),
                subscriberName,
                subscriber.getEmail(),
                subscriber.getMobileNumber(),
                subscriber.getOrganization().getName(),
                subscriber.getOrganization().getEntityId(),
                LocalDateTime.now() // You might want to add a timestamp field to NfcCard
            );
        }
    }
}
