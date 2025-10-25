package ink.trmnl.android.buddy.dev

import ink.trmnl.android.buddy.worker.BlogPostSyncWorker
import ink.trmnl.android.buddy.work.AnnouncementSyncWorker
import ink.trmnl.android.buddy.notification.NotificationHelper
import ink.trmnl.android.buddy.BuildConfig

/**
 * Development configuration flags for testing and debugging.
 *
 * **WARNING**: These flags bypass normal user preferences and permission checks.
 * DO NOT enable in production builds. Use [BuildConfig.DEBUG] checks where needed.
 *
 * @see NotificationHelper
 * @see AnnouncementSyncWorker
 * @see BlogPostSyncWorker
 */
object AppDevConfig {
    /**
     * Force announcement notifications after every sync, regardless of user preference.
     *
     * When enabled:
     * - Bypasses `isRssFeedContentNotificationEnabled` preference check
     * - Shows notification for ANY new announcements detected
     * - Uses real announcement content from the feed
     * - Still respects POST_NOTIFICATIONS permission (Android 13+)
     *
     * Use case: Testing announcement notification content, styling, and behavior
     *
     * Default: `false` (respects user preference)
     */
    const val ENABLE_ANNOUNCEMENT_NOTIFICATION = false

    /**
     * Force blog post notifications after every sync, regardless of user preference.
     *
     * When enabled:
     * - Bypasses `isRssFeedContentNotificationEnabled` preference check
     * - Shows notification for ANY new blog posts detected
     * - Uses real blog post content from the feed
     * - Still respects POST_NOTIFICATIONS permission (Android 13+)
     *
     * Use case: Testing blog post notification content, styling, and behavior
     *
     * Default: `false` (respects user preference)
     */
    const val ENABLE_BLOG_NOTIFICATION = false

    /**
     * Enable verbose logging for notification flow.
     *
     * When enabled:
     * - Logs notification creation attempts
     * - Logs permission check results
     * - Logs preference check results
     * - Logs notification display success/failure
     *
     * Use case: Debugging notification issues
     *
     * Default: `false`
     */
    const val VERBOSE_NOTIFICATION_LOGGING = false
}
