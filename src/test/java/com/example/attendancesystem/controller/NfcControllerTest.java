package com.example.attendancesystem.controller;

import com.example.attendancesystem.config.TestSecurityConfig;
import com.example.attendancesystem.dto.NfcScanDto;
import com.example.attendancesystem.model.*;
import com.example.attendancesystem.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(TestSecurityConfig.class)
public class NfcControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private SubscriberRepository subscriberRepository;

    @Autowired
    private NfcCardRepository nfcCardRepository;

    @Autowired
    private AttendanceSessionRepository attendanceSessionRepository;

    @Autowired
    private AttendanceLogRepository attendanceLogRepository;

    private Organization org1;
    private Subscriber subscriberOrg1;
    private NfcCard nfcCardSubscriberOrg1;
    private AttendanceSession activeSessionOrg1;

    @BeforeEach
    void setUp() {
        // Using entities created by TestSecurityConfig or creating new ones
        org1 = organizationRepository.findByName("Test Org One (EntityController)")
                .orElseThrow(() -> new IllegalStateException("Test Org One not found. Check TestSecurityConfig."));

        // Create a subscriber with an NFC card for org1
        subscriberOrg1 = new Subscriber();
        subscriberOrg1.setFirstName("NFC");
        subscriberOrg1.setLastName("User");
        subscriberOrg1.setEmail("nfc.user@org1.com");
        subscriberOrg1.setOrganization(org1);

        nfcCardSubscriberOrg1 = new NfcCard();
        nfcCardSubscriberOrg1.setCardUid("VALIDNFC123");
        nfcCardSubscriberOrg1.setActive(true);
        nfcCardSubscriberOrg1.setSubscriber(subscriberOrg1);
        subscriberOrg1.setNfcCard(nfcCardSubscriberOrg1);

        subscriberRepository.save(subscriberOrg1); // This should save nfcCardSubscriberOrg1 due to cascade

        // Create an active session for org1
        activeSessionOrg1 = new AttendanceSession();
        activeSessionOrg1.setName("Live NFC Session");
        activeSessionOrg1.setOrganization(org1);
        activeSessionOrg1.setStartTime(LocalDateTime.now().minusHours(1)); // Started an hour ago
        activeSessionOrg1.setEndTime(null); // Active
        attendanceSessionRepository.save(activeSessionOrg1);
    }

    @Test
    @WithUserDetails("entityadmin1") // This user is associated with org1. Auth is needed to know the user context if controller was more restrictive.
                                 // Current NfcController uses isAuthenticated(), so any authenticated user can try.
                                 // The logic inside uses card UID to find subscriber and thus organization.
    void testNfcScan_CheckIn_Success() throws Exception {
        NfcScanDto scanDto = new NfcScanDto(nfcCardSubscriberOrg1.getCardUid());

        mockMvc.perform(post("/nfc/scan")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(scanDto)))
                .andExpect(status().isCreated())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Checked in successfully")));

        Optional<AttendanceLog> log = attendanceLogRepository.findBySubscriberAndSession(subscriberOrg1, activeSessionOrg1);
        assertThat(log).isPresent();
        assertThat(log.get().getCheckInTime()).isNotNull();
        assertThat(log.get().getCheckOutTime()).isNull();
    }

    @Test
    @WithUserDetails("entityadmin1")
    void testNfcScan_CheckOut_Success() throws Exception {
        // First, check-in the user
        AttendanceLog existingLog = new AttendanceLog();
        existingLog.setSubscriber(subscriberOrg1);
        existingLog.setSession(activeSessionOrg1);
        existingLog.setCheckInTime(LocalDateTime.now().minusMinutes(30)); // Checked in 30 mins ago
        attendanceLogRepository.save(existingLog);

        NfcScanDto scanDto = new NfcScanDto(nfcCardSubscriberOrg1.getCardUid());

        mockMvc.perform(post("/nfc/scan")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(scanDto)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Checked out successfully")));

        Optional<AttendanceLog> log = attendanceLogRepository.findBySubscriberAndSession(subscriberOrg1, activeSessionOrg1);
        assertThat(log).isPresent();
        assertThat(log.get().getCheckInTime()).isNotNull(); // Should be the original check-in time
        assertThat(log.get().getCheckOutTime()).isNotNull(); // Should now be set
    }

    @Test
    @WithUserDetails("entityadmin1")
    void testNfcScan_CardNotRegistered() throws Exception {
        NfcScanDto scanDto = new NfcScanDto("UNREGISTERED_CARD_UID");

        mockMvc.perform(post("/nfc/scan")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(scanDto)))
                .andExpect(status().isNotFound()) // From GlobalExceptionHandler for EntityNotFoundException
                .andExpect(jsonPath("$.message").value("NFC card not found with UID: UNREGISTERED_CARD_UID"));
    }

    @Test
    @WithUserDetails("entityadmin1")
    void testNfcScan_NoActiveSession() throws Exception {
        // End the active session
        activeSessionOrg1.setEndTime(LocalDateTime.now().minusMinutes(5)); // Ended 5 mins ago
        attendanceSessionRepository.save(activeSessionOrg1);

        NfcScanDto scanDto = new NfcScanDto(nfcCardSubscriberOrg1.getCardUid());

        mockMvc.perform(post("/nfc/scan")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(scanDto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No active attendance session found for organization: " + org1.getName()));
    }

    @Test
    @WithUserDetails("entityadmin1")
    void testNfcScan_CardInactive() throws Exception {
        nfcCardSubscriberOrg1.setActive(false);
        nfcCardRepository.save(nfcCardSubscriberOrg1);

        NfcScanDto scanDto = new NfcScanDto(nfcCardSubscriberOrg1.getCardUid());

        mockMvc.perform(post("/nfc/scan")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(scanDto)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("NFC card is inactive or not associated with a subscriber."));
    }
}
