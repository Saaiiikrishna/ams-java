package com.example.entityadmin.ui.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.entityadmin.data.TokenManager
import com.example.entityadmin.data.DynamicApiService
import com.example.entityadmin.data.model.LoginRequest
import com.example.entityadmin.data.model.LoginResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val dynamicApiService: DynamicApiService,
    private val tokenManager: TokenManager
) : ViewModel() {

    companion object {
        private const val TAG = "EntityAuthViewModel"
    }

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(tokenManager.hasToken())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    init {
        // Check if user is already logged in
        val hasToken = tokenManager.hasToken()
        Log.i(TAG, "🔐 AuthViewModel initialized. Has token: $hasToken")
        _uiState.value = _uiState.value.copy(isLoggedIn = hasToken)
        _isLoggedIn.value = hasToken
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            try {
                Log.i(TAG, "🚀 Starting login process...")
                Log.i(TAG, "📝 Username: $username")
                Log.i(TAG, "🔗 Current server: ${dynamicApiService.getCurrentBaseUrl()}")

                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    errorMessage = null,
                    successMessage = null
                )

                // Get the current API service (which will use the discovered server)
                val apiService = dynamicApiService.getApiService()
                Log.i(TAG, "🔧 Using API service with server: ${dynamicApiService.getCurrentBaseUrl()}")

                val loginRequest = LoginRequest(username, password)
                Log.i(TAG, "📤 Sending login request...")

                val response = apiService.login(loginRequest)

                Log.i(TAG, "📥 Login response received:")
                Log.i(TAG, "   Status code: ${response.code()}")
                Log.i(TAG, "   Is successful: ${response.isSuccessful}")
                Log.i(TAG, "   Response body: ${response.body()}")
                Log.i(TAG, "   Error body: ${response.errorBody()?.string()}")

                if (response.isSuccessful && response.body() != null) {
                    val loginResponse = response.body()!!
                    Log.i(TAG, "✅ Login successful!")
                    Log.i(TAG, "🎫 JWT received: ${loginResponse.jwt.take(20)}...")
                    Log.i(TAG, "🔄 Refresh token received: ${loginResponse.refreshToken.take(20)}...")

                    // Save JWT token (mirroring Entity Dashboard behavior)
                    tokenManager.saveToken(loginResponse.jwt)

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoggedIn = true,
                        successMessage = "Login successful!"
                    )
                    _isLoggedIn.value = true
                    Log.i(TAG, "✅ Login process completed successfully")
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "❌ Login failed:")
                    Log.e(TAG, "   Status code: ${response.code()}")
                    Log.e(TAG, "   Error body: $errorBody")

                    val errorMsg = when (response.code()) {
                        401 -> "Invalid username or password"
                        403 -> "Access denied. Please check your credentials."
                        404 -> "Service not found. Please try again later."
                        500 -> "Server error. Please try again later."
                        else -> "Login failed (${response.code()}). Please try again."
                    }

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = errorMsg
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Login exception: ${e.javaClass.simpleName}: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Network error: ${e.message}"
                )
            }
        }
    }

    fun logout() {
        Log.i(TAG, "🚪 Logging out...")
        tokenManager.clearToken()
        _uiState.value = AuthUiState()
        _isLoggedIn.value = false
        Log.i(TAG, "✅ Logout completed")
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }
}
