package ink.trmnl.android.buddy.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.work.WorkManager
import timber.log.Timber
import java.io.File

/**
 * [GlanceAppWidgetReceiver] for the TRMNL Device Widget.
 *
 * Handles system lifecycle events:
 *  - **onUpdate**: Triggers a refresh for each widget instance (e.g. after a reboot)
 *  - **onDeleted**: Cancels the refresh worker and deletes the cached image file for each
 *    removed widget to avoid leaking storage
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
            Timber.d("[TrmnlDeviceWidgetReceiver] Cleaning up widget $appWidgetId")

            // Cancel the periodic refresh worker for this widget
            workManager.cancelUniqueWork(TrmnlWidgetRefreshWorker.workName(appWidgetId))

            // Delete the cached display image to avoid leaking storage when widgets are removed
            val imageFile =
                File(
                    context.filesDir,
                    "${TrmnlWidgetRefreshWorker.WIDGET_IMAGES_DIR}/widget_$appWidgetId.png",
                )
            if (imageFile.exists()) {
                imageFile.delete()
                Timber.d("[TrmnlDeviceWidgetReceiver] Deleted cached image for widget $appWidgetId")
            }
        }
    }
}
