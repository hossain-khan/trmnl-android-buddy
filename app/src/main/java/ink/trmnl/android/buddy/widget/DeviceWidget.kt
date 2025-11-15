package ink.trmnl.android.buddy.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.CircularProgressIndicator
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import ink.trmnl.android.buddy.MainActivity
import ink.trmnl.android.buddy.R

/**
 * Glance widget that displays a TRMNL device's current display image.
 */
class DeviceWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = WidgetStateDefinition

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        provideContent {
            GlanceTheme {
                DeviceWidgetContent()
            }
        }
    }

    @Composable
    private fun DeviceWidgetContent() {
        val state = currentState<WidgetState>()

        Box(
            modifier =
                GlanceModifier
                    .fillMaxSize()
                    .background(GlanceTheme.colors.surface)
                    .clickable(onClick = actionStartActivity<MainActivity>()),
            contentAlignment = Alignment.Center,
        ) {
            when {
                state.isLoading -> LoadingContent()
                state.errorMessage != null -> ErrorContent(state.errorMessage)
                state.deviceId == null -> ConfigureContent()
                else -> DeviceContent(state)
            }

            // Refresh button in top-right corner
            if (state.deviceId != null) {
                Box(
                    modifier = GlanceModifier.fillMaxSize(),
                    contentAlignment = Alignment.TopEnd,
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.refresh_24dp_e3e3e3_fill0_wght400_grad0_opsz24),
                        contentDescription = "Refresh",
                        modifier =
                            GlanceModifier
                                .size(40.dp)
                                .padding(8.dp)
                                .clickable(onClick = actionRunCallback<RefreshWidgetCallback>()),
                    )
                }
            }
        }
    }

    @Composable
    private fun LoadingContent() {
        Column(
            modifier = GlanceModifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator()
            Spacer(modifier = GlanceModifier.height(8.dp))
            Text(
                text = "Loading...",
                style = TextStyle(fontSize = 14.sp, color = GlanceTheme.colors.onSurface),
            )
        }
    }

    @Composable
    private fun ErrorContent(errorMessage: String) {
        Column(
            modifier = GlanceModifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "⚠️",
                style = TextStyle(fontSize = 32.sp),
            )
            Spacer(modifier = GlanceModifier.height(8.dp))
            Text(
                text = "Error loading device",
                style =
                    TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlanceTheme.colors.error,
                    ),
                maxLines = 1,
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = errorMessage,
                style = TextStyle(fontSize = 12.sp, color = GlanceTheme.colors.onSurface),
                maxLines = 2,
            )
        }
    }

    @Composable
    private fun ConfigureContent() {
        Column(
            modifier = GlanceModifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "📱",
                style = TextStyle(fontSize = 48.sp),
            )
            Spacer(modifier = GlanceModifier.height(8.dp))
            Text(
                text = "Tap to configure",
                style =
                    TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlanceTheme.colors.onSurface,
                    ),
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = "Select a device to display",
                style = TextStyle(fontSize = 12.sp, color = GlanceTheme.colors.onSurface),
                maxLines = 1,
            )
        }
    }

    @Composable
    private fun DeviceContent(state: WidgetState) {
        Column(
            modifier = GlanceModifier.fillMaxSize().padding(8.dp),
        ) {
            // Device name header
            Text(
                text = state.deviceName ?: "Device",
                style =
                    TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlanceTheme.colors.onSurface,
                    ),
                maxLines = 1,
            )

            Spacer(modifier = GlanceModifier.height(4.dp))

            // Device image
            if (state.imagePath != null) {
                Box(
                    modifier =
                        GlanceModifier
                            .fillMaxWidth()
                            .defaultWeight()
                            .background(GlanceTheme.colors.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    // Load image from saved file
                    val bitmap = WidgetImageLoader.loadSavedImage(state.imagePath)
                    if (bitmap != null) {
                        Image(
                            provider = ImageProvider(bitmap),
                            contentDescription = "Device display",
                            modifier = GlanceModifier.fillMaxSize(),
                        )
                    } else {
                        // Fallback if image loading failed
                        Text(
                            text = "Device Display",
                            style = TextStyle(fontSize = 12.sp, color = GlanceTheme.colors.onSurfaceVariant),
                        )
                    }
                }
            }

            Spacer(modifier = GlanceModifier.height(4.dp))

            // Device status row
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Battery indicator
                if (state.batteryPercent != null) {
                    Text(
                        text = "🔋 ${state.batteryPercent.toInt()}%",
                        style = TextStyle(fontSize = 10.sp, color = GlanceTheme.colors.onSurface),
                    )
                }

                Spacer(modifier = GlanceModifier.width(8.dp))

                // WiFi indicator
                if (state.wifiStrength != null) {
                    Text(
                        text = "📶 ${state.wifiStrength.toInt()}%",
                        style = TextStyle(fontSize = 10.sp, color = GlanceTheme.colors.onSurface),
                    )
                }
            }
        }
    }
}
