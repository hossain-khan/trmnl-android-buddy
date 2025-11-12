package ink.trmnl.android.buddy.api.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * User data model representing a TRMNL user account.
 *
 * This model is returned by the `/me` endpoint to provide information
 * about the currently authenticated user.
 *
 * @property name Full name of the user
 * @property email Email address
 * @property firstName First name
 * @property lastName Last name
 * @property locale User's locale (e.g., "en")
 * @property timeZone Time zone name (e.g., "Eastern Time (US & Canada)")
 * @property timeZoneIana IANA time zone identifier (e.g., "America/New_York")
 * @property utcOffset UTC offset in seconds (e.g., -14400 for EST)
 * @property apiKey User's API key for authentication
 */
@Serializable
data class User(
    @SerialName("name")
    val name: String,
    @SerialName("email")
    val email: String,
    @SerialName("first_name")
    val firstName: String,
    @SerialName("last_name")
    val lastName: String,
    @SerialName("locale")
    val locale: String,
    @SerialName("time_zone")
    val timeZone: String,
    @SerialName("time_zone_iana")
    val timeZoneIana: String,
    @SerialName("utc_offset")
    val utcOffset: Int,
    @SerialName("api_key")
    val apiKey: String,
)
