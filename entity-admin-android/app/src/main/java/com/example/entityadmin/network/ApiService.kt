package com.example.entityadmin.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

data class AuthRequest(val username: String, val password: String)

data class AuthResponse(val token: String)

data class Session(val id: String, val name: String)

interface ApiService {
    @POST("/admin/authenticate")
    suspend fun login(@Body request: AuthRequest): AuthResponse

    @GET("/admin/sessions")
    suspend fun getSessions(@Header("Authorization") token: String): List<Session>
}
