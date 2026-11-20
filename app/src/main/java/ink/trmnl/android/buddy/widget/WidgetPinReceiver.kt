package ink.trmnl.android.buddy.widget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * [BroadcastReceiver] invoked by the system launcher via the `successCallback` `PendingIntent`
 * when a user accepts an in-app widget pin request from [WidgetPinHelper].
 *
 * Pre-configures the newly pinned widget with the device passed in the intent extras so the
 * user doesn't have to pick the device again, then triggers an immediate [TrmnlWidgetRefreshWorker].
 */
class WidgetPinReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val appWidgetId =
            intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID,
            )
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            Timber.w("[WidgetPinReceiver] Missing or invalid appWidgetId")
            return
        }

        val deviceFriendlyId = intent.getStringExtra(EXTRA_DEVICE_FRIENDLY_ID)
        val deviceName = intent.getStringExtra(EXTRA_DEVICE_NAME)
        if (deviceFriendlyId.isNullOrBlank()) {
            Timber.d("[WidgetPinReceiver] Widget $appWidgetId pinned without pre-selected device")
            return
        }

        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val manager = GlanceAppWidgetManager(appContext)
                var glanceId =
                    manager
                        .getGlanceIds(TrmnlDeviceWidget::class.java)
                        .firstOrNull { manager.getAppWidgetId(it) == appWidgetId }
                var attempts = 0
                while (glanceId == null && attempts < 5) {
                    attempts++
                    delay(200)
                    glanceId =
                        manager
                            .getGlanceIds(TrmnlDeviceWidget::class.java)
                            .firstOrNull { manager.getAppWidgetId(it) == appWidgetId }
                }
                if (glanceId == null) {
                    Timber.w("[WidgetPinReceiver] GlanceId not found for widget $appWidgetId")
                    return@launch
                }

                updateAppWidgetState(appContext, glanceId) { mutablePrefs ->
                    mutablePrefs[TrmnlDeviceWidget.APP_WIDGET_ID_KEY] = appWidgetId
                    mutablePrefs[TrmnlDeviceWidget.DEVICE_FRIENDLY_ID_KEY] = deviceFriendlyId
                    if (!deviceName.isNullOrBlank()) {
                        mutablePrefs[TrmnlDeviceWidget.DEVICE_NAME_KEY] = deviceName
                    }
                }
                TrmnlDeviceWidget().update(appContext, glanceId)
                TrmnlWidgetRefreshWorker.enqueue(
                    context = appContext,
                    appWidgetId = appWidgetId,
                    initialDelayMinutes = 0,
                )
                WidgetPinHelper.notifyDevicePinned(deviceFriendlyId)
                Timber.d("[WidgetPinReceiver] Pre-configured pinned widget $appWidgetId for $deviceFriendlyId")
            } catch (e: Exception) {
                Timber.e(e, "[WidgetPinReceiver] Failed to pre-configure pinned widget $appWidgetId")
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_DEVICE_FRIENDLY_ID = "extra_device_friendly_id"
        const val EXTRA_DEVICE_NAME = "extra_device_name"
    }
}
