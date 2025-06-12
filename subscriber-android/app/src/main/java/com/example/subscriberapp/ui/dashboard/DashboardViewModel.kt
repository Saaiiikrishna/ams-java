package com.example.subscriberapp.ui.dashboard

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
class DashboardViewModel @Inject constructor(
    private val apiService: ApiService
) : ViewModel() {

    private val _dashboardState = MutableStateFlow(DashboardState())
    val dashboardState: StateFlow<DashboardState> = _dashboardState.asStateFlow()

    private val _availableSessions = MutableStateFlow<List<Session>>(emptyList())
    val availableSessions: StateFlow<List<Session>> = _availableSessions.asStateFlow()

    private val _attendanceHistory = MutableStateFlow<List<AttendanceRecord>>(emptyList())
    val attendanceHistory: StateFlow<List<AttendanceRecord>> = _attendanceHistory.asStateFlow()

    fun loadDashboard(mobileNumber: String, entityId: String) {
        viewModelScope.launch {
            try {
                _dashboardState.value = _dashboardState.value.copy(isLoading = true, errorMessage = null)

                val response = apiService.getDashboard(mobileNumber, entityId)

                if (response.isSuccessful) {
                    val dashboardData = response.body()!!

                    // Parse the response and update state
                    // For now, we'll use empty lists as the backend response structure needs to be defined
                    _dashboardState.value = _dashboardState.value.copy(
                        isLoading = false,
                        activeSessions = emptyList(),
                        recentAttendance = emptyList(),
                        upcomingSessions = emptyList()
                    )
                } else {
                    _dashboardState.value = _dashboardState.value.copy(
                        isLoading = false,
                        errorMessage = "Failed to load dashboard: ${response.message()}"
                    )
                }

            } catch (e: Exception) {
                _dashboardState.value = _dashboardState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to load dashboard: ${e.message}"
                )
            }
        }
    }

    fun loadAvailableSessions(mobileNumber: String, entityId: String) {
        viewModelScope.launch {
            try {
                val response = apiService.getAvailableSessions(mobileNumber, entityId)
                if (response.isSuccessful) {
                    val sessionsResponse = response.body()!!
                    _availableSessions.value = sessionsResponse.sessions
                }
            } catch (e: Exception) {
                _dashboardState.value = _dashboardState.value.copy(
                    errorMessage = "Failed to load sessions: ${e.message}"
                )
            }
        }
    }

    fun loadAttendanceHistory(mobileNumber: String, entityId: String) {
        viewModelScope.launch {
            try {
                val response = apiService.getAttendanceHistory(mobileNumber, entityId)
                if (response.isSuccessful) {
                    // Parse attendance history from response
                    // For now, we'll use empty list until backend response structure is finalized
                    _attendanceHistory.value = emptyList()
                }
            } catch (e: Exception) {
                _dashboardState.value = _dashboardState.value.copy(
                    errorMessage = "Failed to load attendance history: ${e.message}"
                )
            }
        }
    }

    fun performQrCheckIn(mobileNumber: String, entityId: String, qrCode: String) {
        viewModelScope.launch {
            try {
                _dashboardState.value = _dashboardState.value.copy(isLoading = true)

                val request = mapOf(
                    "mobileNumber" to mobileNumber,
                    "entityId" to entityId,
                    "qrCode" to qrCode
                )

                val response = apiService.qrCheckIn(request)

                if (response.isSuccessful) {
                    val checkInResponse = response.body()!!
                    _dashboardState.value = _dashboardState.value.copy(
                        isLoading = false,
                        successMessage = checkInResponse.message
                    )
                    // Refresh dashboard after successful check-in
                    loadDashboard(mobileNumber, entityId)
                } else {
                    _dashboardState.value = _dashboardState.value.copy(
                        isLoading = false,
                        errorMessage = "Check-in failed: ${response.message()}"
                    )
                }
            } catch (e: Exception) {
                _dashboardState.value = _dashboardState.value.copy(
                    isLoading = false,
                    errorMessage = "Check-in failed: ${e.message}"
                )
            }
        }
    }

    fun clearMessages() {
        _dashboardState.value = _dashboardState.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }
}

data class DashboardState(
    val isLoading: Boolean = false,
    val activeSessions: List<ActiveSession> = emptyList(),
    val recentAttendance: List<AttendanceRecord> = emptyList(),
    val upcomingSessions: List<UpcomingSession> = emptyList(),
    val errorMessage: String? = null,
    val successMessage: String? = null
)

data class ActiveSession(
    val id: Long,
    val name: String,
    val description: String?,
    val startTime: String,
    val allowedMethods: List<String>,
    val qrCode: String?
)

data class AttendanceRecord(
    val id: Long,
    val sessionName: String,
    val checkInTime: String,
    val checkOutTime: String?,
    val method: String,
    val status: String
)

data class UpcomingSession(
    val id: Long,
    val name: String,
    val description: String?,
    val startTime: String,
    val durationMinutes: Int,
    val daysOfWeek: List<String>,
    val allowedMethods: List<String>,
    val isActive: Boolean
)
