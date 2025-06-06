package com.example.entityadmin.network

import javax.inject.Inject

class AuthRepository @Inject constructor(private val api: ApiService, private val tokenManager: TokenManager) {
    suspend fun login(username: String, password: String): Boolean {
        val response = api.login(AuthRequest(username, password))
        tokenManager.saveToken(response.token)
        return true
    }

    suspend fun getSessions(): List<Session> {
        val token = tokenManager.getToken() ?: return emptyList()
        return api.getSessions("Bearer $token")
    }
}
