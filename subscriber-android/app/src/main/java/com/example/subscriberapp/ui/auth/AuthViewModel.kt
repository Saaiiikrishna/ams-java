package com.example.subscriberapp.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.subscriberapp.data.api.ApiService
import com.example.subscriberapp.data.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val apiService: ApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _currentUser = MutableStateFlow<Subscriber?>(null)
    val currentUser: StateFlow<Subscriber?> = _currentUser.asStateFlow()

    private val _currentOrganization = MutableStateFlow<Organization?>(null)
    val currentOrganization: StateFlow<Organization?> = _currentOrganization.asStateFlow()

    private val _authToken = MutableStateFlow<String?>(null)
    val authToken: StateFlow<String?> = _authToken.asStateFlow()

    fun sendOtp(mobileNumber: String, entityId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                
                val request = OtpRequest(mobileNumber, entityId)
                val response = apiService.sendOtp(request)
                
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        otpSent = true,
                        successMessage = "OTP sent successfully"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Failed to send OTP: ${response.message()}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Network error: ${e.message}"
                )
            }
        }
    }

    fun verifyOtp(mobileNumber: String, entityId: String, otpCode: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                
                val request = LoginRequest(
                    mobileNumber = mobileNumber,
                    otpCode = otpCode,
                    deviceId = getDeviceId(),
                    deviceInfo = getDeviceInfo(),
                    entityId = entityId
                )
                
                val response = apiService.verifyOtp(request)
                
                if (response.isSuccessful) {
                    val loginResponse = response.body()!!
                    handleLoginSuccess(loginResponse)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "OTP verification failed: ${response.message()}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Network error: ${e.message}"
                )
            }
        }
    }

    fun loginWithPin(mobileNumber: String, pin: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

                val request = LoginRequest(
                    mobileNumber = mobileNumber,
                    pin = pin,
                    deviceId = getDeviceId(),
                    deviceInfo = getDeviceInfo()
                )

                val response = apiService.loginWithPin(request)

                if (response.isSuccessful) {
                    val loginResponse = response.body()!!
                    handleLoginSuccess(loginResponse)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Login failed: ${response.message()}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Network error: ${e.message}"
                )
            }
        }
    }

    private fun handleLoginSuccess(loginResponse: LoginResponse) {
        // Debug logging BEFORE setting values
        android.util.Log.d("AuthViewModel", "handleLoginSuccess called")
        android.util.Log.d("AuthViewModel", "Response - User: ${loginResponse.subscriber.firstName} ${loginResponse.subscriber.lastName}")
        android.util.Log.d("AuthViewModel", "Response - Organization: ${loginResponse.organization.name}")

        _authToken.value = loginResponse.token
        _currentUser.value = loginResponse.subscriber
        _currentOrganization.value = loginResponse.organization
        _isLoggedIn.value = true

        // Debug logging AFTER setting values
        android.util.Log.d("AuthViewModel", "State after setting - User: ${_currentUser.value?.firstName} ${_currentUser.value?.lastName}")
        android.util.Log.d("AuthViewModel", "State after setting - Organization: ${_currentOrganization.value?.name}")
        android.util.Log.d("AuthViewModel", "State after setting - IsLoggedIn: ${_isLoggedIn.value}")

        _uiState.value = _uiState.value.copy(
            isLoading = false,
            isLoggedIn = true,
            successMessage = loginResponse.message
        )
    }

    fun updatePin(currentPin: String, newPin: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

                val currentUserValue = _currentUser.value
                if (currentUserValue == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "User not logged in"
                    )
                    return@launch
                }

                val request = mapOf(
                    "mobileNumber" to currentUserValue.mobileNumber,
                    "currentPin" to currentPin,
                    "newPin" to newPin
                )

                val response = apiService.updatePin(request)

                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "PIN updated successfully"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Failed to update PIN: ${response.message()}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Network error: ${e.message}"
                )
            }
        }
    }

    fun logout() {
        _authToken.value = null
        _currentUser.value = null
        _currentOrganization.value = null
        _isLoggedIn.value = false
        _uiState.value = AuthUiState()
    }

    private fun getDeviceId(): String {
        // In a real app, you'd get the actual device ID
        return "android_device_${System.currentTimeMillis()}"
    }

    private fun getDeviceInfo(): String {
        return "Android ${android.os.Build.VERSION.RELEASE} - ${android.os.Build.MODEL}"
    }
}

data class AuthUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val otpSent: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)
