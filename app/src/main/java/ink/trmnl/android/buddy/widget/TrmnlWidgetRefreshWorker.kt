package ink.trmnl.android.buddy.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ExistingWorkPolicy.KEEP
import androidx.work.ExistingWorkPolicy.REPLACE
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.slack.eithernet.ApiResult
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import ink.trmnl.android.buddy.api.TrmnlApiService
import ink.trmnl.android.buddy.data.preferences.DeviceTokenRepository
import ink.trmnl.android.buddy.di.AppWorkerFactory.WorkerInstanceFactory
import ink.trmnl.android.buddy.di.WorkerKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * WorkManager [CoroutineWorker] that fetches the current display image from the TRMNL API
 * and updates the widget state.
 *
 * For each run this worker:
 * 1. Resolves the [GlanceId] from the stored `appWidgetId`
 * 2. Reads the device friendly ID from Glance state
 * 3. Looks up the device API token from [DeviceTokenRepository]
 * 4. Calls [TrmnlApiService.getDisplayCurrent] with the token
 * 5. Downloads and caches the display image as PNG to
 *    `filesDir/[WIDGET_IMAGES_DIR]/widget_{appWidgetId}.png`
 * 6. Updates Glance state with the image path and refresh metadata
 * 7. Re-schedules itself using the API-provided `refreshRate`
 *    (floored at [TrmnlDeviceWidget.MIN_REFRESH_INTERVAL_MINUTES])
 *
 * @see TrmnlDeviceWidget
 * @see TrmnlDeviceWidgetReceiver
 */
