package ink.trmnl.android.buddy.data.database

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Fake implementation of BatteryHistoryDao for testing.
 * Uses in-memory list to store battery history data.
 */
class FakeBatteryHistoryDao : BatteryHistoryDao {
    private val batteryHistory = mutableListOf<BatteryHistoryEntity>()
    private var nextId = 1L

    override suspend fun insert(batteryHistory: BatteryHistoryEntity) {
        val entity = batteryHistory.copy(id = nextId++)
        this.batteryHistory.add(entity)
    }

    override suspend fun insertAll(batteryHistories: List<BatteryHistoryEntity>) {
        batteryHistories.forEach { insert(it) }
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

    override suspend fun deleteAll() {
        batteryHistory.clear()
    }
}
