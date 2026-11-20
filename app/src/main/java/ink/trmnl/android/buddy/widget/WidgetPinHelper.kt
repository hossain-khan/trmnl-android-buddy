package ink.trmnl.android.buddy.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.preferencesOf
import androidx.glance.appwidget.GlanceAppWidgetManager
import kotlinx.coroutines.CancellationException
import timber.log.Timber

/**
 * Helper for checking launcher support and triggering in-app widget pinning
 * using [GlanceAppWidgetManager.requestPinGlanceAppWidget].
 */
object WidgetPinHelper {
    /**
     * Returns `true` if the current default launcher supports pinning app widgets from in-app.
     */
    fun isPinSupported(context: Context): Boolean =
        try {
            AppWidgetManager.getInstance(context).isRequestPinAppWidgetSupported
        } catch (e: NoSuchMethodError) {
            Timber.w(e, "[WidgetPinHelper] Pinning API not available")
            false
        } catch (e: Exception) {
            Timber.w(e, "[WidgetPinHelper] Failed to check pin support")
            false
        }

    /**
     * Requests the launcher to pin [TrmnlDeviceWidget] to the home screen.
     *
     * When [deviceFriendlyId] and [deviceName] are provided, attaches a `successCallback`
     * [PendingIntent] targeting [WidgetPinReceiver] so the pinned widget is pre-configured
     * for that device automatically.
     *
     * @return `true` if the pin request was sent to the launcher, `false` otherwise.
     */
    suspend fun requestPinWidget(
        context: Context,
        deviceFriendlyId: String? = null,
        deviceName: String? = null,
    ): Boolean {
        if (!isPinSupported(context)) {
            return false
        }

        val successCallback =
            if (!deviceFriendlyId.isNullOrBlank()) {
                val callbackIntent =
                    Intent(context, WidgetPinReceiver::class.java).apply {
                        putExtra(WidgetPinReceiver.EXTRA_DEVICE_FRIENDLY_ID, deviceFriendlyId)
                        if (!deviceName.isNullOrBlank()) {
                            putExtra(WidgetPinReceiver.EXTRA_DEVICE_NAME, deviceName)
                        }
                    }
                PendingIntent.getBroadcast(
                    context,
                    deviceFriendlyId.hashCode(),
                    callbackIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            } else {
                null
            }

        val previewState =
            if (!deviceFriendlyId.isNullOrBlank()) {
                preferencesOf(
                    TrmnlDeviceWidget.DEVICE_FRIENDLY_ID_KEY to deviceFriendlyId,
                    TrmnlDeviceWidget.DEVICE_NAME_KEY to (deviceName ?: ""),
                )
            } else {
                emptyPreferences()
            }

        return try {
            GlanceAppWidgetManager(context).requestPinGlanceAppWidget(
                receiver = TrmnlDeviceWidgetReceiver::class.java,
                preview = TrmnlDeviceWidget(),
                previewState = previewState,
                successCallback = successCallback,
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w(e, "[WidgetPinHelper] Failed to request widget pin")
            false
        }
    }
}
