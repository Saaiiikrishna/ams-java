package com.example.entityadmin.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val PREFS_NAME = "entity_admin_secure_prefs"
        private const val TOKEN_KEY = "auth_token"
        private const val ENTITY_ID_KEY = "entity_id"
        private const val ADMIN_NAME_KEY = "admin_name"
        private const val ORG_NAME_KEY = "org_name"
    }

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveToken(token: String) {
        sharedPreferences.edit()
            .putString(TOKEN_KEY, token)
            .apply()
    }

    fun getToken(): String? {
        return sharedPreferences.getString(TOKEN_KEY, null)
    }

    fun hasToken(): Boolean {
        return !getToken().isNullOrEmpty()
    }

    fun clearToken() {
        sharedPreferences.edit()
            .remove(TOKEN_KEY)
            .remove(ENTITY_ID_KEY)
            .remove(ADMIN_NAME_KEY)
            .remove(ORG_NAME_KEY)
            .apply()
    }

    fun saveUserInfo(entityId: String, adminName: String, orgName: String) {
        sharedPreferences.edit()
            .putString(ENTITY_ID_KEY, entityId)
            .putString(ADMIN_NAME_KEY, adminName)
            .putString(ORG_NAME_KEY, orgName)
            .apply()
    }

    fun getEntityId(): String? = sharedPreferences.getString(ENTITY_ID_KEY, null)
    fun getAdminName(): String? = sharedPreferences.getString(ADMIN_NAME_KEY, null)
    fun getOrgName(): String? = sharedPreferences.getString(ORG_NAME_KEY, null)
}
