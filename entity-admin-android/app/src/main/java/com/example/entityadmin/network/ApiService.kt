package com.example.entityadmin.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

data class AuthRequest(val username: String, val password: String)

data class AuthResponse(val token: String)

data class Session(val id: String, val name: String)

// Request DTO for creating a session
data class SessionCreationRequest(val name: String)

interface ApiService {
    @POST("/admin/authenticate")
    suspend fun login(@Body request: AuthRequest): AuthResponse

    @GET("/admin/sessions")
    suspend fun getSessions(@Header("Authorization") token: String): List<Session>

    @POST("/admin/sessions")
    suspend fun createSession(@Header("Authorization") token: String, @Body request: SessionCreationRequest): Session

    @GET("/admin/subscribers")
    suspend fun getSubscribers(@Header("Authorization") token: String): List<com.example.entityadmin.model.Subscriber>

    @POST("/admin/subscribers")
    suspend fun createSubscriber(@Header("Authorization") token: String, @Body subscriberRequest: com.example.entityadmin.model.SubscriberRequest): com.example.entityadmin.model.Subscriber

    @GET("/admin/subscribers/{id}")
    suspend fun getSubscriberById(@Header("Authorization") token: String, @retrofit2.http.Path("id") subscriberId: String): com.example.entityadmin.model.Subscriber

    @PUT("/admin/subscribers/{id}")
    suspend fun updateSubscriber(@Header("Authorization") token: String, @retrofit2.http.Path("id") subscriberId: String, @Body subscriberRequest: com.example.entityadmin.model.SubscriberRequest): com.example.entityadmin.model.Subscriber

    @retrofit2.http.DELETE("/admin/subscribers/{id}")
    suspend fun deleteSubscriber(@Header("Authorization") token: String, @retrofit2.http.Path("id") subscriberId: String): retrofit2.Response<Unit>
}
