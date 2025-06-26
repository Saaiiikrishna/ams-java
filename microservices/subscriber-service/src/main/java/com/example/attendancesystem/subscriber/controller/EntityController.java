package com.example.attendancesystem.organization.controller;

import com.example.attendancesystem.dto.AttendanceSessionDto;
import com.example.attendancesystem.dto.ScheduledSessionDto;
import com.example.attendancesystem.dto.SubscriberDto;
import com.example.attendancesystem.organization.model.*;
import com.example.attendancesystem.organization.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.Set;
import com.example.attendancesystem.security.CustomUserDetails;
import com.example.attendancesystem.organization.service.PermissionService;
import com.example.attendancesystem.organization.service.ScheduledSessionService;
import com.example.attendancesystem.organization.service.QrCodeService;
import com.example.attendancesystem.organization.service.SubscriberAuthService;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@PreAuthorize("hasRole('ENTITY_ADMIN')")
public class EntityController {

    private static final Logger logger = LoggerFactory.getLogger(EntityController.class);

    @Autowired
    private SubscriberRepository subscriberRepository;

    @Autowired
    private OrganizationRepository organizationRepository; // To fetch Organization

    @Autowired
    private NfcCardRepository nfcCardRepository;

    @Autowired
    private AttendanceSessionRepository attendanceSessionRepository;

    @Autowired
    private EntityAdminRepository entityAdminRepository;

    @Autowired
    private AttendanceLogRepository attendanceLogRepository;

    @Autowired
    private ScheduledSessionService scheduledSessionService;

    @Autowired
    private QrCodeService qrCodeService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SubscriberAuthService subscriberAuthService;

    @Autowired
    private SubscriberAuthRepository subscriberAuthRepository;

    @Autowired
    private PermissionService permissionService;

