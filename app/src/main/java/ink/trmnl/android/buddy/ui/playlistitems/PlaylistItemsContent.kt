package ink.trmnl.android.buddy.ui.playlistitems

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.slack.circuit.codegen.annotations.CircuitInject
import dev.zacsweers.metro.AppScope
import ink.trmnl.android.buddy.R
import ink.trmnl.android.buddy.domain.models.PlaylistItemUi
import ink.trmnl.android.buddy.ui.components.TrmnlTitle
import ink.trmnl.android.buddy.ui.theme.TrmnlBuddyAppTheme

/**
 * Main UI content for the Playlist Items screen.
 *
 * Displays a list of playlist items showing plugin details, rotation strategy,
 * and rendering status with pull-to-refresh support.
 *
 * @param state Current UI state
 * @param modifier Optional modifier for the component
 */
@OptIn(ExperimentalMaterial3Api::class)
@CircuitInject(PlaylistItemsScreen::class, AppScope::class)
@Composable
fun PlaylistItemsContent(
    state: PlaylistItemsScreen.State,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    TrmnlTitle(
                        if (state.deviceId != null) {
                            "Playlist - ${state.deviceName}"
                        } else {
                            "All Playlist Items"
                        },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { state.eventSink(PlaylistItemsScreen.Event.BackClicked) }) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back_24dp_e3e3e3_fill0_wght400_grad0_opsz24),
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = { state.eventSink(PlaylistItemsScreen.Event.Refresh) },
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            when {
                state.isLoading && state.items.isEmpty() -> {
                    LoadingState()
                }

                state.errorMessage != null && state.items.isEmpty() -> {
                    ErrorState(
                        errorMessage = state.errorMessage,
                        onRetry = { state.eventSink(PlaylistItemsScreen.Event.Refresh) },
                    )
                }

                state.items.isEmpty() -> {
                    EmptyState()
                }

                else -> {
                    PlaylistItemsList(
                        items = state.items,
                        onItemClick = { item ->
                            state.eventSink(PlaylistItemsScreen.Event.ItemClicked(item))
                        },
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
        CircularProgressIndicator()
    }
}

/**
 * Error state UI with retry button.
 */
@Composable
private fun ErrorState(
    errorMessage: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.warning_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.error,
            )
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

/**
 * Empty state UI when no playlist items are found.
 */
@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.list_alt_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "No playlist items found",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Pull down to refresh",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * List of playlist items.
 */
@Composable
private fun PlaylistItemsList(
    items: List<PlaylistItemUi>,
    onItemClick: (PlaylistItemUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items, key = { it.id }) { item ->
            PlaylistItemCard(
                item = item,
                onClick = { onItemClick(item) },
            )
        }
    }
}

/**
 * Individual playlist item card.
 */
@Composable
private fun PlaylistItemCard(
    item: PlaylistItemUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Plugin name / display name
            Text(
                text = item.displayName,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            // Device and visibility info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "Device ID: ${item.deviceId}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (item.isVisible) {
                    Icon(
                        painter = painterResource(R.drawable.baseline_visibility_24),
                        contentDescription = "Visible",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(16.dp),
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.baseline_visibility_off_24),
                        contentDescription = "Hidden",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            // Rendering status
            item.renderedAt?.let { renderedAt ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.check_circle_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                        contentDescription = "Rendered",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "Rendered: $renderedAt",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Mashup indicator
            if (item.isMashup) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.widgets_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                        contentDescription = "Mashup",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "Mashup Content",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }

            // Never rendered indicator
            if (item.isNeverRendered) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.info_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                        contentDescription = "Not rendered",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "Not rendered yet",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

// === Previews ===

// Sample data for previews using domain model
private val sampleItem1 =
    PlaylistItemUi(
        id = 1,
        deviceId = 101,
        displayName = "Weather Forecast",
        isVisible = true,
        isMashup = false,
        isNeverRendered = false,
        renderedAt = "2025-02-10T10:00:00Z",
        rowOrder = 1,
        pluginName = "Weather Forecast",
        mashupId = null,
    )

private val sampleItem2 =
    PlaylistItemUi(
        id = 2,
        deviceId = 101,
        displayName = "Mashup #42",
        isVisible = false,
        isMashup = true,
        isNeverRendered = true,
        renderedAt = null,
        rowOrder = 2,
        pluginName = null,
        mashupId = 42,
    )

private val sampleItem3 =
    PlaylistItemUi(
        id = 3,
        deviceId = 102,
        displayName = "Calendar Events",
        isVisible = true,
        isMashup = false,
        isNeverRendered = false,
        renderedAt = "2025-02-10T08:00:00Z",
        rowOrder = 3,
        pluginName = "Calendar Events",
        mashupId = null,
    )

@PreviewLightDark
@Composable
private fun PlaylistItemsContentPreview() {
    TrmnlBuddyAppTheme {
        PlaylistItemsContent(
            state =
                PlaylistItemsScreen.State(
                    deviceId = 101,
                    deviceName = "Living Room Display",
                    items = listOf(sampleItem1, sampleItem2, sampleItem3),
                    isLoading = false,
                    errorMessage = null,
                ),
        )
    }
}

@PreviewLightDark
@Composable
private fun PlaylistItemsContentLoadingPreview() {
    TrmnlBuddyAppTheme {
        PlaylistItemsContent(
            state =
                PlaylistItemsScreen.State(
                    deviceId = 101,
                    deviceName = "Living Room Display",
                    items = emptyList(),
                    isLoading = true,
                    errorMessage = null,
                ),
        )
    }
}

@PreviewLightDark
@Composable
private fun PlaylistItemsContentErrorPreview() {
    TrmnlBuddyAppTheme {
        PlaylistItemsContent(
            state =
                PlaylistItemsScreen.State(
                    deviceId = 101,
                    deviceName = "Living Room Display",
                    items = emptyList(),
                    isLoading = false,
                    errorMessage = "Network error. Please check your connection.",
                ),
        )
    }
}

@PreviewLightDark
@Composable
private fun PlaylistItemsContentEmptyPreview() {
    TrmnlBuddyAppTheme {
        PlaylistItemsContent(
            state =
                PlaylistItemsScreen.State(
                    deviceId = 101,
                    deviceName = "Living Room Display",
                    items = emptyList(),
                    isLoading = false,
                    errorMessage = null,
                ),
        )
    }
}
