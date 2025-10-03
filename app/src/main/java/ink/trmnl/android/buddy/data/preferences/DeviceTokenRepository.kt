package ink.trmnl.android.buddy.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

// Extension property to create DataStore instance as a singleton
private val Context.deviceTokensDataStore: DataStore<Preferences> by preferencesDataStore(name = "device_tokens")

/**
 * Repository interface for managing Device API Keys (Access Tokens) for each TRMNL device.
 *
 * Device API Keys are stored using DataStore with device friendly ID as the key.
 * Format: "device_token_{friendlyId}" -> "abc-123"
 */
interface DeviceTokenRepository {
    /**
     * Save or update a device API key (Access Token) for a specific device.
     *
     * @param deviceFriendlyId The device friendly ID (e.g., "ABC-123")
     * @param token The device API key (e.g., "abc-123")
     */
    suspend fun saveDeviceToken(
        deviceFriendlyId: String,
        token: String,
    )

    /**
     * Get the device API key for a specific device.
     *
     * @param deviceFriendlyId The device friendly ID (e.g., "ABC-123")
     * @return The device API key or null if not set
     */
    suspend fun getDeviceToken(deviceFriendlyId: String): String?

    /**
     * Get device API key as Flow for a specific device.
     *
     * @param deviceFriendlyId The device friendly ID (e.g., "ABC-123")
     * @return Flow emitting the device API key or null
     */
    fun getDeviceTokenFlow(deviceFriendlyId: String): Flow<String?>

    /**
     * Clear/remove the device API key for a specific device.
     *
     * @param deviceFriendlyId The device friendly ID (e.g., "ABC-123")
     */
    suspend fun clearDeviceToken(deviceFriendlyId: String)

    /**
     * Check if a device has an API key set.
     *
     * @param deviceFriendlyId The device friendly ID (e.g., "ABC-123")
     * @return true if token exists, false otherwise
     */
    suspend fun hasDeviceToken(deviceFriendlyId: String): Boolean

    /**
     * Clear all device API keys from storage.
     * This removes all stored device tokens.
     */
    suspend fun clearAll()
}

/**
 * Implementation of DeviceTokenRepository using DataStore Preferences.
 */
@ContributesBinding(dev.zacsweers.metro.AppScope::class)
@Inject
class DeviceTokenRepositoryImpl(
    private val context: Context,
) : DeviceTokenRepository {
    /**
     * Save or update a device API key (Access Token) for a specific device.
     *
     * @param deviceFriendlyId The device friendly ID (e.g., "ABC-123")
     * @param token The device API key (e.g., "abc-123")
     */
    override suspend fun saveDeviceToken(
        deviceFriendlyId: String,
        token: String,
    ) {
        val key = stringPreferencesKey("device_token_$deviceFriendlyId")
        context.deviceTokensDataStore.edit { preferences ->
            preferences[key] = token
        }
    }

    /**
     * Get the device API key for a specific device.
     *
     * @param deviceFriendlyId The device friendly ID (e.g., "ABC-123")
     * @return The device API key or null if not set
     */
    override suspend fun getDeviceToken(deviceFriendlyId: String): String? {
        val key = stringPreferencesKey("device_token_$deviceFriendlyId")
        return context.deviceTokensDataStore.data
            .map { preferences ->
                preferences[key]
            }.firstOrNull()
    }

    /**
     * Get device API key as Flow for a specific device.
     *
     * @param deviceFriendlyId The device friendly ID (e.g., "ABC-123")
     * @return Flow emitting the device API key or null
     */
    override fun getDeviceTokenFlow(deviceFriendlyId: String): Flow<String?> {
        val key = stringPreferencesKey("device_token_$deviceFriendlyId")
        return context.deviceTokensDataStore.data.map { preferences ->
            preferences[key]
        }
    }

    /**
     * Clear/remove the device API key for a specific device.
     *
     * @param deviceFriendlyId The device friendly ID (e.g., "ABC-123")
     */
    override suspend fun clearDeviceToken(deviceFriendlyId: String) {
        val key = stringPreferencesKey("device_token_$deviceFriendlyId")
        context.deviceTokensDataStore.edit { preferences ->
            preferences.remove(key)
        }
    }

    /**
     * Check if a device has an API key set.
     *
     * @param deviceFriendlyId The device friendly ID (e.g., "ABC-123")
     * @return true if token exists, false otherwise
     */
    override suspend fun hasDeviceToken(deviceFriendlyId: String): Boolean = getDeviceToken(deviceFriendlyId) != null

    /**
     * Clear all device API keys from storage.
     * This removes all stored device tokens.
     */
    override suspend fun clearAll() {
        context.deviceTokensDataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
