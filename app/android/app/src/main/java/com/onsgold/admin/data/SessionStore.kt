package com.onsgold.admin.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.sessionDataStore by preferencesDataStore(name = "ons_admin_session")

class SessionStore(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }
    private val tokenKey = stringPreferencesKey("token")
    private val userKey = stringPreferencesKey("user")

    val sessionFlow: Flow<StoredSession> = context.sessionDataStore.data.map { preferences ->
        StoredSession(
            token = preferences[tokenKey],
            user = preferences[userKey]?.let { raw ->
                runCatching { json.decodeFromString<UserResponse>(raw) }.getOrNull()
            },
        )
    }

    suspend fun saveSession(token: String, user: UserResponse) {
        context.sessionDataStore.edit { preferences ->
            preferences[tokenKey] = token
            preferences[userKey] = json.encodeToString(user)
        }
    }

    suspend fun updateUser(user: UserResponse) {
        context.sessionDataStore.edit { preferences ->
            preferences[userKey] = json.encodeToString(user)
        }
    }

    suspend fun clear() {
        context.sessionDataStore.edit { preferences ->
            preferences.remove(tokenKey)
            preferences.remove(userKey)
        }
    }
}

data class StoredSession(
    val token: String? = null,
    val user: UserResponse? = null,
)
