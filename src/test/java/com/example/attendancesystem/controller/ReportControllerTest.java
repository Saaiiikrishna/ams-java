package com.example.attendancesystem.controller;

import com.example.attendancesystem.config.TestSecurityConfig;
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
import java.util.Arrays;
import java.util.HashSet;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(TestSecurityConfig.class)
public class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private SubscriberRepository subscriberRepository;

    @Autowired
    private AttendanceSessionRepository attendanceSessionRepository;

    @Autowired
    private AttendanceLogRepository attendanceLogRepository;

    @Autowired
    private EntityAdminRepository entityAdminRepository; // Used by TestSecurityConfig

    private Organization org1, org2;
    private Subscriber sub1Org1, sub2Org1, sub3Org1, sub1Org2;
    private AttendanceSession session1Org1, session1Org2;

    @BeforeEach
    void setUp() {
        org1 = organizationRepository.findByName("Test Org One (EntityController)").orElseThrow();
        org2 = organizationRepository.findByName("Test Org Two (EntityController)").orElseThrow();

        // Subscribers for Org1
        sub1Org1 = subscriberRepository.save(new Subscriber("presentsub1", "lastname", "present1@org1.com", org1, null));
        sub2Org1 = subscriberRepository.save(new Subscriber("absentsub2", "lastname", "absent2@org1.com", org1, null));
        sub3Org1 = subscriberRepository.save(new Subscriber("presentsub3", "lastname", "present3@org1.com", org1, null));

        // Subscriber for Org2
        sub1Org2 = subscriberRepository.save(new Subscriber("sub1", "org2", "sub1@org2.com", org2, null));

        // Session for Org1
        session1Org1 = new AttendanceSession("Session One Org1", LocalDateTime.now().minusHours(2), null, org1, new HashSet<>());
        attendanceSessionRepository.save(session1Org1);

        // Session for Org2
        session1Org2 = new AttendanceSession("Session One Org2", LocalDateTime.now().minusHours(2), null, org2, new HashSet<>());
        attendanceSessionRepository.save(session1Org2);

        // Attendance logs for Org1, Session1
        // sub1Org1 attended
        attendanceLogRepository.save(new AttendanceLog(sub1Org1, session1Org1, LocalDateTime.now().minusHours(1), null));
        // sub3Org1 attended
        attendanceLogRepository.save(new AttendanceLog(sub3Org1, session1Org1, LocalDateTime.now().minusMinutes(30), LocalDateTime.now().minusMinutes(5)));

        // Attendance log for Org2, Session1 (for testing cross-org access)
        attendanceLogRepository.save(new AttendanceLog(sub1Org2, session1Org2, LocalDateTime.now().minusHours(1), null));
    }

    @Test
    @WithUserDetails("entityadmin1") // Belongs to org1
    void testGetAbsenteesForSession_Success() throws Exception {
        mockMvc.perform(get("/reports/sessions/" + session1Org1.getId() + "/absentees")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1))) // Only sub2Org1 should be absent
                .andExpect(jsonPath("$[0].email").value(sub2Org1.getEmail()));
    }

    @Test
    @WithUserDetails("entityadmin2") // Belongs to org2
    void testGetAbsenteesForSession_Forbidden_SessionNotInAdminsOrg() throws Exception {
        // entityadmin2 (org2) tries to access report for session1Org1 (org1)
        mockMvc.perform(get("/reports/sessions/" + session1Org1.getId() + "/absentees")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound()); // GlobalExceptionHandler turns EntityNotFoundException to 404
    }

    @Test
    @WithUserDetails("entityadmin1") // Belongs to org1
    void testGetAttendanceForSubscriber_Success() throws Exception {
        // Add another log for sub1Org1 for a different session (or same session if multiple check-ins allowed by logic)
         AttendanceSession session2Org1 = new AttendanceSession("Session Two Org1", LocalDateTime.now().minusDays(1), null, org1, new HashSet<>());
        attendanceSessionRepository.save(session2Org1);
        attendanceLogRepository.save(new AttendanceLog(sub1Org1, session2Org1, LocalDateTime.now().minusDays(1).plusHours(1), null));


        mockMvc.perform(get("/reports/subscribers/" + sub1Org1.getId() + "/attendance")
                .param("startDate", LocalDateTime.now().minusDays(2).toLocalDate().toString())
                .param("endDate", LocalDateTime.now().toLocalDate().toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2))) // Two logs for sub1Org1
                .andExpect(jsonPath("$[0].subscriberId").value(sub1Org1.getId()))
                .andExpect(jsonPath("$[1].subscriberId").value(sub1Org1.getId()));
    }

    @Test
    @WithUserDetails("entityadmin1") // Belongs to org1
    void testGetAttendanceForSubscriber_NoLogsInDateRange() throws Exception {
        mockMvc.perform(get("/reports/subscribers/" + sub1Org1.getId() + "/attendance")
                .param("startDate", LocalDateTime.now().minusDays(5).toLocalDate().toString())
                .param("endDate", LocalDateTime.now().minusDays(4).toLocalDate().toString()) // Range where no logs exist
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @WithUserDetails("entityadmin2") // Belongs to org2
    void testGetAttendanceForSubscriber_Forbidden_SubscriberNotInAdminsOrg() throws Exception {
        // entityadmin2 (org2) tries to access report for sub1Org1 (org1)
        mockMvc.perform(get("/reports/subscribers/" + sub1Org1.getId() + "/attendance")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound()); // GlobalExceptionHandler turns EntityNotFoundException to 404
    }
}
