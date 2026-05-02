package ink.trmnl.android.buddy.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import timber.log.Timber

/**
 * Glance [ActionCallback] that schedules an immediate widget refresh when the user
 * taps the refresh button inside the widget.
 *
 * The target widget is identified by [APP_WIDGET_ID_KEY] in the action parameters.
 */
class RefreshWidgetCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val appWidgetId = parameters[APP_WIDGET_ID_KEY]
        if (appWidgetId == null || appWidgetId == android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID) {
            Timber.w("[RefreshWidgetCallback] Invalid appWidgetId, skipping refresh")
            return
        }
        Timber.d("[RefreshWidgetCallback] Scheduling immediate refresh for widget $appWidgetId")
        TrmnlWidgetRefreshWorker.enqueue(
            context = context,
            appWidgetId = appWidgetId,
            initialDelayMinutes = 0,
        )
    }

    companion object {
        val APP_WIDGET_ID_KEY = ActionParameters.Key<Int>("app_widget_id")
    }
}
