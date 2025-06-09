package com.example.entityadmin.network

import com.example.entityadmin.util.toUserFriendlyMessage // Import the utility
import javax.inject.Inject


class AuthRepository @Inject constructor(private val api: ApiService, private val tokenManager: TokenManager) {
    suspend fun login(username: String, password: String): Result<Unit> { // Return Result<Unit>
        return try {
            val response = api.login(AuthRequest(username, password))
            // Assuming login response itself doesn't need complex parsing for errors,
            // and HttpException will be caught if login fails at network/server level.
            // If login can return a structured error in a 2xx response, that needs handling here.
            tokenManager.saveToken(response.token) // Assuming token is directly in response
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(e.toUserFriendlyMessage(), e))
        }
    }

    // getSessions is used by SessionViewModel, let's update it here as well.
    suspend fun getSessions(): Result<List<Session>> {
        val token = tokenManager.getToken()
        if (token == null) {
            return Result.failure(Exception("User not authenticated. Token is missing."))
        }
        return try {
            val sessions = api.getSessions("Bearer $token")
            Result.success(sessions)
        } catch (e: Exception) {
            Result.failure(Exception(e.toUserFriendlyMessage(), e))
        }
    }
}
