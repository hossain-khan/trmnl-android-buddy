package ink.trmnl.android.buddy.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
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
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import ink.trmnl.android.buddy.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import android.content.Intent as AndroidIntent

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
    companion object {
        val APP_WIDGET_ID_KEY = intPreferencesKey("app_widget_id")
        val DEVICE_FRIENDLY_ID_KEY = stringPreferencesKey("device_friendly_id")
        val DEVICE_NAME_KEY = stringPreferencesKey("device_name")
        val IMAGE_FILE_PATH_KEY = stringPreferencesKey("image_file_path")
        val REFRESH_RATE_KEY = intPreferencesKey("refresh_rate")
        val LAST_UPDATED_KEY = longPreferencesKey("last_updated")
        val ERROR_MESSAGE_KEY = stringPreferencesKey("error_message")

        // Default interval used when no refresh rate is available from the API yet.
        // MIN is the floor enforced on the API-provided refresh rate to avoid excessive polling.
        const val DEFAULT_REFRESH_INTERVAL_MINUTES = 15L
        const val MIN_REFRESH_INTERVAL_MINUTES = 15L
    }

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        // Load initial prefs and decode bitmap outside the composable (IO-safe here).
        val initialPrefs = getAppWidgetState(context, PreferencesGlanceStateDefinition, id)
        val imageFilePath = initialPrefs[IMAGE_FILE_PATH_KEY]
        val bitmap =
            withContext(Dispatchers.IO) {
                imageFilePath?.let { path ->
                    val file = File(path)
                    if (file.exists()) BitmapFactory.decodeFile(path) else null
                }
            }

        provideContent {
            val prefs = currentState<Preferences>()
            val deviceFriendlyId = prefs[DEVICE_FRIENDLY_ID_KEY]
            val deviceName = prefs[DEVICE_NAME_KEY] ?: ""
            val currentImageFilePath = prefs[IMAGE_FILE_PATH_KEY]
            val errorMessage = prefs[ERROR_MESSAGE_KEY]
            val appWidgetId = prefs[APP_WIDGET_ID_KEY] ?: AppWidgetManager.INVALID_APPWIDGET_ID

            GlanceTheme {
                WidgetContent(
                    appWidgetId = appWidgetId,
                    deviceFriendlyId = deviceFriendlyId,
                    deviceName = deviceName,
                    bitmap =
                        if (currentImageFilePath != null && currentImageFilePath == imageFilePath) {
                            bitmap
                        } else {
                            null
                        },
                    errorMessage = errorMessage,
                )
            }
        }
    }

    @Composable
    private fun WidgetContent(
        appWidgetId: Int,
        deviceFriendlyId: String?,
        deviceName: String,
        bitmap: android.graphics.Bitmap?,
        errorMessage: String?,
    ) {
        Box(
            modifier =
                GlanceModifier
                    .fillMaxSize()
                    .background(GlanceTheme.colors.surface)
                    .appWidgetBackground()
                    .cornerRadius(16.dp)
                    .padding(8.dp),
        ) {
            when {
                deviceFriendlyId == null -> UnconfiguredContent(appWidgetId = appWidgetId)
                errorMessage != null ->
                    ErrorContent(
                        deviceName = deviceName,
                        errorMessage = errorMessage,
                        appWidgetId = appWidgetId,
                    )

                bitmap != null ->
                    DisplayContent(
                        deviceName = deviceName,
                        bitmap = bitmap,
                        appWidgetId = appWidgetId,
                    )

                else -> LoadingContent(deviceName = deviceName)
            }
        }
    }

    @Composable
    private fun UnconfiguredContent(appWidgetId: Int) {
        val context = LocalContext.current
        val configureIntent =
            AndroidIntent(context, WidgetConfigurationActivity::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
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
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = if (deviceName.isNotEmpty()) "Loading $deviceName…" else "Loading…",
                style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 14.sp),
            )
        }
    }

    @Composable
    private fun ErrorContent(
        deviceName: String,
        errorMessage: String,
        appWidgetId: Int,
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (deviceName.isNotEmpty()) {
                Text(
                    text = deviceName,
                    style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 12.sp),
                )
                Spacer(modifier = GlanceModifier.height(4.dp))
            }
            Text(
                text = errorMessage,
                style = TextStyle(color = GlanceTheme.colors.error, fontSize = 12.sp),
            )
            Spacer(modifier = GlanceModifier.height(8.dp))
            Image(
                provider = ImageProvider(R.drawable.refresh_24dp_e3e3e3_fill0_wght400_grad0_opsz24),
                contentDescription = "Retry",
                modifier =
                    GlanceModifier
                        .size(24.dp)
                        .clickable(
                            actionRunCallback<RefreshWidgetCallback>(
                                actionParametersOf(
                                    RefreshWidgetCallback.APP_WIDGET_ID_KEY to appWidgetId,
                                ),
                            ),
                        ),
            )
        }
    }

    @Composable
    private fun DisplayContent(
        deviceName: String,
        bitmap: android.graphics.Bitmap,
        appWidgetId: Int,
    ) {
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
                Image(
                    provider = ImageProvider(R.drawable.refresh_24dp_e3e3e3_fill0_wght400_grad0_opsz24),
                    contentDescription = "Refresh",
                    modifier =
                        GlanceModifier
                            .size(20.dp)
                            .clickable(
                                actionRunCallback<RefreshWidgetCallback>(
                                    actionParametersOf(
                                        RefreshWidgetCallback.APP_WIDGET_ID_KEY to appWidgetId,
                                    ),
                                ),
                            ),
                )
            }

            // Device display image
            Image(
                provider = ImageProvider(bitmap),
                contentDescription = "TRMNL display: $deviceName",
                modifier = GlanceModifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        }
    }
}
