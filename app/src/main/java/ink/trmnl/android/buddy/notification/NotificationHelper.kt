package ink.trmnl.android.buddy.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import ink.trmnl.android.buddy.MainActivity
import ink.trmnl.android.buddy.R
import timber.log.Timber

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
            Timber.d("Created notification channel: %s", CHANNEL_ID_LOW_BATTERY)
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
        // Check for POST_NOTIFICATIONS permission on Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            if (permission != PackageManager.PERMISSION_GRANTED) {
                Timber.w(
                    "POST_NOTIFICATIONS permission not granted, cannot show notification. " +
                        "User needs to grant notification permission in app settings.",
                )
                return
            }
        }

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

        Timber.d("Showing low battery notification for %d device(s): %s", deviceNames.size, deviceNames)

        // Create intent to open the app when notification is tapped
        val intent =
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

        val pendingIntent =
            PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val notification =
            NotificationCompat
                .Builder(context, CHANNEL_ID_LOW_BATTERY)
                .setSmallIcon(R.drawable.trmnl_glyph__black)
                .setContentTitle(title)
                .setContentText(contentText)
                .setStyle(
                    NotificationCompat
                        .BigTextStyle()
                        .bigText(contentText),
                ).setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.notify(NOTIFICATION_ID_LOW_BATTERY, notification)
    }
}
