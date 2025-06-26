package com.example.attendancesystem.grpc.service;

import com.example.attendancesystem.grpc.attendance.*;
import com.example.attendancesystem.model.AttendanceLog;
import com.example.attendancesystem.model.AttendanceSession;
import com.example.attendancesystem.model.CheckInMethod;
import com.example.attendancesystem.model.Organization;
import com.example.attendancesystem.model.Subscriber;
import com.example.attendancesystem.repository.AttendanceLogRepository;
import com.example.attendancesystem.repository.AttendanceSessionRepository;
import com.example.attendancesystem.repository.OrganizationRepository;
import com.example.attendancesystem.repository.SubscriberRepository;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@GrpcService
public class AttendanceServiceImpl extends AttendanceServiceGrpc.AttendanceServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(AttendanceServiceImpl.class);

    @Autowired
    private AttendanceSessionRepository attendanceSessionRepository;

    @Autowired
    private AttendanceLogRepository attendanceLogRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private SubscriberRepository subscriberRepository;

    @Override
    public void createAttendanceSession(CreateAttendanceSessionRequest request, StreamObserver<AttendanceSessionResponse> responseObserver) {
        try {
            logger.info("Creating attendance session: {}", request.getName());

            // Check if organization exists
            Optional<Organization> organizationOpt = organizationRepository.findById(request.getOrganizationId());
            if (organizationOpt.isEmpty()) {
                AttendanceSessionResponse response = AttendanceSessionResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Organization not found")
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            AttendanceSession session = new AttendanceSession();
            session.setName(request.getName());
            session.setDescription(request.getDescription());
            session.setStartTime(LocalDateTime.parse(request.getStartTime()));
            session.setEndTime(LocalDateTime.parse(request.getEndTime()));
            session.setOrganization(organizationOpt.get());

            // Generate QR code for the session
            String qrCodeData = "session_" + System.currentTimeMillis();
            session.setQrCode(qrCodeData);
            session.setQrCodeExpiry(LocalDateTime.parse(request.getEndTime()));

            AttendanceSession savedSession = attendanceSessionRepository.save(session);

            com.example.attendancesystem.grpc.attendance.AttendanceSession grpcSession = convertToGrpcAttendanceSession(savedSession);

            AttendanceSessionResponse response = AttendanceSessionResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Attendance session created successfully")
                    .setSession(grpcSession)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
            logger.info("Attendance session created successfully with ID: {}", savedSession.getId());

        } catch (Exception e) {
            logger.error("Error creating attendance session: {}", request.getName(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to create attendance session: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void getAttendanceSession(GetAttendanceSessionRequest request, StreamObserver<AttendanceSessionResponse> responseObserver) {
        try {
            Optional<AttendanceSession> sessionOpt = attendanceSessionRepository.findById(request.getId());

            if (sessionOpt.isEmpty()) {
                AttendanceSessionResponse response = AttendanceSessionResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Attendance session not found")
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            com.example.attendancesystem.grpc.attendance.AttendanceSession grpcSession = convertToGrpcAttendanceSession(sessionOpt.get());

            AttendanceSessionResponse response = AttendanceSessionResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Attendance session retrieved successfully")
                    .setSession(grpcSession)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Error retrieving attendance session with ID: {}", request.getId(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to retrieve attendance session: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void updateAttendanceSession(UpdateAttendanceSessionRequest request, StreamObserver<AttendanceSessionResponse> responseObserver) {
        try {
            Optional<AttendanceSession> sessionOpt = attendanceSessionRepository.findById(request.getId());

            if (sessionOpt.isEmpty()) {
                AttendanceSessionResponse response = AttendanceSessionResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Attendance session not found")
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            AttendanceSession session = sessionOpt.get();
            session.setName(request.getName());
            session.setDescription(request.getDescription());
            session.setStartTime(LocalDateTime.parse(request.getStartTime()));
            session.setEndTime(LocalDateTime.parse(request.getEndTime()));

            AttendanceSession savedSession = attendanceSessionRepository.save(session);

            com.example.attendancesystem.grpc.attendance.AttendanceSession grpcSession = convertToGrpcAttendanceSession(savedSession);

            AttendanceSessionResponse response = AttendanceSessionResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Attendance session updated successfully")
                    .setSession(grpcSession)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Error updating attendance session with ID: {}", request.getId(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to update attendance session: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void deleteAttendanceSession(DeleteAttendanceSessionRequest request, StreamObserver<DeleteResponse> responseObserver) {
        try {
            Optional<AttendanceSession> sessionOpt = attendanceSessionRepository.findById(request.getId());

            if (sessionOpt.isEmpty()) {
                DeleteResponse response = DeleteResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Attendance session not found")
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            // Delete the session (this will cascade to related attendance logs)
            attendanceSessionRepository.deleteById(request.getId());

            DeleteResponse response = DeleteResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Attendance session deleted successfully")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Error deleting attendance session with ID: {}", request.getId(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to delete attendance session: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void listAttendanceSessions(ListAttendanceSessionsRequest request, StreamObserver<ListAttendanceSessionsResponse> responseObserver) {
        try {
            Optional<Organization> organizationOpt = organizationRepository.findById(request.getOrganizationId());
            if (organizationOpt.isEmpty()) {
                ListAttendanceSessionsResponse response = ListAttendanceSessionsResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Organization not found")
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            // Get sessions with pagination
            Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
            Page<AttendanceSession> sessionPage;

            if (request.getSearch().isEmpty()) {
                sessionPage = attendanceSessionRepository.findByOrganization(organizationOpt.get(), pageable);
            } else {
                // Search by session name
                sessionPage = attendanceSessionRepository.findByOrganizationAndNameContainingIgnoreCase(
                        organizationOpt.get(), request.getSearch(), pageable);
            }

            List<com.example.attendancesystem.grpc.attendance.AttendanceSession> grpcSessions = sessionPage.getContent()
                    .stream()
                    .map(this::convertToGrpcAttendanceSession)
                    .collect(Collectors.toList());

            ListAttendanceSessionsResponse response = ListAttendanceSessionsResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Attendance sessions retrieved successfully")
                    .addAllSessions(grpcSessions)
                    .setTotalCount(sessionPage.getTotalElements())
                    .setPage(request.getPage())
                    .setSize(request.getSize())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Error listing attendance sessions", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to list attendance sessions: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void checkIn(CheckInRequest request, StreamObserver<CheckInResponse> responseObserver) {
        try {
            // Validate subscriber
            Optional<Subscriber> subscriberOpt = subscriberRepository.findById(request.getSubscriberId());
            if (subscriberOpt.isEmpty()) {
                CheckInResponse response = CheckInResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Subscriber not found")
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            // Validate session
            Optional<AttendanceSession> sessionOpt = attendanceSessionRepository.findById(request.getSessionId());
            if (sessionOpt.isEmpty()) {
                CheckInResponse response = CheckInResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Attendance session not found")
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            // Check if already checked in
            AttendanceLog existingLog = attendanceLogRepository
                    .findBySubscriberAndSessionAndCheckOutTimeIsNull(subscriberOpt.get(), sessionOpt.get());
            if (existingLog != null) {
                CheckInResponse response = CheckInResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Subscriber is already checked in")
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            // Create attendance log
            AttendanceLog attendanceLog = new AttendanceLog();
            attendanceLog.setSubscriber(subscriberOpt.get());
            attendanceLog.setSession(sessionOpt.get());
            attendanceLog.setCheckInTime(LocalDateTime.now());
            attendanceLog.setCheckInMethod(CheckInMethod.valueOf(request.getMethod()));
            attendanceLog.setLocationInfo(request.getLocation());

            AttendanceLog savedLog = attendanceLogRepository.save(attendanceLog);

            com.example.attendancesystem.grpc.attendance.AttendanceLog grpcLog = convertToGrpcAttendanceLog(savedLog);

            CheckInResponse response = CheckInResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Check-in successful")
                    .setAttendanceLog(grpcLog)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
            logger.info("Check-in successful for subscriber ID: {} in session ID: {}", request.getSubscriberId(), request.getSessionId());

        } catch (Exception e) {
            logger.error("Error during check-in", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Check-in failed: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    private com.example.attendancesystem.grpc.attendance.AttendanceSession convertToGrpcAttendanceSession(AttendanceSession session) {
        // Count total attendees
        List<AttendanceLog> logs = attendanceLogRepository.findBySession(session);
        int totalAttendees = logs.size();

        return com.example.attendancesystem.grpc.attendance.AttendanceSession.newBuilder()
                .setId(session.getId())
                .setName(session.getName())
                .setDescription(session.getDescription() != null ? session.getDescription() : "")
                .setStartTime(session.getStartTime() != null ? session.getStartTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "")
                .setEndTime(session.getEndTime() != null ? session.getEndTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "")
                .setOrganizationId(session.getOrganization().getId())
                .setActive(session.isActive())
                .setCreatedAt("") // Not available in current model
                .setUpdatedAt("") // Not available in current model
                .setQrCodeData(session.getQrCode() != null ? session.getQrCode() : "")
                .setTotalAttendees(totalAttendees)
                .build();
    }

    private com.example.attendancesystem.grpc.attendance.AttendanceLog convertToGrpcAttendanceLog(AttendanceLog log) {
        return com.example.attendancesystem.grpc.attendance.AttendanceLog.newBuilder()
                .setId(log.getId())
                .setSubscriberId(log.getSubscriber().getId())
                .setSessionId(log.getSession().getId())
                .setCheckInTime(log.getCheckInTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .setCheckOutTime(log.getCheckOutTime() != null ? 
                        log.getCheckOutTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "")
                .setCheckInMethod(log.getCheckInMethod() != null ? log.getCheckInMethod().name() : "")
                .setCheckOutMethod(log.getCheckOutMethod() != null ? log.getCheckOutMethod().name() : "")
                .setLocation(log.getLocationInfo() != null ? log.getLocationInfo() : "")
                .setNotes("") // Not available in current model
                .setCreatedAt("") // Not available in current model
                .build();
    }
}
