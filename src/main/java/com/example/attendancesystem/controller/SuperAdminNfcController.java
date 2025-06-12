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
import java.util.HashMap;
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
     * Delete a specific NFC card by ID
     */
    @DeleteMapping("/cards/{cardId}")
    @Transactional
    public ResponseEntity<?> deleteCard(@PathVariable Long cardId) {
        try {
            logger.info("Deleting NFC card with ID: {}", cardId);

            Optional<NfcCard> cardOpt = nfcCardRepository.findById(cardId);
            if (cardOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "Card not found with ID: " + cardId
                ));
            }

            NfcCard card = cardOpt.get();
            String cardUid = card.getCardUid();

            // Check if card is assigned
            if (card.getSubscriber() != null) {
                logger.warn("Cannot delete assigned card: {} (assigned to subscriber: {})",
                           cardUid, card.getSubscriber().getId());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "error", "Cannot delete assigned card. Please unassign first.",
                    "cardUid", cardUid,
                    "subscriberName", card.getSubscriber().getFirstName() + " " + card.getSubscriber().getLastName()
                ));
            }

            nfcCardRepository.delete(card);

            logger.info("Successfully deleted NFC card: {}", cardUid);
            return ResponseEntity.ok(Map.of(
                "message", "NFC card deleted successfully",
                "cardUid", cardUid
            ));
        } catch (Exception e) {
            logger.error("Failed to delete card with ID: {}", cardId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Failed to delete card: " + e.getMessage()
            ));
        }
    }

    /**
     * Assign a card to a subscriber (Super Admin override)
     */
    @PostMapping("/cards/{cardId}/assign")
    @Transactional
    public ResponseEntity<?> assignCard(@PathVariable Long cardId, @RequestBody Map<String, Object> assignmentData) {
        try {
            Long subscriberId = Long.valueOf(assignmentData.get("subscriberId").toString());
            logger.info("Super Admin assigning card {} to subscriber {}", cardId, subscriberId);

            Optional<NfcCard> cardOpt = nfcCardRepository.findById(cardId);
            if (cardOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "Card not found with ID: " + cardId
                ));
            }

            Optional<Subscriber> subscriberOpt = subscriberRepository.findById(subscriberId);
            if (subscriberOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "Subscriber not found with ID: " + subscriberId
                ));
            }

            NfcCard card = cardOpt.get();
            Subscriber subscriber = subscriberOpt.get();

            // Check if card is already assigned
            if (card.getSubscriber() != null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "error", "Card is already assigned to another subscriber",
                    "currentSubscriber", card.getSubscriber().getFirstName() + " " + card.getSubscriber().getLastName()
                ));
            }

            // Check if subscriber already has a card
            Optional<NfcCard> existingCard = nfcCardRepository.findBySubscriber(subscriber);
            if (existingCard.isPresent()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "error", "Subscriber already has an assigned card",
                    "existingCardUid", existingCard.get().getCardUid()
                ));
            }

            // Assign the card
            card.setSubscriber(subscriber);
            nfcCardRepository.save(card);

            logger.info("Successfully assigned card {} to subscriber {} {}",
                       card.getCardUid(), subscriber.getFirstName(), subscriber.getLastName());

            return ResponseEntity.ok(Map.of(
                "message", "Card assigned successfully",
                "card", convertToDto(card)
            ));
        } catch (Exception e) {
            logger.error("Failed to assign card {}", cardId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Failed to assign card: " + e.getMessage()
            ));
        }
    }

    /**
     * Unassign a card from its subscriber (Super Admin override)
     */
    @PostMapping("/cards/{cardId}/unassign")
    @Transactional
    public ResponseEntity<?> unassignCard(@PathVariable Long cardId) {
        try {
            logger.info("Super Admin unassigning card {}", cardId);

            Optional<NfcCard> cardOpt = nfcCardRepository.findById(cardId);
            if (cardOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "Card not found with ID: " + cardId
                ));
            }

            NfcCard card = cardOpt.get();

            if (card.getSubscriber() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "error", "Card is not assigned to any subscriber"
                ));
            }

            String subscriberName = card.getSubscriber().getFirstName() + " " + card.getSubscriber().getLastName();
            card.setSubscriber(null);
            nfcCardRepository.save(card);

            logger.info("Successfully unassigned card {} from subscriber {}", card.getCardUid(), subscriberName);

            return ResponseEntity.ok(Map.of(
                "message", "Card unassigned successfully",
                "card", convertToDto(card),
                "previousSubscriber", subscriberName
            ));
        } catch (Exception e) {
            logger.error("Failed to unassign card {}", cardId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Failed to unassign card: " + e.getMessage()
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

            // Get all active sessions (including those from scheduled sessions)
            List<AttendanceSession> activeSessions = attendanceSessionRepository
                    .findByOrganizationAndEndTimeIsNullAndStartTimeBefore(organization, LocalDateTime.now());

            logger.info("Found {} active sessions for entity {}", activeSessions.size(), entityId);
            
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
     * Get all subscribers for a specific entity (for assignment purposes)
     */
    @GetMapping("/subscribers/entity/{entityId}")
    public ResponseEntity<List<Map<String, Object>>> getSubscribersByEntity(@PathVariable String entityId) {
        try {
            logger.info("Fetching subscribers for entity: {}", entityId);

            Optional<Organization> orgOpt = organizationRepository.findByEntityId(entityId);
            if (orgOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(List.of());
            }

            List<Subscriber> subscribers = subscriberRepository.findAllByOrganization(orgOpt.get());
            List<Map<String, Object>> subscriberData = subscribers.stream()
                    .map(subscriber -> {
                        Map<String, Object> data = new HashMap<>();
                        data.put("id", subscriber.getId());
                        data.put("name", subscriber.getFirstName() + " " + subscriber.getLastName());
                        data.put("email", subscriber.getEmail() != null ? subscriber.getEmail() : "");
                        data.put("mobileNumber", subscriber.getMobileNumber());
                        data.put("hasCard", subscriber.getNfcCard() != null);
                        data.put("cardUid", subscriber.getNfcCard() != null ? subscriber.getNfcCard().getCardUid() : null);
                        return data;
                    })
                    .collect(Collectors.toList());

            logger.info("Found {} subscribers for entity: {}", subscriberData.size(), entityId);
            return ResponseEntity.ok(subscriberData);
        } catch (Exception e) {
            logger.error("Failed to fetch subscribers for entity: {}", entityId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
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
