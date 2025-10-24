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
import ink.trmnl.android.buddy.work.LowBatteryNotificationWorker
import ink.trmnl.android.buddy.work.SampleWorker
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Application class for the app with key initializations.
 */
class TrmnlBuddyApp :
    Application(),
    Configuration.Provider {
    val appGraph by lazy { createGraphFactory<AppGraph.Factory>().create(this) }

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
        scheduleBackgroundWork()
        scheduleBatteryCollection()
    }

    /**
     * Schedules a background work request using the [WorkManager].
     * This is just an example to demonstrate how to use WorkManager with Metro DI.
     */
    private fun scheduleBackgroundWork() {
        val workRequest =
            OneTimeWorkRequestBuilder<SampleWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setInputData(workDataOf(SampleWorker.KEY_WORK_NAME to "Circuit App ${System.currentTimeMillis()}"))
                .setConstraints(
                    Constraints
                        .Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                ).build()

        appGraph.workManager.enqueue(workRequest)
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
}
