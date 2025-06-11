package com.example.attendancesystem.controller;

import com.example.attendancesystem.dto.CardAssignmentDto;
import com.example.attendancesystem.dto.CardRegistrationDto;
import com.example.attendancesystem.model.*;
import com.example.attendancesystem.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
public class CardManagementControllerTest {

    @Autowired
    private WebApplicationContext context;

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
    private RoleRepository roleRepository;

    private MockMvc mockMvc;
    private Organization testOrganization;
    private EntityAdmin testEntityAdmin;
    private Subscriber testSubscriber;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // Create test organization
        testOrganization = new Organization();
        testOrganization.setEntityId("MSD12345");
        testOrganization.setName("Test Organization");
        testOrganization.setAddress("Test Address");
        testOrganization = organizationRepository.save(testOrganization);

        // Create test entity admin
        Role entityAdminRole = roleRepository.findByName("ENTITY_ADMIN")
                .orElseThrow(() -> new RuntimeException("ENTITY_ADMIN role not found"));

        testEntityAdmin = new EntityAdmin();
        testEntityAdmin.setUsername("testadmin");
        testEntityAdmin.setPassword("$2a$10$test"); // Encoded password
        testEntityAdmin.setOrganization(testOrganization);
        testEntityAdmin.setRole(entityAdminRole);
        testEntityAdmin = entityAdminRepository.save(testEntityAdmin);

