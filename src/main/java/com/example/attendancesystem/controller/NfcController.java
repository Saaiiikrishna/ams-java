package com.example.attendancesystem.subscriber.controller;

import com.example.attendancesystem.dto.NfcScanDto;
import com.example.attendancesystem.model.*;
import com.example.attendancesystem.repository.AttendanceLogRepository;
import com.example.attendancesystem.repository.AttendanceSessionRepository;
import com.example.attendancesystem.repository.NfcCardRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/attendance")
@PreAuthorize("hasRole('ENTITY_ADMIN')")
public class NfcController {

    private static final Logger logger = LoggerFactory.getLogger(NfcController.class);

    @Autowired
    private NfcCardRepository nfcCardRepository;

    @Autowired
    private AttendanceSessionRepository attendanceSessionRepository;

    @Autowired
    private AttendanceLogRepository attendanceLogRepository;

    @PostMapping("/checkin")
    @Transactional
    public ResponseEntity<?> recordNfcScan(@RequestBody NfcScanDto nfcScanDto) {
        logger.info("NFC scan request - Card UID: {}", nfcScanDto.getCardUid());

        if (nfcScanDto.getCardUid() == null || nfcScanDto.getCardUid().isEmpty()) {
            logger.warn("NFC scan failed - Card UID is required");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error", "Card UID is required",
                "code", "MISSING_CARD_UID"
            ));
        }

        // Find the card
        Optional<NfcCard> cardOptional = nfcCardRepository.findByCardUid(nfcScanDto.getCardUid());
        if (!cardOptional.isPresent()) {
            logger.warn("NFC scan failed - Card not found: {}", nfcScanDto.getCardUid());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "error", "NFC card not found with UID: " + nfcScanDto.getCardUid(),
                "code", "CARD_NOT_FOUND"
            ));
        }

        NfcCard nfcCard = cardOptional.get();

        // Check if card is active
        if (!nfcCard.isActive()) {
            logger.warn("NFC scan failed - Card inactive: {}", nfcScanDto.getCardUid());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "error", "NFC card is inactive",
                "code", "CARD_INACTIVE"
            ));
        }

        // Check if card is assigned to a subscriber
        if (nfcCard.getSubscriber() == null) {
            logger.warn("NFC scan failed - Card not assigned: {}", nfcScanDto.getCardUid());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "error", "NFC card is not assigned to any subscriber. Please assign the card first.",
                "code", "CARD_NOT_ASSIGNED"
            ));
        }

        Subscriber subscriber = nfcCard.getSubscriber();
        Organization organization = subscriber.getOrganization();

        // Find active sessions for the organization
        // An active session is one that has started but not yet ended.
        List<AttendanceSession> activeSessions = attendanceSessionRepository
            .findByOrganizationAndEndTimeIsNullAndStartTimeBefore(organization, LocalDateTime.now());


        if (activeSessions.isEmpty()) {
            logger.warn("NFC scan failed - No active session for organization: {}", organization.getName());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "error", "No active attendance session found for organization: " + organization.getName(),
                "code", "NO_ACTIVE_SESSION"
            ));
        }

        // Assuming one subscriber can only be part of one session at a time,
        // or the first active session found is the target.
        // Business logic might be more complex here (e.g. subscriber chooses a session if multiple are active).
        // For now, let's pick the most recently started active session.
        AttendanceSession targetSession = activeSessions.stream()
            .max((s1, s2) -> s1.getStartTime().compareTo(s2.getStartTime()))
            .orElse(null); // Should not happen if activeSessions is not empty

        if (targetSession == null) { // Should be redundant given the isEmpty check
            logger.error("NFC scan failed - Error identifying target session");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Error identifying target session",
                "code", "SESSION_ERROR"
            ));
        }


        Optional<AttendanceLog> existingLogOpt = attendanceLogRepository.findBySubscriberAndSession(subscriber, targetSession);

        if (existingLogOpt.isPresent()) {
            AttendanceLog existingLog = existingLogOpt.get();
            if (existingLog.getCheckOutTime() == null) {
                // Already checked in, so check out
                existingLog.setCheckOutTime(LocalDateTime.now());
                existingLog.setCheckOutMethod(CheckInMethod.NFC);
                AttendanceLog updatedLog = attendanceLogRepository.save(existingLog);
                logger.info("Check-out successful - Subscriber: {} {}, Session: {}",
                           subscriber.getFirstName(), subscriber.getLastName(), targetSession.getName());
                return ResponseEntity.ok(Map.of(
                    "message", "Checked out successfully from session: " + targetSession.getName(),
                    "action", "CHECK_OUT",
                    "time", updatedLog.getCheckOutTime(),
                    "subscriber", subscriber.getFirstName() + " " + subscriber.getLastName(),
                    "session", targetSession.getName(),
                    "checkInMethod", updatedLog.getCheckInMethod().toString(),
                    "checkOutMethod", "NFC"
                ));
            } else {
                // Already checked in and out, prevent re-check-in to same log.
                logger.warn("NFC scan failed - Already checked in and out: {} {}",
                           subscriber.getFirstName(), subscriber.getLastName());
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "error", "Already checked in and out for this session",
                    "code", "ALREADY_COMPLETED"
                ));
            }
        } else {
            // New check-in
            AttendanceLog newLog = new AttendanceLog();
            newLog.setSubscriber(subscriber);
            newLog.setSession(targetSession);
            newLog.setCheckInTime(LocalDateTime.now());
            newLog.setCheckInMethod(CheckInMethod.NFC);
            // checkOutTime is null initially
            AttendanceLog savedLog = attendanceLogRepository.save(newLog);
            logger.info("Check-in successful - Subscriber: {} {}, Session: {}",
                       subscriber.getFirstName(), subscriber.getLastName(), targetSession.getName());
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Checked in successfully to session: " + targetSession.getName(),
                "action", "CHECK_IN",
                "time", savedLog.getCheckInTime(),
                "subscriber", subscriber.getFirstName() + " " + subscriber.getLastName(),
                "session", targetSession.getName(),
                "checkInMethod", "NFC",
                "checkOutMethod", (String) null
            ));
        }
    }
}
