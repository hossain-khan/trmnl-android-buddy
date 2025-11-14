package ink.trmnl.android.buddy.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import timber.log.Timber

/**
 * Action callback for refreshing widget data.
 * Triggered when user taps the refresh button on the widget.
 */
class RefreshWidgetCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        Timber.d("RefreshWidgetCallback triggered for widget: $glanceId")

        // Get the app widget ID from glance ID
        // Note: GlanceId doesn't directly expose the app widget ID, so we'll trigger
        // a worker to update all widgets or find another approach

        // For now, trigger the widget update worker
        val workRequest =
            OneTimeWorkRequestBuilder<DeviceWidgetWorker>()
                .setInputData(
                    workDataOf(
                        DeviceWidgetWorker.KEY_FORCE_UPDATE to true,
                    ),
                ).build()

        WorkManager.getInstance(context).enqueue(workRequest)
    }
}
