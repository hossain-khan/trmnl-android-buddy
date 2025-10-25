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
    const val CHANNEL_ID_RSS_FEED = "rss_feed_updates"

    private const val NOTIFICATION_ID_LOW_BATTERY = 1001
    private const val NOTIFICATION_ID_RSS_FEED = 1002

    /**
     * Creates all notification channels for the app.
     * Must be called before showing notifications on Android O+.
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Low Battery Alerts Channel
            val lowBatteryChannel =
                NotificationChannel(
                    CHANNEL_ID_LOW_BATTERY,
                    "Low Battery Alerts",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = "Notifications when device battery falls below threshold"
                }
            notificationManager.createNotificationChannel(lowBatteryChannel)
            Timber.d("Created notification channel: %s", CHANNEL_ID_LOW_BATTERY)

            // RSS Feed Updates Channel (Blog Posts & Announcements)
            val rssFeedChannel =
                NotificationChannel(
                    CHANNEL_ID_RSS_FEED,
                    "RSS Feed Updates",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = "Notifications for new TRMNL blog posts and announcements"
                }
            notificationManager.createNotificationChannel(rssFeedChannel)
            Timber.d("Created notification channel: %s", CHANNEL_ID_RSS_FEED)
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

    /**
     * Shows a notification for new blog posts.
     *
     * @param context Application context
     * @param newPostsCount Number of new blog posts
     */
    fun showBlogPostNotification(
        context: Context,
        newPostsCount: Int,
    ) {
        // Check for POST_NOTIFICATIONS permission on Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            if (permission != PackageManager.PERMISSION_GRANTED) {
                Timber.w(
                    "POST_NOTIFICATIONS permission not granted, cannot show blog post notification. " +
                        "User needs to grant notification permission in app settings.",
                )
                return
            }
        }

        val title = "New TRMNL Blog Posts"
        val contentText =
            if (newPostsCount == 1) {
                "1 new blog post available"
            } else {
                "$newPostsCount new blog posts available"
            }

        Timber.d("Showing blog post notification for %d new post(s)", newPostsCount)

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
                .Builder(context, CHANNEL_ID_RSS_FEED)
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
        notificationManager.notify(NOTIFICATION_ID_RSS_FEED, notification)
    }

    /**
     * Shows a notification for new announcements.
     *
     * @param context Application context
     * @param newAnnouncementsCount Number of new announcements
     */
    fun showAnnouncementNotification(
        context: Context,
        newAnnouncementsCount: Int,
    ) {
        // Check for POST_NOTIFICATIONS permission on Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            if (permission != PackageManager.PERMISSION_GRANTED) {
                Timber.w(
                    "POST_NOTIFICATIONS permission not granted, cannot show announcement notification. " +
                        "User needs to grant notification permission in app settings.",
                )
                return
            }
        }

        val title = "New TRMNL Announcements"
        val contentText =
            if (newAnnouncementsCount == 1) {
                "1 new announcement available"
            } else {
                "$newAnnouncementsCount new announcements available"
            }

        Timber.d("Showing announcement notification for %d new announcement(s)", newAnnouncementsCount)

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
                .Builder(context, CHANNEL_ID_RSS_FEED)
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
        notificationManager.notify(NOTIFICATION_ID_RSS_FEED, notification)
    }
}