    // Helper to get current EntityAdmin's organization
    private Organization getCurrentOrganization() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return userDetails.getEntityAdmin().getOrganization();
    }

    // Helper to check member management permission
    private ResponseEntity<?> checkMemberPermission() {
        Organization organization = getCurrentOrganization();
        if (!permissionService.hasPermission(organization.getEntityId(), FeaturePermission.MEMBER_MANAGEMENT)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied. Member management permission required."));
        }
        return null;
    }

    // Helper to check attendance tracking permission
    private ResponseEntity<?> checkAttendancePermission() {
        Organization organization = getCurrentOrganization();
        if (!permissionService.hasPermission(organization.getEntityId(), FeaturePermission.ATTENDANCE_TRACKING)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied. Attendance tracking permission required."));
        }
        return null;
    }

    // Subscriber Mappings
    @PostMapping("/subscribers")
    @Transactional
    public ResponseEntity<?> addSubscriber(@RequestBody SubscriberDto subscriberDto) {
        // Check permission
        ResponseEntity<?> permissionCheck = checkMemberPermission();
        if (permissionCheck != null) return permissionCheck;

        logger.info("Creating new subscriber: {} {} for organization: {}",
                   subscriberDto.getFirstName(), subscriberDto.getLastName(),
                   getCurrentOrganization().getEntityId());

        Organization organization = getCurrentOrganization();

        // Check for mobile number uniqueness (required field)
        if (subscriberDto.getMobileNumber() == null || subscriberDto.getMobileNumber().trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Mobile number is required.");
        }

        // Validate first name and last name
        if (subscriberDto.getFirstName() == null || subscriberDto.getFirstName().trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("First name is required.");
        }

        if (subscriberDto.getLastName() == null || subscriberDto.getLastName().trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Last name is required.");
        }

        if (subscriberRepository.existsByMobileNumberAndOrganization(subscriberDto.getMobileNumber(), organization)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Subscriber mobile number already exists in this organization.");
        }

        // Check email uniqueness only if email is provided
        if (subscriberDto.getEmail() != null && !subscriberDto.getEmail().trim().isEmpty() &&
            subscriberRepository.existsByEmailAndOrganization(subscriberDto.getEmail(), organization)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Subscriber email already exists in this organization.");
        }

        Subscriber subscriber = new Subscriber();
        subscriber.setFirstName(subscriberDto.getFirstName());
        subscriber.setLastName(subscriberDto.getLastName());
        subscriber.setEmail(subscriberDto.getEmail());
        subscriber.setMobileNumber(subscriberDto.getMobileNumber());
        subscriber.setOrganization(organization);

        if (subscriberDto.getNfcCardUid() != null && !subscriberDto.getNfcCardUid().isEmpty()) {
            if (nfcCardRepository.existsByCardUid(subscriberDto.getNfcCardUid())) {
                 return ResponseEntity.status(HttpStatus.CONFLICT).body("NFC Card UID already in use.");
            }
            NfcCard nfcCard = new NfcCard();
            nfcCard.setCardUid(subscriberDto.getNfcCardUid());
            nfcCard.setSubscriber(subscriber);
            nfcCard.setActive(true);
            nfcCard.setOrganization(organization); // Set the organization (now uses entity_id as FK)
            subscriber.setNfcCard(nfcCard); // This will save NfcCard due to cascade if configured, or save manually
        }

        Subscriber savedSubscriber = subscriberRepository.save(subscriber);
        logger.info("Subscriber saved successfully with ID: {}", savedSubscriber.getId());

        // Automatically create authentication credentials for the subscriber
        // Mobile number as username, "0000" as default PIN
        try {
            subscriberAuthService.createSubscriberAuth(savedSubscriber.getId(), "0000");
            logger.info("Authentication created successfully for subscriber ID: {}", savedSubscriber.getId());
        } catch (IllegalArgumentException e) {
            // Log the error but don't fail the subscriber creation
            logger.warn("Failed to create authentication for subscriber ID {}: {}", savedSubscriber.getId(), e.getMessage());
            // If auth already exists, that's fine - subscriber was created successfully
        } catch (Exception e) {
            // Log the error but don't fail the subscriber creation
            logger.error("Unexpected error creating authentication for subscriber ID {}: {}", savedSubscriber.getId(), e.getMessage(), e);
        }

        logger.info("Subscriber creation completed successfully for: {} {}", savedSubscriber.getFirstName(), savedSubscriber.getLastName());
        return ResponseEntity.status(HttpStatus.CREATED).body(convertToDto(savedSubscriber));
    }

    @GetMapping("/subscribers")
    public ResponseEntity<?> getSubscribers() {
        // Check permission
        ResponseEntity<?> permissionCheck = checkMemberPermission();
        if (permissionCheck != null) return permissionCheck;

        Organization organization = getCurrentOrganization();
        List<Subscriber> subscribers = subscriberRepository.findAllByOrganization(organization);
        return ResponseEntity.ok(subscribers.stream().map(this::convertToDto).collect(Collectors.toList()));
    }

    @PutMapping("/subscribers/{id}")
    @Transactional
    public ResponseEntity<?> updateSubscriber(@PathVariable Long id, @RequestBody SubscriberDto subscriberDto) {
        // Check permission
        ResponseEntity<?> permissionCheck = checkMemberPermission();
        if (permissionCheck != null) return permissionCheck;

        Organization organization = getCurrentOrganization();
        Subscriber subscriber = subscriberRepository.findByIdAndOrganization(id, organization)
                .orElseThrow(() -> new EntityNotFoundException("Subscriber not found with id: " + id + " in your organization."));

        // Check for mobile number uniqueness (required field)
        if (subscriberDto.getMobileNumber() == null || subscriberDto.getMobileNumber().trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Mobile number is required.");
        }

        if (!subscriber.getMobileNumber().equals(subscriberDto.getMobileNumber()) &&
            subscriberRepository.existsByMobileNumberAndOrganization(subscriberDto.getMobileNumber(), organization)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Another subscriber with this mobile number already exists.");
        }

        // Check for email conflict if email is being changed (only if email is provided)
        if (subscriberDto.getEmail() != null && !subscriberDto.getEmail().trim().isEmpty() &&
            (subscriber.getEmail() == null || !subscriber.getEmail().equals(subscriberDto.getEmail())) &&
            subscriberRepository.existsByEmailAndOrganization(subscriberDto.getEmail(), organization)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Another subscriber with this email already exists.");
        }

        subscriber.setFirstName(subscriberDto.getFirstName());
        subscriber.setLastName(subscriberDto.getLastName());
        subscriber.setEmail(subscriberDto.getEmail());
        subscriber.setMobileNumber(subscriberDto.getMobileNumber());

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
                newCard.setOrganization(subscriber.getOrganization()); // Set the organization (now uses entity_id as FK)
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
        // Check permission
        ResponseEntity<?> permissionCheck = checkMemberPermission();
        if (permissionCheck != null) return permissionCheck;

        Organization organization = getCurrentOrganization();
        Subscriber subscriber = subscriberRepository.findByIdAndOrganization(id, organization)
                .orElseThrow(() -> new EntityNotFoundException("Subscriber not found with id: " + id + " in your organization."));

        String subscriberName = subscriber.getFirstName() + " " + subscriber.getLastName();
        String nfcCardUid = null;
        boolean hadAuthentication = false;

        logger.info("Deleting subscriber: {} {} (ID: {})", subscriber.getFirstName(), subscriber.getLastName(), id);

        // 1. Handle NFC card unassignment (keep the card but unassign it)
        NfcCard nfcCard = subscriber.getNfcCard();
        if (nfcCard != null) {
            nfcCardUid = nfcCard.getCardUid();
            logger.info("Unassigning NFC card: {} from subscriber: {}", nfcCardUid, subscriberName);
            // Unassign the card instead of deleting it
            nfcCard.setSubscriber(null);
            nfcCardRepository.save(nfcCard);
            // Remove the card reference from subscriber to prevent cascade deletion
            subscriber.setNfcCard(null);
        }

        // 2. Delete subscriber authentication records (this is what was missing!)
        Optional<SubscriberAuth> subscriberAuth = subscriberAuthRepository.findBySubscriber(subscriber);
        if (subscriberAuth.isPresent()) {
            hadAuthentication = true;
            logger.info("Deleting authentication record for subscriber: {}", subscriberName);
            subscriberAuthRepository.delete(subscriberAuth.get());
        }

        // 3. Delete the subscriber (cascade will handle AttendanceLogs)
        logger.info("Deleting subscriber record: {}", subscriberName);
        subscriberRepository.delete(subscriber);

        Map<String, Object> response = Map.of(
                "message", "Subscriber deleted successfully",
                "subscriberId", id,
                "subscriberName", subscriberName,
                "nfcCardUnassigned", nfcCardUid != null,
                "nfcCardUid", nfcCardUid != null ? nfcCardUid : "None",
                "authenticationRemoved", hadAuthentication
        );

        logger.info("Subscriber deletion completed successfully: {}", subscriberName);
        return ResponseEntity.ok(response);
    }


    // Attendance Session Mappings
    @PostMapping("/sessions")
    public ResponseEntity<?> createSession(@RequestBody AttendanceSessionDto sessionDto) {
        // Check permission
        ResponseEntity<?> permissionCheck = checkAttendancePermission();
        if (permissionCheck != null) return permissionCheck;

        Organization organization = getCurrentOrganization();
        AttendanceSession session = new AttendanceSession();
        session.setName(sessionDto.getName());
        session.setDescription(sessionDto.getDescription());

        // If startTime is provided, use it; otherwise use current time
        if (sessionDto.getStartTime() != null) {
            session.setStartTime(sessionDto.getStartTime());
        } else {
            session.setStartTime(LocalDateTime.now());
        }

        // Set allowed check-in methods (default to NFC if not provided)
        if (sessionDto.getAllowedCheckInMethods() != null && !sessionDto.getAllowedCheckInMethods().isEmpty()) {
            session.setAllowedCheckInMethods(sessionDto.getAllowedCheckInMethods());
        } else {
            session.setAllowedCheckInMethods(Set.of(CheckInMethod.NFC));
        }

        session.setOrganization(organization);
        // endTime is null initially

        // Save session first to get an ID
        AttendanceSession savedSession = attendanceSessionRepository.save(session);

        // Generate QR code if QR is an allowed method and set expiry to null (valid until session ends)
        if (savedSession.getAllowedCheckInMethods().contains(CheckInMethod.QR)) {
            String qrCode = qrCodeService.generateQrCodeForSession(savedSession);
            savedSession.setQrCode(qrCode);
            savedSession.setQrCodeExpiry(null); // QR code is valid until session ends
            savedSession = attendanceSessionRepository.save(savedSession); // Save again with QR code
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(convertToDto(savedSession));
    }

    @PutMapping("/sessions/{id}/end")
    public ResponseEntity<?> endSession(@PathVariable Long id) {
        // Check permission
        ResponseEntity<?> permissionCheck = checkAttendancePermission();
        if (permissionCheck != null) return permissionCheck;

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
    public ResponseEntity<?> getSessions() {
        // Check permission
        ResponseEntity<?> permissionCheck = checkAttendancePermission();
        if (permissionCheck != null) return permissionCheck;

        Organization organization = getCurrentOrganization();
        List<AttendanceSession> sessions = attendanceSessionRepository.findAllByOrganization(organization);
        List<AttendanceSessionDto> sessionDtos = sessions.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(sessionDtos);
    }

    @GetMapping("/sessions/{id}")
    public ResponseEntity<?> getSessionById(@PathVariable Long id) {
        try {
            // Check permission
            ResponseEntity<?> permissionCheck = checkAttendancePermission();
            if (permissionCheck != null) return permissionCheck;

            Organization organization = getCurrentOrganization();
            AttendanceSession session = attendanceSessionRepository.findByIdAndOrganization(id, organization)
                    .orElseThrow(() -> new EntityNotFoundException("Session not found with id: " + id + " in your organization."));

            return ResponseEntity.ok(convertToDto(session));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to fetch session details"));
        }
    }

    @DeleteMapping("/sessions/{id}")
    @Transactional
    public ResponseEntity<?> deleteSession(@PathVariable Long id) {
        // Check permission
        ResponseEntity<?> permissionCheck = checkAttendancePermission();
        if (permissionCheck != null) return permissionCheck;

        Organization organization = getCurrentOrganization();
        AttendanceSession session = attendanceSessionRepository.findByIdAndOrganization(id, organization)
                .orElseThrow(() -> new EntityNotFoundException("Attendance session not found with id: " + id + " in your organization."));

        // Delete the session (this will also delete associated attendance logs if cascade is configured)
        attendanceSessionRepository.delete(session);

        return ResponseEntity.ok(Map.of(
                "message", "Session deleted successfully",
                "sessionId", id,
                "sessionName", session.getName()
        ));
    }

    @GetMapping("/entity/info")
    public ResponseEntity<Map<String, Object>> getEntityInfo(Authentication authentication) {
        try {
            // Get the current entity admin's organization
            String username = authentication.getName();
            Optional<EntityAdmin> entityAdminOpt = entityAdminRepository.findByUsername(username);

            if (entityAdminOpt.isPresent()) {
                EntityAdmin entityAdmin = entityAdminOpt.get();
                Organization organization = entityAdmin.getOrganization();

                Map<String, Object> entityInfo = new HashMap<>();
                entityInfo.put("name", organization.getName());
                entityInfo.put("address", organization.getAddress());
                entityInfo.put("adminName", entityAdmin.getUsername());
                entityInfo.put("contactPerson", organization.getContactPerson());
                entityInfo.put("email", organization.getEmail());

                return ResponseEntity.ok(entityInfo);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Entity admin not found"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch entity info"));
        }
    }

    /**
     * Get recent attendance logs/NFC scans for real-time dashboard updates
     */
    @GetMapping("/attendance/recent")
    public ResponseEntity<?> getRecentAttendanceLogs(
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        try {
            // Check permission
            ResponseEntity<?> permissionCheck = checkAttendancePermission();
            if (permissionCheck != null) return permissionCheck;

            Organization organization = getCurrentOrganization();

            // Get recent attendance logs for this organization
            List<AttendanceLog> recentLogs = attendanceLogRepository
                .findTop10BySessionOrganizationOrderByCheckInTimeDesc(organization);

            List<Map<String, Object>> recentScans = recentLogs.stream()
                .limit(limit)
                .map(log -> {
                    Map<String, Object> scan = new HashMap<>();
                    scan.put("id", log.getId());
                    scan.put("subscriber", log.getSubscriber().getFirstName() + " " + log.getSubscriber().getLastName());
                    scan.put("session", log.getSession().getName());
                    scan.put("time", log.getCheckInTime());
                    scan.put("type", log.getCheckOutTime() != null ? "Check-out" : "Check-in");
                    scan.put("method", log.getCheckInMethod() != null ? log.getCheckInMethod().name() : "NFC");
                    scan.put("checkOutTime", log.getCheckOutTime());
                    return scan;
                })
                .collect(Collectors.toList());

            return ResponseEntity.ok(recentScans);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(List.of(Map.of("error", "Failed to fetch recent attendance logs")));
        }
    }

    /**
     * Get attendance logs for a specific session
     */
    @GetMapping("/sessions/{sessionId}/attendance")
    public ResponseEntity<?> getSessionAttendance(@PathVariable Long sessionId) {
        try {
            // Check permission
            ResponseEntity<?> permissionCheck = checkAttendancePermission();
            if (permissionCheck != null) return permissionCheck;

            Organization organization = getCurrentOrganization();

            // Verify session belongs to this organization
            AttendanceSession session = attendanceSessionRepository.findByIdAndOrganization(sessionId, organization)
                    .orElseThrow(() -> new EntityNotFoundException("Session not found"));

            List<AttendanceLog> attendanceLogs = attendanceLogRepository.findBySession(session);

            List<Map<String, Object>> attendees = attendanceLogs.stream()
                .map(log -> {
                    Map<String, Object> attendee = new HashMap<>();
                    attendee.put("id", log.getId());
                    attendee.put("subscriberId", log.getSubscriber().getId());
                    attendee.put("subscriberName", log.getSubscriber().getFirstName() + " " + log.getSubscriber().getLastName());
                    attendee.put("checkInTime", log.getCheckInTime());
                    attendee.put("checkOutTime", log.getCheckOutTime());
                    attendee.put("checkinMethod", log.getCheckInMethod() != null ? log.getCheckInMethod().toString() : null);
                    attendee.put("checkoutMethod", log.getCheckOutMethod() != null ? log.getCheckOutMethod().toString() : null);
                    attendee.put("status", log.getCheckOutTime() != null ? "checked_out" : "checked_in");

                    // Add subscriber details for better display
                    Map<String, Object> subscriber = new HashMap<>();
                    subscriber.put("id", log.getSubscriber().getId());
                    subscriber.put("firstName", log.getSubscriber().getFirstName());
                    subscriber.put("lastName", log.getSubscriber().getLastName());
                    attendee.put("subscriber", subscriber);

                    return attendee;
                })
                .collect(Collectors.toList());

            return ResponseEntity.ok(attendees);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(List.of(Map.of("error", "Failed to fetch session attendance")));
        }
    }

    /**
     * Manual check-out for entity admins
     */
    @PutMapping("/attendance/{attendanceId}/checkout")
    public ResponseEntity<?> manualCheckOut(@PathVariable Long attendanceId) {
        try {
            // Check permission
            ResponseEntity<?> permissionCheck = checkAttendancePermission();
            if (permissionCheck != null) return permissionCheck;

            logger.info("Manual check-out request for attendance ID: {}", attendanceId);

            Organization organization = getCurrentOrganization();

            Optional<AttendanceLog> attendanceOpt = attendanceLogRepository.findById(attendanceId);
            if (attendanceOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Attendance record not found"));
            }

            AttendanceLog attendance = attendanceOpt.get();

            // Verify the attendance belongs to this organization
            if (!attendance.getSession().getOrganization().getEntityId().equals(organization.getEntityId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Access denied to this attendance record"));
            }

            // Check if already checked out
            if (attendance.getCheckOutTime() != null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Subscriber is already checked out"));
            }

            // Perform check-out
            attendance.setCheckOutTime(LocalDateTime.now());
            attendance.setCheckOutMethod(CheckInMethod.MANUAL); // Add MANUAL to CheckInMethod enum
            attendanceLogRepository.save(attendance);

            logger.info("Manual check-out successful - Subscriber: {} {}, Session: {}, Attendance ID: {}",
                    attendance.getSubscriber().getFirstName(),
                    attendance.getSubscriber().getLastName(),
                    attendance.getSession().getName(),
                    attendanceId);

            return ResponseEntity.ok(Map.of(
                    "message", "Successfully checked out subscriber",
                    "subscriberName", attendance.getSubscriber().getFirstName() + " " + attendance.getSubscriber().getLastName(),
                    "sessionName", attendance.getSession().getName(),
                    "checkOutTime", attendance.getCheckOutTime(),
                    "checkInMethod", attendance.getCheckInMethod().toString(),
                    "checkOutMethod", "MANUAL",
                    "action", "MANUAL_CHECK_OUT"
            ));

        } catch (Exception e) {
            logger.error("Manual check-out failed for attendance ID {}: {}", attendanceId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to check out subscriber: " + e.getMessage()));
        }
    }

    // Scheduled Session Mappings
    @PostMapping("/scheduled-sessions")
    public ResponseEntity<?> createScheduledSession(@RequestBody ScheduledSessionDto sessionDto) {
        try {
            // Check permission
            ResponseEntity<?> permissionCheck = checkAttendancePermission();
            if (permissionCheck != null) return permissionCheck;

            Organization organization = getCurrentOrganization();
            ScheduledSessionDto created = scheduledSessionService.createScheduledSession(sessionDto, organization.getEntityId());
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/scheduled-sessions")
    public ResponseEntity<?> getScheduledSessions() {
        // Check permission
        ResponseEntity<?> permissionCheck = checkAttendancePermission();
        if (permissionCheck != null) return permissionCheck;

        Organization organization = getCurrentOrganization();
        List<ScheduledSessionDto> sessions = scheduledSessionService.getScheduledSessions(organization.getEntityId());
        return ResponseEntity.ok(sessions);
    }

    @GetMapping("/scheduled-sessions/active")
    public ResponseEntity<?> getActiveScheduledSessions() {
        // Check permission
        ResponseEntity<?> permissionCheck = checkAttendancePermission();
        if (permissionCheck != null) return permissionCheck;

        Organization organization = getCurrentOrganization();
        List<ScheduledSessionDto> sessions = scheduledSessionService.getActiveScheduledSessions(organization.getEntityId());
        return ResponseEntity.ok(sessions);
    }

    @GetMapping("/scheduled-sessions/{id}")
    public ResponseEntity<?> getScheduledSessionById(@PathVariable Long id) {
        try {
            // Check permission
            ResponseEntity<?> permissionCheck = checkAttendancePermission();
            if (permissionCheck != null) return permissionCheck;

            Organization organization = getCurrentOrganization();
            ScheduledSessionDto session = scheduledSessionService.getScheduledSessionById(id, organization.getEntityId());
            return ResponseEntity.ok(session);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/scheduled-sessions/{id}")
    public ResponseEntity<?> updateScheduledSession(@PathVariable Long id, @RequestBody ScheduledSessionDto sessionDto) {
        try {
            // Check permission
            ResponseEntity<?> permissionCheck = checkAttendancePermission();
            if (permissionCheck != null) return permissionCheck;

            Organization organization = getCurrentOrganization();
            ScheduledSessionDto updated = scheduledSessionService.updateScheduledSession(id, sessionDto, organization.getEntityId());
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/scheduled-sessions/{id}")
    public ResponseEntity<?> deleteScheduledSession(@PathVariable Long id) {
        try {
            // Check permission
            ResponseEntity<?> permissionCheck = checkAttendancePermission();
            if (permissionCheck != null) return permissionCheck;

            Organization organization = getCurrentOrganization();
            scheduledSessionService.deleteScheduledSession(id, organization.getEntityId());
            return ResponseEntity.ok(Map.of("message", "Scheduled session deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    // QR Code Mappings
    @GetMapping("/sessions/{sessionId}/qr-code")
    public ResponseEntity<?> getSessionQrCode(@PathVariable Long sessionId) {
        try {
            // Check permission
            ResponseEntity<?> permissionCheck = checkAttendancePermission();
            if (permissionCheck != null) return permissionCheck;

            Organization organization = getCurrentOrganization();
            AttendanceSession session = attendanceSessionRepository.findByIdAndOrganization(sessionId, organization)
                    .orElseThrow(() -> new EntityNotFoundException("Session not found"));

            QrCodeService.QrCodeDisplayData qrData = qrCodeService.generateQrCodeDisplayData(session);
            return ResponseEntity.ok(qrData);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/sessions/{sessionId}/refresh-qr")
    public ResponseEntity<?> refreshSessionQrCode(@PathVariable Long sessionId) {
        try {
            // Check permission
            ResponseEntity<?> permissionCheck = checkAttendancePermission();
            if (permissionCheck != null) return permissionCheck;

            Organization organization = getCurrentOrganization();
            AttendanceSession session = attendanceSessionRepository.findByIdAndOrganization(sessionId, organization)
                    .orElseThrow(() -> new EntityNotFoundException("Session not found"));

            String newQrCode = qrCodeService.refreshQrCodeForSession(session);
            session.setQrCode(newQrCode);
            session.setQrCodeExpiry(null); // QR valid until session ends
            attendanceSessionRepository.save(session);

            QrCodeService.QrCodeDisplayData qrData = qrCodeService.generateQrCodeDisplayData(session);
            return ResponseEntity.ok(qrData);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    // Change Password Endpoint
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> passwordData, Authentication authentication) {
        try {
            String currentPassword = passwordData.get("currentPassword");
            String newPassword = passwordData.get("newPassword");

            if (currentPassword == null || newPassword == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Current password and new password are required"));
            }

            if (newPassword.length() < 6) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "New password must be at least 6 characters long"));
            }

            String username = authentication.getName();
            EntityAdmin entityAdmin = entityAdminRepository.findByUsername(username)
                    .orElseThrow(() -> new EntityNotFoundException("Entity admin not found"));

            // Verify current password
            if (!passwordEncoder.matches(currentPassword, entityAdmin.getPassword())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Current password is incorrect"));
            }

            // Update password
            entityAdmin.setPassword(passwordEncoder.encode(newPassword));
            entityAdminRepository.save(entityAdmin);

            return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to change password"));
        }
    }

    // DTO Converters
    private SubscriberDto convertToDto(Subscriber subscriber) {
        String nfcUid = subscriber.getNfcCard() != null ? subscriber.getNfcCard().getCardUid() : null;
        boolean hasNfcCard = subscriber.getNfcCard() != null;

        SubscriberDto dto = new SubscriberDto(
                subscriber.getId(),
                subscriber.getFirstName(),
                subscriber.getLastName(),
                subscriber.getEmail(),
                subscriber.getMobileNumber(),
                subscriber.getOrganization().getId(),
                nfcUid
        );
        dto.setHasNfcCard(hasNfcCard);
        dto.setEntityId(subscriber.getOrganization().getEntityId());

        return dto;
    }

    private AttendanceSessionDto convertToDto(AttendanceSession session) {
        return new AttendanceSessionDto(
                session.getId(),
                session.getName(),
                session.getDescription(),
                session.getStartTime(),
                session.getEndTime(),
                session.getOrganization().getId(),
                session.getAllowedCheckInMethods()
        );
    }
}
