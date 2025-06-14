package com.example.entityadmin.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.DELETE
import retrofit2.http.Path
import retrofit2.Response

data class AuthRequest(val username: String, val password: String)

data class AuthResponse(val jwt: String)

data class Session(val id: String, val name: String)

// Request DTO for creating a session
data class SessionCreationRequest(val name: String)

interface ApiService {
    @POST("/api/auth/login")
    suspend fun login(@Body request: AuthRequest): AuthResponse

    @GET("/api/sessions")
    suspend fun getSessions(@Header("Authorization") token: String): List<Session>

    @POST("/api/sessions")
    suspend fun createSession(@Header("Authorization") token: String, @Body request: SessionCreationRequest): Session

    @GET("/api/subscribers")
    suspend fun getSubscribers(@Header("Authorization") token: String): List<com.example.entityadmin.model.Subscriber>

    @POST("/api/subscribers")
    suspend fun createSubscriber(@Header("Authorization") token: String, @Body subscriberRequest: com.example.entityadmin.model.SubscriberRequest): com.example.entityadmin.model.Subscriber

    @GET("/api/subscribers/{id}")
    suspend fun getSubscriberById(@Header("Authorization") token: String, @Path("id") subscriberId: String): com.example.entityadmin.model.Subscriber

    @PUT("/api/subscribers/{id}")
    suspend fun updateSubscriber(@Header("Authorization") token: String, @Path("id") subscriberId: String, @Body subscriberRequest: com.example.entityadmin.model.SubscriberRequest): com.example.entityadmin.model.Subscriber

    @DELETE("/api/subscribers/{id}")
    suspend fun deleteSubscriber(@Header("Authorization") token: String, @Path("id") subscriberId: String): Response<Unit>
}
