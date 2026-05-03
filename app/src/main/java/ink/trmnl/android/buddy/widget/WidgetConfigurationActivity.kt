package ink.trmnl.android.buddy.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.lifecycle.lifecycleScope
import com.slack.eithernet.ApiResult
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import ink.trmnl.android.buddy.R
import ink.trmnl.android.buddy.api.TrmnlApiService
import ink.trmnl.android.buddy.api.models.Device
import ink.trmnl.android.buddy.data.preferences.DeviceTokenRepository
import ink.trmnl.android.buddy.data.preferences.UserPreferencesRepository
import ink.trmnl.android.buddy.di.ActivityKey
import ink.trmnl.android.buddy.ui.theme.TrmnlBuddyAppTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.io.IOException

/**
 * Configuration activity displayed when the user adds a new TRMNL Device Widget
 * to their home screen.
 *
 * Shows the list of devices in the user's account and lets them pick one.
 * After selection the activity:
 *  1. Stores the chosen device info in the widget's Glance state
 *  2. Attempts an **inline image fetch** while still in the foreground so the widget
 *     shows the display image immediately on first add (avoiding a race where the app
 *     process is killed before WorkManager can execute the first refresh job)
 *  3. Schedules a [TrmnlWidgetRefreshWorker] for the next periodic refresh
 *  4. Returns [Activity.RESULT_OK] to the system so the widget is added
 *
 * If the activity is cancelled (back press / no token) RESULT_CANCELED is
 * returned and the widget is NOT added.
 */
