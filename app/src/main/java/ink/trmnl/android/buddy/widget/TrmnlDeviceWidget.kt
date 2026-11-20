package ink.trmnl.android.buddy.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.graphics.Bitmap
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.LocalState
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.wrapContentHeight
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import ink.trmnl.android.buddy.MainActivity
import ink.trmnl.android.buddy.R
import timber.log.Timber
import android.content.Intent as AndroidIntent

/** Returns an [AndroidIntent] that opens [MainActivity] directly on the devices screen. */
private fun openDevicesIntent(context: Context): AndroidIntent =
    AndroidIntent(context, MainActivity::class.java).apply {
        putExtra(MainActivity.EXTRA_OPEN_DEVICES_SCREEN, true)
        addFlags(AndroidIntent.FLAG_ACTIVITY_NEW_TASK or AndroidIntent.FLAG_ACTIVITY_CLEAR_TOP)
    }

/**
 * TRMNL Device Widget – Glance-based app widget that shows the current
 * display image from a selected TRMNL e-ink device.
 *
 * Widget states:
 *  - **Unconfigured** – Device not yet selected; shows tap-to-configure prompt
 *  - **Loading** – Device configured but image not yet fetched
 *  - **Error** – Refresh failed; shows error text with retry button
 *  - **Content** – Shows device name + current display bitmap
 *
 * Image files are downloaded by [TrmnlWidgetRefreshWorker] and stored at
 * `filesDir/widget_images/widget_{appWidgetId}.png`. The file path is stored
 * in the per-widget Glance PreferencesGlanceState so the bitmap is decoded
 * in [provideGlance] (a suspend function, safe for IO).
 *
 * @see TrmnlDeviceWidgetReceiver
 * @see TrmnlWidgetRefreshWorker
 * @see WidgetConfigurationActivity
 */
class TrmnlDeviceWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    companion object {
        val APP_WIDGET_ID_KEY = intPreferencesKey("app_widget_id")
        val DEVICE_FRIENDLY_ID_KEY = stringPreferencesKey("device_friendly_id")
        val DEVICE_NAME_KEY = stringPreferencesKey("device_name")
        val IMAGE_FILE_PATH_KEY = stringPreferencesKey("image_file_path")
        val REFRESH_RATE_KEY = intPreferencesKey("refresh_rate")
        val LAST_UPDATED_KEY = longPreferencesKey("last_updated")
        val ERROR_MESSAGE_KEY = stringPreferencesKey("error_message")

        /** Set to `true` by [RefreshWidgetCallback] while a manual refresh is in flight. */
        val IS_REFRESHING_KEY = booleanPreferencesKey("is_refreshing")

        // Default interval used when no refresh rate is available from the API yet.
        // MIN is the floor enforced on the API-provided refresh rate to avoid excessive polling.
        const val DEFAULT_REFRESH_INTERVAL_MINUTES = 15L
        const val MIN_REFRESH_INTERVAL_MINUTES = 15L

