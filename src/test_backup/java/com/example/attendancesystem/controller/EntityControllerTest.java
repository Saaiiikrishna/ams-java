package com.example.attendancesystem.controller;

import com.example.attendancesystem.config.TestSecurityConfig;
import com.example.attendancesystem.dto.SubscriberDto;
import com.example.attendancesystem.dto.AttendanceSessionDto;
import com.example.attendancesystem.model.Organization;
import com.example.attendancesystem.model.Subscriber;
import com.example.attendancesystem.model.NfcCard;
import com.example.attendancesystem.model.AttendanceSession;
import com.example.attendancesystem.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(TestSecurityConfig.class) // Import the test user details service
public class EntityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private EntityAdminRepository entityAdminRepository;

    @Autowired
    private SubscriberRepository subscriberRepository;

    @Autowired
    private NfcCardRepository nfcCardRepository;

    @Autowired
    private AttendanceSessionRepository attendanceSessionRepository;

    private Organization org1;
    private Organization org2;

    @BeforeEach
    void setUp() {
        // Repositories are generally cleaned by @Transactional,
        // but explicit deleteAll can be safer depending on test interaction and data setup.
        // TestSecurityConfig already creates some orgs and admins.
        // We fetch them here or ensure they are available.
        org1 = organizationRepository.findByName("Test Org One (EntityController)").orElseThrow();
        org2 = organizationRepository.findByName("Test Org Two (EntityController)").orElseThrow();
    }

    // --- Subscriber Tests ---

    @Test
    @WithUserDetails("entityadmin1") // Belongs to org1
    void testAddSubscriber_Success() throws Exception {
        SubscriberDto subscriberDto = new SubscriberDto("New", "Sub", "new.sub@org1.com", "NFC123ORG1");

        mockMvc.perform(post("/entity/subscribers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(subscriberDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.firstName").value("New"))
                .andExpect(jsonPath("$.email").value("new.sub@org1.com"))
                .andExpect(jsonPath("$.organizationId").value(org1.getId()))
                .andExpect(jsonPath("$.nfcCardUid").value("NFC123ORG1"));

        Optional<Subscriber> savedSub = subscriberRepository.findByEmailAndOrganization("new.sub@org1.com", org1);
        assertThat(savedSub).isPresent();
        assertThat(savedSub.get().getNfcCard()).isNotNull();
        assertThat(savedSub.get().getNfcCard().getCardUid()).isEqualTo("NFC123ORG1");
    }

    @Test
    @WithUserDetails("entityadmin1") // Belongs to org1
    void testAddSubscriber_EmailConflictInSameOrg() throws Exception {
        Subscriber existingSub = new Subscriber();
        existingSub.setEmail("existing.sub@org1.com");
        existingSub.setFirstName("Old");
        existingSub.setLastName("Entry");
        existingSub.setOrganization(org1);
        subscriberRepository.save(existingSub);

        SubscriberDto subscriberDto = new SubscriberDto("New", "Sub", "existing.sub@org1.com", "NFCCONFLICT");

        mockMvc.perform(post("/entity/subscribers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(subscriberDto)))
                .andExpect(status().isConflict())
                .andExpect(content().string("Subscriber email already exists in this organization."));
    }


    @Test
    @WithUserDetails("entityadmin1") // Belongs to org1
    void testAddSubscriber_NfcCardUidConflict() throws Exception {
        // Create a card that already exists, possibly with another subscriber in another org
        Organization otherOrg = organizationRepository.findByName("Test Org Two (EntityController)").orElseGet(() -> {
            Organization o = new Organization();
            o.setName("Temporary Other Org");
            o.setAddress("Other St");
            return organizationRepository.save(o);
        });
        Subscriber otherSub = new Subscriber();
        otherSub.setEmail("other.sub@other.com");
        otherSub.setOrganization(otherOrg); // Different org

        NfcCard existingNfc = new NfcCard();
        existingNfc.setCardUid("SHAREDNFCID");
        existingNfc.setActive(true);
       // existingNfc.setSubscriber(otherSub); // Card might not be assigned yet, or assigned to someone else.
        nfcCardRepository.save(existingNfc); // Save a card with this UID

        SubscriberDto subscriberDto = new SubscriberDto("Conflict", "NFC", "conflict.nfc@org1.com", "SHAREDNFCID");

        mockMvc.perform(post("/entity/subscribers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(subscriberDto)))
                .andExpect(status().isConflict())
                .andExpect(content().string("NFC Card UID already in use."));
    }


    @Test
    @WithUserDetails("entityadmin1") // Belongs to org1
    void testGetSubscribers_Success_Org1() throws Exception {
        // Add some subscribers to org1
        Subscriber sub1Org1 = new Subscriber();
        sub1Org1.setEmail("s1@org1.com");
        sub1Org1.setOrganization(org1);
        subscriberRepository.save(sub1Org1);

        Subscriber sub2Org1 = new Subscriber();
        sub2Org1.setEmail("s2@org1.com");
        sub2Org1.setOrganization(org1);
        subscriberRepository.save(sub2Org1);

        // Add a subscriber to org2 (should not be visible)
        Subscriber sub1Org2 = new Subscriber();
        sub1Org2.setEmail("s1@org2.com");
        sub1Org2.setOrganization(org2);
        subscriberRepository.save(sub1Org2);

        mockMvc.perform(get("/entity/subscribers")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2))) // Only sees subscribers from org1
                .andExpect(jsonPath("$[0].organizationId").value(org1.getId()))
                .andExpect(jsonPath("$[1].organizationId").value(org1.getId()));
    }

    @Test
    @WithUserDetails("entityadmin2") // Belongs to org2
    void testGetSubscribers_Success_Org2_SeesOnlyOwn() throws Exception {
        // Add a subscriber to org2
        Subscriber sub1Org2 = new Subscriber();
        sub1Org2.setEmail("s1@org2.com");
        sub1Org2.setOrganization(org2);
        subscriberRepository.save(sub1Org2);

        mockMvc.perform(get("/entity/subscribers")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].organizationId").value(org2.getId()));
    }


    @Test
    @WithMockUser(roles = {"SUPER_ADMIN"}) // A super admin should not access entity specific endpoints
    void testGetSubscribers_Forbidden_ForSuperAdmin() throws Exception {
         mockMvc.perform(get("/entity/subscribers")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    // --- Attendance Session Tests ---
    @Test
    @WithUserDetails("entityadmin1") // Belongs to org1
    void testCreateSession_Success() throws Exception {
        AttendanceSessionDto sessionDto = new AttendanceSessionDto("Morning Session", LocalDateTime.now().plusHours(1));

        mockMvc.perform(post("/entity/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sessionDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Morning Session"))
                .andExpect(jsonPath("$.organizationId").value(org1.getId()));

        assertThat(attendanceSessionRepository.findAllByOrganization(org1)).hasSize(1);
    }

    @Test
    @WithUserDetails("entityadmin1") // Belongs to org1
    void testEndSession_Success() throws Exception {
        AttendanceSession session = new AttendanceSession();
        session.setName("Active Session");
        session.setOrganization(org1);
        session.setStartTime(LocalDateTime.now().minusHours(1));
        AttendanceSession savedSession = attendanceSessionRepository.save(session);

        mockMvc.perform(put("/entity/sessions/" + savedSession.getId() + "/end")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedSession.getId()))
                .andExpect(jsonPath("$.endTime").exists());

        Optional<AttendanceSession> endedSession = attendanceSessionRepository.findById(savedSession.getId());
        assertThat(endedSession).isPresent();
        assertThat(endedSession.get().getEndTime()).isNotNull();
    }

    @Test
    @WithUserDetails("entityadmin2") // Belongs to org2
    void testEndSession_Forbidden_WrongOrganization() throws Exception {
        // Session belongs to org1
        AttendanceSession sessionOrg1 = new AttendanceSession();
        sessionOrg1.setName("Org1 Session");
        sessionOrg1.setOrganization(org1);
        sessionOrg1.setStartTime(LocalDateTime.now().minusHours(1));
        AttendanceSession savedSessionOrg1 = attendanceSessionRepository.save(sessionOrg1);

        // entityadmin2 tries to end it
        mockMvc.perform(put("/entity/sessions/" + savedSessionOrg1.getId() + "/end")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound()); // GlobalExceptionHandler turns EntityNotFoundException to 404
                                                 // This is correct as admin from org2 cannot "find" session from org1
    }
     @Test
    @WithUserDetails("entityadmin1")
    void testUpdateSubscriber_Success() throws Exception {
        Subscriber subscriber = new Subscriber("Old", "Name", "old.name@org1.com", org1, null);
        subscriberRepository.save(subscriber);

        SubscriberDto updatedDto = new SubscriberDto(subscriber.getId(), "New", "Name", "new.name@org1.com", org1.getId(), "NFCNEW123");

        mockMvc.perform(put("/entity/subscribers/" + subscriber.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("New"))
                .andExpect(jsonPath("$.email").value("new.name@org1.com"))
                .andExpect(jsonPath("$.nfcCardUid").value("NFCNEW123"));

        Subscriber updatedSub = subscriberRepository.findById(subscriber.getId()).get();
        assertThat(updatedSub.getFirstName()).isEqualTo("New");
        assertThat(updatedSub.getNfcCard()).isNotNull();
        assertThat(updatedSub.getNfcCard().getCardUid()).isEqualTo("NFCNEW123");
    }

    @Test
    @WithUserDetails("entityadmin1")
    void testDeleteSubscriber_Success() throws Exception {
        Subscriber subscriber = new Subscriber("To", "Delete", "delete.me@org1.com", org1, null);
        NfcCard card = new NfcCard("DELNFC1", subscriber, true);
        subscriber.setNfcCard(card); // Assuming bidirectional or manual set
        // nfcCardRepository.save(card); // Not needed if cascade is from Subscriber
        subscriberRepository.save(subscriber); // This should save card too due to CascadeType.ALL on Subscriber.nfcCard

        Long subId = subscriber.getId();
        Long cardId = card.getId();

        mockMvc.perform(delete("/entity/subscribers/" + subId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("Subscriber with id " + subId + " deleted successfully."));

        assertThat(subscriberRepository.findById(subId)).isEmpty();
        assertThat(nfcCardRepository.findById(cardId)).isEmpty(); // Check cascade delete
    }

    // --- Tests for GET /entity/sessions ---

    @Test
    @WithUserDetails("entityadmin1") // Belongs to org1
    void testGetSessions_Success_Org1() throws Exception {
        // org1 is set up in @BeforeEach and by TestSecurityConfig
        // Create some sessions for org1
        attendanceSessionRepository.save(new AttendanceSession("Session A", LocalDateTime.now(), null, org1, null));
        attendanceSessionRepository.save(new AttendanceSession("Session B", LocalDateTime.now().minusDays(1), LocalDateTime.now(), org1, null));

        // Create a session for org2 (should not be visible)
        Organization org2 = organizationRepository.findByName("Test Org Two (EntityController)").orElseThrow();
        attendanceSessionRepository.save(new AttendanceSession("Session C", LocalDateTime.now(), null, org2, null));

        mockMvc.perform(get("/entity/sessions")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2))) // Only sees sessions from org1
                .andExpect(jsonPath("$[0].organizationId").value(org1.getId()))
                .andExpect(jsonPath("$[1].organizationId").value(org1.getId()));
    }

    @Test
    @WithUserDetails("entityadmin1") // Belongs to org1
    void testGetSessions_Success_EmptyList() throws Exception {
        // Ensure no sessions for org1 (default state after setup might have some if not cleaned well,
        // but @Transactional should handle it. Let's explicitly ensure org1 has no sessions for this test)
        attendanceSessionRepository.deleteAll(attendanceSessionRepository.findAllByOrganization(org1));

        mockMvc.perform(get("/entity/sessions")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @WithUserDetails("entityadmin2") // Belongs to org2
    void testGetSessions_Success_Org2_SeesOnlyOwn() throws Exception {
         // Create a session for org2
        Organization org2 = organizationRepository.findByName("Test Org Two (EntityController)").orElseThrow();
        attendanceSessionRepository.save(new AttendanceSession("Session D Org2", LocalDateTime.now(), null, org2, null));

        // Ensure org1 also has sessions that should not be seen
        attendanceSessionRepository.save(new AttendanceSession("Session E Org1", LocalDateTime.now(), null, org1, null));

        mockMvc.perform(get("/entity/sessions")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1))) // Only sees its own session
                .andExpect(jsonPath("$[0].organizationId").value(org2.getId()))
                .andExpect(jsonPath("$[0].name").value("Session D Org2"));
    }

    @Test
    @WithMockUser(roles = {"SUPER_ADMIN"}) // SUPER_ADMIN should not access entity-specific session list
    void testGetSessions_Forbidden_ForSuperAdmin() throws Exception {
        mockMvc.perform(get("/entity/sessions")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGetSessions_Unauthorized_NoAuth() throws Exception { // No @WithUserDetails or @WithMockUser
        mockMvc.perform(get("/entity/sessions")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}
