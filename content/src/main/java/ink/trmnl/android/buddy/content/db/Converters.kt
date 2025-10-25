package ink.trmnl.android.buddy.content.db

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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
     * Convert List<String> to JSON string for database storage.
     * Uses JSON serialization to properly handle URLs with special characters.
     */
    @TypeConverter
    fun fromStringList(list: List<String>?): String? = list?.let { Json.encodeToString(it) }

    /**
     * Convert JSON string from database to List<String>.
     * Returns null if the value is null or if deserialization fails.
     */
    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        if (value.isNullOrBlank()) return null
        return try {
            Json.decodeFromString<List<String>>(value).takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            null
        }
    }
}