        // Upper-bound widget target dimensions (dp) used for bitmap downsampling.
        private const val MAX_WIDGET_WIDTH_DP = 400
        private const val MAX_WIDGET_HEIGHT_DP = 300
    }

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        // Hoist decoded bitmap cache outside provideContent so SizeMode.Exact
        // shares one Bitmap across all size compositions without re-decoding,
        // while re-decoding when the path or LAST_UPDATED_KEY changes during an active session.
        var cachedPath: String? = null
        var cachedLastUpdated = -1L
        var cachedBitmap: Bitmap? = null

        provideContent {
            val prefs = (LocalState.current as? Preferences) ?: emptyPreferences()
            val deviceFriendlyId = prefs[DEVICE_FRIENDLY_ID_KEY]
            val deviceName = prefs[DEVICE_NAME_KEY] ?: ""
            val errorMessage = prefs[ERROR_MESSAGE_KEY]
            val appWidgetId = prefs[APP_WIDGET_ID_KEY] ?: AppWidgetManager.INVALID_APPWIDGET_ID
            val imageFilePath = prefs[IMAGE_FILE_PATH_KEY]
            val lastUpdated = prefs[LAST_UPDATED_KEY] ?: 0L
            val isRefreshing = prefs[IS_REFRESHING_KEY] ?: false

            if (imageFilePath != cachedPath || lastUpdated != cachedLastUpdated) {
                cachedPath = imageFilePath
                cachedLastUpdated = lastUpdated
                cachedBitmap =
                    imageFilePath?.let { path ->
                        WidgetBitmapUtils.decodeDownsampledWidgetBitmap(
                            context = context,
                            filePath = path,
                            maxWidthDp = MAX_WIDGET_WIDTH_DP,
                            maxHeightDp = MAX_WIDGET_HEIGHT_DP,
                            distinctImageCount = 1,
                            config = Bitmap.Config.RGB_565,
                        )
                    }
            }

            GlanceTheme {
                WidgetContent(
                    appWidgetId = appWidgetId,
                    deviceFriendlyId = deviceFriendlyId,
                    deviceName = deviceName,
                    bitmap = cachedBitmap,
                    errorMessage = errorMessage,
                    isRefreshing = isRefreshing,
                )
            }
        }
    }

    override fun onCompositionError(
        context: Context,
        glanceId: GlanceId,
        appWidgetId: Int,
        throwable: Throwable,
    ) {
        Timber.e(throwable, "[TrmnlDeviceWidget] Composition error for widget $appWidgetId")
        val fallback =
            RemoteViews(context.packageName, R.layout.trmnl_widget_initial_layout).apply {
                setTextViewText(R.id.widget_status_text, "Unable to display widget. Tap to open app.")
            }
        AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, fallback)
    }

    @Composable
    internal fun WidgetContent(
        appWidgetId: Int,
        deviceFriendlyId: String?,
        deviceName: String,
        bitmap: Bitmap?,
        errorMessage: String?,
        isRefreshing: Boolean,
    ) {
        val size = LocalSize.current
        val isCompact = size.width < 180.dp || size.height < 110.dp
        val contentPadding = if (isCompact) 6.dp else 8.dp

        Box(
            modifier =
                GlanceModifier
                    .fillMaxSize()
                    .background(GlanceTheme.colors.surface)
                    .appWidgetBackground()
                    .cornerRadius(16.dp)
                    .padding(contentPadding),
        ) {
            when {
                deviceFriendlyId == null -> {
                    UnconfiguredContent(appWidgetId = appWidgetId)
                }

                errorMessage != null -> {
                    ErrorContent(
                        deviceName = deviceName,
                        appWidgetId = appWidgetId,
                        isRefreshing = isRefreshing,
                    )
                }

                bitmap != null -> {
                    DisplayContent(
                        deviceName = deviceName,
                        bitmap = bitmap,
                        appWidgetId = appWidgetId,
                        isRefreshing = isRefreshing,
                    )
                }

                else -> {
                    LoadingContent(deviceName = deviceName)
                }
            }
        }
    }

    @Composable
    private fun UnconfiguredContent(appWidgetId: Int) {
        val context = LocalContext.current
        val configureIntent =
            AndroidIntent(context, WidgetConfigurationActivity::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                addFlags(AndroidIntent.FLAG_ACTIVITY_NEW_TASK)
            }
        Column(
            modifier =
                GlanceModifier
                    .fillMaxSize()
                    .clickable(actionStartActivity(configureIntent)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                provider = ImageProvider(R.drawable.widgets_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                contentDescription = null,
                modifier = GlanceModifier.size(48.dp),
                colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurfaceVariant),
            )
            Spacer(modifier = GlanceModifier.height(8.dp))
            Text(
                text = "Tap to select a device",
                style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 14.sp),
            )
        }
    }

    @Composable
    private fun LoadingContent(deviceName: String) {
        val context = LocalContext.current
        Column(
            modifier =
                GlanceModifier
                    .fillMaxSize()
                    .clickable(actionStartActivity(openDevicesIntent(context))),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                provider = ImageProvider(R.drawable.clock_loader_40_24dp_999999_fill0_wght400_grad0_opsz24),
                contentDescription = null,
                modifier = GlanceModifier.size(36.dp),
                alpha = 0.6f,
                colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurfaceVariant),
            )
            Spacer(modifier = GlanceModifier.height(10.dp))
            if (deviceName.isNotEmpty()) {
                Text(
                    text = deviceName,
                    style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 13.sp),
                )
                Spacer(modifier = GlanceModifier.height(2.dp))
            }
            Text(
                text = "Fetching display…",
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp),
            )
        }
    }

    @Composable
    private fun ErrorContent(
        deviceName: String,
        appWidgetId: Int,
        isRefreshing: Boolean,
    ) {
        val context = LocalContext.current
        Column(
            modifier =
                GlanceModifier
                    .fillMaxSize()
                    .clickable(actionStartActivity(openDevicesIntent(context))),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                provider = ImageProvider(R.drawable.warning_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                contentDescription = null,
                modifier = GlanceModifier.size(32.dp),
                alpha = 0.8f,
                colorFilter = ColorFilter.tint(GlanceTheme.colors.error),
            )
            Spacer(modifier = GlanceModifier.height(8.dp))
            if (deviceName.isNotEmpty()) {
                Text(
                    text = deviceName,
                    style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 13.sp),
                )
                Spacer(modifier = GlanceModifier.height(2.dp))
            }
            Text(
                text = "Couldn't refresh display",
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp),
            )
            Spacer(modifier = GlanceModifier.height(12.dp))
            if (isRefreshing) {
                Image(
                    provider = ImageProvider(R.drawable.clock_loader_40_24dp_999999_fill0_wght400_grad0_opsz24),
                    contentDescription = "Refreshing…",
                    modifier = GlanceModifier.size(32.dp),
                    alpha = 0.5f,
                    colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurfaceVariant),
                )
            } else {
                Image(
                    provider = ImageProvider(R.drawable.refresh_24dp_e3e3e3_fill0_wght400_grad0_opsz24),
                    contentDescription = "Retry",
                    modifier =
                        GlanceModifier
                            .size(32.dp)
                            .clickable(
                                actionRunCallback<RefreshWidgetCallback>(
                                    actionParametersOf(
                                        RefreshWidgetCallback.APP_WIDGET_ID_KEY to appWidgetId,
                                    ),
                                ),
                            ),
                    colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurface),
                )
            }
        }
    }

    @Composable
    private fun DisplayContent(
        deviceName: String,
        bitmap: Bitmap,
        appWidgetId: Int,
        isRefreshing: Boolean,
    ) {
        val context = LocalContext.current
        Column(modifier = GlanceModifier.fillMaxSize()) {
            // Header row: device name + refresh button
            Row(
                modifier =
                    GlanceModifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = deviceName,
                    modifier = GlanceModifier.defaultWeight(),
                    style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 12.sp),
                )
                if (isRefreshing) {
                    // Show a static clock-loader icon while the refresh worker is running.
                    // Glance does not support animated drawables, so a static frame communicates
                    // the in-progress state without animation.
                    Box(
                        modifier = GlanceModifier.size(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            provider = ImageProvider(R.drawable.clock_loader_40_24dp_999999_fill0_wght400_grad0_opsz24),
                            contentDescription = "Refreshing…",
                            modifier = GlanceModifier.size(20.dp),
                            alpha = 0.5f,
                            colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurfaceVariant),
                        )
                    }
                } else {
                    Box(
                        modifier =
                            GlanceModifier
                                .size(32.dp)
                                .clickable(
                                    actionRunCallback<RefreshWidgetCallback>(
                                        actionParametersOf(
                                            RefreshWidgetCallback.APP_WIDGET_ID_KEY to appWidgetId,
                                        ),
                                    ),
                                ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            provider = ImageProvider(R.drawable.refresh_24dp_e3e3e3_fill0_wght400_grad0_opsz24),
                            contentDescription = "Refresh",
                            modifier = GlanceModifier.size(20.dp),
                            colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurface),
                        )
                    }
                }
            }

            // Device display image — tap opens the app directly on the devices screen
            Image(
                provider = ImageProvider(bitmap),
                contentDescription = "TRMNL display: $deviceName",
                modifier =
                    GlanceModifier
                        .fillMaxSize()
                        .clickable(actionStartActivity(openDevicesIntent(context))),
                contentScale = ContentScale.Fit,
            )
        }
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 250, heightDp = 130)
@Composable
private fun TrmnlDeviceWidgetUnconfiguredPreview() {
    GlanceTheme {
        TrmnlDeviceWidget().WidgetContent(
            appWidgetId = 1,
            deviceFriendlyId = null,
            deviceName = "",
            bitmap = null,
            errorMessage = null,
            isRefreshing = false,
        )
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 250, heightDp = 130)
@Composable
private fun TrmnlDeviceWidgetLoadingPreview() {
    GlanceTheme {
        TrmnlDeviceWidget().WidgetContent(
            appWidgetId = 1,
            deviceFriendlyId = "ABC123",
            deviceName = "Living Room TRMNL",
            bitmap = null,
            errorMessage = null,
            isRefreshing = false,
        )
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 250, heightDp = 130)
@Composable
private fun TrmnlDeviceWidgetErrorPreview() {
    GlanceTheme {
        TrmnlDeviceWidget().WidgetContent(
            appWidgetId = 1,
            deviceFriendlyId = "ABC123",
            deviceName = "Living Room TRMNL",
            bitmap = null,
            errorMessage = "Network error",
            isRefreshing = false,
        )
    }
}
