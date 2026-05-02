package ink.trmnl.android.buddy.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.work.WorkManager
import timber.log.Timber

/**
 * [GlanceAppWidgetReceiver] for the TRMNL Device Widget.
 *
 * Handles system lifecycle events:
 *  - **onUpdate**: Triggers a refresh for each widget instance (e.g. after a reboot)
 *  - **onDeleted**: Cancels the refresh worker and cleans up state for removed widgets
 */
class TrmnlDeviceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = TrmnlDeviceWidget()

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        Timber.d("[TrmnlDeviceWidgetReceiver] onUpdate for ${appWidgetIds.size} widgets")
        for (appWidgetId in appWidgetIds) {
            TrmnlWidgetRefreshWorker.enqueue(
                context = context,
                appWidgetId = appWidgetId,
                initialDelayMinutes = 0,
            )
        }
    }

    override fun onDeleted(
        context: Context,
        appWidgetIds: IntArray,
    ) {
        super.onDeleted(context, appWidgetIds)
        val workManager = WorkManager.getInstance(context)
        for (appWidgetId in appWidgetIds) {
            Timber.d("[TrmnlDeviceWidgetReceiver] Cancelling refresh worker for widget $appWidgetId")
            workManager.cancelUniqueWork(TrmnlWidgetRefreshWorker.workName(appWidgetId))
        }
    }
}
