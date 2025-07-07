package com.example.attendancesystem.attendance.grpc;

import com.example.attendancesystem.attendance.model.AttendanceLog;
import com.example.attendancesystem.attendance.model.AttendanceSession;
import com.example.attendancesystem.attendance.model.CheckInMethod;
import com.example.attendancesystem.attendance.repository.AttendanceLogRepository;
import com.example.attendancesystem.attendance.repository.AttendanceSessionRepository;
import com.example.attendancesystem.attendance.service.QrCodeService;
// Removed cross-service dependencies for microservices independence
// import com.example.attendancesystem.attendance.client.OrganizationServiceGrpcClient;
// import com.example.attendancesystem.attendance.client.UserServiceGrpcClient;
// import com.example.attendancesystem.attendance.dto.OrganizationDto;
// import com.example.attendancesystem.attendance.dto.UserDto;

import com.example.attendancesystem.grpc.attendance.*;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@GrpcService
public class AttendanceServiceImpl extends AttendanceServiceGrpc.AttendanceServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(AttendanceServiceImpl.class);

    @Autowired
    private AttendanceSessionRepository attendanceSessionRepository;

    @Autowired
    private AttendanceLogRepository attendanceLogRepository;

    @Autowired
    private QrCodeService qrCodeService;

    // Removed cross-service dependencies for microservices independence
    // @Autowired
    // private OrganizationServiceGrpcClient organizationServiceGrpcClient;

    // @Autowired
    // private UserServiceGrpcClient userServiceGrpcClient;

    @Override
    public void createAttendanceSession(CreateAttendanceSessionRequest request,
                                      StreamObserver<AttendanceSessionResponse> responseObserver) {
        try {
            logger.info("Creating attendance session: {}", request.getName());

            // For microservices independence, validate organization ID is provided
            if (request.getOrganizationId() <= 0) {
                AttendanceSessionResponse response = AttendanceSessionResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Invalid organization ID")
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            // Create new session
            AttendanceSession session = new AttendanceSession();
            session.setName(request.getName());
            session.setDescription(request.getDescription());
            session.setStartTime(LocalDateTime.parse(request.getStartTime()));
            session.setEndTime(LocalDateTime.parse(request.getEndTime()));
            session.setOrganizationId(request.getOrganizationId());

            // Generate QR code for the session
            String qrCodeData = "session_" + System.currentTimeMillis();
            session.setQrCode(qrCodeData);
            session.setQrCodeExpiry(LocalDateTime.parse(request.getEndTime()));

            AttendanceSession savedSession = attendanceSessionRepository.save(session);

            AttendanceSessionResponse response = AttendanceSessionResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Session created successfully - ID: " + savedSession.getId())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Error creating attendance session: {}", e.getMessage(), e);
            AttendanceSessionResponse response = AttendanceSessionResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Failed to create session: " + e.getMessage())
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void listAttendanceSessions(ListAttendanceSessionsRequest request,
                                     StreamObserver<ListAttendanceSessionsResponse> responseObserver) {
        try {
            logger.info("Listing attendance sessions for organization: {}", request.getOrganizationId());

            // For microservices independence, validate organization ID is provided
            if (request.getOrganizationId() <= 0) {
                ListAttendanceSessionsResponse response = ListAttendanceSessionsResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Invalid organization ID")
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            // Use findAll with filter for independence (simplified approach)
            List<AttendanceSession> allSessions = attendanceSessionRepository.findAll();
            List<AttendanceSession> sessions = allSessions.stream()
                    .filter(s -> s.getOrganizationId().equals(request.getOrganizationId()))
                    .toList();

            ListAttendanceSessionsResponse.Builder responseBuilder = ListAttendanceSessionsResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Sessions retrieved successfully - Count: " + sessions.size());

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Error listing attendance sessions: {}", e.getMessage(), e);
            ListAttendanceSessionsResponse response = ListAttendanceSessionsResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Failed to list sessions: " + e.getMessage())
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void checkIn(CheckInRequest request, StreamObserver<CheckInResponse> responseObserver) {
        try {
            logger.info("Processing check-in for subscriber: {} in session: {}", 
                       request.getSubscriberId(), request.getSessionId());

            // For microservices independence, validate subscriber ID is provided
            if (request.getSubscriberId() <= 0) {
                CheckInResponse response = CheckInResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Invalid subscriber ID")
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            // Get session
            Optional<AttendanceSession> sessionOpt = attendanceSessionRepository.findById(request.getSessionId());
            if (sessionOpt.isEmpty()) {
                CheckInResponse response = CheckInResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Session not found")
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            AttendanceSession session = sessionOpt.get();

            // Check if already checked in
            Optional<AttendanceLog> existingLog = attendanceLogRepository
                    .findByUserIdAndSessionId(request.getSubscriberId(), session.getId());

            if (existingLog.isPresent()) {
                CheckInResponse response = CheckInResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Already checked in")
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            // Create attendance log - simplified for microservices independence
            AttendanceLog log = new AttendanceLog(
                request.getSubscriberId(),
                "User-" + request.getSubscriberId(), // Simplified name for independence
                "N/A", // Mobile number not available for independence
                session,
                LocalDateTime.now(),
                CheckInMethod.valueOf(request.getMethod())
            );

            attendanceLogRepository.save(log);

            CheckInResponse response = CheckInResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Check-in successful")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Error processing check-in: {}", e.getMessage(), e);
            CheckInResponse response = CheckInResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Check-in failed: " + e.getMessage())
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    // Simplified stub implementations for other methods
    @Override
    public void checkOut(CheckOutRequest request, StreamObserver<CheckOutResponse> responseObserver) {
        CheckOutResponse response = CheckOutResponse.newBuilder()
                .setSuccess(false)
                .setMessage("Check-out temporarily simplified for microservices independence")
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getAttendanceLogs(GetAttendanceLogsRequest request, StreamObserver<ListAttendanceLogsResponse> responseObserver) {
        ListAttendanceLogsResponse response = ListAttendanceLogsResponse.newBuilder()
                .setSuccess(false)
                .setMessage("Attendance logs temporarily simplified for microservices independence")
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getActiveSession(GetActiveSessionRequest request, StreamObserver<AttendanceSessionResponse> responseObserver) {
        AttendanceSessionResponse response = AttendanceSessionResponse.newBuilder()
                .setSuccess(false)
                .setMessage("Active session lookup temporarily simplified for microservices independence")
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
