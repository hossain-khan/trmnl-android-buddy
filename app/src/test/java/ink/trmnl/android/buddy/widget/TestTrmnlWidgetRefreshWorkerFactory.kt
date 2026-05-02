package ink.trmnl.android.buddy.widget

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import ink.trmnl.android.buddy.api.TrmnlApiService
import ink.trmnl.android.buddy.data.preferences.DeviceTokenRepository
import okhttp3.OkHttpClient

/**
 * Test [WorkerFactory] for creating [TrmnlWidgetRefreshWorker] instances with injected fakes.
 *
 * Follows the same pattern used by [ink.trmnl.android.buddy.work.TestBatteryCollectionWorkerFactory].
 */
class TestTrmnlWidgetRefreshWorkerFactory(
    private val apiService: TrmnlApiService,
    private val deviceTokenRepository: DeviceTokenRepository,
    private val okHttpClient: OkHttpClient,
) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
    ): ListenableWorker? =
        when (workerClassName) {
            TrmnlWidgetRefreshWorker::class.java.name ->
                TrmnlWidgetRefreshWorker(
                    context = appContext,
                    params = workerParameters,
                    apiService = apiService,
                    deviceTokenRepository = deviceTokenRepository,
                    okHttpClient = okHttpClient,
                )

            else -> null
        }
}
