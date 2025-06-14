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
            // Handle null or empty response gracefully
            val safeSubscribers = subscribers ?: emptyList()
            Result.success(safeSubscribers)
        } catch (e: retrofit2.HttpException) {
            // Handle specific HTTP error codes
            val errorMessage = when (e.code()) {
                401 -> "Authentication failed. Please login again."
                403 -> "Access denied. You don't have permission to view subscribers."
                404 -> "Subscribers endpoint not found. Please contact support."
                500 -> "Server error. Please try again later."
                else -> e.toUserFriendlyMessage()
            }
            Result.failure(Exception(errorMessage, e))
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
                            val message = jsonObject.getString("message")
                            if (message.isNotBlank()) {
                                errorMessage = message
                            }
                        } else if (jsonObject.has("error")) {
                            val error = jsonObject.getString("error")
                            if (error.isNotBlank()) {
                                errorMessage = error
                            }
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
