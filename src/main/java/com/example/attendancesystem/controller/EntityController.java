package com.example.attendancesystem.controller;

import com.example.attendancesystem.dto.AttendanceSessionDto;
import com.example.attendancesystem.dto.SubscriberDto;
import com.example.attendancesystem.model.*;
import com.example.attendancesystem.repository.*;
import com.example.attendancesystem.security.CustomUserDetails;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*; // Keep this for existing annotations

import java.util.Collections; // For empty list if needed, though repository methods handle it

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/entity")
@PreAuthorize("hasRole('ENTITY_ADMIN')")
public class EntityController {

    @Autowired
    private SubscriberRepository subscriberRepository;

    @Autowired
    private OrganizationRepository organizationRepository; // To fetch Organization

    @Autowired
    private NfcCardRepository nfcCardRepository;

    @Autowired
    private AttendanceSessionRepository attendanceSessionRepository;

    // Helper to get current EntityAdmin's organization
    private Organization getCurrentOrganization() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return userDetails.getEntityAdmin().getOrganization();
    }

    // Subscriber Mappings
    @PostMapping("/subscribers")
    @Transactional
    public ResponseEntity<?> addSubscriber(@RequestBody SubscriberDto subscriberDto) {
        Organization organization = getCurrentOrganization();
        if (subscriberRepository.existsByEmailAndOrganization(subscriberDto.getEmail(), organization)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Subscriber email already exists in this organization.");
        }

        Subscriber subscriber = new Subscriber();
        subscriber.setFirstName(subscriberDto.getFirstName());
        subscriber.setLastName(subscriberDto.getLastName());
        subscriber.setEmail(subscriberDto.getEmail());
        subscriber.setOrganization(organization);

        if (subscriberDto.getNfcCardUid() != null && !subscriberDto.getNfcCardUid().isEmpty()) {
            if (nfcCardRepository.existsByCardUid(subscriberDto.getNfcCardUid())) {
                 return ResponseEntity.status(HttpStatus.CONFLICT).body("NFC Card UID already in use.");
            }
            NfcCard nfcCard = new NfcCard();
            nfcCard.setCardUid(subscriberDto.getNfcCardUid());
            nfcCard.setSubscriber(subscriber);
            nfcCard.setActive(true);
            subscriber.setNfcCard(nfcCard); // This will save NfcCard due to cascade if configured, or save manually
        }

        Subscriber savedSubscriber = subscriberRepository.save(subscriber);
        // nfcCardRepository.save(subscriber.getNfcCard()); // if not cascaded
        return ResponseEntity.status(HttpStatus.CREATED).body(convertToDto(savedSubscriber));
    }

    @GetMapping("/subscribers")
    public ResponseEntity<List<SubscriberDto>> getSubscribers() {
        Organization organization = getCurrentOrganization();
        List<Subscriber> subscribers = subscriberRepository.findAllByOrganization(organization);
        return ResponseEntity.ok(subscribers.stream().map(this::convertToDto).collect(Collectors.toList()));
    }

    @PutMapping("/subscribers/{id}")
    @Transactional
    public ResponseEntity<?> updateSubscriber(@PathVariable Long id, @RequestBody SubscriberDto subscriberDto) {
        Organization organization = getCurrentOrganization();
        Subscriber subscriber = subscriberRepository.findByIdAndOrganization(id, organization)
                .orElseThrow(() -> new EntityNotFoundException("Subscriber not found with id: " + id + " in your organization."));

        // Check for email conflict if email is being changed
        if (!subscriber.getEmail().equals(subscriberDto.getEmail()) &&
            subscriberRepository.existsByEmailAndOrganization(subscriberDto.getEmail(), organization)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Another subscriber with this email already exists.");
        }

        subscriber.setFirstName(subscriberDto.getFirstName());
        subscriber.setLastName(subscriberDto.getLastName());
        subscriber.setEmail(subscriberDto.getEmail());

        // NFC Card handling (update or assign)
        NfcCard existingNfcCard = subscriber.getNfcCard();
        String newNfcCardUid = subscriberDto.getNfcCardUid();

        if (newNfcCardUid != null && !newNfcCardUid.isEmpty()) {
            if (existingNfcCard != null && !existingNfcCard.getCardUid().equals(newNfcCardUid)) {
                // UID is changing, ensure new one is not taken
                if (nfcCardRepository.existsByCardUid(newNfcCardUid)) {
                    return ResponseEntity.status(HttpStatus.CONFLICT).body("New NFC Card UID ("+newNfcCardUid+") already in use.");
                }
                existingNfcCard.setCardUid(newNfcCardUid);
            } else if (existingNfcCard == null) {
                // Assigning new card
                 if (nfcCardRepository.existsByCardUid(newNfcCardUid)) {
                    return ResponseEntity.status(HttpStatus.CONFLICT).body("NFC Card UID ("+newNfcCardUid+") already in use.");
                }
                NfcCard newCard = new NfcCard();
                newCard.setCardUid(newNfcCardUid);
                newCard.setSubscriber(subscriber);
                newCard.setActive(true);
                subscriber.setNfcCard(newCard);
                // nfcCardRepository.save(newCard); // If not cascaded
            }
        } else if (existingNfcCard != null) {
            // NFC Card UID is removed in DTO, so unassign/delete card
            nfcCardRepository.delete(existingNfcCard);
            subscriber.setNfcCard(null);
        }

        Subscriber updatedSubscriber = subscriberRepository.save(subscriber);
        return ResponseEntity.ok(convertToDto(updatedSubscriber));
    }

    @DeleteMapping("/subscribers/{id}")
    @Transactional
    public ResponseEntity<?> deleteSubscriber(@PathVariable Long id) {
        Organization organization = getCurrentOrganization();
        Subscriber subscriber = subscriberRepository.findByIdAndOrganization(id, organization)
                .orElseThrow(() -> new EntityNotFoundException("Subscriber not found with id: " + id + " in your organization."));

        // Consider what to do with AttendanceLogs: anonymize, delete, or keep.
        // For now, we'll delete the subscriber. Associated NfcCard will be deleted by cascade if set up.
        // If NfcCard is not set to cascade delete, delete it manually:
        if (subscriber.getNfcCard() != null) {
            nfcCardRepository.delete(subscriber.getNfcCard());
        }
        subscriberRepository.delete(subscriber);
        return ResponseEntity.ok().body("Subscriber with id " + id + " deleted successfully.");
    }


    // Attendance Session Mappings
    @PostMapping("/sessions")
    public ResponseEntity<?> createSession(@RequestBody AttendanceSessionDto sessionDto) {
        Organization organization = getCurrentOrganization();
        AttendanceSession session = new AttendanceSession();
        session.setName(sessionDto.getName());
        session.setStartTime(sessionDto.getStartTime() != null ? sessionDto.getStartTime() : LocalDateTime.now());
        session.setOrganization(organization);
        // endTime is null initially

        AttendanceSession savedSession = attendanceSessionRepository.save(session);
        return ResponseEntity.status(HttpStatus.CREATED).body(convertToDto(savedSession));
    }

    @PutMapping("/sessions/{id}/end")
    public ResponseEntity<?> endSession(@PathVariable Long id) {
        Organization organization = getCurrentOrganization();
        AttendanceSession session = attendanceSessionRepository.findByIdAndOrganization(id, organization)
                .orElseThrow(() -> new EntityNotFoundException("Attendance session not found with id: " + id + " in your organization."));

        if (session.getEndTime() != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Session already ended.");
        }
        session.setEndTime(LocalDateTime.now());
        AttendanceSession updatedSession = attendanceSessionRepository.save(session);
        return ResponseEntity.ok(convertToDto(updatedSession));
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<AttendanceSessionDto>> getSessions() {
        Organization organization = getCurrentOrganization();
        List<AttendanceSession> sessions = attendanceSessionRepository.findAllByOrganization(organization);
        List<AttendanceSessionDto> sessionDtos = sessions.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(sessionDtos);
    }

    // DTO Converters
    private SubscriberDto convertToDto(Subscriber subscriber) {
        String nfcUid = subscriber.getNfcCard() != null ? subscriber.getNfcCard().getCardUid() : null;
        return new SubscriberDto(
                subscriber.getId(),
                subscriber.getFirstName(),
                subscriber.getLastName(),
                subscriber.getEmail(),
                subscriber.getOrganization().getId(),
                nfcUid
        );
    }

    private AttendanceSessionDto convertToDto(AttendanceSession session) {
        return new AttendanceSessionDto(
                session.getId(),
                session.getName(),
                session.getStartTime(),
                session.getEndTime(),
                session.getOrganization().getId()
        );
    }
}
