package ink.trmnl.android.buddy.content.db

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import org.junit.Before
import org.junit.Test
import java.time.Instant

/**
 * Unit tests for [Converters] to verify Room type conversions.
 */
class ConvertersTest {
    private lateinit var converters: Converters

    @Before
    fun setup() {
        converters = Converters()
    }

    // Instant conversion tests

    @Test
    fun `fromInstant converts Instant to epoch seconds`() {
        val instant = Instant.ofEpochSecond(1672531200) // 2023-01-01 00:00:00 UTC
        val result = converters.fromInstant(instant)

        assertThat(result).isEqualTo(1672531200L)
    }

    @Test
    fun `fromInstant returns null for null input`() {
        val result = converters.fromInstant(null)

        assertThat(result).isNull()
    }

    @Test
    fun `toInstant converts epoch seconds to Instant`() {
        val epochSecond = 1672531200L
        val result = converters.toInstant(epochSecond)

        assertThat(result).isEqualTo(Instant.ofEpochSecond(1672531200))
    }

    @Test
    fun `toInstant returns null for null input`() {
        val result = converters.toInstant(null)

        assertThat(result).isNull()
    }

    // String list conversion tests

    @Test
    fun `fromStringList converts list to JSON string`() {
        val list = listOf("https://example.com/image1.jpg", "https://example.com/image2.png")
        val result = converters.fromStringList(list)

        // Should be valid JSON array
        assertThat(result).isEqualTo("""["https://example.com/image1.jpg","https://example.com/image2.png"]""")
    }

    @Test
    fun `fromStringList handles URLs with commas`() {
        // URLs with commas (e.g., in query parameters)
        val list = listOf("https://example.com/api?data=1,2,3", "https://example.com/image.jpg")
        val result = converters.fromStringList(list)

        // Should properly encode as JSON (commas inside URLs won't break parsing)
        assertThat(result).isEqualTo("""["https://example.com/api?data=1,2,3","https://example.com/image.jpg"]""")
    }

    @Test
    fun `fromStringList handles URLs with special characters`() {
        // URLs with various special characters
        val list =
            listOf(
                "https://example.com/image?param=hello&other=world",
                "https://example.com/image%20with%20spaces.jpg",
                "https://example.com/image#fragment",
            )
        val result = converters.fromStringList(list)

        // Should properly encode all special characters in JSON
        assertThat(result).isEqualTo(
            """["https://example.com/image?param=hello&other=world","https://example.com/image%20with%20spaces.jpg","https://example.com/image#fragment"]""",
        )
    }

    @Test
    fun `fromStringList returns null for null input`() {
        val result = converters.fromStringList(null)

        assertThat(result).isNull()
    }

    @Test
    fun `fromStringList returns JSON array for empty list`() {
        val result = converters.fromStringList(emptyList())

        assertThat(result).isEqualTo("[]")
    }

    @Test
    fun `toStringList converts JSON string to list`() {
        val jsonString = """["https://example.com/image1.jpg","https://example.com/image2.png"]"""
        val result = converters.toStringList(jsonString)

        assertThat(result).isEqualTo(
            listOf("https://example.com/image1.jpg", "https://example.com/image2.png"),
        )
    }

    @Test
    fun `toStringList correctly parses URLs with commas`() {
        // JSON string with URLs containing commas
        val jsonString = """["https://example.com/api?data=1,2,3","https://example.com/image.jpg"]"""
        val result = converters.toStringList(jsonString)

        // Should correctly parse without breaking on commas inside URLs
        assertThat(result).isEqualTo(
            listOf("https://example.com/api?data=1,2,3", "https://example.com/image.jpg"),
        )
    }

    @Test
    fun `toStringList correctly parses URLs with special characters`() {
        val jsonString =
            """["https://example.com/image?param=hello&other=world","https://example.com/image%20with%20spaces.jpg","https://example.com/image#fragment"]"""
        val result = converters.toStringList(jsonString)

        assertThat(result).isEqualTo(
            listOf(
                "https://example.com/image?param=hello&other=world",
                "https://example.com/image%20with%20spaces.jpg",
                "https://example.com/image#fragment",
            ),
        )
    }

    @Test
    fun `toStringList returns null for null input`() {
        val result = converters.toStringList(null)

        assertThat(result).isNull()
    }

    @Test
    fun `toStringList returns null for blank input`() {
        val result = converters.toStringList("   ")

        assertThat(result).isNull()
    }

    @Test
    fun `toStringList returns null for empty JSON array`() {
        val result = converters.toStringList("[]")

        assertThat(result).isNull()
    }

    @Test
    fun `toStringList returns null for invalid JSON`() {
        val result = converters.toStringList("not valid json")

        assertThat(result).isNull()
    }

    @Test
    fun `roundtrip conversion preserves data`() {
        val original = listOf("https://example.com/image1.jpg", "https://example.com/image2.png")
        val json = converters.fromStringList(original)
        val restored = converters.toStringList(json)

        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun `roundtrip conversion preserves URLs with special characters`() {
        val original =
            listOf(
                "https://example.com/api?data=1,2,3&key=value",
                "https://example.com/image%20with%20spaces.jpg",
                "https://example.com/image#fragment",
            )
        val json = converters.fromStringList(original)
        val restored = converters.toStringList(json)

        assertThat(restored).isEqualTo(original)
    }
}
