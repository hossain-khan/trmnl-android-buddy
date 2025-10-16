package ink.trmnl.android.buddy.data.database

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for battery history data operations.
 */
interface BatteryHistoryRepository {
    /**
     * Record a new battery reading for a device.
     *
     * @param deviceId The device friendly ID
     * @param percentCharged Battery charge percentage (0.0 to 100.0)
     * @param batteryVoltage Battery voltage in volts (nullable)
     * @param timestamp Unix timestamp in milliseconds
     */
    suspend fun recordBatteryReading(
        deviceId: String,
        percentCharged: Double,
        batteryVoltage: Double?,
        timestamp: Long = System.currentTimeMillis(),
    )

    /**
     * Get all battery history for a specific device.
     *
     * @param deviceId The device friendly ID
     * @return Flow of battery history list, ordered by timestamp descending
     */
    fun getBatteryHistoryForDevice(deviceId: String): Flow<List<BatteryHistoryEntity>>

    /**
     * Get the latest battery reading for a device.
     *
     * @param deviceId The device friendly ID
     * @return The most recent battery history record, or null if none exists
     */
    suspend fun getLatestBatteryReading(deviceId: String): BatteryHistoryEntity?

    /**
     * Get battery history within a time range.
     *
     * @param deviceId The device friendly ID
     * @param startTime Start timestamp (inclusive)
     * @param endTime End timestamp (inclusive)
     * @return Flow of battery history list, ordered by timestamp ascending
     */
    fun getBatteryHistoryInRange(
        deviceId: String,
        startTime: Long,
        endTime: Long,
    ): Flow<List<BatteryHistoryEntity>>

    /**
     * Delete all battery history for a specific device.
     *
     * @param deviceId The device friendly ID
     */
    suspend fun deleteHistoryForDevice(deviceId: String)
}

/**
 * Implementation of BatteryHistoryRepository using Room database.
 */
@Inject
@ContributesBinding(AppScope::class)
class BatteryHistoryRepositoryImpl(
    private val dao: BatteryHistoryDao,
) : BatteryHistoryRepository {
    override suspend fun recordBatteryReading(
        deviceId: String,
        percentCharged: Double,
        batteryVoltage: Double?,
        timestamp: Long,
    ) {
        val entity =
            BatteryHistoryEntity(
                deviceId = deviceId,
                percentCharged = percentCharged,
                batteryVoltage = batteryVoltage,
                timestamp = timestamp,
            )
        dao.insert(entity)
    }

    override fun getBatteryHistoryForDevice(deviceId: String): Flow<List<BatteryHistoryEntity>> = dao.getBatteryHistoryForDevice(deviceId)

    override suspend fun getLatestBatteryReading(deviceId: String): BatteryHistoryEntity? = dao.getLatestBatteryReading(deviceId)

    override fun getBatteryHistoryInRange(
        deviceId: String,
        startTime: Long,
        endTime: Long,
    ): Flow<List<BatteryHistoryEntity>> = dao.getBatteryHistoryInRange(deviceId, startTime, endTime)

    override suspend fun deleteHistoryForDevice(deviceId: String) {
        dao.deleteHistoryForDevice(deviceId)
    }
}
