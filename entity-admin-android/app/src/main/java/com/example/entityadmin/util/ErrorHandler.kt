package com.example.entityadmin.util

import retrofit2.HttpException
import java.io.IOException
import org.json.JSONObject // Using org.json for simplicity

fun Throwable.toUserFriendlyMessage(): String {
    return when (this) {
        is HttpException -> {
            val errorBody = this.response()?.errorBody()?.string()
            var message = "Server error: ${this.code()} ${this.message()}" // Default HTTP error message
            if (errorBody != null) {
                try {
                    // Attempt to parse a "message" field from JSON error response
                    val jsonObject = JSONObject(errorBody)
                    if (jsonObject.has("message")) {
                        val serverMessage = jsonObject.getString("message")
                        if (serverMessage.isNotBlank()) {
                            message = serverMessage // Use server message if available
                        }
                    } else if (jsonObject.has("error")) { // Check for "error" field as another common pattern
                        val serverError = jsonObject.getString("error")
                        if (serverError.isNotBlank()) {
                            message = serverError
                        }
                    }
                } catch (e: Exception) {
                    // Ignore parsing error, fall back to the generic HTTP error message
                }
            }
            message
        }
        is IOException -> "Network error. Please check your connection and try again."
        is IllegalArgumentException -> this.message ?: "Invalid input." // For local validation errors
        else -> this.localizedMessage ?: "An unexpected error occurred: ${this.javaClass.simpleName}"
    }
}
