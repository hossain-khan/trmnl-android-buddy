package ink.trmnl.android.buddy.fakes

import ink.trmnl.android.buddy.data.database.BatteryHistoryEntity
import ink.trmnl.android.buddy.data.database.BatteryHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Fake implementation of [BatteryHistoryRepository] for testing.
 *
 * This fake provides a working in-memory implementation suitable for unit tests,
 * following the project's testing guidelines of using fakes instead of mocks.
 *
 * Exposes properties for test assertions to verify repository methods were called
 * with expected values.
 */
class FakeBatteryHistoryRepository(
    private val shouldThrowOnRecord: Boolean = false,
) : BatteryHistoryRepository {
    private val batteryHistory = mutableListOf<BatteryHistoryEntity>()
    private var nextId = 1L

    /**
     * Test-visible list of recorded battery readings.
     */
    val recordedReadings: List<BatteryHistoryEntity>
        get() = batteryHistory.toList()

    /**
     * Clear all recorded readings. Useful for test setup.
     */
    fun clear() {
        batteryHistory.clear()
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
        batteryHistory.add(entity)
    }

    override fun getBatteryHistoryForDevice(deviceId: String): Flow<List<BatteryHistoryEntity>> =
        flow {
            emit(
                batteryHistory
                    .filter { it.deviceId == deviceId }
                    .sortedByDescending { it.timestamp },
            )
        }

    override suspend fun getLatestBatteryReading(deviceId: String): BatteryHistoryEntity? =
        batteryHistory
            .filter { it.deviceId == deviceId }
            .maxByOrNull { it.timestamp }

    override fun getBatteryHistoryInRange(
        deviceId: String,
        startTime: Long,
        endTime: Long,
    ): Flow<List<BatteryHistoryEntity>> =
        flow {
            emit(
                batteryHistory
                    .filter { it.deviceId == deviceId && it.timestamp in startTime..endTime }
                    .sortedBy { it.timestamp },
            )
        }

    override suspend fun deleteHistoryForDevice(deviceId: String) {
        batteryHistory.removeAll { it.deviceId == deviceId }
    }
}
