package com.example.entityadmin.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.entityadmin.data.api.ApiService
import com.example.entityadmin.data.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val isLoading: Boolean = false,
    val stats: DashboardStats? = null,
    val subscribers: List<Subscriber> = emptyList(),
    val sessions: List<AttendanceSession> = emptyList(),
    val recentAttendance: List<AttendanceLog> = emptyList(),
    val errorMessage: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val apiService: ApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            try {
                // Load all dashboard data concurrently
                val subscribersResponse = apiService.getSubscribers()
                val sessionsResponse = apiService.getSessions()

                // Calculate dashboard stats from the fetched data
                val subscribers = if (subscribersResponse.isSuccessful) subscribersResponse.body() ?: emptyList() else emptyList()
                val sessions = if (sessionsResponse.isSuccessful) sessionsResponse.body() ?: emptyList() else emptyList()

                val dashboardStats = DashboardStats(
                    totalSubscribers = subscribers.size,
                    activeSubscribers = subscribers.count { it.isActive },
                    activeSessions = sessions.count { it.endTime == null }, // Sessions without end time are active
                    todayAttendance = 0 // TODO: Calculate from attendance logs if needed
                )

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    stats = dashboardStats,
                    subscribers = subscribers,
                    sessions = sessions
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to load dashboard data: ${e.message}"
                )
            }
        }
    }

    fun createSubscriber(request: CreateSubscriberRequest) {
        viewModelScope.launch {
            try {
                val response = apiService.createSubscriber(request)
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        successMessage = "Subscriber created successfully"
                    )
                    loadSubscribers() // Refresh the list
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Failed to create subscriber"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Error creating subscriber: ${e.message}"
                )
            }
        }
    }

    fun deleteSubscriber(subscriberId: Long) {
        viewModelScope.launch {
            try {
                val response = apiService.deleteSubscriber(subscriberId)
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        successMessage = "Subscriber deleted successfully"
                    )
                    loadSubscribers() // Refresh the list
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Failed to delete subscriber"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Error deleting subscriber: ${e.message}"
                )
            }
        }
    }

    fun createSession(request: CreateSessionRequest) {
        viewModelScope.launch {
            try {
                val response = apiService.createSession(request)
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        successMessage = "Session created successfully"
                    )
                    loadSessions() // Refresh the list
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Failed to create session"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Error creating session: ${e.message}"
                )
            }
        }
    }

    fun endSession(sessionId: Long) {
        viewModelScope.launch {
            try {
                val response = apiService.endSession(sessionId)
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        successMessage = "Session ended successfully"
                    )
                    loadSessions() // Refresh the list
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Failed to end session"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Error ending session: ${e.message}"
                )
            }
        }
    }

    fun loadSessionAttendance(sessionId: Long) {
        viewModelScope.launch {
            try {
                val response = apiService.getSessionAttendance(sessionId)
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        recentAttendance = response.body() ?: emptyList()
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Error loading attendance: ${e.message}"
                )
            }
        }
    }

    private fun loadSubscribers() {
        viewModelScope.launch {
            try {
                val response = apiService.getSubscribers()
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        subscribers = response.body() ?: emptyList()
                    )
                }
            } catch (e: Exception) {
                // Handle error silently for background refresh
            }
        }
    }

    private fun loadSessions() {
        viewModelScope.launch {
            try {
                val response = apiService.getSessions()
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        sessions = response.body() ?: emptyList()
                    )
                }
            } catch (e: Exception) {
                // Handle error silently for background refresh
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }
}
