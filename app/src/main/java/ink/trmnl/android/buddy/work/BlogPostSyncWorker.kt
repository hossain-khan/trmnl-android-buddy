package ink.trmnl.android.buddy.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import ink.trmnl.android.buddy.content.repository.BlogPostRepository
import ink.trmnl.android.buddy.data.preferences.UserPreferencesRepository
import ink.trmnl.android.buddy.dev.AppDevConfig
import ink.trmnl.android.buddy.di.AppWorkerFactory
import ink.trmnl.android.buddy.di.WorkerKey
import ink.trmnl.android.buddy.notification.NotificationHelper
import kotlinx.coroutines.flow.first
import timber.log.Timber

/**
 * Background worker for syncing blog posts from TRMNL RSS feed.
 *
 * Features:
 * - Runs periodically (daily) to fetch new blog posts
 * - Only runs when device has network connectivity
 * - Only runs when device is not in battery saver mode
 * - Shows notification when new posts are fetched (if enabled in preferences)
 * - Handles errors gracefully with retry logic
 */
@Inject
class BlogPostSyncWorker(
    context: Context,
    @Assisted params: WorkerParameters,
    private val blogPostRepository: BlogPostRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        Timber.Forest.d("BlogPostSyncWorker: Starting blog post sync")

        return try {
            // Get user preferences to check if notifications are enabled
            val preferences = userPreferencesRepository.userPreferencesFlow.first()

            // Get unread count before refresh
            val unreadCountBefore = blogPostRepository.getUnreadCount().first()

            // Fetch blog posts from RSS feed
            val result = blogPostRepository.refreshBlogPosts()

            if (result.isSuccess) {
                // Get unread count after refresh
                val unreadCountAfter = blogPostRepository.getUnreadCount().first()
                val newPostsCount = unreadCountAfter - unreadCountBefore

                Timber.Forest.d("BlogPostSyncWorker: Sync successful. New posts: $newPostsCount")

                // Show notification if new posts were fetched AND user has notifications enabled
                // OR if dev flag is enabled for testing
                val shouldShowNotification =
                    newPostsCount > 0 &&
                        (preferences.isRssFeedContentNotificationEnabled || AppDevConfig.ENABLE_BLOG_NOTIFICATION)

                if (shouldShowNotification) {
                    if (AppDevConfig.ENABLE_BLOG_NOTIFICATION) {
                        Timber.Forest.d("BlogPostSyncWorker: Dev flag enabled - showing blog post notification for testing")
                    }
                    NotificationHelper.showBlogPostNotification(applicationContext, newPostsCount)
                } else if (newPostsCount > 0) {
                    Timber.Forest.d("BlogPostSyncWorker: Notifications disabled, skipping notification")
                }

                Result.success()
            } else {
                val error = result.exceptionOrNull()
                Timber.Forest.e(error, "BlogPostSyncWorker: Failed to sync blog posts")

                // Retry on failure (WorkManager will use exponential backoff)
                Result.retry()
            }
        } catch (e: Exception) {
            Timber.Forest.e(e, "BlogPostSyncWorker: Unexpected error during sync")
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "blog_post_sync"
    }

    @WorkerKey(BlogPostSyncWorker::class)
    @ContributesIntoMap(
        AppScope::class,
        binding = binding<AppWorkerFactory.WorkerInstanceFactory<*>>(),
    )
    @AssistedFactory
    abstract class Factory : AppWorkerFactory.WorkerInstanceFactory<BlogPostSyncWorker>
}