        // Create test subscriber
        testSubscriber = new Subscriber();
        testSubscriber.setFirstName("John");
        testSubscriber.setLastName("Doe");
        testSubscriber.setEmail("john.doe@test.com");
        testSubscriber.setMobileNumber("1234567890");
        testSubscriber.setOrganization(testOrganization);
        testSubscriber = subscriberRepository.save(testSubscriber);
    }

    @Test
    @WithUserDetails("testadmin")
    void testRegisterCard_Success() throws Exception {
        CardRegistrationDto registrationDto = new CardRegistrationDto();
        registrationDto.setCardUid("TEST_CARD_001");
        registrationDto.setActive(true);

        mockMvc.perform(post("/api/cards/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registrationDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Card registered successfully"))
                .andExpect(jsonPath("$.card.cardUid").value("TEST_CARD_001"))
                .andExpect(jsonPath("$.card.isActive").value(true))
                .andExpect(jsonPath("$.card.subscriberId").doesNotExist());
    }

    @Test
    @WithUserDetails("testadmin")
    void testRegisterCard_DuplicateUid() throws Exception {
        // First, register a card
        NfcCard existingCard = new NfcCard();
        existingCard.setCardUid("DUPLICATE_CARD");
        existingCard.setActive(true);
        nfcCardRepository.save(existingCard);

        CardRegistrationDto registrationDto = new CardRegistrationDto();
        registrationDto.setCardUid("DUPLICATE_CARD");
        registrationDto.setActive(true);

        mockMvc.perform(post("/api/cards/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registrationDto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Card UID already exists: DUPLICATE_CARD"));
    }

    @Test
    @WithUserDetails("testadmin")
    void testAssignCard_Success() throws Exception {
        // Create an unassigned card
        NfcCard unassignedCard = new NfcCard();
        unassignedCard.setCardUid("UNASSIGNED_CARD");
        unassignedCard.setActive(true);
        unassignedCard = nfcCardRepository.save(unassignedCard);

        CardAssignmentDto assignmentDto = new CardAssignmentDto();
        assignmentDto.setCardUid("UNASSIGNED_CARD");
        assignmentDto.setSubscriberId(testSubscriber.getId());

        mockMvc.perform(post("/api/cards/assign")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(assignmentDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Card assigned successfully"))
                .andExpect(jsonPath("$.card.cardUid").value("UNASSIGNED_CARD"))
                .andExpect(jsonPath("$.card.subscriberId").value(testSubscriber.getId()))
                .andExpect(jsonPath("$.card.subscriberName").value("John Doe"));
    }

    @Test
    @WithUserDetails("testadmin")
    void testAssignCard_CardNotFound() throws Exception {
        CardAssignmentDto assignmentDto = new CardAssignmentDto();
        assignmentDto.setCardUid("NON_EXISTENT_CARD");
        assignmentDto.setSubscriberId(testSubscriber.getId());

        mockMvc.perform(post("/api/cards/assign")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(assignmentDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Card not found with UID: NON_EXISTENT_CARD"));
    }

    @Test
    @WithUserDetails("testadmin")
    void testAssignCard_AlreadyAssigned() throws Exception {
        // Create a card already assigned to another subscriber
        Subscriber anotherSubscriber = new Subscriber();
        anotherSubscriber.setFirstName("Jane");
        anotherSubscriber.setLastName("Smith");
        anotherSubscriber.setMobileNumber("0987654321");
        anotherSubscriber.setOrganization(testOrganization);
        anotherSubscriber = subscriberRepository.save(anotherSubscriber);

        NfcCard assignedCard = new NfcCard();
        assignedCard.setCardUid("ASSIGNED_CARD");
        assignedCard.setActive(true);
        assignedCard.setSubscriber(anotherSubscriber);
        assignedCard = nfcCardRepository.save(assignedCard);

        CardAssignmentDto assignmentDto = new CardAssignmentDto();
        assignmentDto.setCardUid("ASSIGNED_CARD");
        assignmentDto.setSubscriberId(testSubscriber.getId());

        mockMvc.perform(post("/api/cards/assign")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(assignmentDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Card is already assigned to subscriber: Jane Smith"));
    }

    @Test
    @WithUserDetails("testadmin")
    void testUnassignCard_Success() throws Exception {
        // Create an assigned card
        NfcCard assignedCard = new NfcCard();
        assignedCard.setCardUid("ASSIGNED_CARD_TO_UNASSIGN");
        assignedCard.setActive(true);
        assignedCard.setSubscriber(testSubscriber);
        assignedCard = nfcCardRepository.save(assignedCard);

        mockMvc.perform(post("/api/cards/unassign/ASSIGNED_CARD_TO_UNASSIGN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Card unassigned successfully"))
                .andExpect(jsonPath("$.card.cardUid").value("ASSIGNED_CARD_TO_UNASSIGN"))
                .andExpect(jsonPath("$.card.subscriberId").doesNotExist());
    }

    @Test
    @WithUserDetails("testadmin")
    void testUnassignCard_NotAssigned() throws Exception {
        // Create an unassigned card
        NfcCard unassignedCard = new NfcCard();
        unassignedCard.setCardUid("UNASSIGNED_CARD_TO_UNASSIGN");
        unassignedCard.setActive(true);
        unassignedCard = nfcCardRepository.save(unassignedCard);

        mockMvc.perform(post("/api/cards/unassign/UNASSIGNED_CARD_TO_UNASSIGN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Card is not assigned to any subscriber"));
    }

    @Test
    @WithUserDetails("testadmin")
    void testGetAllCards() throws Exception {
        // Create test cards
        NfcCard assignedCard = new NfcCard();
        assignedCard.setCardUid("ASSIGNED_TEST_CARD");
        assignedCard.setActive(true);
        assignedCard.setSubscriber(testSubscriber);
        nfcCardRepository.save(assignedCard);

        NfcCard unassignedCard = new NfcCard();
        unassignedCard.setCardUid("UNASSIGNED_TEST_CARD");
        unassignedCard.setActive(true);
        nfcCardRepository.save(unassignedCard);

        mockMvc.perform(get("/api/cards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @WithUserDetails("testadmin")
    void testGetUnassignedCards() throws Exception {
        // Create test cards
        NfcCard assignedCard = new NfcCard();
        assignedCard.setCardUid("ASSIGNED_FILTER_CARD");
        assignedCard.setActive(true);
        assignedCard.setSubscriber(testSubscriber);
        nfcCardRepository.save(assignedCard);

        NfcCard unassignedCard = new NfcCard();
        unassignedCard.setCardUid("UNASSIGNED_FILTER_CARD");
        unassignedCard.setActive(true);
        nfcCardRepository.save(unassignedCard);

        mockMvc.perform(get("/api/cards/unassigned"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].cardUid").value("UNASSIGNED_FILTER_CARD"));
    }

    @Test
    @WithUserDetails("testadmin")
    void testGetCardByUid() throws Exception {
        NfcCard testCard = new NfcCard();
        testCard.setCardUid("GET_BY_UID_CARD");
        testCard.setActive(true);
        testCard.setSubscriber(testSubscriber);
        nfcCardRepository.save(testCard);

        mockMvc.perform(get("/api/cards/GET_BY_UID_CARD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardUid").value("GET_BY_UID_CARD"))
                .andExpect(jsonPath("$.subscriberName").value("John Doe"));
    }

    @Test
    @WithUserDetails("testadmin")
    void testDeleteCard_Success() throws Exception {
        NfcCard unassignedCard = new NfcCard();
        unassignedCard.setCardUid("DELETE_TEST_CARD");
        unassignedCard.setActive(true);
        nfcCardRepository.save(unassignedCard);

        mockMvc.perform(delete("/api/cards/DELETE_TEST_CARD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Card deleted successfully"));
    }

    @Test
    @WithUserDetails("testadmin")
    void testDeleteCard_AssignedCard() throws Exception {
        NfcCard assignedCard = new NfcCard();
        assignedCard.setCardUid("DELETE_ASSIGNED_CARD");
        assignedCard.setActive(true);
        assignedCard.setSubscriber(testSubscriber);
        nfcCardRepository.save(assignedCard);

        mockMvc.perform(delete("/api/cards/DELETE_ASSIGNED_CARD"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Cannot delete assigned card. Please unassign first."));
    }

    @Test
    @WithUserDetails("testadmin")
    void testGetCardStatistics() throws Exception {
        // Create test cards for statistics
        NfcCard assignedCard1 = new NfcCard();
        assignedCard1.setCardUid("STATS_ASSIGNED_1");
        assignedCard1.setActive(true);
        assignedCard1.setSubscriber(testSubscriber);
        nfcCardRepository.save(assignedCard1);

        NfcCard unassignedCard1 = new NfcCard();
        unassignedCard1.setCardUid("STATS_UNASSIGNED_1");
        unassignedCard1.setActive(true);
        nfcCardRepository.save(unassignedCard1);

        NfcCard unassignedCard2 = new NfcCard();
        unassignedCard2.setCardUid("STATS_UNASSIGNED_2");
        unassignedCard2.setActive(true);
        nfcCardRepository.save(unassignedCard2);

        mockMvc.perform(get("/api/cards/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.assigned").value(1))
                .andExpect(jsonPath("$.unassigned").value(2));
    }
}
