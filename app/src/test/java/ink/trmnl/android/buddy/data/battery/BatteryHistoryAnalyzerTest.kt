package ink.trmnl.android.buddy.data.battery

import assertk.assertThat
import assertk.assertions.isCloseTo
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

    // ========== Battery Prediction Tests ==========

    @Test
    fun `predictBatteryDepletion returns null for empty list`() {
        // Given
        val batteryHistory = emptyList<BatteryHistoryEntity>()

        // When
        val result = BatteryHistoryAnalyzer.predictBatteryDepletion(batteryHistory)

        // Then
        assertThat(result).isNull()
    }

    @Test
    fun `predictBatteryDepletion returns null for less than 3 entries`() {
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
            )

        // When
        val result = BatteryHistoryAnalyzer.predictBatteryDepletion(batteryHistory)

        // Then
        assertThat(result).isNull()
    }

    @Test
    fun `predictBatteryDepletion returns null when less than 3 drainage points after filtering`() {
        // Given - 4 total points but only 2 drainage points due to charging spike
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
                    percentCharged = 95.0, // Charging spike
                    batteryVoltage = 3.8,
                    timestamp = 3000L,
                ),
                BatteryHistoryEntity(
                    deviceId = "ABC-123",
                    percentCharged = 90.0,
                    batteryVoltage = 3.75,
                    timestamp = 4000L,
                ),
            )

        // When
        val result = BatteryHistoryAnalyzer.predictBatteryDepletion(batteryHistory)

        // Then
        assertThat(result).isNull()
    }

    @Test
    fun `predictBatteryDepletion calculates prediction for linear battery drain`() {
        // Given - Battery draining from 90% to 60% over 30 days
        // Expected: ~90 days until 0% (draining 1% per day)
        val currentTime = System.currentTimeMillis()
        val batteryHistory =
            listOf(
                BatteryHistoryEntity(
                    deviceId = "ABC-123",
                    percentCharged = 90.0,
                    batteryVoltage = 3.8,
                    timestamp = currentTime - TimeUnit.DAYS.toMillis(30),
                ),
                BatteryHistoryEntity(
                    deviceId = "ABC-123",
                    percentCharged = 75.0,
                    batteryVoltage = 3.7,
                    timestamp = currentTime - TimeUnit.DAYS.toMillis(15),
                ),
                BatteryHistoryEntity(
                    deviceId = "ABC-123",
                    percentCharged = 60.0,
                    batteryVoltage = 3.6,
                    timestamp = currentTime,
                ),
            )

        // When
        val result = BatteryHistoryAnalyzer.predictBatteryDepletion(batteryHistory, currentTime)

        // Then
        assertThat(result).isNotNull()
        assertThat(result!!.dataPointsUsed).isEqualTo(3)
        // Should predict roughly 60 more days (current 60% at 1%/day rate)
        val expectedDaysRemaining = 60.0
        val actualDaysRemaining =
            (result.depletionTimeMillis - currentTime) / (1000.0 * 60 * 60 * 24)
        assertThat(actualDaysRemaining).isCloseTo(expectedDaysRemaining, 2.0) // Within 2 days tolerance
    }

    @Test
    fun `predictBatteryDepletion filters out charging spikes and uses longest drainage sequence`() {
        // Given - Battery drains normally, then has a charging spike, then drains again
        val currentTime = System.currentTimeMillis()
        val batteryHistory =
            listOf(
                // First drainage sequence: 80% -> 70% -> 40% (3 points)
                BatteryHistoryEntity(
                    deviceId = "ABC-123",
                    percentCharged = 80.0,
                    batteryVoltage = 3.7,
                    timestamp = currentTime - TimeUnit.DAYS.toMillis(60),
                ),
                BatteryHistoryEntity(
                    deviceId = "ABC-123",
                    percentCharged = 70.0,
                    batteryVoltage = 3.6,
                    timestamp = currentTime - TimeUnit.DAYS.toMillis(50),
                ),
                BatteryHistoryEntity(
                    deviceId = "ABC-123",
                    percentCharged = 40.0,
                    batteryVoltage = 3.4,
                    timestamp = currentTime - TimeUnit.DAYS.toMillis(40),
                ),
                // Charging spike detected (40% -> 95% = 55% jump > 50% threshold)
                BatteryHistoryEntity(
                    deviceId = "ABC-123",
                    percentCharged = 95.0, // Charging spike
                    batteryVoltage = 3.8,
                    timestamp = currentTime - TimeUnit.DAYS.toMillis(30),
                ),
                // Second drainage sequence: 95% -> 90% -> 85% -> 80% (4 points - longest)
                BatteryHistoryEntity(
                    deviceId = "ABC-123",
                    percentCharged = 90.0,
                    batteryVoltage = 3.75,
                    timestamp = currentTime - TimeUnit.DAYS.toMillis(20),
                ),
                BatteryHistoryEntity(
                    deviceId = "ABC-123",
                    percentCharged = 85.0,
                    batteryVoltage = 3.7,
                    timestamp = currentTime - TimeUnit.DAYS.toMillis(10),
                ),
                BatteryHistoryEntity(
                    deviceId = "ABC-123",
                    percentCharged = 80.0,
                    batteryVoltage = 3.65,
                    timestamp = currentTime,
                ),
            )

        // When
        val result = BatteryHistoryAnalyzer.predictBatteryDepletion(batteryHistory, currentTime)

        // Then - Should use the longest drainage sequence: 95% -> 90% -> 85% -> 80% (4 points)
        assertThat(result).isNotNull()
        assertThat(result!!.dataPointsUsed).isEqualTo(4)
    }

    @Test
    fun `predictBatteryDepletion returns null for positive slope (battery increasing)`() {
        // Given - Battery actually increasing (shouldn't happen in drainage scenario)
        val currentTime = System.currentTimeMillis()
        val batteryHistory =
            listOf(
                BatteryHistoryEntity(
                    deviceId = "ABC-123",
                    percentCharged = 60.0,
                    batteryVoltage = 3.6,
                    timestamp = currentTime - TimeUnit.DAYS.toMillis(30),
                ),
                BatteryHistoryEntity(
                    deviceId = "ABC-123",
                    percentCharged = 60.0,
                    batteryVoltage = 3.6,
                    timestamp = currentTime - TimeUnit.DAYS.toMillis(15),
                ),
                BatteryHistoryEntity(
                    deviceId = "ABC-123",
                    percentCharged = 60.0,
                    batteryVoltage = 3.6,
                    timestamp = currentTime,
                ),
            )

        // When
        val result = BatteryHistoryAnalyzer.predictBatteryDepletion(batteryHistory, currentTime)

        // Then
        assertThat(result).isNull()
    }

    @Test
    fun `predictBatteryDepletion returns null for unrealistic future prediction`() {
        // Given - Very slow drain that would predict > 5 years
        val currentTime = System.currentTimeMillis()
        val batteryHistory =
            listOf(
                BatteryHistoryEntity(
                    deviceId = "ABC-123",
                    percentCharged = 100.0,
                    batteryVoltage = 3.9,
                    timestamp = currentTime - TimeUnit.DAYS.toMillis(30),
                ),
                BatteryHistoryEntity(
                    deviceId = "ABC-123",
                    percentCharged = 99.5,
                    batteryVoltage = 3.9,
                    timestamp = currentTime - TimeUnit.DAYS.toMillis(15),
                ),
                BatteryHistoryEntity(
                    deviceId = "ABC-123",
                    percentCharged = 99.0,
                    batteryVoltage = 3.9,
                    timestamp = currentTime,
                ),
            )

        // When
        val result = BatteryHistoryAnalyzer.predictBatteryDepletion(batteryHistory, currentTime)

        // Then - Should be null because prediction is too far in future (unrealistic)
        assertThat(result).isNull()
    }

    @Test
    fun `predictBatteryDepletion works with unsorted data`() {
        // Given - Unsorted battery history
        val currentTime = System.currentTimeMillis()
        val batteryHistory =
            listOf(
                BatteryHistoryEntity(
                    deviceId = "ABC-123",
                    percentCharged = 60.0,
                    batteryVoltage = 3.6,
                    timestamp = currentTime,
                ),
                BatteryHistoryEntity(
                    deviceId = "ABC-123",
                    percentCharged = 90.0,
                    batteryVoltage = 3.8,
                    timestamp = currentTime - TimeUnit.DAYS.toMillis(30),
                ),
                BatteryHistoryEntity(
                    deviceId = "ABC-123",
                    percentCharged = 75.0,
                    batteryVoltage = 3.7,
                    timestamp = currentTime - TimeUnit.DAYS.toMillis(15),
                ),
            )

        // When
        val result = BatteryHistoryAnalyzer.predictBatteryDepletion(batteryHistory, currentTime)

        // Then - Should sort and calculate prediction correctly
        assertThat(result).isNotNull()
        assertThat(result!!.dataPointsUsed).isEqualTo(3)
    }

    @Test
    fun `formatTimeRemaining shows months weeks and days correctly`() {
        // Given - 2 months, 3 weeks, 4 days in future (2*30 + 3*7 + 4 = 85 days)
        val currentTime = System.currentTimeMillis()
        val depletionTime = currentTime + TimeUnit.DAYS.toMillis(85)
        val prediction =
            BatteryHistoryAnalyzer.BatteryPrediction(
                depletionTimeMillis = depletionTime,
                drainageRatePercentPerDay = 1.0,
                dataPointsUsed = 5,
            )

        // When
        val formatted = prediction.formatTimeRemaining(currentTime)

        // Then
        assertThat(formatted).isEqualTo("2 months, 3 weeks, 4 days")
    }

    @Test
    fun `formatTimeRemaining shows only weeks and days when less than 1 month`() {
        // Given - 2 weeks, 3 days (17 days)
        val currentTime = System.currentTimeMillis()
        val depletionTime = currentTime + TimeUnit.DAYS.toMillis(17)
        val prediction =
            BatteryHistoryAnalyzer.BatteryPrediction(
                depletionTimeMillis = depletionTime,
                drainageRatePercentPerDay = 2.0,
                dataPointsUsed = 4,
            )

        // When
        val formatted = prediction.formatTimeRemaining(currentTime)

        // Then
        assertThat(formatted).isEqualTo("2 weeks, 3 days")
    }

    @Test
    fun `formatTimeRemaining shows only days when less than 1 week`() {
        // Given - 5 days
        val currentTime = System.currentTimeMillis()
        val depletionTime = currentTime + TimeUnit.DAYS.toMillis(5)
        val prediction =
            BatteryHistoryAnalyzer.BatteryPrediction(
                depletionTimeMillis = depletionTime,
                drainageRatePercentPerDay = 3.0,
                dataPointsUsed = 6,
            )

        // When
        val formatted = prediction.formatTimeRemaining(currentTime)

        // Then
        assertThat(formatted).isEqualTo("5 days")
    }

    @Test
    fun `formatTimeRemaining shows singular forms correctly`() {
        // Given - 1 month, 1 week, 1 day (38 days)
        val currentTime = System.currentTimeMillis()
        val depletionTime = currentTime + TimeUnit.DAYS.toMillis(38)
        val prediction =
            BatteryHistoryAnalyzer.BatteryPrediction(
                depletionTimeMillis = depletionTime,
                drainageRatePercentPerDay = 1.5,
                dataPointsUsed = 3,
            )

        // When
        val formatted = prediction.formatTimeRemaining(currentTime)

        // Then
        assertThat(formatted).isEqualTo("1 month, 1 week, 1 day")
    }

    @Test
    fun `formatTimeRemaining omits days when exactly on week boundary`() {
        // Given - Exactly 2 weeks (14 days)
        val currentTime = System.currentTimeMillis()
        val depletionTime = currentTime + TimeUnit.DAYS.toMillis(14)
        val prediction =
            BatteryHistoryAnalyzer.BatteryPrediction(
                depletionTimeMillis = depletionTime,
                drainageRatePercentPerDay = 2.5,
                dataPointsUsed = 4,
            )

        // When
        val formatted = prediction.formatTimeRemaining(currentTime)

        // Then - Should omit "0 days" for cleaner display
        assertThat(formatted).isEqualTo("2 weeks")
    }

    @Test
    fun `formatTimeRemaining returns depleted message for past time`() {
        // Given - Depletion time in the past
        val currentTime = System.currentTimeMillis()
        val depletionTime = currentTime - TimeUnit.DAYS.toMillis(5)
        val prediction =
            BatteryHistoryAnalyzer.BatteryPrediction(
                depletionTimeMillis = depletionTime,
                drainageRatePercentPerDay = 5.0,
                dataPointsUsed = 3,
            )

        // When
        val formatted = prediction.formatTimeRemaining(currentTime)

        // Then
        assertThat(formatted).isEqualTo("Battery depleted")
    }
}
