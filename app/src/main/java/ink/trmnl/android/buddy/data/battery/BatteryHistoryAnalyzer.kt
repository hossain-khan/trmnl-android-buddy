package ink.trmnl.android.buddy.data.battery

import ink.trmnl.android.buddy.data.database.BatteryHistoryEntity
import java.util.concurrent.TimeUnit

/**
 * Analyzer for battery history data to detect charging events and stale data.
 *
 * This class provides utility functions to analyze battery history readings and determine
 * if the data contains charging events or if the data is too old to be useful.
 */
object BatteryHistoryAnalyzer {
    /**
     * Threshold for detecting a charging event between consecutive battery readings.
     * A jump of more than 50% between readings indicates the device was charged.
     */
    private const val CHARGING_THRESHOLD_PERCENT = 50.0

    /**
     * Threshold for considering battery history data as stale.
     * Data older than 6 months (approximately 182.5 days) is considered outdated.
     */
    private val STALE_DATA_THRESHOLD_MILLIS = TimeUnit.DAYS.toMillis(183) // ~6 months

    /**
     * Detects if there are any charging events in the battery history.
     *
     * A charging event is detected when the battery percentage increases by more than
     * [CHARGING_THRESHOLD_PERCENT] between consecutive readings.
     *
     * @param batteryHistory List of battery history readings, ordered by timestamp
     * @return true if a charging event (>50% battery jump) is detected, false otherwise
     */
    fun hasChargingEvent(batteryHistory: List<BatteryHistoryEntity>): Boolean {
        if (batteryHistory.size < 2) return false

        // Sort by timestamp to ensure chronological order
        val sortedHistory = batteryHistory.sortedBy { it.timestamp }

        for (i in 1 until sortedHistory.size) {
            val previous = sortedHistory[i - 1]
            val current = sortedHistory[i]
            val batteryChange = current.percentCharged - previous.percentCharged

            if (batteryChange > CHARGING_THRESHOLD_PERCENT) {
                return true
            }
        }

        return false
    }

    /**
     * Checks if the battery history contains data older than 6 months.
     *
     * @param batteryHistory List of battery history readings
     * @param currentTimeMillis Current time in milliseconds (defaults to System.currentTimeMillis())
     * @return true if the oldest reading is older than 6 months, false otherwise
     */
    fun hasStaleData(
        batteryHistory: List<BatteryHistoryEntity>,
        currentTimeMillis: Long = System.currentTimeMillis(),
    ): Boolean {
        if (batteryHistory.isEmpty()) return false

        val oldestTimestamp = batteryHistory.minOf { it.timestamp }
        val ageMillis = currentTimeMillis - oldestTimestamp

        return ageMillis > STALE_DATA_THRESHOLD_MILLIS
    }

    /**
     * Determines if battery history should be cleared.
     *
     * History should be cleared if either:
     * - A charging event (>50% battery jump) is detected
     * - The oldest data point is more than 6 months old
     *
     * @param batteryHistory List of battery history readings
     * @param currentTimeMillis Current time in milliseconds (defaults to System.currentTimeMillis())
     * @return true if history should be cleared, false otherwise
     */
    fun shouldClearHistory(
        batteryHistory: List<BatteryHistoryEntity>,
        currentTimeMillis: Long = System.currentTimeMillis(),
    ): Boolean = hasChargingEvent(batteryHistory) || hasStaleData(batteryHistory, currentTimeMillis)

    /**
     * Gets the reason why battery history should be cleared.
     *
     * @param batteryHistory List of battery history readings
     * @param currentTimeMillis Current time in milliseconds (defaults to System.currentTimeMillis())
     * @return A [ClearHistoryReason] indicating why history should be cleared, or null if it shouldn't
     */
    fun getClearHistoryReason(
        batteryHistory: List<BatteryHistoryEntity>,
        currentTimeMillis: Long = System.currentTimeMillis(),
    ): ClearHistoryReason? {
        val hasCharging = hasChargingEvent(batteryHistory)
        val hasStale = hasStaleData(batteryHistory, currentTimeMillis)

        return when {
            hasCharging && hasStale -> ClearHistoryReason.BOTH
            hasCharging -> ClearHistoryReason.CHARGING_DETECTED
            hasStale -> ClearHistoryReason.STALE_DATA
            else -> null
        }
    }

    /**
     * Enum representing the reason for clearing battery history.
     */
    enum class ClearHistoryReason {
        /** Battery history contains a charging event (>50% jump). */
        CHARGING_DETECTED,

        /** Battery history contains data older than 6 months. */
        STALE_DATA,

        /** Battery history has both charging events and stale data. */
        BOTH,
    }
}
