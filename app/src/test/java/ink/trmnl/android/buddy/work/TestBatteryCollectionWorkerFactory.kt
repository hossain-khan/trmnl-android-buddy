package ink.trmnl.android.buddy.work

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import ink.trmnl.android.buddy.api.TrmnlApiService
import ink.trmnl.android.buddy.data.database.BatteryHistoryRepository
import ink.trmnl.android.buddy.data.preferences.UserPreferencesRepository

/**
 * Test WorkerFactory for creating [BatteryCollectionWorker] instances with fake dependencies.
 *
 * This factory is used in tests to inject fake repositories and services,
 * following Android WorkManager testing best practices.
 */
class TestBatteryCollectionWorkerFactory(
    private val apiService: TrmnlApiService,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val batteryHistoryRepository: BatteryHistoryRepository,
) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
    ): ListenableWorker? =
        when (workerClassName) {
            BatteryCollectionWorker::class.java.name -> {
                BatteryCollectionWorker(
                    context = appContext,
                    params = workerParameters,
                    apiService = apiService,
                    userPreferencesRepository = userPreferencesRepository,
                    batteryHistoryRepository = batteryHistoryRepository,
                )
            }
            else -> null
        }
}
