package com.example.entityadmin.data.api

import com.example.entityadmin.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>
    

    
    // Subscriber Management
    @GET("api/subscribers")
    suspend fun getSubscribers(): Response<List<Subscriber>>
    
    @POST("api/subscribers")
    suspend fun createSubscriber(@Body request: CreateSubscriberRequest): Response<ApiResponse<Subscriber>>
    
    @PUT("api/subscribers/{id}")
    suspend fun updateSubscriber(@Path("id") id: Long, @Body request: CreateSubscriberRequest): Response<ApiResponse<Subscriber>>
    
    @DELETE("api/subscribers/{id}")
    suspend fun deleteSubscriber(@Path("id") id: Long): Response<ApiResponse<Unit>>
    
    // Session Management
    @GET("api/sessions")
    suspend fun getSessions(): Response<List<AttendanceSession>>
    
    @POST("api/sessions")
    suspend fun createSession(@Body request: CreateSessionRequest): Response<ApiResponse<AttendanceSession>>
    
    @PUT("api/sessions/{id}")
    suspend fun updateSession(@Path("id") id: Long, @Body request: CreateSessionRequest): Response<ApiResponse<AttendanceSession>>
    
    @DELETE("api/sessions/{id}")
    suspend fun deleteSession(@Path("id") id: Long): Response<ApiResponse<Unit>>
    
    @POST("api/sessions/{id}/end")
    suspend fun endSession(@Path("id") id: Long): Response<ApiResponse<Unit>>
    
    // Attendance Management
    @GET("api/sessions/{sessionId}/attendance")
    suspend fun getSessionAttendance(@Path("sessionId") sessionId: Long): Response<List<AttendanceLog>>
    
    @GET("api/subscribers/{subscriberId}/attendance")
    suspend fun getSubscriberAttendance(
        @Path("subscriberId") subscriberId: Long,
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null
    ): Response<List<AttendanceLog>>
    
    // Reports
    @GET("api/reports/sessions/{sessionId}/attendance-pdf")
    suspend fun downloadSessionReport(@Path("sessionId") sessionId: Long): Response<ByteArray>
    
    @GET("api/reports/subscribers/{subscriberId}/activity-pdf")
    suspend fun downloadSubscriberReport(
        @Path("subscriberId") subscriberId: Long,
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String
    ): Response<ByteArray>
    

}
