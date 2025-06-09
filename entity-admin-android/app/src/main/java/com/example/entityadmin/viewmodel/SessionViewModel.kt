package com.example.entityadmin.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.entityadmin.network.AuthRepository
import com.example.entityadmin.network.Session
import com.example.entityadmin.util.toUserFriendlyMessage // Added
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch // Added
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(private val repository: AuthRepository) : ViewModel() {

    private val _sessionsLiveData = MutableLiveData<List<Session>>()
    val sessionsLiveData: LiveData<List<Session>> = _sessionsLiveData

    // Expose sessions directly for current usage in SessionListFragment, but also LiveData
    var sessions: List<Session> = emptyList()
        private set


    private val _isLoadingLiveData = MutableLiveData<Boolean>()
    val isLoadingLiveData: LiveData<Boolean> = _isLoadingLiveData

    private val _errorLiveData = MutableLiveData<String?>()
    val errorLiveData: LiveData<String?> = _errorLiveData

    fun loadSessions() { // Changed to non-suspend, launches coroutine internally
        viewModelScope.launch {
            _isLoadingLiveData.value = true
            _errorLiveData.value = null
            val result = repository.getSessions()
            result.onSuccess {
                sessions = it // Keep direct property for compatibility if SessionListFragment isn't fully switched to LiveData yet
                _sessionsLiveData.value = it
            }.onFailure {
                sessions = emptyList() // Clear sessions on error
                _sessionsLiveData.value = emptyList()
                _errorLiveData.value = it.toUserFriendlyMessage()
            }
            _isLoadingLiveData.value = false
        }
    }
}
