package ink.trmnl.android.buddy.data.battery

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import ink.trmnl.android.buddy.data.database.BatteryHistoryEntity
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * Unit tests for [BatteryHistoryAnalyzer].
 */
class BatteryHistoryAnalyzerTest {
    @Test
    fun `hasChargingEvent returns false for empty list`() {
        // Given
        val batteryHistory = emptyList<BatteryHistoryEntity>()

        // When
        val result = BatteryHistoryAnalyzer.hasChargingEvent(batteryHistory)

        // Then
        assertThat(result).isFalse()
    }

    @Test
    fun `hasChargingEvent returns false for single entry`() {
        // Given
        val batteryHistory =
            listOf(
                BatteryHistoryEntity(
                    deviceId = "ABC-123",
                    percentCharged = 80.0,
                    batteryVoltage = 3.7,
                    timestamp = 1000L,
                ),
            )

        // When
        val result = BatteryHistoryAnalyzer.hasChargingEvent(batteryHistory)

        // Then
        assertThat(result).isFalse()
    }

    @Test
    fun `hasChargingEvent returns false for gradual battery drain`() {
        // Given
        val batteryHistory =
            listOf(
                BatteryHistoryEntity(
                    deviceId = "ABC-123",
                    percentCharged = 80.0,
                    batteryVoltage = 3.7,
                    timestamp = 1000L,
                ),
                BatteryHistoryEntity(
                    deviceId = "ABC-123",
                    percentCharged = 75.0,
                    batteryVoltage = 3.6,
                    timestamp = 2000L,
                ),
                BatteryHistoryEntity(
                    deviceId = "ABC-123",
                    percentCharged = 70.0,
                    batteryVoltage = 3.5,
                    timestamp = 3000L,
                ),
            )

        // When
        val result = BatteryHistoryAnalyzer.hasChargingEvent(batteryHistory)

        // Then
        assertThat(result).isFalse()
    }

    @Test
    fun `hasChargingEvent returns true for charging event (more than 50 percent jump)`() {
        // Given
        val batteryHistory =
            listOf(
                BatteryHistoryEntity(
                    deviceId = "ABC-123",
                    percentCharged = 30.0,
                    batteryVoltage = 3.3,
                    timestamp = 1000L,
                ),
                BatteryHistoryEntity(
                    deviceId = "ABC-123",
                    percentCharged = 85.0, // 55% jump
                    batteryVoltage = 3.7,
                    timestamp = 2000L,
                ),
            )

        // When
        val result = BatteryHistoryAnalyzer.hasChargingEvent(batteryHistory)

        // Then
        assertThat(result).isTrue()
    }

    @Test
    fun `hasChargingEvent returns true for exactly 51 percent jump`() {
        // Given
        val batteryHistory =
            listOf(
                BatteryHistoryEntity(
                    deviceId = "ABC-123",
                    percentCharged = 30.0,
                    batteryVoltage = 3.3,
                    timestamp = 1000L,
                ),
                BatteryHistoryEntity(
                    deviceId = "ABC-123",
                    percentCharged = 81.0, // Exactly 51% jump
                    batteryVoltage = 3.7,
                    timestamp = 2000L,
                ),
            )

        // When
        val result = BatteryHistoryAnalyzer.hasChargingEvent(batteryHistory)

        // Then
        assertThat(result).isTrue()
    }

    @Test
    fun `hasChargingEvent returns false for exactly 50 percent jump`() {
        // Given
        val batteryHistory =
            listOf(
                BatteryHistoryEntity(
                    deviceId = "ABC-123",
                    percentCharged = 30.0,
                    batteryVoltage = 3.3,
                    timestamp = 1000L,
                ),
                BatteryHistoryEntity(
                    deviceId = "ABC-123",
                    percentCharged = 80.0, // Exactly 50% jump - at threshold
                    batteryVoltage = 3.7,
                    timestamp = 2000L,
                ),
            )

        // When
        val result = BatteryHistoryAnalyzer.hasChargingEvent(batteryHistory)

        // Then
        assertThat(result).isFalse()
    }

    @Test
    fun `hasChargingEvent works with unsorted data`() {
        // Given - timestamps are not in order
        val batteryHistory =
            listOf(
                BatteryHistoryEntity(
                    deviceId = "ABC-123",
                    percentCharged = 85.0,
                    batteryVoltage = 3.7,
                    timestamp = 3000L,
                ),
                BatteryHistoryEntity(
                    deviceId = "ABC-123",
                    percentCharged = 30.0,
                    batteryVoltage = 3.3,
                    timestamp = 1000L,
                ),
                BatteryHistoryEntity(
                    deviceId = "ABC-123",
                    percentCharged = 20.0,
                    batteryVoltage = 3.2,
                    timestamp = 2000L,
                ),
            )

        // When
        val result = BatteryHistoryAnalyzer.hasChargingEvent(batteryHistory)

        // Then - Should detect charging between timestamp 2000 and 3000 (20% -> 85% = 65% jump)
        assertThat(result).isTrue()
    }

