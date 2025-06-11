package com.example.entityadmin.network

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = EncryptedSharedPreferences.create(
        "token_prefs",
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveToken(token: String) {
        prefs.edit().putString("jwt", token).apply()
    }

    fun getToken(): String? = prefs.getString("jwt", null)

    fun saveEntityName(entityName: String) {
        prefs.edit().putString("entity_name", entityName).apply()
    }

    fun getEntityName(): String? = prefs.getString("entity_name", null)

    fun saveUserName(userName: String) {
        prefs.edit().putString("user_name", userName).apply()
    }

    fun getUserName(): String? = prefs.getString("user_name", null)

    fun clearToken() {
        prefs.edit()
            .remove("jwt")
            .remove("entity_name")
            .remove("user_name")
            .apply()
    }
}
