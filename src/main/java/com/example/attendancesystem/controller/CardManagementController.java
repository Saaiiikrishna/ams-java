package com.example.attendancesystem.controller;

import com.example.attendancesystem.dto.CardAssignmentDto;
import com.example.attendancesystem.dto.CardRegistrationDto;
import com.example.attendancesystem.dto.NfcCardDto;
import com.example.attendancesystem.model.EntityAdmin;
import com.example.attendancesystem.security.CustomUserDetails;
import com.example.attendancesystem.service.NfcCardService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/cards")
@PreAuthorize("hasRole('ENTITY_ADMIN')")
public class CardManagementController {

    private static final Logger logger = LoggerFactory.getLogger(CardManagementController.class);

    @Autowired
    private NfcCardService nfcCardService;

    /**
     * Register a new NFC card
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerCard(@Valid @RequestBody CardRegistrationDto registrationDto,
                                         Authentication authentication) {
        try {
            String entityId = getEntityIdFromAuthentication(authentication);
            logger.info("Card registration request received - UID: {}, User: {}, Entity: {}",
                       registrationDto.getCardUid(), authentication.getName(), entityId);
            logger.debug("Authentication principal type: {}", authentication.getPrincipal().getClass().getName());

            NfcCardDto cardDto = nfcCardService.registerCard(registrationDto, entityId);
            logger.info("Card registered successfully - UID: {}, Entity: {}", registrationDto.getCardUid(), entityId);

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Card registered successfully",
                "card", cardDto
            ));
        } catch (IllegalArgumentException e) {
            logger.warn("Card registration failed - validation error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "error", e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("Failed to register card - UID: {}", registrationDto.getCardUid(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Failed to register card: " + e.getMessage()
            ));
        }
    }

    /**
     * Assign a card to a subscriber
     */
    @PostMapping("/assign")
    public ResponseEntity<?> assignCard(@Valid @RequestBody CardAssignmentDto assignmentDto, 
                                       Authentication authentication) {
        try {
            String entityId = getEntityIdFromAuthentication(authentication);
            logger.info("Card assignment request - UID: {}, Subscriber: {}, Entity: {}", 
                       assignmentDto.getCardUid(), assignmentDto.getSubscriberId(), entityId);
            
            NfcCardDto cardDto = nfcCardService.assignCard(assignmentDto, entityId);
            
            return ResponseEntity.ok(Map.of(
                "message", "Card assigned successfully",
                "card", cardDto
            ));
        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.warn("Card assignment failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error", e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("Failed to assign card", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Failed to assign card: " + e.getMessage()
            ));
        }
    }

    /**
     * Unassign a card from its subscriber
     */
    @PostMapping("/unassign/{cardUid}")
    public ResponseEntity<?> unassignCard(@PathVariable String cardUid) {
        try {
            logger.info("Card unassignment request - UID: {}", cardUid);
            
            NfcCardDto cardDto = nfcCardService.unassignCard(cardUid);
            
            return ResponseEntity.ok(Map.of(
                "message", "Card unassigned successfully",
                "card", cardDto
            ));
        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.warn("Card unassignment failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error", e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("Failed to unassign card", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Failed to unassign card: " + e.getMessage()
            ));
        }
    }

    /**
     * Get all cards with optional filtering
     */
    @GetMapping
    public ResponseEntity<List<NfcCardDto>> getAllCards(
            @RequestParam(required = false) Boolean assigned,
            Authentication authentication) {
        try {
            String entityId = getEntityIdFromAuthentication(authentication);
            logger.debug("Fetching cards - assigned: {}, entity: {}", assigned, entityId);
            
            List<NfcCardDto> cards = nfcCardService.getAllCards(assigned, entityId);
            
            return ResponseEntity.ok(cards);
        } catch (Exception e) {
            logger.error("Failed to fetch cards", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get unassigned cards for assignment workflow
     */
    @GetMapping("/unassigned")
    public ResponseEntity<List<NfcCardDto>> getUnassignedCards(Authentication authentication) {
        try {
            String entityId = getEntityIdFromAuthentication(authentication);
            logger.debug("Fetching unassigned cards for entity: {}", entityId);

            List<NfcCardDto> cards = nfcCardService.getUnassignedCards(entityId);

            return ResponseEntity.ok(cards);
        } catch (Exception e) {
            logger.error("Failed to fetch unassigned cards", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get card by UID
     */
    @GetMapping("/{cardUid}")
    public ResponseEntity<?> getCardByUid(@PathVariable String cardUid) {
        try {
            logger.debug("Fetching card by UID: {}", cardUid);
            
            Optional<NfcCardDto> cardDto = nfcCardService.getCardByUid(cardUid);
            
            if (cardDto.isPresent()) {
                return ResponseEntity.ok(cardDto.get());
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "Card not found with UID: " + cardUid
                ));
            }
        } catch (Exception e) {
            logger.error("Failed to fetch card by UID: {}", cardUid, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Failed to fetch card: " + e.getMessage()
            ));
        }
    }

    /**
     * Delete a card (only if unassigned)
     */
    @DeleteMapping("/{cardUid}")
    public ResponseEntity<?> deleteCard(@PathVariable String cardUid) {
        try {
            logger.info("Card deletion request - UID: {}", cardUid);
            
            nfcCardService.deleteCard(cardUid);
            
            return ResponseEntity.ok(Map.of(
                "message", "Card deleted successfully"
            ));
        } catch (IllegalArgumentException e) {
            logger.warn("Card deletion failed - not found: {}", cardUid);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "error", e.getMessage()
            ));
        } catch (IllegalStateException e) {
            logger.warn("Card deletion failed - assigned: {}", cardUid);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error", e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("Failed to delete card: {}", cardUid, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Failed to delete card: " + e.getMessage()
            ));
        }
    }

    /**
     * Get card statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Long>> getCardStatistics(Authentication authentication) {
        try {
            String entityId = getEntityIdFromAuthentication(authentication);
            logger.debug("Fetching card statistics for entity: {}", entityId);

            Map<String, Long> statistics = nfcCardService.getCardStatistics(entityId);

            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            logger.error("Failed to fetch card statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Extract entity ID from authentication context
     */
    private String getEntityIdFromAuthentication(Authentication authentication) {
        if (authentication.getPrincipal() instanceof CustomUserDetails) {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            EntityAdmin entityAdmin = userDetails.getEntityAdmin();
            if (entityAdmin != null && entityAdmin.getOrganization() != null) {
                return entityAdmin.getOrganization().getEntityId();
            }
        }
        throw new IllegalStateException("Unable to determine entity context from authentication");
    }
}
