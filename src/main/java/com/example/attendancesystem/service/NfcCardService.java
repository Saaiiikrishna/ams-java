package com.example.attendancesystem.service;

import com.example.attendancesystem.dto.CardAssignmentDto;
import com.example.attendancesystem.dto.CardRegistrationDto;
import com.example.attendancesystem.dto.NfcCardDto;
import com.example.attendancesystem.model.NfcCard;
import com.example.attendancesystem.model.Organization;
import com.example.attendancesystem.model.Subscriber;
import com.example.attendancesystem.repository.NfcCardRepository;
import com.example.attendancesystem.repository.OrganizationRepository;
import com.example.attendancesystem.repository.SubscriberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class NfcCardService {

    private static final Logger logger = LoggerFactory.getLogger(NfcCardService.class);

    @Autowired
    private NfcCardRepository nfcCardRepository;

    @Autowired
    private SubscriberRepository subscriberRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    /**
     * Register a new NFC card without assigning it to any subscriber
     */
    @Transactional
    public NfcCardDto registerCard(CardRegistrationDto registrationDto, String entityId) {
        logger.info("Registering new NFC card with UID: {} for entity: {}", registrationDto.getCardUid(), entityId);

        // Check if card UID already exists
        if (nfcCardRepository.existsByCardUid(registrationDto.getCardUid())) {
            logger.warn("Card registration failed - UID already exists: {}", registrationDto.getCardUid());
            throw new IllegalArgumentException("Card UID already exists: " + registrationDto.getCardUid());
        }

        // Find the organization
        Organization organization = organizationRepository.findByEntityId(entityId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found with entity ID: " + entityId));

        // Create new unassigned card
        NfcCard card = new NfcCard();
        card.setCardUid(registrationDto.getCardUid());
        card.setActive(registrationDto.isActive());
        card.setSubscriber(null); // Unassigned
        card.setOrganization(organization); // Assign to organization

        NfcCard savedCard = nfcCardRepository.save(card);
        logger.info("NFC card registered successfully - ID: {}, UID: {}, Entity: {}",
                   savedCard.getId(), savedCard.getCardUid(), entityId);

        return convertToDto(savedCard);
    }

    /**
     * Assign a card to a subscriber
     */
    @Transactional
    public NfcCardDto assignCard(CardAssignmentDto assignmentDto, String entityId) {
        logger.info("Assigning card {} to subscriber {} in entity {}", 
                   assignmentDto.getCardUid(), assignmentDto.getSubscriberId(), entityId);

        // Find the card
        NfcCard card = nfcCardRepository.findByCardUid(assignmentDto.getCardUid())
                .orElseThrow(() -> new IllegalArgumentException("Card not found with UID: " + assignmentDto.getCardUid()));

        // Find the subscriber and verify it belongs to the correct entity
        Subscriber subscriber = subscriberRepository.findByIdAndOrganizationEntityId(
                assignmentDto.getSubscriberId(), entityId)
                .orElseThrow(() -> new IllegalArgumentException("Subscriber not found or doesn't belong to this entity"));

        // Check if card is already assigned
        if (card.getSubscriber() != null) {
            logger.warn("Card assignment failed - card already assigned: {} to subscriber {}", 
                       assignmentDto.getCardUid(), card.getSubscriber().getId());
            throw new IllegalStateException("Card is already assigned to subscriber: " + 
                                          card.getSubscriber().getFirstName() + " " + card.getSubscriber().getLastName());
        }

        // Check if subscriber already has a card - unassign it first
        Optional<NfcCard> existingCard = nfcCardRepository.findBySubscriber(subscriber);
        if (existingCard.isPresent()) {
            logger.info("Unassigning existing card {} from subscriber {}", 
                       existingCard.get().getCardUid(), subscriber.getId());
            existingCard.get().setSubscriber(null);
            nfcCardRepository.save(existingCard.get());
        }

        // Assign the new card
        card.setSubscriber(subscriber);
        NfcCard savedCard = nfcCardRepository.save(card);

        logger.info("Card assigned successfully - UID: {}, Subscriber: {} {}", 
                   savedCard.getCardUid(), subscriber.getFirstName(), subscriber.getLastName());

        return convertToDto(savedCard);
    }

    /**
     * Unassign a card from its subscriber
     */
    @Transactional
    public NfcCardDto unassignCard(String cardUid) {
        logger.info("Unassigning card with UID: {}", cardUid);

        NfcCard card = nfcCardRepository.findByCardUid(cardUid)
                .orElseThrow(() -> new IllegalArgumentException("Card not found with UID: " + cardUid));

        if (card.getSubscriber() == null) {
            logger.warn("Card unassignment failed - card not assigned: {}", cardUid);
            throw new IllegalStateException("Card is not assigned to any subscriber");
        }

        Subscriber previousSubscriber = card.getSubscriber();
        card.setSubscriber(null);
        NfcCard savedCard = nfcCardRepository.save(card);

        logger.info("Card unassigned successfully - UID: {}, Previous subscriber: {} {}", 
                   cardUid, previousSubscriber.getFirstName(), previousSubscriber.getLastName());

        return convertToDto(savedCard);
    }

    /**
     * Get all cards with optional filtering for a specific entity
     */
    public List<NfcCardDto> getAllCards(Boolean assigned, String entityId) {
        logger.debug("Fetching cards - assigned: {}, entityId: {}", assigned, entityId);

        List<NfcCard> cards;

        if (entityId != null) {
            // Filter by entity and assignment status
            if (assigned != null) {
                if (assigned) {
                    cards = nfcCardRepository.findBySubscriberIsNotNullAndOrganizationEntityId(entityId);
                } else {
                    cards = nfcCardRepository.findBySubscriberIsNullAndOrganizationEntityId(entityId);
                }
            } else {
                // Get all cards for the entity
                cards = nfcCardRepository.findByOrganizationEntityId(entityId);
            }
        } else {
            // Global access (for super admin) - filter by assignment status only
            if (assigned != null) {
                if (assigned) {
                    cards = nfcCardRepository.findBySubscriberIsNotNull();
                } else {
                    cards = nfcCardRepository.findBySubscriberIsNull();
                }
            } else {
                // Get all cards globally
                cards = nfcCardRepository.findAll();
            }
        }

        return cards.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get unassigned cards for assignment workflow for a specific entity
     */
    public List<NfcCardDto> getUnassignedCards(String entityId) {
        logger.debug("Fetching unassigned active cards for entity: {}", entityId);

        List<NfcCard> unassignedCards = nfcCardRepository.findUnassignedActiveCardsByEntityId(entityId);
        return unassignedCards.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get card by UID
     */
    public Optional<NfcCardDto> getCardByUid(String cardUid) {
        logger.debug("Fetching card by UID: {}", cardUid);
        
        return nfcCardRepository.findByCardUid(cardUid)
                .map(this::convertToDto);
    }

    /**
     * Delete a card (only if unassigned)
     */
    @Transactional
    public void deleteCard(String cardUid) {
        logger.info("Deleting card with UID: {}", cardUid);

        NfcCard card = nfcCardRepository.findByCardUid(cardUid)
                .orElseThrow(() -> new IllegalArgumentException("Card not found with UID: " + cardUid));

        if (card.getSubscriber() != null) {
            logger.warn("Card deletion failed - card is assigned: {}", cardUid);
            throw new IllegalStateException("Cannot delete assigned card. Please unassign first.");
        }

        nfcCardRepository.delete(card);
        logger.info("Card deleted successfully - UID: {}", cardUid);
    }

    /**
     * Convert NfcCard entity to DTO
     */
    private NfcCardDto convertToDto(NfcCard card) {
        if (card.getSubscriber() == null) {
            // Unassigned card
            NfcCardDto dto = new NfcCardDto(card.getId(), card.getCardUid(), card.isActive());
            // Set organization info if available (now using entity_id FK)
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

    /**
     * Get card statistics for a specific entity
     */
    public Map<String, Long> getCardStatistics(String entityId) {
        long totalCards = nfcCardRepository.countByOrganizationEntityId(entityId);
        long assignedCards = nfcCardRepository.countBySubscriberIsNotNullAndOrganizationEntityId(entityId);
        long unassignedCards = nfcCardRepository.countBySubscriberIsNullAndOrganizationEntityId(entityId);

        Map<String, Long> stats = new HashMap<>();
        stats.put("total", totalCards);
        stats.put("assigned", assignedCards);
        stats.put("unassigned", unassignedCards);

        return stats;
    }

    /**
     * Get global card statistics (for super admin)
     */
    public Map<String, Long> getGlobalCardStatistics() {
        long totalCards = nfcCardRepository.count();
        long assignedCards = nfcCardRepository.countBySubscriberIsNotNull();
        long unassignedCards = nfcCardRepository.countBySubscriberIsNull();

        Map<String, Long> stats = new HashMap<>();
        stats.put("total", totalCards);
        stats.put("assigned", assignedCards);
        stats.put("unassigned", unassignedCards);

        return stats;
    }
}
