package com.example.attendancesystem.organization.grpc;

import com.example.attendancesystem.grpc.subscriber.*;
import com.example.attendancesystem.organization.model.NfcCard;
import com.example.attendancesystem.organization.model.Organization;
import com.example.attendancesystem.organization.model.Subscriber;
import com.example.attendancesystem.organization.model.SubscriberAuth;
import com.example.attendancesystem.organization.repository.NfcCardRepository;
import com.example.attendancesystem.organization.repository.OrganizationRepository;
import com.example.attendancesystem.organization.repository.SubscriberAuthRepository;
import com.example.attendancesystem.organization.repository.SubscriberRepository;
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
    public void changeSubscriberPassword(ChangePasswordRequest request, StreamObserver<ChangePasswordResponse> responseObserver) {
        try {
            Optional<Subscriber> subscriberOpt = subscriberRepository.findById(request.getSubscriberId());
            if (subscriberOpt.isEmpty()) {
                ChangePasswordResponse response = ChangePasswordResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Subscriber not found")
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            // Find subscriber auth
            Optional<SubscriberAuth> authOpt = subscriberAuthRepository.findBySubscriber(subscriberOpt.get());
            if (authOpt.isEmpty()) {
                ChangePasswordResponse response = ChangePasswordResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Subscriber authentication not found")
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            // Update password
            SubscriberAuth auth = authOpt.get();
            auth.setPin(passwordEncoder.encode(request.getNewPassword()));
            subscriberAuthRepository.save(auth);

            com.example.attendancesystem.grpc.subscriber.Subscriber grpcSubscriber = convertToGrpcSubscriber(subscriberOpt.get());

            ChangePasswordResponse response = ChangePasswordResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Password updated successfully")
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

    @Override
    public void assignNfcCard(AssignNfcCardRequest request, StreamObserver<NfcCardResponse> responseObserver) {
        try {
            Optional<Subscriber> subscriberOpt = subscriberRepository.findById(request.getSubscriberId());
            if (subscriberOpt.isEmpty()) {
                NfcCardResponse response = NfcCardResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Subscriber not found")
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            // Check if card is already assigned
            Optional<NfcCard> existingCard = nfcCardRepository.findByCardUid(request.getCardId());
            if (existingCard.isPresent() && existingCard.get().getSubscriber() != null) {
                NfcCardResponse response = NfcCardResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("NFC card is already assigned to another subscriber")
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            NfcCard nfcCard;
            if (existingCard.isPresent()) {
                nfcCard = existingCard.get();
            } else {
                nfcCard = new NfcCard();
                nfcCard.setCardUid(request.getCardId());
            }

            nfcCard.setSubscriber(subscriberOpt.get());
            nfcCard.setActive(true);
            nfcCard.setOrganization(subscriberOpt.get().getOrganization());

            NfcCard savedCard = nfcCardRepository.save(nfcCard);

            com.example.attendancesystem.grpc.subscriber.NfcCard grpcNfcCard = convertToGrpcNfcCard(savedCard);

            NfcCardResponse response = NfcCardResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("NFC card assigned successfully")
                    .setNfcCard(grpcNfcCard)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Error assigning NFC card", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to assign NFC card: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void unassignNfcCard(UnassignNfcCardRequest request, StreamObserver<NfcCardResponse> responseObserver) {
        try {
            Optional<NfcCard> nfcCardOpt = nfcCardRepository.findById(Long.parseLong(request.getCardId()));
            if (nfcCardOpt.isEmpty()) {
                NfcCardResponse response = NfcCardResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("NFC card not found")
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            NfcCard nfcCard = nfcCardOpt.get();
            nfcCard.setSubscriber(null);
            nfcCard.setActive(false);
            nfcCardRepository.save(nfcCard);

            NfcCardResponse response = NfcCardResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("NFC card unassigned successfully")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Error unassigning NFC card", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to unassign NFC card: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void getSubscriberNfcCards(GetSubscriberNfcCardsRequest request, StreamObserver<ListNfcCardsResponse> responseObserver) {
        try {
            Optional<Subscriber> subscriberOpt = subscriberRepository.findById(request.getSubscriberId());
            if (subscriberOpt.isEmpty()) {
                ListNfcCardsResponse response = ListNfcCardsResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Subscriber not found")
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            // Get NFC cards for this subscriber
            Optional<NfcCard> nfcCardOpt = Optional.ofNullable(subscriberOpt.get().getNfcCard());
            List<NfcCard> nfcCards = nfcCardOpt.map(List::of).orElse(List.of());
            List<com.example.attendancesystem.grpc.subscriber.NfcCard> grpcNfcCards = nfcCards.stream()
                    .map(this::convertToGrpcNfcCard)
                    .collect(Collectors.toList());

            ListNfcCardsResponse response = ListNfcCardsResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("NFC cards retrieved successfully")
                    .addAllNfcCards(grpcNfcCards)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Error getting subscriber NFC cards", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to get NFC cards: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void registerFace(RegisterFaceRequest request, StreamObserver<FaceRegistrationResponse> responseObserver) {
        try {
            Optional<Subscriber> subscriberOpt = subscriberRepository.findById(request.getSubscriberId());
            if (subscriberOpt.isEmpty()) {
                FaceRegistrationResponse response = FaceRegistrationResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Subscriber not found")
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            Subscriber subscriber = subscriberOpt.get();
            // Store face image data
            subscriber.setFaceEncoding(request.getFaceImage().toByteArray());
            subscriber.setFaceEncodingVersion("1.0");
            subscriber.setFaceRegisteredAt(LocalDateTime.now());
            subscriber.setFaceUpdatedAt(LocalDateTime.now());

            subscriberRepository.save(subscriber);

            FaceRegistrationResponse response = FaceRegistrationResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Face registered successfully")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Error registering face", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to register face: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void updateFaceData(UpdateFaceDataRequest request, StreamObserver<FaceRegistrationResponse> responseObserver) {
        try {
            Optional<Subscriber> subscriberOpt = subscriberRepository.findById(request.getSubscriberId());
            if (subscriberOpt.isEmpty()) {
                FaceRegistrationResponse response = FaceRegistrationResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Subscriber not found")
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            Subscriber subscriber = subscriberOpt.get();
            // Update face image data
            subscriber.setFaceEncoding(request.getFaceImage().toByteArray());
            subscriber.setFaceUpdatedAt(LocalDateTime.now());

            subscriberRepository.save(subscriber);

            FaceRegistrationResponse response = FaceRegistrationResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Face updated successfully")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Error updating face", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to update face: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void deleteFaceData(DeleteFaceDataRequest request, StreamObserver<DeleteResponse> responseObserver) {
        try {
            Optional<Subscriber> subscriberOpt = subscriberRepository.findById(request.getSubscriberId());
            if (subscriberOpt.isEmpty()) {
                DeleteResponse response = DeleteResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Subscriber not found")
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            Subscriber subscriber = subscriberOpt.get();
            // Clear face data
            subscriber.setFaceEncoding(null);
            subscriber.setFaceEncodingVersion(null);
            subscriber.setFaceRegisteredAt(null);
            subscriber.setFaceUpdatedAt(LocalDateTime.now());

            subscriberRepository.save(subscriber);

            DeleteResponse response = DeleteResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Face data deleted successfully")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Error deleting face", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to delete face: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    private com.example.attendancesystem.grpc.subscriber.NfcCard convertToGrpcNfcCard(NfcCard nfcCard) {
        return com.example.attendancesystem.grpc.subscriber.NfcCard.newBuilder()
                .setId(nfcCard.getId())
                .setCardId(nfcCard.getCardUid())
                .setSubscriberId(nfcCard.getSubscriber() != null ? nfcCard.getSubscriber().getId() : 0)
                .setActive(nfcCard.isActive())
                .setAssignedAt("") // Not available in current model
                .setLastUsed("") // Not available in current model
                .build();
    }

    @Override
    public void getSubscriberProfile(GetSubscriberProfileRequest request, StreamObserver<SubscriberResponse> responseObserver) {
        // This is the same as getSubscriber for now
        GetSubscriberRequest getRequest = GetSubscriberRequest.newBuilder()
                .setId(request.getSubscriberId())
                .build();
        getSubscriber(getRequest, responseObserver);
    }

    @Override
    public void updateSubscriberProfile(UpdateSubscriberProfileRequest request, StreamObserver<SubscriberResponse> responseObserver) {
        // This is similar to updateSubscriber for now
        UpdateSubscriberRequest updateRequest = UpdateSubscriberRequest.newBuilder()
                .setId(request.getSubscriberId())
                .setEmail(request.getEmail())
                .setFirstName(request.getFirstName())
                .setLastName(request.getLastName())
                .build();
        updateSubscriber(updateRequest, responseObserver);
    }

    @Override
    public void registerNfcCard(RegisterNfcCardRequest request, StreamObserver<NfcCardResponse> responseObserver) {
        // This is the same as assignNfcCard for now
        AssignNfcCardRequest assignRequest = AssignNfcCardRequest.newBuilder()
                .setSubscriberId(request.getSubscriberId())
                .setCardId(request.getCardId())
                .build();
        assignNfcCard(assignRequest, responseObserver);
    }
}