    @Test
    fun `hasStaleData returns false for empty list`() {
        // Given
        val batteryHistory = emptyList<BatteryHistoryEntity>()

        // When
        val result = BatteryHistoryAnalyzer.hasStaleData(batteryHistory)

        // Then
        assertThat(result).isFalse()
    }

    @Test
    fun `hasStaleData returns false for recent data`() {
        // Given - data from 30 days ago
        val currentTime = System.currentTimeMillis()
        val thirtyDaysAgo = currentTime - TimeUnit.DAYS.toMillis(30)
        val batteryHistory =
            listOf(
                BatteryHistoryEntity(
                    deviceId = "ABC-123",
                    percentCharged = 80.0,
                    batteryVoltage = 3.7,
                    timestamp = thirtyDaysAgo,
                ),
            )

        // When
        val result = BatteryHistoryAnalyzer.hasStaleData(batteryHistory, currentTime)

        // Then
        assertThat(result).isFalse()
    }

    @Test
    fun `hasStaleData returns true for data older than 6 months`() {
        // Given - data from 200 days ago (more than 6 months)
        val currentTime = System.currentTimeMillis()
        val twoHundredDaysAgo = currentTime - TimeUnit.DAYS.toMillis(200)
        val batteryHistory =
            listOf(
                BatteryHistoryEntity(
                    deviceId = "ABC-123",
                    percentCharged = 80.0,
                    batteryVoltage = 3.7,
                    timestamp = twoHundredDaysAgo,
                ),
            )

        // When
        val result = BatteryHistoryAnalyzer.hasStaleData(batteryHistory, currentTime)

        // Then
        assertThat(result).isTrue()
    }

    @Test
    fun `hasStaleData returns false for data exactly at 183 days`() {
        // Given - data from exactly 183 days ago (threshold)
        val currentTime = System.currentTimeMillis()
        val exactlyThreshold = currentTime - TimeUnit.DAYS.toMillis(183)
        val batteryHistory =
            listOf(
                BatteryHistoryEntity(
                    deviceId = "ABC-123",
                    percentCharged = 80.0,
                    batteryVoltage = 3.7,
                    timestamp = exactlyThreshold,
                ),
            )

        // When
        val result = BatteryHistoryAnalyzer.hasStaleData(batteryHistory, currentTime)

        // Then - Should be false since it's not > threshold
        assertThat(result).isFalse()
    }

    @Test
    fun `hasStaleData checks oldest entry when multiple entries exist`() {
        // Given - oldest entry is 200 days ago, newest is 10 days ago
        val currentTime = System.currentTimeMillis()
        val twoHundredDaysAgo = currentTime - TimeUnit.DAYS.toMillis(200)
        val tenDaysAgo = currentTime - TimeUnit.DAYS.toMillis(10)
        val batteryHistory =
            listOf(
                BatteryHistoryEntity(
                    deviceId = "ABC-123",
                    percentCharged = 90.0,
                    batteryVoltage = 3.8,
                    timestamp = tenDaysAgo,
                ),
                BatteryHistoryEntity(
                    deviceId = "ABC-123",
                    percentCharged = 80.0,
                    batteryVoltage = 3.7,
                    timestamp = twoHundredDaysAgo,
                ),
            )

        // When
        val result = BatteryHistoryAnalyzer.hasStaleData(batteryHistory, currentTime)

        // Then - Should be true because oldest entry is > 6 months
        assertThat(result).isTrue()
    }

    @Test
    fun `shouldClearHistory returns false for empty list`() {
        // Given
        val batteryHistory = emptyList<BatteryHistoryEntity>()

        // When
        val result = BatteryHistoryAnalyzer.shouldClearHistory(batteryHistory)

        // Then
        assertThat(result).isFalse()
    }

    @Test
    fun `shouldClearHistory returns true when charging event detected`() {
        // Given
        val batteryHistory =
            listOf(
                BatteryHistoryEntity(
                    deviceId = "ABC-123",
                    percentCharged = 30.0,
                    batteryVoltage = 3.3,
                    timestamp = 1000L,
                ),
                BatteryHistoryEntity(
                    deviceId = "ABC-123",
                    percentCharged = 85.0, // 55% jump
                    batteryVoltage = 3.7,
                    timestamp = 2000L,
                ),
            )

        // When
        val result = BatteryHistoryAnalyzer.shouldClearHistory(batteryHistory)

        // Then
        assertThat(result).isTrue()
    }

    @Test
    fun `shouldClearHistory returns true when data is stale`() {
        // Given - data from 200 days ago
        val currentTime = System.currentTimeMillis()
        val twoHundredDaysAgo = currentTime - TimeUnit.DAYS.toMillis(200)
        val batteryHistory =
            listOf(
                BatteryHistoryEntity(
                    deviceId = "ABC-123",
                    percentCharged = 80.0,
                    batteryVoltage = 3.7,
                    timestamp = twoHundredDaysAgo,
                ),
            )

        // When
        val result = BatteryHistoryAnalyzer.shouldClearHistory(batteryHistory, currentTime)

        // Then
        assertThat(result).isTrue()
    }

