package com.example.entityadmin.network

import com.example.entityadmin.model.Subscriber
import com.example.entityadmin.util.toUserFriendlyMessage // Added
import org.json.JSONObject // Added for error body parsing
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubscriberRepository @Inject constructor(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) {
    suspend fun getSubscribers(): Result<List<Subscriber>> {
        val token = tokenManager.getToken()
        if (token == null) {
            return Result.failure(IllegalArgumentException("User not authenticated. Token is missing."))
        }
        return try {
            val subscribers = apiService.getSubscribers("Bearer $token")
            Result.success(subscribers)
        } catch (e: Exception) {
            Result.failure(Exception(e.toUserFriendlyMessage(), e))
        }
    }

    suspend fun createSubscriber(subscriberRequest: com.example.entityadmin.model.SubscriberRequest): Result<com.example.entityadmin.model.Subscriber> {
        val token = tokenManager.getToken()
        if (token == null) {
            return Result.failure(IllegalArgumentException("User not authenticated. Token is missing."))
        }
        return try {
            val newSubscriber = apiService.createSubscriber("Bearer $token", subscriberRequest)
            Result.success(newSubscriber)
        } catch (e: Exception) {
            Result.failure(Exception(e.toUserFriendlyMessage(), e))
        }
    }

    suspend fun getSubscriberById(subscriberId: String): Result<com.example.entityadmin.model.Subscriber> {
        val token = tokenManager.getToken()
        if (token == null) {
            return Result.failure(IllegalArgumentException("User not authenticated. Token is missing."))
        }
        return try {
            val subscriber = apiService.getSubscriberById("Bearer $token", subscriberId)
            Result.success(subscriber)
        } catch (e: Exception) {
            Result.failure(Exception(e.toUserFriendlyMessage(), e))
        }
    }

    suspend fun updateSubscriber(subscriberId: String, subscriberRequest: com.example.entityadmin.model.SubscriberRequest): Result<com.example.entityadmin.model.Subscriber> {
        val token = tokenManager.getToken()
        if (token == null) {
            return Result.failure(IllegalArgumentException("User not authenticated. Token is missing."))
        }
        return try {
            val updatedSubscriber = apiService.updateSubscriber("Bearer $token", subscriberId, subscriberRequest)
            Result.success(updatedSubscriber)
        } catch (e: Exception) {
            Result.failure(Exception(e.toUserFriendlyMessage(), e))
        }
    }

    suspend fun deleteSubscriber(subscriberId: String): Result<Unit> {
        val token = tokenManager.getToken()
        if (token == null) {
            return Result.failure(IllegalArgumentException("User not authenticated. Token is missing."))
        }
        return try {
            val response = apiService.deleteSubscriber("Bearer $token", subscriberId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                var errorMessage = "Failed to delete subscriber: ${response.code()} - ${response.message()}"
                response.errorBody()?.string()?.let { errorBody ->
                    try {
                        val jsonObject = JSONObject(errorBody)
                        if (jsonObject.has("message")) {
                            jsonObject.getString("message").takeIf { it.isNotBlank() }?.let { errorMessage = it }
                        } else if (jsonObject.has("error")) {
                            jsonObject.getString("error").takeIf { it.isNotBlank() }?.let { errorMessage = it }
                        }
                    } catch (e: Exception) { /* Ignore parsing error */ }
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.toUserFriendlyMessage(), e))
        }
    }
}
