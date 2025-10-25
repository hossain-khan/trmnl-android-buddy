package ink.trmnl.android.buddy

import android.app.Application
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dev.zacsweers.metro.createGraphFactory
import ink.trmnl.android.buddy.di.AppGraph
import ink.trmnl.android.buddy.notification.NotificationHelper
import ink.trmnl.android.buddy.work.BatteryCollectionWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Application class for the app with key initializations.
 */
class TrmnlBuddyApp :
    Application(),
    Configuration.Provider {
    val appGraph by lazy { createGraphFactory<AppGraph.Factory>().create(this) }

    // Application-scoped coroutine scope for background tasks
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    fun appGraph(): AppGraph = appGraph

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(appGraph.workerFactory).build()

    override fun onCreate() {
        super.onCreate()

        // Initialize Timber for logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        NotificationHelper.createNotificationChannels(this)
        scheduleBatteryCollection()
        scheduleRssFeedContentWorkers()
    }

    /**
     * Schedules weekly battery data collection using WorkManager.
     * The worker collects battery information for all devices to track battery health over time.
     */
    private fun scheduleBatteryCollection() {
        val batteryWorkRequest =
            PeriodicWorkRequestBuilder<BatteryCollectionWorker>(
                repeatInterval = 7,
                repeatIntervalTimeUnit = TimeUnit.DAYS,
            ).setConstraints(
                Constraints
                    .Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            ).build()

        // Use KEEP policy to avoid duplicates
        appGraph.workManager.enqueueUniquePeriodicWork(
            BatteryCollectionWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            batteryWorkRequest,
        )
    }

    /**
     * Schedules RSS feed content workers (announcements and blog posts) based on user preferences.
     * Checks the user's RSS feed content preference and schedules or cancels workers accordingly.
     */
    private fun scheduleRssFeedContentWorkers() {
        applicationScope.launch {
            val preferences = appGraph.userPreferencesRepository.userPreferencesFlow.first()
            if (preferences.isRssFeedContentEnabled) {
                Timber.d("RSS feed content enabled - scheduling announcement and blog post sync workers")
                appGraph.workerScheduler.scheduleAnnouncementSync()
                appGraph.workerScheduler.scheduleBlogPostSync()
            } else {
                Timber.d("RSS feed content disabled - canceling announcement and blog post sync workers")
                appGraph.workerScheduler.cancelAnnouncementSync()
                appGraph.workerScheduler.cancelBlogPostSync()
            }
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        applicationScope.cancel()
    }
}
