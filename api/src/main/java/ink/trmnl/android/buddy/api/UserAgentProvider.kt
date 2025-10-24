package ink.trmnl.android.buddy.api

import android.os.Build
import okhttp3.OkHttp

/**
 * Provides a properly formatted User-Agent string following industry best practices.
 *
 * Format: `AppName/Version (Android APILevel; DeviceModel) OkHttp/Version`
 * Example: `TrmnlAndroidBuddy/1.6.0 (Android 14; Pixel 7) OkHttp/5.1.0`
 *
 * This helps the server identify:
 * - The client application and version
 * - The Android API level
 * - The device model
 * - The HTTP client library and version
 */
object UserAgentProvider {
    private const val APP_NAME = "TrmnlAndroidBuddy"

    /**
     * Generates a user agent string with the provided app version.
     *
     * @param appVersion The application version (e.g., "1.6.0")
     * @return Formatted user agent string
     */
    fun getUserAgent(appVersion: String): String {
        val androidVersion = Build.VERSION.SDK_INT
        val deviceModel = Build.MODEL ?: "Unknown"
        val okhttpVersion = OkHttp.VERSION

        return "$APP_NAME/$appVersion (Android $androidVersion; $deviceModel) OkHttp/$okhttpVersion"
    }
}
