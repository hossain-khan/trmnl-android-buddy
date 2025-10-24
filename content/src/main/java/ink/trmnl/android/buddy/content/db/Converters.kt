package ink.trmnl.android.buddy.content.db

import androidx.room.TypeConverter
import kotlinx.datetime.Instant

/**
 * Type converters for Room database.
 * Handles conversion between Kotlin types and database types.
 */
class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Instant? = value?.let { Instant.fromEpochMilliseconds(it) }

    @TypeConverter
    fun instantToTimestamp(instant: Instant?): Long? = instant?.toEpochMilliseconds()
}
