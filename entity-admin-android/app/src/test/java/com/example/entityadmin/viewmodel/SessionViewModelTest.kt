package com.example.entityadmin.viewmodel

import com.example.entityadmin.network.AuthRepository
import com.example.entityadmin.network.Session
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionViewModelTest {
    private val repository = mockk<AuthRepository>()
    private val viewModel = SessionViewModel(repository)

    @Test
    fun loadSessionsPopulatesList() = runTest {
        val sessions = listOf(Session("1", "Test"))
        coEvery { repository.getSessions() } returns sessions

        viewModel.loadSessions()
        assertEquals(sessions, viewModel.sessions)
    }
}
