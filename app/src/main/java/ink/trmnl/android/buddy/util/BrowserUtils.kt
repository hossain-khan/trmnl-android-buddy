package ink.trmnl.android.buddy.util

import android.content.Context
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import timber.log.Timber

/**
 * Utility functions for opening URLs in browser.
 */
object BrowserUtils {
    /**
     * Open URL in Chrome Custom Tabs with app theme colors.
     *
     * Falls back to default browser if Chrome Custom Tabs not available.
     *
     * @param context Android context
     * @param url URL to open
     * @param toolbarColor Primary color for toolbar (in ARGB format)
     * @param secondaryColor Secondary color for UI elements (in ARGB format)
     */
    fun openUrlInCustomTab(
        context: Context,
        url: String,
        toolbarColor: Int,
        secondaryColor: Int,
    ) {
        try {
            val colorSchemeParams =
                CustomTabColorSchemeParams
                    .Builder()
                    .setToolbarColor(toolbarColor)
                    .setSecondaryToolbarColor(secondaryColor)
                    .build()

            val customTabsIntent =
                CustomTabsIntent
                    .Builder()
                    .setDefaultColorSchemeParams(colorSchemeParams)
                    .setShowTitle(true)
                    .setShareState(CustomTabsIntent.SHARE_STATE_ON)
                    .setUrlBarHidingEnabled(false)
                    .build()

            // Add FLAG_ACTIVITY_NEW_TASK when launching from Application context
            customTabsIntent.intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)

            customTabsIntent.launchUrl(context, url.toUri())
            Timber.d("Opened URL in Custom Tabs: %s", url)
        } catch (e: Exception) {
            Timber.e(e, "Failed to open URL in Custom Tabs: %s", url)
        }
    }
}
