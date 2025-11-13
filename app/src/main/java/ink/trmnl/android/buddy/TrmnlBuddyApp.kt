package ink.trmnl.android.buddy

import android.app.Application
import android.content.Context
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import coil3.ImageLoader
import coil3.SingletonImageLoader
import dev.zacsweers.metro.createGraphFactory
import ink.trmnl.android.buddy.di.AppGraph
import ink.trmnl.android.buddy.notification.NotificationHelper
import ink.trmnl.android.buddy.work.BatteryCollectionWorker
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Application class for the app with key initializations.
 */
class TrmnlBuddyApp :
    Application(),
    Configuration.Provider,
    SingletonImageLoader.Factory {
    val appGraph by lazy { createGraphFactory<AppGraph.Factory>().create(this) }

    fun appGraph(): AppGraph = appGraph

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(appGraph.workerFactory).build()

    override fun newImageLoader(context: Context): ImageLoader = appGraph.imageLoader

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
     * Schedules RSS feed content workers (announcements and blog posts) on app launch.
     * Uses KEEP policy to avoid duplicates if workers are already scheduled.
     * Workers will be cancelled when user disables RSS feed content in settings.
     */
    private fun scheduleRssFeedContentWorkers() {
        Timber.d("Scheduling RSS feed content workers (announcement and blog post sync)")
        appGraph.workerScheduler.scheduleAnnouncementSync()
        appGraph.workerScheduler.scheduleBlogPostSync()
    }
}
