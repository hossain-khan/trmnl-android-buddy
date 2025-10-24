package ink.trmnl.android.buddy.content.db

import androidx.room.TypeConverter
import java.time.Instant

/**
 * Type converters for Room database to handle non-primitive types.
 */
class Converters {
    /**
     * Convert Instant to Long (epoch seconds) for database storage.
     */
    @TypeConverter
    fun fromInstant(instant: Instant?): Long? = instant?.epochSecond

    /**
     * Convert Long (epoch seconds) from database to Instant.
     */
    @TypeConverter
    fun toInstant(epochSecond: Long?): Instant? = epochSecond?.let { Instant.ofEpochSecond(it) }
}
