package com.example.entityadmin.network

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthRepositoryTest {
    private val api = mockk<ApiService>()
    private val tokenManager = mockk<TokenManager>(relaxed = true)
    private val repository = AuthRepository(api, tokenManager)

    @Test
    fun loginStoresToken() = runTest {
        coEvery { api.login(any()) } returns AuthResponse("xyz")

        val result = repository.login("a@a.com", "pass")

        assertTrue(result)
        verify { tokenManager.saveToken("xyz") }
    }

    @Test
    fun getSessionsUsesBearerToken() = runTest {
        every { tokenManager.getToken() } returns "abc"
        val sessions = listOf(Session("1", "Name"))
        coEvery { api.getSessions("Bearer abc") } returns sessions

        val loaded = repository.getSessions()
        assertEquals(sessions, loaded)
    }
}