    @Test
    fun `shouldClearHistory returns false for good data`() {
        // Given - recent data with no charging
        val currentTime = System.currentTimeMillis()
        val thirtyDaysAgo = currentTime - TimeUnit.DAYS.toMillis(30)
        val batteryHistory =
            listOf(
                BatteryHistoryEntity(
                    deviceId = "ABC-123",
                    percentCharged = 80.0,
                    batteryVoltage = 3.7,
                    timestamp = thirtyDaysAgo,
                ),
                BatteryHistoryEntity(
                    deviceId = "ABC-123",
                    percentCharged = 75.0,
                    batteryVoltage = 3.6,
                    timestamp = currentTime - TimeUnit.DAYS.toMillis(20),
                ),
            )

        // When
        val result = BatteryHistoryAnalyzer.shouldClearHistory(batteryHistory, currentTime)

        // Then
        assertThat(result).isFalse()
    }

    @Test
    fun `getClearHistoryReason returns null for good data`() {
        // Given - recent data with no charging
        val currentTime = System.currentTimeMillis()
        val thirtyDaysAgo = currentTime - TimeUnit.DAYS.toMillis(30)
        val batteryHistory =
            listOf(
                BatteryHistoryEntity(
                    deviceId = "ABC-123",
                    percentCharged = 80.0,
                    batteryVoltage = 3.7,
                    timestamp = thirtyDaysAgo,
                ),
            )

        // When
        val result = BatteryHistoryAnalyzer.getClearHistoryReason(batteryHistory, currentTime)

        // Then
        assertThat(result).isNull()
    }

    @Test
    fun `getClearHistoryReason returns CHARGING_DETECTED when charging event found`() {
        // Given - recent data with charging event
        val currentTime = System.currentTimeMillis()
        val thirtyDaysAgo = currentTime - TimeUnit.DAYS.toMillis(30)
        val twentyDaysAgo = currentTime - TimeUnit.DAYS.toMillis(20)
        val batteryHistory =
            listOf(
                BatteryHistoryEntity(
                    deviceId = "ABC-123",
                    percentCharged = 30.0,
                    batteryVoltage = 3.3,
                    timestamp = thirtyDaysAgo,
                ),
                BatteryHistoryEntity(
                    deviceId = "ABC-123",
                    percentCharged = 85.0, // 55% jump
                    batteryVoltage = 3.7,
                    timestamp = twentyDaysAgo,
                ),
            )

        // When
        val result = BatteryHistoryAnalyzer.getClearHistoryReason(batteryHistory, currentTime)

        // Then
        assertThat(result).isNotNull()
        assertThat(result).isEqualTo(BatteryHistoryAnalyzer.ClearHistoryReason.CHARGING_DETECTED)
    }

    @Test
    fun `getClearHistoryReason returns STALE_DATA when data is old`() {
        // Given - data from 200 days ago
        val currentTime = System.currentTimeMillis()
        val twoHundredDaysAgo = currentTime - TimeUnit.DAYS.toMillis(200)
        val batteryHistory =
            listOf(
                BatteryHistoryEntity(
                    deviceId = "ABC-123",
                    percentCharged = 80.0,
                    batteryVoltage = 3.7,
                    timestamp = twoHundredDaysAgo,
                ),
                BatteryHistoryEntity(
                    deviceId = "ABC-123",
                    percentCharged = 75.0,
                    batteryVoltage = 3.6,
                    timestamp = twoHundredDaysAgo + 1000L,
                ),
            )

        // When
        val result = BatteryHistoryAnalyzer.getClearHistoryReason(batteryHistory, currentTime)

        // Then
        assertThat(result).isNotNull()
        assertThat(result).isEqualTo(BatteryHistoryAnalyzer.ClearHistoryReason.STALE_DATA)
    }

    @Test
    fun `getClearHistoryReason returns BOTH when charging event and stale data found`() {
        // Given - old data with charging event
        val currentTime = System.currentTimeMillis()
        val twoHundredDaysAgo = currentTime - TimeUnit.DAYS.toMillis(200)
        val batteryHistory =
            listOf(
                BatteryHistoryEntity(
                    deviceId = "ABC-123",
                    percentCharged = 30.0,
                    batteryVoltage = 3.3,
                    timestamp = twoHundredDaysAgo,
                ),
                BatteryHistoryEntity(
                    deviceId = "ABC-123",
                    percentCharged = 85.0, // 55% jump
                    batteryVoltage = 3.7,
                    timestamp = twoHundredDaysAgo + 1000L,
                ),
            )

        // When
        val result = BatteryHistoryAnalyzer.getClearHistoryReason(batteryHistory, currentTime)

        // Then
        assertThat(result).isNotNull()
        assertThat(result).isEqualTo(BatteryHistoryAnalyzer.ClearHistoryReason.BOTH)
    }
}