@ActivityKey(WidgetConfigurationActivity::class)
@ContributesIntoMap(AppScope::class, binding = binding<Activity>())
@Inject
@OptIn(ExperimentalMaterial3Api::class)
class WidgetConfigurationActivity(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val trmnlApiService: TrmnlApiService,
    private val deviceTokenRepository: DeviceTokenRepository,
    private val okHttpClient: okhttp3.OkHttpClient,
) : ComponentActivity() {
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Default result = CANCELED so pressing Back removes the widget
        setResult(Activity.RESULT_CANCELED)

        appWidgetId =
            intent?.extras?.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID,
            ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            TrmnlBuddyAppTheme {
                ConfigurationScreen(
                    onDeviceSelected = { device, onError -> onDeviceSelected(device, onError) },
                    onCancelled = { finish() },
                )
            }
        }
    }

    @Composable
    private fun ConfigurationScreen(
        onDeviceSelected: (Device, onError: () -> Unit) -> Unit,
        onCancelled: () -> Unit,
    ) {
        var devices by remember { mutableStateOf<List<Device>>(emptyList()) }
        var devicesWithToken by remember { mutableStateOf<Set<String>>(emptySet()) }
        var isLoading by remember { mutableStateOf(true) }
        var isConfiguringWidget by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(Unit) {
            try {
                val prefs = userPreferencesRepository.userPreferencesFlow.first()
                val apiToken = prefs.apiToken
                if (apiToken.isNullOrBlank()) {
                    errorMessage =
                        "Sign in required. Open the TRMNL Buddy app to authenticate before adding a widget."
                    isLoading = false
                    return@LaunchedEffect
                }
                when (val result = trmnlApiService.getDevices("Bearer $apiToken")) {
                    is ApiResult.Success -> {
                        val loadedDevices = result.value.data
                        devices = loadedDevices
                        devicesWithToken =
                            loadedDevices
                                .filter { deviceTokenRepository.hasDeviceToken(it.friendlyId) }
                                .map { it.friendlyId }
                                .toSet()
                        isLoading = false
                    }

                    is ApiResult.Failure.HttpFailure -> {
                        errorMessage =
                            if (result.code == 401) {
                                "Authentication failed (HTTP 401). Re-open the app and check your API token."
                            } else {
                                "Server error (HTTP ${result.code}). Please try again later."
                            }
                        isLoading = false
                    }

                    is ApiResult.Failure.NetworkFailure -> {
                        errorMessage = "No network connection. Check your internet and try again."
                        isLoading = false
                    }

                    else -> {
                        errorMessage = "Failed to load devices. Please try again."
                        isLoading = false
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "[WidgetConfig] Error loading devices")
                errorMessage = "Error loading devices: ${e.message}"
                isLoading = false
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(title = { Text(stringResource(R.string.widget_configure_title)) })
            },
        ) { innerPadding ->
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    isConfiguringWidget -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(24.dp),
                        ) {
                            CircularProgressIndicator()
                            Text(
                                text = "Setting up widget…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                    isLoading -> CircularProgressIndicator()
                    errorMessage != null -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(24.dp),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.deviceinfo_thin_outline),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(48.dp),
                            )
                            Text(
                                text = errorMessage.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                            )
                            OutlinedButton(onClick = onCancelled) {
                                Text("Cancel")
                            }
                        }
                    }

                    devices.isEmpty() -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(24.dp),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.devices_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp),
                            )
                            Text(
                                text = "No devices found in your account.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            OutlinedButton(onClick = onCancelled) {
                                Text("Cancel")
                            }
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors =
                                        CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.Top,
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.widgets_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.size(20.dp),
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(
                                                text = "Widget will show your device's current display image and refresh automatically.",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            )
                                            Text(
                                                text = "Select the TRMNL device you want to mirror on your home screen.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            )
                                        }
                                    }
                                }
                            }
                            items(devices) { device ->
                                val hasToken = device.friendlyId in devicesWithToken
                                Card(
                                    onClick = {
                                        if (hasToken) {
                                            isConfiguringWidget = true
                                            onDeviceSelected(device) { isConfiguringWidget = false }
                                        }
                                    },
                                    enabled = hasToken,
                                    modifier = Modifier.fillMaxWidth(),
                                    elevation = CardDefaults.cardElevation(defaultElevation = if (hasToken) 1.dp else 0.dp),
                                    colors =
                                        CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f),
                                        ),
                                ) {
                                    ListItem(
                                        headlineContent = {
                                            Text(
                                                text = device.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color =
                                                    if (hasToken) {
                                                        MaterialTheme.colorScheme.onSurface
                                                    } else {
                                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                                    },
                                            )
                                        },
                                        supportingContent = {
                                            Text(
                                                text =
                                                    if (hasToken) {
                                                        "ID: ${device.friendlyId}"
                                                    } else {
                                                        "ID: ${device.friendlyId} · Device API key not configured"
                                                    },
                                                style = MaterialTheme.typography.bodySmall,
                                                color =
                                                    if (hasToken) {
                                                        MaterialTheme.colorScheme.onSurfaceVariant
                                                    } else {
                                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                                    },
                                            )
                                        },
                                        leadingContent = {
                                            Icon(
                                                painter = painterResource(R.drawable.devices_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                                                contentDescription = null,
                                                tint =
                                                    if (hasToken) {
                                                        MaterialTheme.colorScheme.primary
                                                    } else {
                                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                                    },
                                                modifier = Modifier.size(24.dp),
                                            )
                                        },
                                        trailingContent = {
                                            Icon(
                                                painter =
                                                    painterResource(
                                                        R.drawable.chevron_forward_24dp_e8eaed_fill0_wght400_grad0_opsz24,
                                                    ),
                                                contentDescription = null,
                                                tint =
                                                    if (hasToken) {
                                                        MaterialTheme.colorScheme.onSurfaceVariant
                                                    } else {
                                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                                                    },
                                                modifier = Modifier.size(20.dp),
                                            )
                                        },
                                        colors =
                                            ListItemDefaults.colors(
                                                containerColor =
                                                    MaterialTheme.colorScheme.surfaceContainer.copy(
                                                        alpha = if (hasToken) 1f else 0.5f,
                                                    ),
                                            ),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun onDeviceSelected(
        device: Device,
        onError: () -> Unit,
    ) {
        lifecycleScope.launch {
            try {
                val manager = GlanceAppWidgetManager(applicationContext)
                val glanceIds = manager.getGlanceIds(TrmnlDeviceWidget::class.java)
                val glanceId =
                    glanceIds.firstOrNull { manager.getAppWidgetId(it) == appWidgetId }
                        ?: run {
                            Timber.e("[WidgetConfig] GlanceId not found for widget $appWidgetId")
                            setResult(Activity.RESULT_CANCELED)
                            finish()
                            return@launch
                        }

                updateAppWidgetState(applicationContext, glanceId) { mutablePrefs ->
                    mutablePrefs[TrmnlDeviceWidget.APP_WIDGET_ID_KEY] = appWidgetId
                    mutablePrefs[TrmnlDeviceWidget.DEVICE_FRIENDLY_ID_KEY] = device.friendlyId
                    mutablePrefs[TrmnlDeviceWidget.DEVICE_NAME_KEY] = device.name
                }

                // Trigger initial render (shows loading state)
                TrmnlDeviceWidget().update(applicationContext, glanceId)

                // Attempt an inline image fetch while still in the foreground.
                //
                // Background: after finish() the app process may be killed by aggressive
                // OEM battery/process managers before WorkManager can execute the enqueued
                // TrmnlWidgetRefreshWorker, leaving the widget stuck on "Loading…" until
                // the user manually opens the app again.
                //
                // By fetching here — while the configuration activity is the foreground
                // task — we guarantee the first image load succeeds regardless of whether
                // WorkManager fires promptly.  If the fetch fails (no token, network issue,
                // timeout) we fall back gracefully: the widget stays on Loading and the
                // scheduled worker will retry.
                val refreshRate =
                    attemptInlineImageFetch(
                        glanceId = glanceId,
                        deviceFriendlyId = device.friendlyId,
                        appWidgetId = appWidgetId,
                    )

                // Schedule future refreshes.  If the inline fetch succeeded we already
                // know the API-supplied refresh rate and can schedule with the proper delay
                // rather than running the worker again immediately.
                val initialDelayMinutes =
                    if (refreshRate != null) {
                        maxOf(TrmnlDeviceWidget.MIN_REFRESH_INTERVAL_MINUTES, refreshRate / 60L)
                    } else {
                        0L // inline fetch failed — run immediately so the worker retries soon
                    }
                TrmnlWidgetRefreshWorker.enqueue(
                    context = applicationContext,
                    appWidgetId = appWidgetId,
                    initialDelayMinutes = initialDelayMinutes,
                )

                val resultValue =
                    Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                setResult(Activity.RESULT_OK, resultValue)
                finish()
            } catch (e: Exception) {
                Timber.e(e, "[WidgetConfig] Error configuring widget")
                // Reset the configuring flag so the device list reappears and the
                // user can retry rather than being stuck on the progress screen.
                onError()
            }
        }
    }

    /**
     * Attempts to fetch the current display image from the TRMNL API and save it to the widget's
     * local cache while the configuration activity is still in the foreground.
     *
     * This is a best-effort operation: any failure (missing token, network error, timeout) is
     * silently swallowed so the caller can fall back to scheduling the background worker.
     *
     * @return the API-provided refresh rate in seconds on success, or `null` if the fetch failed.
     */
    private suspend fun attemptInlineImageFetch(
        glanceId: GlanceId,
        deviceFriendlyId: String,
        appWidgetId: Int,
    ): Int? {
        val deviceToken = deviceTokenRepository.getDeviceToken(deviceFriendlyId)
        if (deviceToken.isNullOrBlank()) {
            Timber.d("[WidgetConfig] No device token for $deviceFriendlyId — skipping inline fetch")
            return null
        }

        return try {
            val fetchResult =
                withTimeoutOrNull(INLINE_FETCH_TIMEOUT_MS) {
                    when (val result = trmnlApiService.getDisplayCurrent(deviceToken)) {
                        is ApiResult.Success -> {
                            val display = result.value
                            val imageUrl = display.imageUrl
                            if (!imageUrl.isNullOrBlank()) {
                                val imageFile =
                                    File(
                                        applicationContext.filesDir,
                                        "${TrmnlWidgetRefreshWorker.WIDGET_IMAGES_DIR}/widget_$appWidgetId.png",
                                    )
                                downloadAndSaveWidgetImage(imageUrl, imageFile)
                                updateAppWidgetState(applicationContext, glanceId) { mutablePrefs ->
                                    mutablePrefs[TrmnlDeviceWidget.IMAGE_FILE_PATH_KEY] = imageFile.absolutePath
                                    mutablePrefs[TrmnlDeviceWidget.REFRESH_RATE_KEY] = display.refreshRate
                                    mutablePrefs[TrmnlDeviceWidget.LAST_UPDATED_KEY] = System.currentTimeMillis()
                                    mutablePrefs.remove(TrmnlDeviceWidget.ERROR_MESSAGE_KEY)
                                }
                                // Re-render widget so the home screen sees the image immediately
                                TrmnlDeviceWidget().update(applicationContext, glanceId)
                                Timber.d("[WidgetConfig] Inline fetch succeeded for widget $appWidgetId")
                                display.refreshRate
                            } else {
                                Timber.d("[WidgetConfig] API returned no image URL — skipping inline save")
                                null
                            }
                        }
                        else -> {
                            Timber.d("[WidgetConfig] Inline fetch API call failed — worker will retry")
                            null
                        }
                    }
                }
            if (fetchResult == null) {
                Timber.d("[WidgetConfig] Inline fetch timed out or returned no result — worker will retry")
            }
            fetchResult
        } catch (e: CancellationException) {
            throw e // Do not swallow structured-concurrency cancellation
        } catch (e: Exception) {
            Timber.w(e, "[WidgetConfig] Inline fetch error — worker will retry")
            null
        }
    }

    /**
     * Downloads an image from [imageUrl] and saves it as a PNG to [outputFile].
     * Mirrors the download logic in [TrmnlWidgetRefreshWorker].
     */
    private suspend fun downloadAndSaveWidgetImage(
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
                    throw IOException("Failed to create widget image directory: ${parentDir.absolutePath}")
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

    companion object {
        /** Timeout for the inline image fetch performed in the configuration activity. */
        private const val INLINE_FETCH_TIMEOUT_MS = 15_000L
    }
}
