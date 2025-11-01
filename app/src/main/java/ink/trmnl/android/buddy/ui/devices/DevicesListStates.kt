package ink.trmnl.android.buddy.ui.devices

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ink.trmnl.android.buddy.R
import ink.trmnl.android.buddy.api.models.Device
import ink.trmnl.android.buddy.content.models.ContentItem
import ink.trmnl.android.buddy.ui.theme.TrmnlBuddyAppTheme

/**
 * Loading state composable.
 * Shows a centered loading indicator with message.
 */
@Composable
internal fun LoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CircularProgressIndicator()
            Text("Loading devices...")
        }
    }
}

/**
 * Error state composable.
 * Shows error message and optional reset button.
 */
@Composable
internal fun ErrorState(
    errorMessage: String,
    isUnauthorized: Boolean,
    onResetToken: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Error",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.error,
            )
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (isUnauthorized) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = onResetToken) {
                    Text("Reset Token")
                }
            }
        }
    }
}

/**
 * Empty state composable.
 * Shows when no devices are found.
 */
@Composable
internal fun EmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.trmnl_device_frame),
                contentDescription = "No devices",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "No devices found",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "Your TRMNL devices will appear here",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Devices list composable.
 * Shows scrollable list of device cards with content carousel at top.
 */
@Composable
internal fun DevicesList(
    devices: List<Device>,
    deviceTokens: Map<String, String?>,
    devicePreviews: Map<String, DevicePreviewInfo?>,
    isPrivacyEnabled: Boolean,
    innerPadding: PaddingValues,
    latestContent: List<ContentItem>,
    isContentLoading: Boolean,
    isRssFeedContentEnabled: Boolean,
    onDeviceClick: (Device) -> Unit,
    onSettingsClick: (Device) -> Unit,
    onPreviewClick: (Device, DevicePreviewInfo) -> Unit,
    onContentItemClick: (ContentItem) -> Unit,
    onViewAllContentClick: () -> Unit,
    eventSink: (TrmnlDevicesScreen.Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier =
            modifier
                .fillMaxSize()
                .padding(innerPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Content carousel at the top
        // Only show if RSS feed content is enabled AND there is unread content
        // latestContent already contains only unread items from getLatestUnreadContent()
        if (isRssFeedContentEnabled && latestContent.isNotEmpty()) {
            item {
                ContentCarousel(
                    content = latestContent,
                    isLoading = isContentLoading,
                    onContentClick = onContentItemClick,
                    onViewAllClick = onViewAllContentClick,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        items(
            items = devices,
            key = { device -> device.id },
        ) { device ->
            DeviceCard(
                device = device,
                hasToken = deviceTokens[device.friendlyId] != null,
                previewInfo = devicePreviews[device.friendlyId],
                isPrivacyEnabled = isPrivacyEnabled,
                onClick = { onDeviceClick(device) },
                onSettingsClick = { onSettingsClick(device) },
                onPreviewClick = {
                    devicePreviews[device.friendlyId]?.let { previewInfo ->
                        onPreviewClick(device, previewInfo)
                    }
                },
                eventSink = eventSink,
            )
        }
    }
}

// ========== Previews ==========

/**
 * Sample device data for previews.
 */
private val sampleDevice1 =
    Device(
        id = 1,
        name = "Living Room Display",
        friendlyId = "ABC-123",
        macAddress = "12:34:56:78:9A:BC",
        batteryVoltage = 3.7,
        rssi = -45,
        percentCharged = 85.0,
        wifiStrength = 90.0,
    )

private val sampleDevice2 =
    Device(
        id = 2,
        name = "Kitchen Dashboard",
        friendlyId = "DEF-456",
        macAddress = "12:34:56:78:9A:BD",
        batteryVoltage = 3.5,
        rssi = -65,
        percentCharged = 45.0,
        wifiStrength = 60.0,
    )

private val sampleDevice3 =
    Device(
        id = 3,
        name = "Office Monitor",
        friendlyId = "GHI-789",
        macAddress = "12:34:56:78:9A:BE",
        batteryVoltage = 3.2,
        rssi = -80,
        percentCharged = 15.0,
        wifiStrength = 25.0,
    )

/**
 * Sample content items for previews.
 */
private val sampleAnnouncement =
    ContentItem.Announcement(
        id = "1",
        title = "New Feature: Screen Sharing",
        summary =
            "We've added the ability to share your screen content with others. " +
                "Check out the new settings panel to get started.",
        link = "https://usetrmnl.com/announcements/screen-sharing",
        publishedDate =
            java.time.Instant
                .now()
                .minus(2, java.time.temporal.ChronoUnit.DAYS),
        isRead = false,
    )

private val sampleBlogPost =
    ContentItem.BlogPost(
        id = "2",
        title = "Building the Perfect E-Ink Dashboard",
        summary =
            "Learn how to create an efficient and beautiful dashboard for your TRMNL device. " +
                "We'll cover layout design, data sources, and optimization tips.",
        link = "https://usetrmnl.com/blog/perfect-dashboard",
        publishedDate =
            java.time.Instant
                .now()
                .minus(5, java.time.temporal.ChronoUnit.HOURS),
        isRead = false,
        authorName = "John Doe",
        category = "Tutorial",
        featuredImageUrl = "https://usetrmnl.com/images/blog/dashboard.jpg",
        isFavorite = false,
    )

private val sampleContentList =
    listOf(
        sampleBlogPost, // Recent blog post (5 hours ago)
        sampleAnnouncement, // Older announcement (2 days ago)
    )

@Preview
@Composable
private fun LoadingStatePreview() {
    TrmnlBuddyAppTheme {
        LoadingState()
    }
}

@PreviewLightDark
@Composable
private fun ErrorStateUnauthorizedPreview() {
    TrmnlBuddyAppTheme {
        ErrorState(
            errorMessage = "Unauthorized. Please check your API token.",
            isUnauthorized = true,
            onResetToken = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun ErrorStateNetworkPreview() {
    TrmnlBuddyAppTheme {
        ErrorState(
            errorMessage = "Network error. Please check your connection.",
            isUnauthorized = false,
            onResetToken = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun EmptyStatePreview() {
    TrmnlBuddyAppTheme {
        EmptyState()
    }
}

@PreviewLightDark
@Composable
private fun DevicesListPreview() {
    TrmnlBuddyAppTheme {
        DevicesList(
            devices = listOf(sampleDevice1, sampleDevice2, sampleDevice3),
            deviceTokens = mapOf("ABC-123" to "token1", "DEF-456" to "token2"),
            devicePreviews = emptyMap(),
            isPrivacyEnabled = false,
            innerPadding = PaddingValues(0.dp),
            latestContent = sampleContentList,
            isContentLoading = false,
            isRssFeedContentEnabled = true,
            onDeviceClick = {},
            onSettingsClick = {},
            onPreviewClick = { _, _ -> },
            onContentItemClick = {},
            onViewAllContentClick = {},
            eventSink = {},
        )
    }
}
