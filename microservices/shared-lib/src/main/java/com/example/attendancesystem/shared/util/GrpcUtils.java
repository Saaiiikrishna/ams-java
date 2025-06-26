package com.example.attendancesystem.shared.util;

import com.google.protobuf.Timestamp;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Utility class for gRPC operations and conversions
 */
public class GrpcUtils {

    private GrpcUtils() {
        // Utility class
    }

    /**
     * Convert LocalDateTime to Protobuf Timestamp
     */
    public static Timestamp toTimestamp(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        Instant instant = localDateTime.toInstant(ZoneOffset.UTC);
        return Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
    }

    /**
     * Convert Protobuf Timestamp to LocalDateTime
     */
    public static LocalDateTime fromTimestamp(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        Instant instant = Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    /**
     * Create a gRPC StatusRuntimeException with INVALID_ARGUMENT status
     */
    public static StatusRuntimeException invalidArgument(String message) {
        return Status.INVALID_ARGUMENT
                .withDescription(message)
                .asRuntimeException();
    }

    /**
     * Create a gRPC StatusRuntimeException with NOT_FOUND status
     */
    public static StatusRuntimeException notFound(String message) {
        return Status.NOT_FOUND
                .withDescription(message)
                .asRuntimeException();
    }

    /**
     * Create a gRPC StatusRuntimeException with ALREADY_EXISTS status
     */
    public static StatusRuntimeException alreadyExists(String message) {
        return Status.ALREADY_EXISTS
                .withDescription(message)
                .asRuntimeException();
    }

    /**
     * Create a gRPC StatusRuntimeException with PERMISSION_DENIED status
     */
    public static StatusRuntimeException permissionDenied(String message) {
        return Status.PERMISSION_DENIED
                .withDescription(message)
                .asRuntimeException();
    }

    /**
     * Create a gRPC StatusRuntimeException with UNAUTHENTICATED status
     */
    public static StatusRuntimeException unauthenticated(String message) {
        return Status.UNAUTHENTICATED
                .withDescription(message)
                .asRuntimeException();
    }

    /**
     * Create a gRPC StatusRuntimeException with INTERNAL status
     */
    public static StatusRuntimeException internal(String message) {
        return Status.INTERNAL
                .withDescription(message)
                .asRuntimeException();
    }

    /**
     * Create a gRPC StatusRuntimeException with INTERNAL status and cause
     */
    public static StatusRuntimeException internal(String message, Throwable cause) {
        return Status.INTERNAL
                .withDescription(message)
                .withCause(cause)
                .asRuntimeException();
    }

    /**
     * Create a gRPC StatusRuntimeException with UNAVAILABLE status
     */
    public static StatusRuntimeException unavailable(String message) {
        return Status.UNAVAILABLE
                .withDescription(message)
                .asRuntimeException();
    }

    /**
     * Create a gRPC StatusRuntimeException with DEADLINE_EXCEEDED status
     */
    public static StatusRuntimeException deadlineExceeded(String message) {
        return Status.DEADLINE_EXCEEDED
                .withDescription(message)
                .asRuntimeException();
    }

    /**
     * Check if a string is null or empty
     */
    public static boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * Validate required field and throw INVALID_ARGUMENT if null or empty
     */
    public static void validateRequired(String value, String fieldName) {
        if (isNullOrEmpty(value)) {
            throw invalidArgument(fieldName + " is required");
        }
    }

    /**
     * Validate required field and throw INVALID_ARGUMENT if null
     */
    public static void validateRequired(Object value, String fieldName) {
        if (value == null) {
            throw invalidArgument(fieldName + " is required");
        }
    }

    /**
     * Validate ID field and throw INVALID_ARGUMENT if invalid
     */
    public static void validateId(long id, String fieldName) {
        if (id <= 0) {
            throw invalidArgument(fieldName + " must be a positive number");
        }
    }

    /**
     * Validate email format
     */
    public static void validateEmail(String email) {
        if (isNullOrEmpty(email)) {
            throw invalidArgument("Email is required");
        }
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw invalidArgument("Invalid email format");
        }
    }

    /**
     * Validate phone number format
     */
    public static void validatePhoneNumber(String phoneNumber) {
        if (isNullOrEmpty(phoneNumber)) {
            throw invalidArgument("Phone number is required");
        }
        if (!phoneNumber.matches("^[+]?[0-9]{10,15}$")) {
            throw invalidArgument("Invalid phone number format");
        }
    }

    /**
     * Safe string conversion with null check
     */
    public static String safeString(String value) {
        return value != null ? value : "";
    }

    /**
     * Safe long conversion with null check
     */
    public static long safeLong(Long value) {
        return value != null ? value : 0L;
    }

    /**
     * Safe boolean conversion with null check
     */
    public static boolean safeBoolean(Boolean value) {
        return value != null ? value : false;
    }
}