@Inject
class TrmnlWidgetRefreshWorker(
    context: Context,
    @Assisted params: WorkerParameters,
    private val apiService: TrmnlApiService,
    private val deviceTokenRepository: DeviceTokenRepository,
    private val okHttpClient: OkHttpClient,
) : androidx.work.CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val appWidgetId = inputData.getInt(KEY_APP_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            Timber.e("[$WORK_TAG] Invalid appWidgetId in input data")
            return Result.failure()
        }

        val manager = GlanceAppWidgetManager(applicationContext)
        val glanceIds = manager.getGlanceIds(TrmnlDeviceWidget::class.java)
        val glanceId = glanceIds.firstOrNull { manager.getAppWidgetId(it) == appWidgetId }
        if (glanceId == null) {
            Timber.w("[$WORK_TAG] Widget $appWidgetId no longer exists, skipping")
            return Result.success()
        }

        val prefs = getAppWidgetState(applicationContext, PreferencesGlanceStateDefinition, glanceId)
        val deviceFriendlyId = prefs[TrmnlDeviceWidget.DEVICE_FRIENDLY_ID_KEY]
        if (deviceFriendlyId.isNullOrBlank()) {
            Timber.w("[$WORK_TAG] Widget $appWidgetId has no device configured, skipping")
            return Result.success()
        }

        val deviceToken = deviceTokenRepository.getDeviceToken(deviceFriendlyId)
        if (deviceToken.isNullOrBlank()) {
            Timber.w("[$WORK_TAG] No device token for $deviceFriendlyId")
            updateGlanceError(
                applicationContext,
                glanceId,
                "Device token not set. Open app to configure.",
            )
            TrmnlDeviceWidget().update(applicationContext, glanceId)
            return Result.success()
        }

        return when (val result = apiService.getDisplayCurrent(deviceToken)) {
            is ApiResult.Success -> {
                val display = result.value
                val imageUrl = display.imageUrl
                val refreshRate = display.refreshRate

                // Check whether a cached image already exists before attempting download
                val existingImagePath = prefs[TrmnlDeviceWidget.IMAGE_FILE_PATH_KEY]

                var imageFilePath: String? = null
                var downloadError: String? = null
                if (!imageUrl.isNullOrBlank()) {
                    try {
                        val imageFile =
                            File(
                                applicationContext.filesDir,
                                "$WIDGET_IMAGES_DIR/widget_$appWidgetId.png",
                            )
                        downloadAndSaveImage(imageUrl, imageFile)
                        imageFilePath = imageFile.absolutePath
                    } catch (e: Exception) {
                        Timber.e(e, "[$WORK_TAG] Failed to download image from $imageUrl")
                        downloadError = "Failed to download display image."
                    }
                } else {
                    Timber.w("[$WORK_TAG] API returned no image URL for widget $appWidgetId")
                    downloadError = "No display image available from API."
                }

                val filePath = imageFilePath
                updateAppWidgetState(applicationContext, glanceId) { mutablePrefs ->
                    mutablePrefs[TrmnlDeviceWidget.REFRESH_RATE_KEY] = refreshRate
                    mutablePrefs[TrmnlDeviceWidget.LAST_UPDATED_KEY] = System.currentTimeMillis()
                    when {
                        filePath != null -> {
                            // New image successfully downloaded — update path and clear any error
                            mutablePrefs[TrmnlDeviceWidget.IMAGE_FILE_PATH_KEY] = filePath
                            mutablePrefs.remove(TrmnlDeviceWidget.ERROR_MESSAGE_KEY)
                        }
                        existingImagePath != null -> {
                            // Keep existing cached image; don't surface a transient download error
                            // since the user can still see the last known display image.
                            Timber.d("[$WORK_TAG] Image download failed for widget $appWidgetId; retaining existing cached image")
                        }
                        else -> {
                            // No image available at all — surface an actionable error
                            mutablePrefs[TrmnlDeviceWidget.ERROR_MESSAGE_KEY] =
                                downloadError ?: "No display image available."
                        }
                    }
                }
                TrmnlDeviceWidget().update(applicationContext, glanceId)

                // Schedule next refresh using the API-provided rate, floored at the minimum
                val nextRefreshMinutes =
                    maxOf(
                        TrmnlDeviceWidget.MIN_REFRESH_INTERVAL_MINUTES,
                        (refreshRate / 60L),
                    )
                enqueue(applicationContext, appWidgetId, initialDelayMinutes = nextRefreshMinutes)

                Result.success()
            }

            is ApiResult.Failure.HttpFailure -> {
                Timber.e("[$WORK_TAG] HTTP ${result.code} fetching display for widget $appWidgetId")
                if (result.code == 401) {
                    updateGlanceError(
                        applicationContext,
                        glanceId,
                        "Authentication failed. Check device token.",
                    )
                    TrmnlDeviceWidget().update(applicationContext, glanceId)
                    Result.failure()
                } else {
                    Result.retry()
                }
            }

            is ApiResult.Failure.NetworkFailure -> {
                Timber.e(result.error, "[$WORK_TAG] Network error for widget $appWidgetId")
                Result.retry()
            }

            is ApiResult.Failure.ApiFailure -> {
                Timber.e("[$WORK_TAG] API error for widget $appWidgetId: ${result.error}")
                updateGlanceError(applicationContext, glanceId, "API error. Tap to retry.")
                TrmnlDeviceWidget().update(applicationContext, glanceId)
                Result.failure()
            }

            is ApiResult.Failure.UnknownFailure -> {
                Timber.e(result.error, "[$WORK_TAG] Unknown error for widget $appWidgetId")
                Result.retry()
            }
        }
    }

    private suspend fun updateGlanceError(
        context: Context,
        glanceId: androidx.glance.GlanceId,
        message: String,
    ) {
        updateAppWidgetState(context, glanceId) { mutablePrefs ->
            mutablePrefs[TrmnlDeviceWidget.ERROR_MESSAGE_KEY] = message
        }
    }

    private suspend fun downloadAndSaveImage(
        imageUrl: String,
        outputFile: File,
    ) {
        val request = Request.Builder().url(imageUrl).build()
        withContext(Dispatchers.IO) {
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
                val body = response.body ?: throw IOException("Empty response body")
                val bytes = body.bytes()
                val bitmap =
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        ?: throw IOException("Failed to decode image from $imageUrl")
                val parentDir = outputFile.parentFile
                if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
                    throw IOException("Failed to create image cache directory: ${parentDir.absolutePath}")
                }
                outputFile.outputStream().use { out ->
                    // PNG compression quality is a no-op for the Android PNG encoder (lossless format).
                    // PNG is preferred here because TRMNL display images are monochrome/grayscale,
                    // which compresses very efficiently as PNG without any visual degradation.
                    bitmap.compress(Bitmap.CompressFormat.PNG, 0, out)
                }
            }
        }
    }

    @WorkerKey(TrmnlWidgetRefreshWorker::class)
    @ContributesIntoMap(
        AppScope::class,
        binding = binding<WorkerInstanceFactory<*>>(),
    )
    @AssistedFactory
    abstract class Factory : WorkerInstanceFactory<TrmnlWidgetRefreshWorker>

    companion object {
        private const val WORK_TAG = "TrmnlWidgetRefreshWorker"
        const val KEY_APP_WIDGET_ID = "app_widget_id"

        /** Sub-directory under [Context.getFilesDir] where downloaded widget images are stored. */
        const val WIDGET_IMAGES_DIR = "widget_images"

        fun workName(appWidgetId: Int) = "trmnl_widget_refresh_$appWidgetId"

        fun enqueue(
            context: Context,
            appWidgetId: Int,
            initialDelayMinutes: Long = TrmnlDeviceWidget.DEFAULT_REFRESH_INTERVAL_MINUTES,
            existingWorkPolicy: ExistingWorkPolicy = REPLACE,
        ) {
            val inputData =
                Data
                    .Builder()
                    .putInt(KEY_APP_WIDGET_ID, appWidgetId)
                    .build()
            val constraints =
                Constraints
                    .Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            val requestBuilder =
                OneTimeWorkRequestBuilder<TrmnlWidgetRefreshWorker>()
                    .setInputData(inputData)
                    .setConstraints(constraints)
                    .addTag(workName(appWidgetId))
            if (initialDelayMinutes > 0) {
                requestBuilder.setInitialDelay(initialDelayMinutes, TimeUnit.MINUTES)
            }
            WorkManager.getInstance(context).enqueueUniqueWork(
                workName(appWidgetId),
                existingWorkPolicy,
                requestBuilder.build(),
            )
        }
    }
}
