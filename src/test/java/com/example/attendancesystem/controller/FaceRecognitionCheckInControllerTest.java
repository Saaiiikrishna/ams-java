package com.example.attendancesystem.controller;

import com.example.attendancesystem.dto.FaceRecognitionDto;
import com.example.attendancesystem.facerecognition.FaceRecognitionResult;
import com.example.attendancesystem.model.*;
import com.example.attendancesystem.repository.*;
import com.example.attendancesystem.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(FaceRecognitionCheckInController.class)
class FaceRecognitionCheckInControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FaceRecognitionService faceRecognitionService;

    @MockBean
    private AttendanceSessionRepository attendanceSessionRepository;

    @MockBean
    private SubscriberRepository subscriberRepository;

    @MockBean
    private AttendanceLogRepository attendanceLogRepository;

    @MockBean
    private FileStorageService fileStorageService;

    @Autowired
    private ObjectMapper objectMapper;

    private AttendanceSession testSession;
    private Organization testOrganization;
    private Subscriber testSubscriber;
    private String testBase64Image;

    @BeforeEach
    void setUp() {
        // Create test organization
        testOrganization = new Organization();
        testOrganization.setEntityId("MSD00001");
        testOrganization.setName("Test Organization");

        // Create test session
        testSession = new AttendanceSession();
        testSession.setId(1L);
        testSession.setName("Test Session");
        testSession.setStartTime(LocalDateTime.now().minusHours(1));
        testSession.setEndTime(LocalDateTime.now().plusHours(1));
        testSession.setOrganization(testOrganization);

        // Create test subscriber
        testSubscriber = new Subscriber();
        testSubscriber.setId(1L);
        testSubscriber.setFirstName("John");
        testSubscriber.setLastName("Doe");
        testSubscriber.setMobileNumber("1234567890");
        testSubscriber.setOrganization(testOrganization);
        testSubscriber.setFaceEncoding(new byte[]{1, 2, 3, 4}); // Has face recognition

        // Create test Base64 image
        byte[] simpleImage = new byte[]{
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
            0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01,
            0x01, 0x01, 0x00, 0x48, 0x00, 0x48, 0x00, 0x00,
            (byte) 0xFF, (byte) 0xD9
        };
        testBase64Image = Base64.getEncoder().encodeToString(simpleImage);
    }

    @Test
    @WithMockUser(username = "admin@MSD00001")
    void testFaceRecognitionCheckIn_Success() throws Exception {
        // Arrange
        FaceRecognitionDto request = new FaceRecognitionDto();
        request.setSessionId(1L);
        request.setBase64Image(testBase64Image);
        request.setDeviceInfo("Test Device");

        FaceRecognitionResult recognitionResult = new FaceRecognitionResult(true, true, 0.95f, 0.05f, 150);
        recognitionResult.setMatchedSubscriberId(1L);
        recognitionResult.setMatchedSubscriberName("John Doe");

        when(attendanceSessionRepository.findByIdAndOrganizationEntityId(1L, "MSD00001"))
            .thenReturn(Optional.of(testSession));
        when(faceRecognitionService.recognizeFace(any(byte[].class), eq("MSD00001")))
            .thenReturn(recognitionResult);
        when(subscriberRepository.findById(1L))
            .thenReturn(Optional.of(testSubscriber));
        when(attendanceLogRepository.findBySubscriberAndSessionAndCheckOutTimeIsNull(testSubscriber, testSession))
            .thenReturn(null); // Not checked in yet
        when(fileStorageService.storeFaceRecognitionImage(any(byte[].class), eq(1L), anyString()))
            .thenReturn("audit_image_123.jpg");

        // Act & Assert
        mockMvc.perform(post("/api/checkin/face")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.action").value("CHECK_IN"))
                .andExpect(jsonPath("$.message").value("Check-in successful via face recognition"))
                .andExpect(jsonPath("$.subscriberId").value(1))
                .andExpect(jsonPath("$.subscriberName").value("John Doe"))
                .andExpect(jsonPath("$.sessionId").value(1))
                .andExpect(jsonPath("$.sessionName").value("Test Session"))
                .andExpect(jsonPath("$.confidenceScore").value(0.95))
                .andExpect(jsonPath("$.checkInTime").exists())
                .andExpect(jsonPath("$.auditImagePath").value("audit_image_123.jpg"));

        verify(faceRecognitionService).recognizeFace(any(byte[].class), eq("MSD00001"));
        verify(attendanceLogRepository).save(any(AttendanceLog.class));
        verify(faceRecognitionService).logRecognitionAttempt(eq(recognitionResult), eq(testSession), eq(testSubscriber), eq("Test Device"));
    }

    @Test
    @WithMockUser(username = "admin@MSD00001")
    void testFaceRecognitionCheckOut_Success() throws Exception {
        // Arrange
        FaceRecognitionDto request = new FaceRecognitionDto();
        request.setSessionId(1L);
        request.setBase64Image(testBase64Image);
        request.setDeviceInfo("Test Device");

        FaceRecognitionResult recognitionResult = new FaceRecognitionResult(true, true, 0.95f, 0.05f, 150);
        recognitionResult.setMatchedSubscriberId(1L);
        recognitionResult.setMatchedSubscriberName("John Doe");

        AttendanceLog existingLog = new AttendanceLog();
        existingLog.setSubscriber(testSubscriber);
        existingLog.setSession(testSession);
        existingLog.setCheckInTime(LocalDateTime.now().minusHours(1));
        existingLog.setCheckInMethod(CheckInMethod.FACE_RECOGNITION);

        when(attendanceSessionRepository.findByIdAndOrganizationEntityId(1L, "MSD00001"))
            .thenReturn(Optional.of(testSession));
        when(faceRecognitionService.recognizeFace(any(byte[].class), eq("MSD00001")))
            .thenReturn(recognitionResult);
        when(subscriberRepository.findById(1L))
            .thenReturn(Optional.of(testSubscriber));
        when(attendanceLogRepository.findBySubscriberAndSessionAndCheckOutTimeIsNull(testSubscriber, testSession))
            .thenReturn(existingLog); // Already checked in

        // Act & Assert
        mockMvc.perform(post("/api/checkin/face")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.action").value("CHECK_OUT"))
                .andExpect(jsonPath("$.message").value("Check-out successful via face recognition"))
                .andExpect(jsonPath("$.subscriberId").value(1))
                .andExpect(jsonPath("$.subscriberName").value("John Doe"))
                .andExpect(jsonPath("$.sessionId").value(1))
                .andExpect(jsonPath("$.sessionName").value("Test Session"))
                .andExpect(jsonPath("$.confidenceScore").value(0.95))
                .andExpect(jsonPath("$.checkInTime").exists())
                .andExpect(jsonPath("$.checkOutTime").exists())
                .andExpect(jsonPath("$.duration").exists());

        verify(faceRecognitionService).recognizeFace(any(byte[].class), eq("MSD00001"));
        verify(attendanceLogRepository).save(existingLog);
        verify(faceRecognitionService).logRecognitionAttempt(eq(recognitionResult), eq(testSession), eq(testSubscriber), eq("Test Device"));
    }

    @Test
    @WithMockUser(username = "admin@MSD00001")
    void testFaceRecognitionCheckIn_SessionNotFound() throws Exception {
        // Arrange
        FaceRecognitionDto request = new FaceRecognitionDto();
        request.setSessionId(999L);
        request.setBase64Image(testBase64Image);

        when(attendanceSessionRepository.findByIdAndOrganizationEntityId(999L, "MSD00001"))
            .thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(post("/api/checkin/face")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Session not found or access denied"));

        verify(faceRecognitionService, never()).recognizeFace(any(), any());
    }

    @Test
    @WithMockUser(username = "admin@MSD00001")
    void testFaceRecognitionCheckIn_SessionInactive() throws Exception {
        // Arrange
        testSession.setStartTime(LocalDateTime.now().plusHours(1)); // Future session
        testSession.setEndTime(LocalDateTime.now().plusHours(2));

        FaceRecognitionDto request = new FaceRecognitionDto();
        request.setSessionId(1L);
        request.setBase64Image(testBase64Image);

        when(attendanceSessionRepository.findByIdAndOrganizationEntityId(1L, "MSD00001"))
            .thenReturn(Optional.of(testSession));

        // Act & Assert
        mockMvc.perform(post("/api/checkin/face")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Session is not currently active"))
                .andExpect(jsonPath("$.sessionStatus").value("NOT_STARTED"));

        verify(faceRecognitionService, never()).recognizeFace(any(), any());
    }

    @Test
    @WithMockUser(username = "admin@MSD00001")
    void testFaceRecognitionCheckIn_FaceNotRecognized() throws Exception {
        // Arrange
        FaceRecognitionDto request = new FaceRecognitionDto();
        request.setSessionId(1L);
        request.setBase64Image(testBase64Image);

        FaceRecognitionResult recognitionResult = new FaceRecognitionResult(true, false, 0.3f, 0.7f, 150);

        when(attendanceSessionRepository.findByIdAndOrganizationEntityId(1L, "MSD00001"))
            .thenReturn(Optional.of(testSession));
        when(faceRecognitionService.recognizeFace(any(byte[].class), eq("MSD00001")))
            .thenReturn(recognitionResult);

        // Act & Assert
        mockMvc.perform(post("/api/checkin/face")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Face not recognized. Please ensure you are registered for face recognition."))
                .andExpect(jsonPath("$.confidenceScore").value(0.3));

        verify(faceRecognitionService).recognizeFace(any(byte[].class), eq("MSD00001"));
        verify(faceRecognitionService).logRecognitionAttempt(eq(recognitionResult), eq(testSession), eq(null), any());
        verify(attendanceLogRepository, never()).save(any());
    }

    @Test
    @WithMockUser(username = "admin@MSD00001")
    void testFaceRecognitionCheckIn_RecognitionFailed() throws Exception {
        // Arrange
        FaceRecognitionDto request = new FaceRecognitionDto();
        request.setSessionId(1L);
        request.setBase64Image(testBase64Image);

        FaceRecognitionResult recognitionResult = new FaceRecognitionResult(false, "No face detected in image");

        when(attendanceSessionRepository.findByIdAndOrganizationEntityId(1L, "MSD00001"))
            .thenReturn(Optional.of(testSession));
        when(faceRecognitionService.recognizeFace(any(byte[].class), eq("MSD00001")))
            .thenReturn(recognitionResult);

        // Act & Assert
        mockMvc.perform(post("/api/checkin/face")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Face recognition failed: No face detected in image"));

        verify(faceRecognitionService).recognizeFace(any(byte[].class), eq("MSD00001"));
        verify(faceRecognitionService).logRecognitionAttempt(eq(recognitionResult), eq(testSession), eq(null), any());
        verify(attendanceLogRepository, never()).save(any());
    }

    @Test
    @WithMockUser(username = "admin@MSD00001")
    void testFaceRecognitionCheckIn_InvalidImage() throws Exception {
        // Arrange
        FaceRecognitionDto request = new FaceRecognitionDto();
        request.setSessionId(1L);
        request.setBase64Image("invalid_base64_data");

        when(attendanceSessionRepository.findByIdAndOrganizationEntityId(1L, "MSD00001"))
            .thenReturn(Optional.of(testSession));

        // Act & Assert
        mockMvc.perform(post("/api/checkin/face")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid image format"));

        verify(faceRecognitionService, never()).recognizeFace(any(), any());
    }

    @Test
    @WithMockUser(username = "admin@MSD00001")
    void testGetFaceRecognitionStats_Success() throws Exception {
        // Arrange
        when(attendanceSessionRepository.findByIdAndOrganizationEntityId(1L, "MSD00001"))
            .thenReturn(Optional.of(testSession));
        when(attendanceLogRepository.findBySessionAndCheckInMethod(testSession, CheckInMethod.FACE_RECOGNITION))
            .thenReturn(java.util.Arrays.asList(new AttendanceLog(), new AttendanceLog()));
        when(attendanceLogRepository.findBySessionAndCheckOutMethod(testSession, CheckInMethod.FACE_RECOGNITION))
            .thenReturn(java.util.Arrays.asList(new AttendanceLog()));
        when(subscriberRepository.countByOrganizationEntityIdAndFaceEncodingIsNotNull("MSD00001"))
            .thenReturn(5L);
        when(faceRecognitionService.isFaceRecognitionAvailable())
            .thenReturn(true);

        // Act & Assert
        mockMvc.perform(get("/api/checkin/face/stats/1")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.sessionId").value(1))
                .andExpect(jsonPath("$.sessionName").value("Test Session"))
                .andExpect(jsonPath("$.faceCheckIns").value(2))
                .andExpect(jsonPath("$.faceCheckOuts").value(1))
                .andExpect(jsonPath("$.totalRegisteredFaces").value(5))
                .andExpect(jsonPath("$.faceRecognitionEnabled").value(true));
    }
}
