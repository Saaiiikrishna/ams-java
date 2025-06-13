package com.example.subscriberapp.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Subscriber(
    val id: Long,
    val firstName: String,
    val lastName: String,
    val mobileNumber: String,
    val email: String?,
    val hasNfcCard: Boolean
) : Parcelable

@Parcelize
data class Organization(
    val entityId: String,
    val name: String,
    val address: String?
) : Parcelable

@Parcelize
data class Session(
    val id: Long,
    val name: String,
    val description: String?,
    val startTime: String,
    val allowedMethods: List<String>,
    val isActive: Boolean = true
) : Parcelable

data class LoginRequest(
    val mobileNumber: String,
    val pin: String? = null,
    val otpCode: String? = null,
    val deviceId: String,
    val deviceInfo: String,
    val entityId: String? = null
)

data class LoginResponse(
    val token: String,
    val subscriber: Subscriber,
    val organization: Organization,
    val message: String
)

data class OtpRequest(
    val mobileNumber: String,
    val entityId: String
)

data class OtpResponse(
    val message: String,
    val mobileNumber: String,
    val expiryTime: String,
    val otp: String? = null // For development only
)

data class CheckInRequest(
    val sessionId: Long,
    val checkInMethod: String,
    val qrCode: String? = null,
    val deviceId: String? = null,
    val deviceInfo: String? = null,
    val locationInfo: String? = null,
    val nfcData: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)

data class CheckInResponse(
    val action: String, // CHECK_IN or CHECK_OUT
    val message: String,
    val session: String,
    val time: String,
    val method: String
)

data class SessionsResponse(
    val sessions: List<Session>,
    val count: Int
)

enum class CheckInMethod(val value: String, val displayName: String) {
    NFC("NFC", "NFC Card"),
    QR("QR", "QR Code"),
    BLUETOOTH("BLUETOOTH", "Bluetooth"),
    WIFI("WIFI", "WiFi"),
    MOBILE_NFC("MOBILE_NFC", "Mobile NFC")
}

// Additional data classes for API requests
data class QrCheckInRequest(
    val mobileNumber: String,
    val entityId: String,
    val qrCode: String
)
