package ink.trmnl.android.buddy.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * AppWidgetProvider for the TRMNL Device Widget.
 *
 * This receiver handles widget lifecycle events like creation, updates, and deletion.
 * The actual widget UI is defined in [DeviceWidget].
 */
class DeviceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DeviceWidget()
}
