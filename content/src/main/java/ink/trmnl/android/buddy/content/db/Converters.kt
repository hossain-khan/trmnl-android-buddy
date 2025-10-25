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

    /**
     * Convert List<String> to comma-separated String for database storage.
     */
    @TypeConverter
    fun fromStringList(list: List<String>?): String? = list?.joinToString(",")

    /**
     * Convert comma-separated String from database to List<String>.
     */
    @TypeConverter
    fun toStringList(value: String?): List<String>? = value?.split(",")?.filter { it.isNotBlank() }?.takeIf { it.isNotEmpty() }
}
