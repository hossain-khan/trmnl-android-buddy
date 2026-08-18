package ink.trmnl.android.buddy.api

import android.os.Build
import okhttp3.OkHttp

/**
 * Provides a properly formatted User-Agent string following industry best practices.
 *
 * Format: `AppName/Version (Android APILevel; DeviceModel) OkHttp/Version`
 * Example: `TrmnlAndroidBuddy/2.15.0 (Android 14; Pixel 7) OkHttp/5.1.0`
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
     * @param androidVersion The Android SDK version (defaults to [Build.VERSION.SDK_INT])
     * @param deviceModel The device model name (defaults to [Build.MODEL])
     * @param okhttpVersion The OkHttp library version (defaults to [OkHttp.VERSION])
     * @return Formatted and sanitized user agent string
     */
    fun getUserAgent(
        appVersion: String,
        androidVersion: Int = Build.VERSION.SDK_INT,
        deviceModel: String? = Build.MODEL,
        okhttpVersion: String = OkHttp.VERSION,
    ): String {
        val sanitizedVersion = appVersion.trim().ifBlank { "unknown" }
        val sanitizedModel = deviceModel?.trim()?.ifBlank { "Unknown" } ?: "Unknown"
        val sanitizedOkHttp = okhttpVersion.trim().ifBlank { "unknown" }

        return "$APP_NAME/$sanitizedVersion (Android $androidVersion; $sanitizedModel) OkHttp/$sanitizedOkHttp"
    }
}
