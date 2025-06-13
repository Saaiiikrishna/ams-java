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

                android.util.Log.d("DashboardViewModel", "Loading dashboard for $mobileNumber, $entityId")
                val response = apiService.getDashboard(mobileNumber, entityId)

                if (response.isSuccessful) {
                    val dashboardData = response.body()
                    android.util.Log.d("DashboardViewModel", "Dashboard response: $dashboardData")

                    dashboardData?.let { data ->
                        // Parse active sessions
                        val activeSessionsList = data["activeSessions"] as? List<Map<String, Any>>
                        val activeSessions = activeSessionsList?.map { sessionData ->
                            ActiveSession(
                                id = (sessionData["id"] as? Number)?.toLong() ?: 0L,
                                name = sessionData["name"] as? String ?: "Unknown Session",
                                description = sessionData["description"] as? String,
                                startTime = sessionData["startTime"] as? String ?: "",
                                allowedMethods = (sessionData["allowedMethods"] as? List<String>) ?: emptyList(),
                                qrCode = sessionData["qrCode"] as? String
                            )
                        } ?: emptyList()

                        // Parse recent attendance
                        val recentAttendanceList = data["recentAttendance"] as? List<Map<String, Any>>
                        val recentAttendance = recentAttendanceList?.map { attendanceData ->
                            AttendanceRecord(
                                id = (attendanceData["id"] as? Number)?.toLong() ?: 0L,
                                sessionName = attendanceData["sessionName"] as? String ?: "Unknown Session",
                                checkInTime = attendanceData["checkInTime"] as? String ?: "",
                                checkOutTime = attendanceData["checkOutTime"] as? String,
                                checkInMethod = attendanceData["checkInMethod"] as? String ?: "Unknown",
                                checkOutMethod = attendanceData["checkOutMethod"] as? String,
                                status = if (attendanceData["checkOutTime"] != null) "completed" else "active"
                            )
                        } ?: emptyList()

                        // Parse current check-in status
                        val currentCheckIn = data["currentCheckIn"] as? Map<String, Any>
                        val checkInStatus = currentCheckIn?.let { checkInData ->
                            CheckInStatus(
                                isCheckedIn = checkInData["isCheckedIn"] as? Boolean ?: false,
                                sessionName = checkInData["sessionName"] as? String ?: "",
                                checkInTime = checkInData["checkInTime"] as? String ?: "",
                                checkInMethod = checkInData["checkInMethod"] as? String ?: "",
                                sessionId = (checkInData["sessionId"] as? Number)?.toLong() ?: 0L
                            )
                        }

                        // Parse upcoming sessions
                        val upcomingSessionsList = data["upcomingSessions"] as? List<Map<String, Any>>
                        val upcomingSessions = upcomingSessionsList?.map { sessionData ->
                            UpcomingSession(
                                id = (sessionData["id"] as? Number)?.toLong() ?: 0L,
                                name = sessionData["name"] as? String ?: "Unknown Session",
                                description = sessionData["description"] as? String,
                                startTime = sessionData["startTime"] as? String ?: "",
                                durationMinutes = (sessionData["durationMinutes"] as? Number)?.toInt() ?: 60,
                                daysOfWeek = (sessionData["daysOfWeek"] as? List<String>) ?: emptyList(),
                                allowedMethods = (sessionData["allowedMethods"] as? List<String>) ?: emptyList(),
                                isActive = (sessionData["isActive"] as? Boolean) ?: true
                            )
                        } ?: emptyList()

                        _dashboardState.value = _dashboardState.value.copy(
                            isLoading = false,
                            activeSessions = activeSessions,
                            recentAttendance = recentAttendance,
                            upcomingSessions = upcomingSessions,
                            currentCheckInStatus = checkInStatus,
                            errorMessage = null
                        )

                        android.util.Log.d("DashboardViewModel", "Dashboard loaded: ${activeSessions.size} active, ${recentAttendance.size} recent, ${upcomingSessions.size} upcoming")
                    } ?: run {
                        _dashboardState.value = _dashboardState.value.copy(
                            isLoading = false,
                            activeSessions = emptyList(),
                            recentAttendance = emptyList(),
                            upcomingSessions = emptyList(),
                            errorMessage = null
                        )
                    }
                } else {
                    android.util.Log.e("DashboardViewModel", "Failed to load dashboard: ${response.code()} ${response.message()}")
                    // Don't show error to user - just use empty data
                    _dashboardState.value = _dashboardState.value.copy(
                        isLoading = false,
                        activeSessions = emptyList(),
                        recentAttendance = emptyList(),
                        upcomingSessions = emptyList()
                        // No error message to avoid red error card
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("DashboardViewModel", "Exception loading dashboard", e)
                // Don't show error to user - just use empty data
                _dashboardState.value = _dashboardState.value.copy(
                    isLoading = false,
                    activeSessions = emptyList(),
                    recentAttendance = emptyList(),
                    upcomingSessions = emptyList()
                    // No error message to avoid red error card
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
                android.util.Log.d("DashboardViewModel", "Loading attendance history for $mobileNumber, $entityId")
                val response = apiService.getAttendanceHistory(mobileNumber, entityId)
                if (response.isSuccessful) {
                    val historyResponse = response.body()
                    android.util.Log.d("DashboardViewModel", "Attendance history response: $historyResponse")

                    historyResponse?.let { data ->
                        val historyList = data["history"] as? List<Map<String, Any>>
                        if (historyList != null) {
                            val attendanceRecords = historyList.map { historyItem ->
                                AttendanceRecord(
                                    id = (historyItem["id"] as? Number)?.toLong() ?: 0L,
                                    sessionName = historyItem["sessionName"] as? String ?: "Unknown Session",
                                    checkInTime = historyItem["checkInTime"] as? String ?: "",
                                    checkOutTime = historyItem["checkOutTime"] as? String,
                                    checkInMethod = historyItem["checkInMethod"] as? String ?: "Unknown",
                                    checkOutMethod = historyItem["checkOutMethod"] as? String,
                                    status = if (historyItem["checkOutTime"] != null) "completed" else "active"
                                )
                            }
                            _attendanceHistory.value = attendanceRecords
                            android.util.Log.d("DashboardViewModel", "Loaded ${attendanceRecords.size} attendance records")
                        } else {
                            _attendanceHistory.value = emptyList()
                            android.util.Log.d("DashboardViewModel", "No attendance history found")
                        }
                    }
                } else {
                    android.util.Log.e("DashboardViewModel", "Failed to load attendance history: ${response.code()} ${response.message()}")
                    _attendanceHistory.value = emptyList()
                }
            } catch (e: Exception) {
                android.util.Log.e("DashboardViewModel", "Exception loading attendance history", e)
                _dashboardState.value = _dashboardState.value.copy(
                    errorMessage = "Failed to load attendance history: ${e.message}"
                )
            }
        }
    }

    fun performQrCheckIn(mobileNumber: String, entityId: String, qrCode: String) {
        viewModelScope.launch {
            try {
                android.util.Log.d("DashboardViewModel", "Starting QR check-in for mobile: $mobileNumber, entity: $entityId, qr: $qrCode")
                _dashboardState.value = _dashboardState.value.copy(isLoading = true)

                val request = mapOf(
                    "mobileNumber" to mobileNumber,
                    "entityId" to entityId,
                    "qrCode" to qrCode
                )

                android.util.Log.d("DashboardViewModel", "Sending QR check-in request: $request")
                val response = apiService.qrCheckIn(request)
                android.util.Log.d("DashboardViewModel", "QR check-in response code: ${response.code()}")

                if (response.isSuccessful) {
                    val checkInResponse = response.body()!!
                    android.util.Log.d("DashboardViewModel", "QR check-in successful: ${checkInResponse.message}")
                    _dashboardState.value = _dashboardState.value.copy(
                        isLoading = false,
                        successMessage = checkInResponse.message
                    )
                    // Refresh dashboard after successful check-in
                    loadDashboard(mobileNumber, entityId)
                } else {
                    val errorBody = response.errorBody()?.string()
                    android.util.Log.e("DashboardViewModel", "QR check-in failed: ${response.code()} - ${response.message()}")
                    android.util.Log.e("DashboardViewModel", "Error body: $errorBody")
                    _dashboardState.value = _dashboardState.value.copy(
                        isLoading = false,
                        errorMessage = "Check-in failed: ${response.message()} (${response.code()})"
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("DashboardViewModel", "QR check-in exception", e)
                _dashboardState.value = _dashboardState.value.copy(
                    isLoading = false,
                    errorMessage = "Check-in failed: ${e.message}"
                )
            }
        }
    }

    fun performWifiCheckIn(mobileNumber: String, entityId: String, wifiNetworkName: String) {
        viewModelScope.launch {
            try {
                android.util.Log.d("DashboardViewModel", "Starting WiFi check-in for mobile: $mobileNumber, entity: $entityId, network: $wifiNetworkName")
                _dashboardState.value = _dashboardState.value.copy(isLoading = true)

                val request = mapOf(
                    "mobileNumber" to mobileNumber,
                    "entityId" to entityId,
                    "wifiNetworkName" to wifiNetworkName,
                    "deviceInfo" to "Android Mobile App"
                )

                android.util.Log.d("DashboardViewModel", "Sending WiFi check-in request: $request")
                val response = apiService.wifiCheckIn(request)
                android.util.Log.d("DashboardViewModel", "WiFi check-in response code: ${response.code()}")

                if (response.isSuccessful) {
                    val checkInResponse = response.body()!!
                    android.util.Log.d("DashboardViewModel", "WiFi check-in successful: ${checkInResponse.message}")
                    _dashboardState.value = _dashboardState.value.copy(
                        isLoading = false,
                        successMessage = checkInResponse.message
                    )
                    // Refresh dashboard after successful check-in
                    loadDashboard(mobileNumber, entityId)
                } else {
                    val errorBody = response.errorBody()?.string()
                    android.util.Log.e("DashboardViewModel", "WiFi check-in failed: ${response.code()} - ${response.message()}")
                    android.util.Log.e("DashboardViewModel", "Error body: $errorBody")

                    // Parse error message for better user feedback
                    val errorMessage = try {
                        val errorJson = com.google.gson.Gson().fromJson(errorBody, Map::class.java)
                        errorJson["error"] as? String ?: "WiFi check-in failed"
                    } catch (e: Exception) {
                        "WiFi check-in failed: ${response.message()}"
                    }

                    _dashboardState.value = _dashboardState.value.copy(
                        isLoading = false,
                        errorMessage = errorMessage
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("DashboardViewModel", "WiFi check-in exception", e)
                _dashboardState.value = _dashboardState.value.copy(
                    isLoading = false,
                    errorMessage = "WiFi check-in failed: ${e.message}"
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
    val currentCheckInStatus: CheckInStatus? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

data class CheckInStatus(
    val isCheckedIn: Boolean,
    val sessionName: String,
    val checkInTime: String,
    val checkInMethod: String,
    val sessionId: Long
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
    val checkInMethod: String,
    val checkOutMethod: String?,
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
