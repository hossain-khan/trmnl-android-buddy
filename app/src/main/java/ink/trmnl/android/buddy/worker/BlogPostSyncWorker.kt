package ink.trmnl.android.buddy.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import ink.trmnl.android.buddy.MainActivity
import ink.trmnl.android.buddy.R
import ink.trmnl.android.buddy.content.repository.BlogPostRepository
import ink.trmnl.android.buddy.di.AppWorkerFactory.WorkerInstanceFactory
import ink.trmnl.android.buddy.di.WorkerKey
import timber.log.Timber

/**
 * Background worker for syncing blog posts from TRMNL RSS feed.
 *
 * Features:
 * - Runs periodically (daily) to fetch new blog posts
 * - Only runs when device has network connectivity
 * - Only runs when device is not in battery saver mode
 * - Shows notification when new posts are fetched
 * - Handles errors gracefully with retry logic
 */
@Inject
class BlogPostSyncWorker(
    context: Context,
    @Assisted params: WorkerParameters,
    private val blogPostRepository: BlogPostRepository,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        Timber.d("BlogPostSyncWorker: Starting blog post sync")

        return try {
            // Get unread count before refresh
            val unreadCountBefore = blogPostRepository.getUnreadCount()

            // Fetch blog posts from RSS feed
            val result = blogPostRepository.refreshBlogPosts()

            if (result.isSuccess) {
                // Get unread count after refresh
                val unreadCountAfter = blogPostRepository.getUnreadCount()
                val newPostsCount = unreadCountAfter - unreadCountBefore

                Timber.d("BlogPostSyncWorker: Sync successful. New posts: $newPostsCount")

                // Show notification if new posts were fetched
                if (newPostsCount > 0) {
                    showNotification(newPostsCount)
                }

                Result.success()
            } else {
                val error = result.exceptionOrNull()
                Timber.e(error, "BlogPostSyncWorker: Failed to sync blog posts")

                // Retry on failure (WorkManager will use exponential backoff)
                Result.retry()
            }
        } catch (e: Exception) {
            Timber.e(e, "BlogPostSyncWorker: Unexpected error during sync")
            Result.retry()
        }
    }

    /**
     * Show notification for new blog posts.
     */
    private fun showNotification(newPostsCount: Int) {
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel (required for Android O+)
        createNotificationChannel(notificationManager)

        // Intent to open app when notification is tapped
        val intent =
            Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

        val pendingIntent =
            PendingIntent.getActivity(
                applicationContext,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        // Build notification
        val notification =
            NotificationCompat
                .Builder(applicationContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.notification_important_24dp_e8eaed_fill0_wght400_grad0_opsz24)
                .setContentTitle("New TRMNL Blog Posts")
                .setContentText(
                    if (newPostsCount == 1) {
                        "1 new blog post available"
                    } else {
                        "$newPostsCount new blog posts available"
                    },
                ).setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
        Timber.d("BlogPostSyncWorker: Notification shown for $newPostsCount new posts")
    }

    /**
     * Create notification channel for blog post updates.
     * Required for Android O (API 26) and above.
     */
    private fun createNotificationChannel(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    "Blog Post Updates",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = "Notifications for new TRMNL blog posts"
                }

            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val WORK_NAME = "blog_post_sync"
        private const val CHANNEL_ID = "blog_post_updates"
        private const val NOTIFICATION_ID = 1001
    }

    @WorkerKey(BlogPostSyncWorker::class)
    @ContributesIntoMap(
        AppScope::class,
        binding = binding<WorkerInstanceFactory<*>>(),
    )
    @AssistedFactory
    abstract class Factory : WorkerInstanceFactory<BlogPostSyncWorker>
}
