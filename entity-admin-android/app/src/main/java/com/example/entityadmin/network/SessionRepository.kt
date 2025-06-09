package com.example.entityadmin.network

import com.example.entityadmin.util.toUserFriendlyMessage // Added
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepository @Inject constructor(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) {
    suspend fun createSession(name: String): Result<Session> {
        val token = tokenManager.getToken()
        if (token == null) {
            // Consistent error message style
            return Result.failure(IllegalArgumentException("User not authenticated. Token is missing."))
        }
        return try {
            val request = SessionCreationRequest(name = name)
            val response = apiService.createSession("Bearer $token", request)
            // Assuming if API call is not successful, it throws HttpException, caught below.
            // If it could return a non-2xx with a valid body that needs custom success/failure logic,
            // that would need to be handled by inspecting 'response.isSuccessful' from a Response<Session> object.
            // Current ApiService.createSession directly returns Session, so Retrofit handles this.
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(Exception(e.toUserFriendlyMessage(), e))
        }
    }
}
