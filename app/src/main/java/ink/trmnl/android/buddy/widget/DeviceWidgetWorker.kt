package ink.trmnl.android.buddy.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.slack.eithernet.ApiResult
import dev.zacsweers.metro.Inject
import ink.trmnl.android.buddy.api.TrmnlApiClient
import ink.trmnl.android.buddy.data.preferences.DeviceTokenRepository
import ink.trmnl.android.buddy.data.preferences.UserPreferences
import ink.trmnl.android.buddy.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import timber.log.Timber

/**
 * Worker for updating device widget data.
 *
 * Fetches device data from the TRMNL API and updates all widget instances
 * with their respective device information.
 */
@Inject
class DeviceWidgetWorker(
    private val context: Context,
    workerParams: WorkerParameters,
    private val userPrefsRepository: UserPreferencesRepository,
    private val deviceTokenRepository: DeviceTokenRepository,
    private val widgetConfigRepository: WidgetConfigRepository,
    private val apiClient: TrmnlApiClient,
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        Timber.d("DeviceWidgetWorker started")

        try {
            // Get user API key from preferences flow
            val userPrefs = userPrefsRepository.userPreferencesFlow.first()
            val apiKey = userPrefs.apiToken

            if (apiKey.isNullOrEmpty()) {
                Timber.w("No API key found, skipping widget update")
                return Result.success()
            }

            // Create API service and repository
            val apiService = apiClient.create(apiKey)
            val repository =
                ink.trmnl.android.buddy.api
                    .TrmnlDeviceRepository(apiService, apiKey)

            // Get all widget IDs
            val glanceManager = GlanceAppWidgetManager(context)
            val glanceIds = glanceManager.getGlanceIds(DeviceWidget::class.java)

            Timber.d("Found ${glanceIds.size} widget(s) to update")

            // Update each widget
            for (glanceId in glanceIds) {
                try {
                    updateWidget(glanceId, repository)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to update widget: $glanceId")
                }
            }

            Timber.d("DeviceWidgetWorker completed successfully")
            return Result.success()
        } catch (e: Exception) {
            Timber.e(e, "DeviceWidgetWorker failed")
            return Result.failure()
        }
    }

    private suspend fun updateWidget(
        glanceId: androidx.glance.GlanceId,
        repository: ink.trmnl.android.buddy.api.TrmnlDeviceRepository,
    ) {
        // Get the app widget ID from glance ID
        val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(context)
        val appWidgetIds =
            appWidgetManager.getAppWidgetIds(
                android.content.ComponentName(context, DeviceWidgetReceiver::class.java),
            )

        for (appWidgetId in appWidgetIds) {
            try {
                val deviceId = widgetConfigRepository.getWidgetDevice(appWidgetId) ?: continue

                // Update to loading state
                updateAppWidgetState(context, glanceId) {
                    WidgetState(
                        deviceId = deviceId,
                        isLoading = true,
                    )
                }

                // Fetch device data
                when (val deviceResult = repository.getDevice(deviceId)) {
                    is ApiResult.Success -> {
                        val device = deviceResult.value

                        // Get device token to fetch display image
                        val deviceToken = deviceTokenRepository.getDeviceToken(device.friendlyId)

                        var imageUrl: String? = null
                        var imagePath: String? = null
                        if (deviceToken != null) {
                            // Create a new API service for device API
                            val deviceApiService = apiClient.create()

                            // Fetch current display
                            when (val displayResult = deviceApiService.getDisplayCurrent(deviceToken)) {
                                is ApiResult.Success -> {
                                    imageUrl = displayResult.value.imageUrl
                                    // Download and save the image for widget display
                                    if (imageUrl != null) {
                                        imagePath =
                                            WidgetImageLoader.downloadAndSaveImage(
                                                context,
                                                imageUrl,
                                                device.id,
                                            )
                                    }
                                }

                                else -> {
                                    Timber.w("Failed to fetch display for device ${device.id}")
                                }
                            }
                        }

                        // Update widget state with data
                        updateAppWidgetState(context, glanceId) {
                            WidgetState(
                                deviceId = device.id,
                                deviceName = device.name,
                                deviceFriendlyId = device.friendlyId,
                                imageUrl = imageUrl,
                                imagePath = imagePath,
                                batteryPercent = device.percentCharged,
                                wifiStrength = device.wifiStrength,
                                lastUpdated = System.currentTimeMillis(),
                                isLoading = false,
                                errorMessage = null,
                            )
                        }

                        DeviceWidget().update(context, glanceId)
                        Timber.d("Updated widget $appWidgetId with device ${device.name}")
                    }

                    is ApiResult.Failure -> {
                        val errorMsg =
                            when (deviceResult) {
                                is ApiResult.Failure.NetworkFailure -> "Network error"
                                is ApiResult.Failure.HttpFailure -> "HTTP error ${deviceResult.code}"
                                else -> "Failed to load device"
                            }

                        updateAppWidgetState(context, glanceId) {
                            WidgetState(
                                deviceId = deviceId,
                                isLoading = false,
                                errorMessage = errorMsg,
                            )
                        }

                        DeviceWidget().update(context, glanceId)
                        Timber.w("Failed to fetch device $deviceId: $errorMsg")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to update widget $appWidgetId")
            }
        }
    }

    companion object {
        const val KEY_FORCE_UPDATE = "force_update"
        const val WORK_NAME = "device_widget_update"
    }
}
