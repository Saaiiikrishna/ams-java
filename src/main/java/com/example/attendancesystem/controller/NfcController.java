package com.example.attendancesystem.controller;

import com.example.attendancesystem.dto.NfcScanDto;
import com.example.attendancesystem.model.*;
import com.example.attendancesystem.repository.AttendanceLogRepository;
import com.example.attendancesystem.repository.AttendanceSessionRepository;
import com.example.attendancesystem.repository.NfcCardRepository;
import jakarta.persistence.EntityNotFoundException;
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
import java.util.Optional;

@RestController
@RequestMapping("/nfc")
// @PreAuthorize("hasRole('ENTITY_ADMIN') or hasRole('NFC_DEVICE')") // Or some other appropriate role
// For now, let's assume an EntityAdmin might test this, or a specific device role.
// If open to devices, they would need their own auth mechanism (e.g. API key, device JWT)
// For simplicity in this phase, let's keep it to ENTITY_ADMIN or even broader if testing.
// We will refine this authorization later if needed. For now, let's allow authenticated users.
@PreAuthorize("isAuthenticated()")
public class NfcController {

    @Autowired
    private NfcCardRepository nfcCardRepository;

    @Autowired
    private AttendanceSessionRepository attendanceSessionRepository;

    @Autowired
    private AttendanceLogRepository attendanceLogRepository;

    @PostMapping("/scan")
    @Transactional
    public ResponseEntity<?> recordNfcScan(@RequestBody NfcScanDto nfcScanDto) {
        if (nfcScanDto.getCardUid() == null || nfcScanDto.getCardUid().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Card UID is required.");
        }

        NfcCard nfcCard = nfcCardRepository.findByCardUid(nfcScanDto.getCardUid())
                .orElseThrow(() -> new EntityNotFoundException("NFC card not found with UID: " + nfcScanDto.getCardUid()));

        if (!nfcCard.isActive() || nfcCard.getSubscriber() == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("NFC card is inactive or not associated with a subscriber.");
        }

        Subscriber subscriber = nfcCard.getSubscriber();
        Organization organization = subscriber.getOrganization();

        // Find active sessions for the organization
        // An active session is one that has started but not yet ended.
        List<AttendanceSession> activeSessions = attendanceSessionRepository
            .findByOrganizationAndEndTimeIsNullAndStartTimeBefore(organization, LocalDateTime.now());


        if (activeSessions.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No active attendance session found for organization: " + organization.getName());
        }

        // Assuming one subscriber can only be part of one session at a time,
        // or the first active session found is the target.
        // Business logic might be more complex here (e.g. subscriber chooses a session if multiple are active).
        // For now, let's pick the most recently started active session.
        AttendanceSession targetSession = activeSessions.stream()
            .max((s1, s2) -> s1.getStartTime().compareTo(s2.getStartTime()))
            .orElse(null); // Should not happen if activeSessions is not empty

        if (targetSession == null) { // Should be redundant given the isEmpty check
             return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Error identifying target session.");
        }


        Optional<AttendanceLog> existingLogOpt = attendanceLogRepository.findBySubscriberAndSession(subscriber, targetSession);

        if (existingLogOpt.isPresent()) {
            AttendanceLog existingLog = existingLogOpt.get();
            if (existingLog.getCheckOutTime() == null) {
                // Already checked in, so check out
                existingLog.setCheckOutTime(LocalDateTime.now());
                AttendanceLog updatedLog = attendanceLogRepository.save(existingLog);
                return ResponseEntity.ok("Checked out successfully from session: " + targetSession.getName() + " at " + updatedLog.getCheckOutTime());
            } else {
                // Already checked in and out, prevent re-check-in to same log.
                // Business rule: one check-in/out per session per subscriber.
                // Could create a new log if re-entry is allowed, but that's more complex.
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Already checked in and out for this session.");
            }
        } else {
            // New check-in
            AttendanceLog newLog = new AttendanceLog();
            newLog.setSubscriber(subscriber);
            newLog.setSession(targetSession);
            newLog.setCheckInTime(LocalDateTime.now());
            // checkOutTime is null initially
            AttendanceLog savedLog = attendanceLogRepository.save(newLog);
            return ResponseEntity.status(HttpStatus.CREATED).body("Checked in successfully to session: " + targetSession.getName() + " at " + savedLog.getCheckInTime());
        }
    }
}
