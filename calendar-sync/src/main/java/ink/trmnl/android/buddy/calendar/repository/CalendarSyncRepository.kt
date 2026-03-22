package ink.trmnl.android.buddy.calendar.repository

import android.content.Context
import android.content.SharedPreferences
import ink.trmnl.android.buddy.calendar.models.SyncCalendar
import ink.trmnl.android.buddy.calendar.models.SyncEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject

/**
 * Repository for managing calendar sync operations.
 *
 * Responsibilities:
 * - Query available calendars from device
 * - Manage user's calendar selection preferences
 * - Query events from selected calendars
 * - Track sync status and errors
 *
 * PERMISSIONS: READ_CALENDAR only (no write operations)
 */
class CalendarSyncRepository
    @Inject
    constructor(
        context: Context,
    ) : CalendarSyncRepositoryInterface {
        private val calendarProvider = CalendarProvider(context)
        private val sharedPrefs: SharedPreferences =
            context.getSharedPreferences(
                "calendar_sync_prefs",
                Context.MODE_PRIVATE,
            )
        private val json = Json { ignoreUnknownKeys = true }

        companion object {
            private const val TAG = "CalendarSyncRepository"
            private const val KEY_SELECTED_CALENDARS = "selected_calendars"
            private const val KEY_LAST_SYNC_TIME = "last_sync_time"
            private const val KEY_LAST_SYNC_ERROR = "last_sync_error"
            private const val KEY_SYNC_ENABLED = "sync_enabled"
            private const val KEY_PLUGIN_SETTING_ID = "plugin_setting_id"
            private const val PLUGIN_SETTING_ID_UNSET = -1
        }

        /**
         * Get all available calendars on the device.
         */
        override suspend fun getAvailableCalendars(): List<SyncCalendar> =
            withContext(Dispatchers.IO) {
                val calendars = calendarProvider.getAllCalendars()
                val selectedIds = getSelectedCalendarIds()

                calendars.map { calendar ->
                    calendar.copy(isSelected = calendar.calendarId in selectedIds)
                }
            }

        /**
         * Update calendar selection preferences.
         *
         * @param selectedCalendarIds List of calendar IDs user wants to sync
         */
        override suspend fun updateCalendarSelection(selectedCalendarIds: List<Long>) =
            withContext(Dispatchers.IO) {
                sharedPrefs.edit().apply {
                    putString(
                        KEY_SELECTED_CALENDARS,
                        selectedCalendarIds.joinToString(","),
                    )
                    apply()
                }
                Timber.d("[$TAG] Updated calendar selection: $selectedCalendarIds")
            }

        /**
         * Get currently selected calendar IDs.
         */
        private fun getSelectedCalendarIds(): List<Long> {
            val selectedStr = sharedPrefs.getString(KEY_SELECTED_CALENDARS, "") ?: ""
            return if (selectedStr.isEmpty()) {
                emptyList()
            } else {
                selectedStr.split(",").mapNotNull { it.toLongOrNull() }
            }
        }

        /**
         * Get events from selected calendars within time window.
         *
         * @param startTime Events after this timestamp (milliseconds since epoch)
         * @param endTime Events before this timestamp (milliseconds since epoch)
         * @return List of events in TRMNL sync format
         */
        override suspend fun getEventsForSync(
            startTime: Long,
            endTime: Long,
        ): List<SyncEvent> =
            withContext(Dispatchers.IO) {
                val selectedIds = getSelectedCalendarIds()

                if (selectedIds.isEmpty()) {
                    Timber.w("[$TAG] No calendars selected for sync")
                    return@withContext emptyList()
                }

                val events =
                    calendarProvider.getEventsForCalendars(
                        calendarIds = selectedIds,
                        startTime = startTime,
                        endTime = endTime,
                    )

                Timber.d("[$TAG] Retrieved ${events.size} events for sync")
                events
            }

        /**
         * Check if calendar sync is enabled by user.
         */
        override fun isSyncEnabled(): Boolean = sharedPrefs.getBoolean(KEY_SYNC_ENABLED, false)

        /**
         * Enable or disable calendar sync.
         *
         * @param enabled True to enable sync, false to disable
         */
        override fun setSyncEnabled(enabled: Boolean) {
            sharedPrefs.edit().apply {
                putBoolean(KEY_SYNC_ENABLED, enabled)
                apply()
            }
            Timber.d("[$TAG] Calendar sync enabled: $enabled")
        }

        /**
         * Get the timestamp of last successful sync.
         *
         * @return Milliseconds since epoch, or 0 if never synced
         */
        override fun getLastSyncTime(): Long = sharedPrefs.getLong(KEY_LAST_SYNC_TIME, 0)

        /**
         * Record successful sync completion.
         */
        override fun recordSyncSuccess() {
            sharedPrefs.edit().apply {
                putLong(KEY_LAST_SYNC_TIME, System.currentTimeMillis())
                putString(KEY_LAST_SYNC_ERROR, null)
                apply()
            }
            Timber.d("[$TAG] Sync recorded as successful")
        }

        /**
         * Record sync error.
         *
         * @param error Error message
         */
        override fun recordSyncError(error: String) {
            sharedPrefs.edit().apply {
                putString(KEY_LAST_SYNC_ERROR, error)
                apply()
            }
            Timber.e("[$TAG] Sync error recorded: $error")
        }

        /**
         * Get last sync error message.
         *
         * @return Error message, or null if no error
         */
        override fun getLastSyncError(): String? = sharedPrefs.getString(KEY_LAST_SYNC_ERROR, null)

        /**
         * Disconnect calendar sync (disable and clear selection).
         */
        override fun disconnect() {
            sharedPrefs.edit().apply {
                putBoolean(KEY_SYNC_ENABLED, false)
                putString(KEY_SELECTED_CALENDARS, "")
                putInt(KEY_PLUGIN_SETTING_ID, PLUGIN_SETTING_ID_UNSET)
                apply()
            }
            Timber.d("[$TAG] Calendar sync disconnected")
        }

        /**
         * Get cached plugin setting ID for the calendar plugin.
         *
         * @return Cached plugin setting ID, or null if not cached
         */
        override fun getCachedPluginSettingId(): Int? {
            val id = sharedPrefs.getInt(KEY_PLUGIN_SETTING_ID, PLUGIN_SETTING_ID_UNSET)
            return if (id == PLUGIN_SETTING_ID_UNSET) null else id
        }

        /**
         * Cache the plugin setting ID for the calendar plugin.
         *
         * @param id Plugin setting ID returned by GET /plugin_settings?plugin_id=calendars
         */
        override fun cachePluginSettingId(id: Int) {
            sharedPrefs.edit().apply {
                putInt(KEY_PLUGIN_SETTING_ID, id)
                apply()
            }
            Timber.d("[$TAG] Cached plugin setting ID: $id")
        }
    }
