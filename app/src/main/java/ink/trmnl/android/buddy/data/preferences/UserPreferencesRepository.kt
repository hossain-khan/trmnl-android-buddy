package ink.trmnl.android.buddy.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import ink.trmnl.android.buddy.di.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Extension property to create DataStore instance
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

/**
 * Repository for managing user preferences using DataStore.
 * Follows repository pattern to abstract the data source.
 */
interface UserPreferencesRepository {
    /**
     * Flow of user preferences that emits whenever preferences change.
     */
    val userPreferencesFlow: Flow<UserPreferences>

    /**
     * Save the API token to preferences.
     */
    suspend fun saveApiToken(token: String)

    /**
     * Clear the API token from preferences.
     */
    suspend fun clearApiToken()

    /**
     * Mark onboarding as completed.
     */
    suspend fun setOnboardingCompleted()

    /**
     * Set battery tracking preference.
     */
    suspend fun setBatteryTrackingEnabled(enabled: Boolean)

    /**
     * Clear all preferences.
     */
    suspend fun clearAll()
}

@ContributesBinding(AppScope::class)
@Inject
class UserPreferencesRepositoryImpl
    constructor(
        @ApplicationContext private val context: Context,
    ) : UserPreferencesRepository {
        private object PreferencesKeys {
            val API_TOKEN = stringPreferencesKey("api_token")
            val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
            val BATTERY_TRACKING_ENABLED = booleanPreferencesKey("battery_tracking_enabled")
        }

        override val userPreferencesFlow: Flow<UserPreferences> =
            context.dataStore.data.map { preferences ->
                UserPreferences(
                    apiToken = preferences[PreferencesKeys.API_TOKEN],
                    isOnboardingCompleted = preferences[PreferencesKeys.ONBOARDING_COMPLETED] ?: false,
                    isBatteryTrackingEnabled = preferences[PreferencesKeys.BATTERY_TRACKING_ENABLED] ?: true,
                )
            }

        override suspend fun saveApiToken(token: String) {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.API_TOKEN] = token
            }
        }

        override suspend fun clearApiToken() {
            context.dataStore.edit { preferences ->
                preferences.remove(PreferencesKeys.API_TOKEN)
            }
        }

        override suspend fun setOnboardingCompleted() {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.ONBOARDING_COMPLETED] = true
            }
        }

        override suspend fun setBatteryTrackingEnabled(enabled: Boolean) {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.BATTERY_TRACKING_ENABLED] = enabled
            }
        }

        override suspend fun clearAll() {
            context.dataStore.edit { preferences ->
                preferences.clear()
            }
        }
    }
