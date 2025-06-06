package com.example.entityadmin.viewmodel

import androidx.lifecycle.ViewModel
import com.example.entityadmin.network.AuthRepository
import com.example.entityadmin.network.Session
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(private val repository: AuthRepository) : ViewModel() {
    var sessions: List<Session> = emptyList()
        private set

    suspend fun loadSessions() {
        sessions = repository.getSessions()
    }
}
