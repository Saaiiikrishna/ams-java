package com.example.attendancesystem.grpc.service;

import com.example.attendancesystem.grpc.subscriber.*;
import com.example.attendancesystem.model.NfcCard;
import com.example.attendancesystem.model.Organization;
import com.example.attendancesystem.model.Subscriber;
import com.example.attendancesystem.model.SubscriberAuth;
import com.example.attendancesystem.repository.NfcCardRepository;
import com.example.attendancesystem.repository.OrganizationRepository;
import com.example.attendancesystem.repository.SubscriberAuthRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@GrpcService
public class SubscriberServiceImpl extends SubscriberServiceGrpc.SubscriberServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(SubscriberServiceImpl.class);

    @Autowired
    private SubscriberRepository subscriberRepository;

    @Autowired
    private SubscriberAuthRepository subscriberAuthRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private NfcCardRepository nfcCardRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void createSubscriber(CreateSubscriberRequest request, StreamObserver<SubscriberResponse> responseObserver) {
        try {
            logger.info("Creating subscriber: {}", request.getUsername());

            // Check if organization exists
            Optional<Organization> organizationOpt = organizationRepository.findById(request.getOrganizationId());
            if (organizationOpt.isEmpty()) {
                SubscriberResponse response = SubscriberResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Organization not found")
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            // Check if mobile number already exists in this organization
            boolean exists = subscriberRepository.existsByMobileNumberAndOrganization(
                    request.getUsername(), organizationOpt.get());
            if (exists) {
                SubscriberResponse response = SubscriberResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Mobile number already exists in this organization")
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            // Create subscriber
            Subscriber subscriber = new Subscriber();
            subscriber.setMobileNumber(request.getUsername());
            subscriber.setEmail(request.getEmail());
            subscriber.setFirstName(request.getFirstName());
            subscriber.setLastName(request.getLastName());
            subscriber.setOrganization(organizationOpt.get());

            Subscriber savedSubscriber = subscriberRepository.save(subscriber);

            // Create subscriber auth
            SubscriberAuth subscriberAuth = new SubscriberAuth();
            subscriberAuth.setSubscriber(savedSubscriber);
            subscriberAuth.setPin(passwordEncoder.encode(request.getPassword()));
            subscriberAuth.setIsActive(true);
            subscriberAuth.setCreatedAt(LocalDateTime.now());
            subscriberAuth.setUpdatedAt(LocalDateTime.now());
            subscriberAuthRepository.save(subscriberAuth);

            com.example.attendancesystem.grpc.subscriber.Subscriber grpcSubscriber = convertToGrpcSubscriber(savedSubscriber);

            SubscriberResponse response = SubscriberResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Subscriber created successfully")
                    .setSubscriber(grpcSubscriber)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
            logger.info("Subscriber created successfully with ID: {}", savedSubscriber.getId());

        } catch (Exception e) {
            logger.error("Error creating subscriber: {}", request.getUsername(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to create subscriber: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void getSubscriber(GetSubscriberRequest request, StreamObserver<SubscriberResponse> responseObserver) {
        try {
            Optional<Subscriber> subscriberOpt = subscriberRepository.findById(request.getId());

            if (subscriberOpt.isEmpty()) {
                SubscriberResponse response = SubscriberResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Subscriber not found")
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            com.example.attendancesystem.grpc.subscriber.Subscriber grpcSubscriber = convertToGrpcSubscriber(subscriberOpt.get());

            SubscriberResponse response = SubscriberResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Subscriber retrieved successfully")
                    .setSubscriber(grpcSubscriber)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Error retrieving subscriber with ID: {}", request.getId(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to retrieve subscriber: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void updateSubscriber(UpdateSubscriberRequest request, StreamObserver<SubscriberResponse> responseObserver) {
        try {
            Optional<Subscriber> subscriberOpt = subscriberRepository.findById(request.getId());

            if (subscriberOpt.isEmpty()) {
                SubscriberResponse response = SubscriberResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Subscriber not found")
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            Subscriber subscriber = subscriberOpt.get();
            subscriber.setEmail(request.getEmail());
            subscriber.setFirstName(request.getFirstName());
            subscriber.setLastName(request.getLastName());

            Subscriber savedSubscriber = subscriberRepository.save(subscriber);

            com.example.attendancesystem.grpc.subscriber.Subscriber grpcSubscriber = convertToGrpcSubscriber(savedSubscriber);

            SubscriberResponse response = SubscriberResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Subscriber updated successfully")
                    .setSubscriber(grpcSubscriber)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Error updating subscriber with ID: {}", request.getId(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to update subscriber: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void deleteSubscriber(DeleteSubscriberRequest request, StreamObserver<DeleteResponse> responseObserver) {
        try {
            Optional<Subscriber> subscriberOpt = subscriberRepository.findById(request.getId());

            if (subscriberOpt.isEmpty()) {
                DeleteResponse response = DeleteResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Subscriber not found")
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            // Delete subscriber (this will cascade to related entities)
            subscriberRepository.deleteById(request.getId());

            DeleteResponse response = DeleteResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Subscriber deleted successfully")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Error deleting subscriber with ID: {}", request.getId(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to delete subscriber: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void listSubscribers(ListSubscribersRequest request, StreamObserver<ListSubscribersResponse> responseObserver) {
        try {
            Optional<Organization> organizationOpt = organizationRepository.findById(request.getOrganizationId());
            if (organizationOpt.isEmpty()) {
                ListSubscribersResponse response = ListSubscribersResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Organization not found")
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            // Get subscribers with pagination
            Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
            Page<Subscriber> subscriberPage;

            if (request.getSearch().isEmpty()) {
                subscriberPage = subscriberRepository.findByOrganization(organizationOpt.get(), pageable);
            } else {
                // Search by first name or last name
                subscriberPage = subscriberRepository.findByOrganizationAndFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
                        organizationOpt.get(), request.getSearch(), request.getSearch(), pageable);
            }

            List<com.example.attendancesystem.grpc.subscriber.Subscriber> grpcSubscribers = subscriberPage.getContent()
                    .stream()
                    .map(this::convertToGrpcSubscriber)
                    .collect(Collectors.toList());

            ListSubscribersResponse response = ListSubscribersResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Subscribers retrieved successfully")
                    .addAllSubscribers(grpcSubscribers)
                    .setTotalCount(subscriberPage.getTotalElements())
                    .setPage(request.getPage())
                    .setSize(request.getSize())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Error listing subscribers", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to list subscribers: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void updateSubscriberPassword(UpdatePasswordRequest request, StreamObserver<SubscriberResponse> responseObserver) {
        try {
            Optional<Subscriber> subscriberOpt = subscriberRepository.findById(request.getSubscriberId());
            if (subscriberOpt.isEmpty()) {
                SubscriberResponse response = SubscriberResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Subscriber not found")
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            // Find subscriber auth
            List<SubscriberAuth> authList = subscriberAuthRepository.findBySubscriber(subscriberOpt.get());
            if (authList.isEmpty()) {
                SubscriberResponse response = SubscriberResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Subscriber authentication not found")
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            // Update password
            SubscriberAuth auth = authList.get(0);
            auth.setPin(passwordEncoder.encode(request.getNewPassword()));
            subscriberAuthRepository.save(auth);

            com.example.attendancesystem.grpc.subscriber.Subscriber grpcSubscriber = convertToGrpcSubscriber(subscriberOpt.get());

            SubscriberResponse response = SubscriberResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Password updated successfully")
                    .setSubscriber(grpcSubscriber)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Error updating subscriber password", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to update password: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    private com.example.attendancesystem.grpc.subscriber.Subscriber convertToGrpcSubscriber(Subscriber subscriber) {
        // Get NFC card for this subscriber
        Optional<NfcCard> nfcCardOpt = Optional.ofNullable(subscriber.getNfcCard());
        
        var builder = com.example.attendancesystem.grpc.subscriber.Subscriber.newBuilder()
                .setId(subscriber.getId())
                .setUsername(subscriber.getMobileNumber())
                .setEmail(subscriber.getEmail() != null ? subscriber.getEmail() : "")
                .setFirstName(subscriber.getFirstName())
                .setLastName(subscriber.getLastName())
                .setPhone(subscriber.getMobileNumber())
                .setEmployeeId("") // Not available in current model
                .setDepartment("") // Not available in current model
                .setPosition("") // Not available in current model
                .setOrganizationId(subscriber.getOrganization().getId())
                .setActive(true) // Default to true
                .setCreatedAt("") // Not available in current model
                .setUpdatedAt("") // Not available in current model
                .setProfilePhotoUrl(subscriber.getProfilePhotoPath() != null ? subscriber.getProfilePhotoPath() : "")
                .setFaceRegistered(subscriber.hasFaceRecognition());

        // Add NFC card if present
        if (nfcCardOpt.isPresent()) {
            NfcCard nfcCard = nfcCardOpt.get();
            com.example.attendancesystem.grpc.subscriber.NfcCard grpcNfcCard = 
                com.example.attendancesystem.grpc.subscriber.NfcCard.newBuilder()
                    .setId(nfcCard.getId())
                    .setCardId(nfcCard.getCardUid())
                    .setSubscriberId(subscriber.getId())
                    .setActive(nfcCard.isActive())
                    .setAssignedAt("") // Not available in current model
                    .setLastUsed("") // Not available in current model
                    .build();
            builder.addNfcCards(grpcNfcCard);
        }

        return builder.build();
    }
}
