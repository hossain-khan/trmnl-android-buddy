package ink.trmnl.android.buddy.ui.devicecatalog

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.slack.circuit.codegen.annotations.CircuitInject
import dev.zacsweers.metro.AppScope
import ink.trmnl.android.buddy.R
import ink.trmnl.android.buddy.api.models.DeviceModel
import ink.trmnl.android.buddy.ui.components.TrmnlTitle
import ink.trmnl.android.buddy.ui.theme.TrmnlBuddyAppTheme
import kotlinx.coroutines.launch

/**
 * Main UI content for the device catalog screen.
 *
 * Displays a list of device models with filtering capabilities by device kind.
 * Includes loading, error, and empty states.
 *
 * @param state Current UI state
 * @param modifier Optional modifier for the component
 */
@OptIn(ExperimentalMaterial3Api::class)
@CircuitInject(DeviceCatalogScreen::class, AppScope::class)
@Composable
fun DeviceCatalogContent(
    state: DeviceCatalogScreen.State,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { TrmnlTitle("Device Catalog") },
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
    ) { innerPadding ->
        when {
            state.isLoading -> {
                LoadingState(modifier = Modifier.padding(innerPadding))
            }

            state.error != null -> {
                ErrorState(
                    errorMessage = state.error,
                    onRetry = { state.eventSink(DeviceCatalogScreen.Event.RetryClicked) },
                    modifier = Modifier.padding(innerPadding),
                )
            }

            state.devices.isEmpty() -> {
                EmptyState(modifier = Modifier.padding(innerPadding))
            }

            else -> {
                DeviceListContent(
                    devices = state.devices,
                    selectedFilter = state.selectedFilter,
                    onFilterSelected = { filter ->
                        state.eventSink(DeviceCatalogScreen.Event.FilterSelected(filter))
                    },
                    onDeviceClick = { device ->
                        state.eventSink(DeviceCatalogScreen.Event.DeviceClicked(device))
                    },
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }

        // Bottom sheet for device details
        if (state.selectedDevice != null) {
            ModalBottomSheet(
                onDismissRequest = {
                    state.eventSink(DeviceCatalogScreen.Event.DismissBottomSheet)
                },
                sheetState = sheetState,
            ) {
                DeviceDetailsBottomSheet(
                    device = state.selectedDevice,
                    onDismiss = {
                        scope.launch {
                            sheetState.hide()
                            state.eventSink(DeviceCatalogScreen.Event.DismissBottomSheet)
                        }
                    },
                )
            }
        }
    }
}

/**
 * Content showing the device list with filters.
 */
@Composable
private fun DeviceListContent(
    devices: List<DeviceModel>,
    selectedFilter: DeviceKind?,
    onFilterSelected: (DeviceKind?) -> Unit,
    onDeviceClick: (DeviceModel) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Filter devices based on selected filter
    val filteredDevices =
        when (selectedFilter) {
            null -> devices // All
            DeviceKind.TRMNL -> devices.filter { it.kind == "trmnl" }
            DeviceKind.KINDLE -> devices.filter { it.kind == "kindle" }
            DeviceKind.BYOD -> devices.filter { it.kind == "byod" }
        }

    // Calculate counts for each filter
    val totalCount = devices.size
    val trmnlCount = devices.count { it.kind == "trmnl" }
    val kindleCount = devices.count { it.kind == "kindle" }
    val byodCount = devices.count { it.kind == "byod" }

    Column(modifier = modifier.fillMaxSize()) {
        // Filter chips row
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = selectedFilter == null,
                onClick = { onFilterSelected(null) },
                label = { Text("All ($totalCount)") },
            )
            FilterChip(
                selected = selectedFilter == DeviceKind.TRMNL,
                onClick = { onFilterSelected(DeviceKind.TRMNL) },
                label = { Text("TRMNL ($trmnlCount)") },
            )
            FilterChip(
                selected = selectedFilter == DeviceKind.KINDLE,
                onClick = { onFilterSelected(DeviceKind.KINDLE) },
                label = { Text("Kindle ($kindleCount)") },
            )
            FilterChip(
                selected = selectedFilter == DeviceKind.BYOD,
                onClick = { onFilterSelected(DeviceKind.BYOD) },
                label = { Text("BYOD ($byodCount)") },
            )
        }

        // Device list
        if (filteredDevices.isEmpty()) {
            // Show empty state when filter returns no results
            EmptyState()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(filteredDevices, key = { it.name }) { device ->
                    DeviceListItem(
                        device = device,
                        onClick = { onDeviceClick(device) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

/**
 * Loading state UI.
 */
@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

/**
 * Error state UI.
 */
@Composable
private fun ErrorState(
    errorMessage: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(R.drawable.info_24dp_e8eaed_fill0_wght400_grad0_opsz24),
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Failed to load devices",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}

/**
 * Empty state UI.
 */
@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(R.drawable.nest_display_24dp_e3e3e3_fill0_wght400_grad0_opsz24),
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No devices found",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

// ============================================
// Composable Previews
// ============================================

@PreviewLightDark
@Composable
private fun DeviceCatalogContentPreview() {
    TrmnlBuddyAppTheme {
        DeviceCatalogContent(
            state =
                DeviceCatalogScreen.State(
                    devices = previewDevices,
                    selectedFilter = null,
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
                    devices = previewDevices,
                    selectedFilter = DeviceKind.TRMNL,
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
                    error = "Network error. Please check your connection.",
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
                ),
        )
    }
}

/**
 * Preview device models for previews.
 */
private val previewDevices =
    listOf(
        DeviceModel(
            name = "og_png",
            label = "TRMNL OG (1-bit)",
            description = "TRMNL OG (1-bit)",
            width = 800,
            height = 480,
            colors = 2,
            bitDepth = 1,
            scaleFactor = 1.0,
            rotation = 0,
            mimeType = "image/png",
            offsetX = 0,
            offsetY = 0,
            publishedAt = "2024-01-01T00:00:00.000Z",
            kind = "trmnl",
            paletteIds = listOf("bw"),
        ),
        DeviceModel(
            name = "og_plus",
            label = "TRMNL OG (2-bit)",
            description = "TRMNL OG (2-bit)",
            width = 800,
            height = 480,
            colors = 4,
            bitDepth = 2,
            scaleFactor = 1.0,
            rotation = 0,
            mimeType = "image/png",
            offsetX = 0,
            offsetY = 0,
            publishedAt = "2024-01-01T00:00:00.000Z",
            kind = "trmnl",
            paletteIds = listOf("gray-4", "bw"),
        ),
        DeviceModel(
            name = "amazon_kindle_2024",
            label = "Amazon Kindle 2024",
            description = "Amazon Kindle 2024",
            width = 1400,
            height = 840,
            colors = 256,
            bitDepth = 8,
            scaleFactor = 1.75,
            rotation = 90,
            mimeType = "image/png",
            offsetX = 75,
            offsetY = 25,
            publishedAt = "2024-01-01T00:00:00.000Z",
            kind = "kindle",
            paletteIds = listOf("gray-256"),
        ),
        DeviceModel(
            name = "inkplate_10",
            label = "Inkplate 10",
            description = "Inkplate 10",
            width = 1200,
            height = 820,
            colors = 8,
            bitDepth = 3,
            scaleFactor = 1.0,
            rotation = 0,
            mimeType = "image/png",
            offsetX = 0,
            offsetY = 0,
            publishedAt = "2024-01-01T00:00:00.000Z",
            kind = "byod",
            paletteIds = listOf("gray-4", "bw"),
        ),
    )
