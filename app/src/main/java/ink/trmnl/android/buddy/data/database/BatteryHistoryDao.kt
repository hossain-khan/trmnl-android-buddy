package ink.trmnl.android.buddy.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for battery history operations.
 *
 * Provides methods to insert battery readings and query historical data
 * for battery health analysis and trajectory predictions.
 */
@Dao
interface BatteryHistoryDao {
    /**
     * Insert a new battery history record.
     * Replaces existing record if there's a conflict.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(batteryHistory: BatteryHistoryEntity)

    /**
     * Insert multiple battery history records.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(batteryHistories: List<BatteryHistoryEntity>)

    /**
     * Get all battery history for a specific device, ordered by timestamp descending.
     *
     * @param deviceId The device friendly ID
     * @return Flow of battery history list
     */
    @Query("SELECT * FROM battery_history WHERE device_id = :deviceId ORDER BY timestamp DESC")
    fun getBatteryHistoryForDevice(deviceId: String): Flow<List<BatteryHistoryEntity>>

    /**
     * Get the latest battery reading for a specific device.
     *
     * @param deviceId The device friendly ID
     * @return The most recent battery history record, or null if none exists
     */
    @Query("SELECT * FROM battery_history WHERE device_id = :deviceId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestBatteryReading(deviceId: String): BatteryHistoryEntity?

    /**
     * Get battery history for a device within a time range.
     *
     * @param deviceId The device friendly ID
     * @param startTime Start timestamp (inclusive)
     * @param endTime End timestamp (inclusive)
     * @return Flow of battery history list
     */
    @Query(
        """
        SELECT * FROM battery_history 
        WHERE device_id = :deviceId 
        AND timestamp BETWEEN :startTime AND :endTime 
        ORDER BY timestamp ASC
        """,
    )
    fun getBatteryHistoryInRange(
        deviceId: String,
        startTime: Long,
        endTime: Long,
    ): Flow<List<BatteryHistoryEntity>>

    /**
     * Delete all battery history records for a specific device.
     *
     * @param deviceId The device friendly ID
     */
    @Query("DELETE FROM battery_history WHERE device_id = :deviceId")
    suspend fun deleteHistoryForDevice(deviceId: String)

    /**
     * Delete all battery history records.
     */
    @Query("DELETE FROM battery_history")
    suspend fun deleteAll()
}
