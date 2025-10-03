package ink.trmnl.android.buddy.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

// Extension property to create DataStore instance as a singleton
private val Context.deviceTokensDataStore: DataStore<Preferences> by preferencesDataStore(name = "device_tokens")

/**
 * Repository for managing Device API Keys (Access Tokens) for each TRMNL device.
 *
 * Device API Keys are stored using DataStore with device ID as the key.
 * Format: "device_token_{deviceId}" -> "abc-123"
 */
@Inject
class DeviceTokenRepository(
    private val context: Context,
) {
    /**
     * Save or update a device API key (Access Token) for a specific device.
     *
     * @param deviceId The device ID
     * @param token The device API key (e.g., "abc-123")
     */
    suspend fun saveDeviceToken(
        deviceId: Int,
        token: String,
    ) {
        val key = stringPreferencesKey("device_token_$deviceId")
        context.deviceTokensDataStore.edit { preferences ->
            preferences[key] = token
        }
    }

    /**
     * Get the device API key for a specific device.
     *
     * @param deviceId The device ID
     * @return The device API key or null if not set
     */
    suspend fun getDeviceToken(deviceId: Int): String? {
        val key = stringPreferencesKey("device_token_$deviceId")
        return context.deviceTokensDataStore.data
            .map { preferences ->
                preferences[key]
            }.firstOrNull()
    }

    /**
     * Get device API key as Flow for a specific device.
     *
     * @param deviceId The device ID
     * @return Flow emitting the device API key or null
     */
    fun getDeviceTokenFlow(deviceId: Int): Flow<String?> {
        val key = stringPreferencesKey("device_token_$deviceId")
        return context.deviceTokensDataStore.data.map { preferences ->
            preferences[key]
        }
    }

    /**
     * Clear/remove the device API key for a specific device.
     *
     * @param deviceId The device ID
     */
    suspend fun clearDeviceToken(deviceId: Int) {
        val key = stringPreferencesKey("device_token_$deviceId")
        context.deviceTokensDataStore.edit { preferences ->
            preferences.remove(key)
        }
    }

    /**
     * Check if a device has an API key set.
     *
     * @param deviceId The device ID
     * @return true if token exists, false otherwise
     */
    suspend fun hasDeviceToken(deviceId: Int): Boolean = getDeviceToken(deviceId) != null
}
