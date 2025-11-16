package ink.trmnl.android.buddy.fakes

import ink.trmnl.android.buddy.data.database.BatteryHistoryEntity
import ink.trmnl.android.buddy.data.database.BatteryHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Fake implementation of [BatteryHistoryRepository] for testing.
 *
 * This fake provides a working in-memory implementation suitable for unit tests,
 * following the project's testing guidelines of using fakes instead of mocks.
 *
 * Uses MutableStateFlow for reactive updates - when data changes, all collectors
 * automatically receive the new values.
 *
 * Exposes properties for test assertions to verify repository methods were called
 * with expected values.
 *
 * @param initialHistory Optional list of battery history entities to pre-populate the repository
 * @param shouldThrowOnRecord If true, throws an exception when recording readings
 */
class FakeBatteryHistoryRepository(
    initialHistory: List<BatteryHistoryEntity> = emptyList(),
    private val shouldThrowOnRecord: Boolean = false,
) : BatteryHistoryRepository {
    private val batteryHistoryFlow = MutableStateFlow(initialHistory.toList())
    private var nextId = (initialHistory.maxOfOrNull { it.id } ?: 0L) + 1L

    /**
     * Test-visible list of recorded battery readings.
     */
    val recordedReadings: List<BatteryHistoryEntity>
        get() = batteryHistoryFlow.value

    /**
     * Clear all recorded readings. Useful for test setup.
     */
    fun clear() {
        batteryHistoryFlow.value = emptyList()
        nextId = 1L
    }

    override suspend fun recordBatteryReading(
        deviceId: String,
        percentCharged: Double,
        batteryVoltage: Double?,
        timestamp: Long,
    ) {
        if (shouldThrowOnRecord) {
            throw Exception("Test exception for recordBatteryReading")
        }

        val entity =
            BatteryHistoryEntity(
                id = nextId++,
                deviceId = deviceId,
                percentCharged = percentCharged,
                batteryVoltage = batteryVoltage,
                timestamp = timestamp,
            )
        batteryHistoryFlow.update { current -> current + entity }
    }

    override fun getBatteryHistoryForDevice(deviceId: String): Flow<List<BatteryHistoryEntity>> =
        batteryHistoryFlow.map { history ->
            history
                .filter { it.deviceId == deviceId }
                .sortedByDescending { it.timestamp }
        }

    override suspend fun getLatestBatteryReading(deviceId: String): BatteryHistoryEntity? =
        batteryHistoryFlow.value
            .filter { it.deviceId == deviceId }
            .maxByOrNull { it.timestamp }

    override fun getBatteryHistoryInRange(
        deviceId: String,
        startTime: Long,
        endTime: Long,
    ): Flow<List<BatteryHistoryEntity>> =
        batteryHistoryFlow.map { history ->
            history
                .filter { it.deviceId == deviceId && it.timestamp in startTime..endTime }
                .sortedBy { it.timestamp }
        }

    override suspend fun deleteHistoryForDevice(deviceId: String) {
        batteryHistoryFlow.update { current ->
            current.filter { it.deviceId != deviceId }
        }
    }
}
