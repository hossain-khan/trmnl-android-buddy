package ink.trmnl.android.buddy.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Database entity for storing battery level history of TRMNL devices.
 *
 * This entity tracks battery percentage over time to enable battery health
 * analysis and drain trajectory predictions.
 *
 * @property id Auto-generated primary key
 * @property deviceId TRMNL device friendly ID (e.g., "ABC-123")
 * @property percentCharged Battery charge percentage from 0.0 to 100.0
 * @property batteryVoltage Battery voltage in volts (e.g., 3.7V). Nullable.
 * @property timestamp Unix timestamp in milliseconds when this reading was recorded
 */
@Entity(tableName = "battery_history")
data class BatteryHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "device_id")
    val deviceId: String,
    @ColumnInfo(name = "percent_charged")
    val percentCharged: Double,
    @ColumnInfo(name = "battery_voltage")
    val batteryVoltage: Double?,
    @ColumnInfo(name = "timestamp")
    val timestamp: Long,
)
