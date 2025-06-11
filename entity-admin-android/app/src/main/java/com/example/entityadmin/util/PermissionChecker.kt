package com.example.entityadmin.util

import com.example.entityadmin.network.TokenManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionChecker @Inject constructor(
    private val tokenManager: TokenManager
) {
    
    /**
     * Check if the current user has permission to access subscribers
     * This is a basic implementation - in a real app, this would check JWT claims or user roles
     */
    fun hasSubscriberAccess(): Boolean {
        val token = tokenManager.getToken()
        val userName = tokenManager.getUserName()
        
        // Basic permission check - you can enhance this based on your backend's role system
        return when {
            token == null -> false
            userName == null -> false
            // Add specific user role checks here if your backend provides role information
            // For now, we'll allow access but handle errors gracefully in the repository
            else -> true
        }
    }
    
    /**
     * Check if the current user has permission to create/edit subscribers
     */
    fun hasSubscriberEditAccess(): Boolean {
        val token = tokenManager.getToken()
        val userName = tokenManager.getUserName()
        
        return when {
            token == null -> false
            userName == null -> false
            // Add specific admin role checks here
            else -> true
        }
    }
    
    /**
     * Check if the current user has permission to delete subscribers
     */
    fun hasSubscriberDeleteAccess(): Boolean {
        val token = tokenManager.getToken()
        val userName = tokenManager.getUserName()
        
        return when {
            token == null -> false
            userName == null -> false
            // Add specific admin role checks here
            else -> true
        }
    }
    
    /**
     * Get user-friendly permission error message
     */
    fun getPermissionErrorMessage(action: String): String {
        return "You don't have permission to $action. Please contact your administrator for access."
    }
}
