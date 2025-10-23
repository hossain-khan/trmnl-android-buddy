package ink.trmnl.android.buddy.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import ink.trmnl.android.buddy.R

/**
 * Helper class for managing app notifications.
 * Handles notification channel creation and notification display.
 */
object NotificationHelper {
    const val CHANNEL_ID_LOW_BATTERY = "low_battery_alerts"
    private const val NOTIFICATION_ID_LOW_BATTERY = 1001

    /**
     * Creates notification channel for low battery alerts.
     * Must be called before showing notifications on Android O+.
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    CHANNEL_ID_LOW_BATTERY,
                    "Low Battery Alerts",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = "Notifications when device battery falls below threshold"
                }

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Shows a notification for low battery on one or more devices.
     *
     * @param context Application context
     * @param deviceNames List of device names with low battery
     * @param thresholdPercent The battery threshold that triggered the alert
     */
    fun showLowBatteryNotification(
        context: Context,
        deviceNames: List<String>,
        thresholdPercent: Int,
    ) {
        val title =
            if (deviceNames.size == 1) {
                "Low Battery: ${deviceNames[0]}"
            } else {
                "Low Battery: ${deviceNames.size} devices"
            }

        val contentText =
            if (deviceNames.size == 1) {
                "Battery level is below $thresholdPercent%"
            } else {
                "${deviceNames.joinToString(", ")} are below $thresholdPercent%"
            }

        val notification =
            NotificationCompat
                .Builder(context, CHANNEL_ID_LOW_BATTERY)
                .setSmallIcon(R.drawable.outline_battery_android_1_24)
                .setContentTitle(title)
                .setContentText(contentText)
                .setStyle(
                    NotificationCompat
                        .BigTextStyle()
                        .bigText(contentText),
                ).setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()

        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.notify(NOTIFICATION_ID_LOW_BATTERY, notification)
    }
}
