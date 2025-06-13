package com.example.subscriberapp.data.api

import com.example.subscriberapp.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    
    @POST("subscriber/send-otp")
    suspend fun sendOtp(@Body request: OtpRequest): Response<OtpResponse>
    
    @POST("subscriber/verify-otp")
    suspend fun verifyOtp(@Body request: LoginRequest): Response<LoginResponse>
    
    @POST("subscriber/login-pin")
    suspend fun loginWithPin(@Body request: LoginRequest): Response<LoginResponse>
    
    @PUT("subscriber/update-pin")
    suspend fun updatePin(@Body request: Map<String, String>): Response<Map<String, String>>
    
    @GET("subscriber/profile")
    suspend fun getProfile(): Response<Subscriber>
    
    @GET("subscriber/health")
    suspend fun healthCheck(): Response<Map<String, Any>>
    
    // Dashboard and session endpoints for mobile app
    @GET("subscriber/mobile/dashboard")
    suspend fun getDashboard(
        @Query("mobileNumber") mobileNumber: String,
        @Query("entityId") entityId: String
    ): Response<Map<String, Any>>

    @GET("subscriber/mobile/sessions")
    suspend fun getAvailableSessions(
        @Query("mobileNumber") mobileNumber: String,
        @Query("entityId") entityId: String
    ): Response<SessionsResponse>

    @GET("subscriber/mobile/attendance/history")
    suspend fun getAttendanceHistory(
        @Query("mobileNumber") mobileNumber: String,
        @Query("entityId") entityId: String
    ): Response<Map<String, Any>>

    // Check-in endpoints for mobile app
    @POST("subscriber/mobile/checkin/qr")
    suspend fun qrCheckIn(@Body request: Map<String, String>): Response<CheckInResponse>

    @POST("subscriber/mobile/checkin/wifi")
    suspend fun wifiCheckIn(@Body request: Map<String, String>): Response<CheckInResponse>

    @POST("checkin/bluetooth")
    suspend fun bluetoothCheckIn(@Body request: CheckInRequest): Response<CheckInResponse>

    @POST("checkin/wifi")
    suspend fun wifiCheckInGeneric(@Body request: CheckInRequest): Response<CheckInResponse>

    @POST("checkin/mobile-nfc")
    suspend fun mobileNfcCheckIn(@Body request: CheckInRequest): Response<CheckInResponse>
}
