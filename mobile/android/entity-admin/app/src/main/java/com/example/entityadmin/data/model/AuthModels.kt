package com.example.entityadmin.data.model

data class LoginRequest(
    val username: String,
    val password: String
)

data class LoginResponse(
    val jwt: String,
    val refreshToken: String
)

data class Subscriber(
    val id: Long,
    val firstName: String,
    val lastName: String,
    val email: String,
    val mobileNumber: String,
    val isActive: Boolean = true
)

data class AttendanceSession(
    val id: Long,
    val name: String,
    val description: String?,
    val startTime: String,
    val endTime: String?,
    val isActive: Boolean,
    val attendeeCount: Int = 0
)

data class AttendanceLog(
    val id: Long,
    val subscriberId: Long,
    val subscriberName: String,
    val sessionId: Long,
    val sessionName: String,
    val checkInTime: String,
    val checkOutTime: String?,
    val checkInMethod: String,
    val checkOutMethod: String?
)

data class DashboardStats(
    val totalSubscribers: Int,
    val activeSubscribers: Int,
    val activeSessions: Int,
    val todayAttendance: Int
)

data class CreateSessionRequest(
    val name: String,
    val description: String?,
    val startTime: String,
    val endTime: String?
)

data class CreateSubscriberRequest(
    val firstName: String,
    val lastName: String,
    val email: String,
    val mobileNumber: String
)

data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T?
)
