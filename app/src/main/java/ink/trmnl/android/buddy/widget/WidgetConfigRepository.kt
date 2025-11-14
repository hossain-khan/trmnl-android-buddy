package ink.trmnl.android.buddy.widget

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Extension property to create DataStore instance as a singleton
private val Context.widgetDataStore: DataStore<Preferences> by preferencesDataStore(name = "widget_config")

/**
 * Repository for managing widget configuration.
 * Stores which device each widget instance is displaying.
 */
interface WidgetConfigRepository {
    /**
     * Save the device ID for a specific widget.
     *
     * @param appWidgetId The widget ID
     * @param deviceId The device ID to display
     */
    suspend fun saveWidgetDevice(
        appWidgetId: Int,
        deviceId: Int,
    )

    /**
     * Get the device ID for a specific widget.
     *
     * @param appWidgetId The widget ID
     * @return The device ID or null if not configured
     */
    suspend fun getWidgetDevice(appWidgetId: Int): Int?

    /**
     * Get the device ID as Flow for a specific widget.
     *
     * @param appWidgetId The widget ID
     * @return Flow emitting the device ID or null
     */
    fun getWidgetDeviceFlow(appWidgetId: Int): Flow<Int?>

    /**
     * Remove the configuration for a specific widget.
     * Called when a widget is deleted.
     *
     * @param appWidgetId The widget ID
     */
    suspend fun removeWidget(appWidgetId: Int)
}

/**
 * Implementation of WidgetConfigRepository using DataStore Preferences.
 */
@ContributesBinding(AppScope::class)
@Inject
class WidgetConfigRepositoryImpl(
    private val context: Context,
) : WidgetConfigRepository {
    override suspend fun saveWidgetDevice(
        appWidgetId: Int,
        deviceId: Int,
    ) {
        val key = intPreferencesKey("widget_device_$appWidgetId")
        context.widgetDataStore.edit { preferences ->
            preferences[key] = deviceId
        }
    }

    override suspend fun getWidgetDevice(appWidgetId: Int): Int? {
        val key = intPreferencesKey("widget_device_$appWidgetId")
        return context.widgetDataStore.data
            .map { preferences -> preferences[key] }
            .first()
    }

    private suspend fun <T> Flow<T>.first(): T {
        var result: T? = null
        collect { value ->
            result = value
            return@collect
        }
        @Suppress("UNCHECKED_CAST")
        return result as T
    }

    override fun getWidgetDeviceFlow(appWidgetId: Int): Flow<Int?> {
        val key = intPreferencesKey("widget_device_$appWidgetId")
        return context.widgetDataStore.data.map { preferences ->
            preferences[key]
        }
    }

    override suspend fun removeWidget(appWidgetId: Int) {
        val key = intPreferencesKey("widget_device_$appWidgetId")
        context.widgetDataStore.edit { preferences ->
            preferences.remove(key)
        }
    }
}
