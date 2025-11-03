package ink.trmnl.android.buddy.ui.devicecatalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.slack.circuit.codegen.annotations.CircuitInject
import dev.zacsweers.metro.AppScope
import ink.trmnl.android.buddy.R
import ink.trmnl.android.buddy.api.models.DeviceKind
import ink.trmnl.android.buddy.api.models.DeviceModel
import ink.trmnl.android.buddy.ui.theme.TrmnlBuddyAppTheme

/**
 * Device Catalog screen content.
 *
 * Displays a filterable list of TRMNL device models with support for:
 * - Official TRMNL devices
 * - Amazon Kindle e-readers
 * - BYOD (Bring Your Own Device) compatible displays
 */
@CircuitInject(DeviceCatalogScreen::class, AppScope::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceCatalogContent(
    state: DeviceCatalogScreen.State,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Device Catalog") },
                navigationIcon = {
                    IconButton(onClick = { state.eventSink(DeviceCatalogScreen.Event.BackClicked) }) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back_24dp_e3e3e3_fill0_wght400_grad0_opsz24),
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        when {
            state.isLoading -> {
                LoadingState(modifier = Modifier.padding(innerPadding))
            }
            state.error != null -> {
                ErrorState(
                    error = state.error,
                    onRetry = { state.eventSink(DeviceCatalogScreen.Event.RetryClicked) },
                    modifier = Modifier.padding(innerPadding),
                )
            }
            state.devices.isEmpty() -> {
                EmptyState(modifier = Modifier.padding(innerPadding))
            }
            else -> {
                DeviceList(
                    state = state,
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }
    }
}

@Composable
private fun DeviceList(
    state: DeviceCatalogScreen.State,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Filter chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            // All filter chip
            item {
                FilterChip(
                    selected = state.selectedFilter == null,
                    onClick = { state.eventSink(DeviceCatalogScreen.Event.FilterSelected(null)) },
                    label = { Text("All (${state.getCountForKind(null)})") },
                )
            }

            // Device kind filter chips
            items(DeviceKind.entries) { kind ->
                val count = state.getCountForKind(kind)
                FilterChip(
                    selected = state.selectedFilter == kind,
                    onClick = { state.eventSink(DeviceCatalogScreen.Event.FilterSelected(kind)) },
                    label = { Text("${kind.displayName} ($count)") },
                    enabled = count > 0,
                )
            }
        }

        // Device list
        if (state.filteredDevices.isEmpty()) {
            // No devices match filter
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No devices found for this filter",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(
                    items = state.filteredDevices,
                    key = { it.name },
                ) { device ->
                    DeviceListItem(
                        device = device,
                        onClick = { state.eventSink(DeviceCatalogScreen.Event.DeviceClicked(device)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorState(
    error: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
            TextButton(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "No devices available",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

// Previews

@PreviewLightDark
@Composable
private fun DeviceCatalogContentPreview() {
    TrmnlBuddyAppTheme {
        DeviceCatalogContent(
            state =
                DeviceCatalogScreen.State(
                    devices = sampleDevices,
                    filteredDevices = sampleDevices,
                    selectedFilter = null,
                    isLoading = false,
                    error = null,
                ),
        )
    }
}

@PreviewLightDark
@Composable
private fun DeviceCatalogContentFilteredPreview() {
    TrmnlBuddyAppTheme {
        DeviceCatalogContent(
            state =
                DeviceCatalogScreen.State(
                    devices = sampleDevices,
                    filteredDevices = sampleDevices.filter { it.kind == "trmnl" },
                    selectedFilter = DeviceKind.TRMNL,
                    isLoading = false,
                    error = null,
                ),
        )
    }
}

@PreviewLightDark
@Composable
private fun DeviceCatalogContentLoadingPreview() {
    TrmnlBuddyAppTheme {
        DeviceCatalogContent(
            state =
                DeviceCatalogScreen.State(
                    isLoading = true,
                ),
        )
    }
}

@PreviewLightDark
@Composable
private fun DeviceCatalogContentErrorPreview() {
    TrmnlBuddyAppTheme {
        DeviceCatalogContent(
            state =
                DeviceCatalogScreen.State(
                    error = "Failed to load devices: Network error",
                ),
        )
    }
}

@PreviewLightDark
@Composable
private fun DeviceCatalogContentEmptyPreview() {
    TrmnlBuddyAppTheme {
        DeviceCatalogContent(
            state =
                DeviceCatalogScreen.State(
                    devices = emptyList(),
                    filteredDevices = emptyList(),
                    selectedFilter = null,
                    isLoading = false,
                    error = null,
                ),
        )
    }
}

// Sample data for previews
private val sampleDevices =
    listOf(
        DeviceModel(
            name = "og_png",
            label = "TRMNL",
            description = "The original TRMNL device",
            width = 800,
            height = 480,
            colors = 256,
            bitDepth = 8,
            scaleFactor = 1.0,
            rotation = 0,
            mimeType = "image/png",
            offsetX = 0,
            offsetY = 0,
            publishedAt = "2024-01-01T00:00:00Z",
            kind = "trmnl",
            paletteIds = listOf("gray-256"),
        ),
        DeviceModel(
            name = "og_plus",
            label = "TRMNL Plus",
            description = "The enhanced TRMNL device",
            width = 800,
            height = 480,
            colors = 7,
            bitDepth = 3,
            scaleFactor = 1.0,
            rotation = 0,
            mimeType = "image/png",
            offsetX = 0,
            offsetY = 0,
            publishedAt = "2024-01-01T00:00:00Z",
            kind = "trmnl",
            paletteIds = listOf("color-7a"),
        ),
        DeviceModel(
            name = "kindle_2024",
            label = "Kindle (2024)",
            description = "Latest Kindle model",
            width = 758,
            height = 1024,
            colors = 16,
            bitDepth = 4,
            scaleFactor = 1.0,
            rotation = 0,
            mimeType = "image/png",
            offsetX = 0,
            offsetY = 0,
            publishedAt = "2024-01-01T00:00:00Z",
            kind = "kindle",
            paletteIds = listOf("gray-16"),
        ),
    )
