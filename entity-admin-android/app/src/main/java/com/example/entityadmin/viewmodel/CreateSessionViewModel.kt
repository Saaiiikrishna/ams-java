package com.example.entityadmin.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.entityadmin.network.Session
import com.example.entityadmin.network.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateSessionViewModel @Inject constructor(
    private val sessionRepository: SessionRepository
) : ViewModel() {

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _sessionCreationResult = MutableLiveData<Result<Session>>()
    val sessionCreationResult: LiveData<Result<Session>> = _sessionCreationResult

    fun createSession(name: String) {
        if (name.isBlank()) {
            _sessionCreationResult.value = Result.failure(IllegalArgumentException("Session name cannot be empty."))
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            val result = sessionRepository.createSession(name)
            _sessionCreationResult.value = result
            _isLoading.value = false
        }
    }
}
