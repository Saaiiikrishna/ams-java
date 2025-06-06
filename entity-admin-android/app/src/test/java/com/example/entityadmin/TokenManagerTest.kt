package com.example.entityadmin

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.entityadmin.network.TokenManager
import org.junit.Assert.assertEquals
import org.junit.Test

class TokenManagerTest {
    @Test
    fun saveAndRetrieveToken() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = TokenManager(context)
        manager.saveToken("abc")
        assertEquals("abc", manager.getToken())
    }
}
