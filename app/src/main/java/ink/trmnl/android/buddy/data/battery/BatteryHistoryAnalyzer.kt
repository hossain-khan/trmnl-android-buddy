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

    /**
     * Predicts when the battery will run out based on historical drainage trend.
     *
     * This function:
     * 1. Filters out charging spikes (only considers decreasing battery points)
     * 2. Requires at least 3 data points for a meaningful prediction
     * 3. Uses linear regression to calculate the drainage rate
     * 4. Predicts when battery will reach 0%
     *
     * @param batteryHistory List of battery history readings
     * @param currentTimeMillis Current time in milliseconds (defaults to System.currentTimeMillis())
     * @return [BatteryPrediction] if prediction can be made (≥3 drainage points), null otherwise
     */
    fun predictBatteryDepletion(
        batteryHistory: List<BatteryHistoryEntity>,
        currentTimeMillis: Long = System.currentTimeMillis(),
    ): BatteryPrediction? {
        if (batteryHistory.size < 3) return null

        // Sort by timestamp to ensure chronological order
        val sortedHistory = batteryHistory.sortedBy { it.timestamp }

        // Filter to only keep decreasing battery points (ignore charging spikes)
        // When a charging spike is detected (>50% jump), start a new drainage sequence
        val drainagePoints = mutableListOf<BatteryHistoryEntity>()
        var currentSequence = mutableListOf<BatteryHistoryEntity>()
        currentSequence.add(sortedHistory[0])

        for (i in 1 until sortedHistory.size) {
            val previous = sortedHistory[i - 1]
            val current = sortedHistory[i]
            val batteryChange = current.percentCharged - previous.percentCharged

            if (batteryChange > CHARGING_THRESHOLD_PERCENT) {
                // Charging detected - save current sequence if it's the longest
                if (currentSequence.size > drainagePoints.size) {
                    drainagePoints.clear()
                    drainagePoints.addAll(currentSequence)
                }
                // Start new sequence from this charged point
                currentSequence.clear()
                currentSequence.add(current)
            } else if (current.percentCharged <= currentSequence.last().percentCharged) {
                // Continue drainage - only add if battery decreased or stayed the same
                currentSequence.add(current)
            }
        }

        // Check if final sequence is the longest
        if (currentSequence.size > drainagePoints.size) {
            drainagePoints.clear()
            drainagePoints.addAll(currentSequence)
        }

        // Need at least 3 drainage points for a meaningful prediction
        if (drainagePoints.size < 3) return null

        // Perform linear regression on drainage points
        // Convert timestamps to days since first reading for easier calculation
        val firstTimestamp = drainagePoints.first().timestamp
        val dataPoints =
            drainagePoints.map { entity ->
                val daysElapsed = (entity.timestamp - firstTimestamp) / (1000.0 * 60 * 60 * 24)
                Pair(daysElapsed, entity.percentCharged)
            }

        // Calculate linear regression: y = mx + b
        val n = dataPoints.size
        val sumX = dataPoints.sumOf { it.first }
        val sumY = dataPoints.sumOf { it.second }
        val sumXY = dataPoints.sumOf { it.first * it.second }
        val sumX2 = dataPoints.sumOf { it.first * it.first }

        val slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX)
        val intercept = (sumY - slope * sumX) / n

        // If slope is positive or zero, battery is not draining (unusual case)
        if (slope >= 0) return null

        // Calculate when battery will reach 0%
        // 0 = slope * x + intercept
        // x = -intercept / slope
        val daysUntilDepleted = -intercept / slope

        // If prediction is in the past or negative, return null
        if (daysUntilDepleted <= 0) return null

        // Convert to milliseconds from current time
        val currentDaysElapsed = (currentTimeMillis - firstTimestamp) / (1000.0 * 60 * 60 * 24)
        val remainingDays = daysUntilDepleted - currentDaysElapsed

        // If remaining time is negative or too far in future (unrealistic), return null
        if (remainingDays <= 0 || remainingDays > 1825) return null // Max 5 years

        val depletionTimeMillis = currentTimeMillis + (remainingDays * 24 * 60 * 60 * 1000).toLong()

        return BatteryPrediction(
            depletionTimeMillis = depletionTimeMillis,
            drainageRatePercentPerDay = -slope, // Positive value for display
            dataPointsUsed = drainagePoints.size,
        )
    }

    /**
     * Data class representing battery depletion prediction.
     *
     * @property depletionTimeMillis Predicted timestamp when battery will reach 0%
     * @property drainageRatePercentPerDay Average battery drainage rate (% per day)
     * @property dataPointsUsed Number of drainage data points used for prediction
     */
    data class BatteryPrediction(
        val depletionTimeMillis: Long,
        val drainageRatePercentPerDay: Double,
        val dataPointsUsed: Int,
    ) {
        /**
         * Formats the time remaining until battery depletion in a human-readable format.
         *
         * Returns format: "X months, Y weeks, Z days" where:
         * - Months are shown if >= 1 month
         * - Weeks are shown if >= 1 week (remaining after months)
         * - Days are always shown (remaining after months and weeks)
         *
         * @param currentTimeMillis Current time in milliseconds
         * @return Formatted string like "2 months, 3 weeks, 4 days"
         */
        fun formatTimeRemaining(currentTimeMillis: Long = System.currentTimeMillis()): String {
            val remainingMillis = depletionTimeMillis - currentTimeMillis
            if (remainingMillis <= 0) return "Battery depleted"

            val totalDays = (remainingMillis / (1000.0 * 60 * 60 * 24)).toInt()

            val months = totalDays / 30
            val remainingAfterMonths = totalDays % 30
            val weeks = remainingAfterMonths / 7
            val days = remainingAfterMonths % 7

            val parts = mutableListOf<String>()
            if (months > 0) {
                parts.add("$months ${if (months == 1) "month" else "months"}")
            }
            if (weeks > 0) {
                parts.add("$weeks ${if (weeks == 1) "week" else "weeks"}")
            }
            if (days > 0 || parts.isEmpty()) {
                parts.add("$days ${if (days == 1) "day" else "days"}")
            }

            return parts.joinToString(", ")
        }
    }
}
