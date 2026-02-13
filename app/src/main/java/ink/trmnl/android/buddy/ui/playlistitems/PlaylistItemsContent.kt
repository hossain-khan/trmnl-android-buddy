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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
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
import ink.trmnl.android.buddy.util.formatRelativeTime
import java.time.Instant

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
 * Determines the clock loader icon based on when the item was rendered.
 *
 * Distribution:
 * - Most recently rendered ("Now Showing"): clock_loader_10 (just started)
 * - Oldest rendered: clock_loader_90 (most time elapsed)
 * - Items in between: interpolated across 10, 20, 40, 60, 80, 90
 *
 * @param item The item to get the icon for
 * @param allItems All items to determine the age distribution
 * @return The drawable resource ID for the appropriate clock loader icon
 */
private fun getClockLoaderIconForItem(
    item: PlaylistItemUi,
    allItems: List<PlaylistItemUi>,
): Int {
    val renderedItems = allItems.filter { it.renderedAt != null }
    if (renderedItems.isEmpty() || item.renderedAt == null) {
        return R.drawable.clock_loader_10_24dp_999999_fill0_wght400_grad0_opsz24
    }

    val minTimestamp =
        renderedItems.minOf { Instant.parse(it.renderedAt!!).epochSecond }
    val maxTimestamp =
        renderedItems.maxOf { Instant.parse(it.renderedAt!!).epochSecond }

    if (minTimestamp == maxTimestamp) {
        return R.drawable.clock_loader_10_24dp_999999_fill0_wght400_grad0_opsz24
    }

    val timeInSeconds = Instant.parse(item.renderedAt!!).epochSecond
    val progress =
        (maxTimestamp - timeInSeconds).toFloat() / (maxTimestamp - minTimestamp)
    val percentage = (progress * 80 + 10).toInt()

    val closestPercentage =
        when {
            percentage < 15 -> 10
            percentage < 30 -> 20
            percentage < 50 -> 40
            percentage < 70 -> 60
            percentage < 85 -> 80
            else -> 90
        }

    return when (closestPercentage) {
        10 -> R.drawable.clock_loader_10_24dp_999999_fill0_wght400_grad0_opsz24
        20 -> R.drawable.clock_loader_20_24dp_999999_fill0_wght400_grad0_opsz24
        40 -> R.drawable.clock_loader_40_24dp_999999_fill0_wght400_grad0_opsz24
        60 -> R.drawable.clock_loader_60_24dp_999999_fill0_wght400_grad0_opsz24
        80 -> R.drawable.clock_loader_80_24dp_999999_fill0_wght400_grad0_opsz24
        else -> R.drawable.clock_loader_90_24dp_999999_fill0_wght400_grad0_opsz24
    }
}

/**
 * Summary card showing playlist statistics and currently playing item.
 *
 * Displays:
 * - Total number of items
 * - Number of visible (active) items
 * - Number of hidden items
 * - Currently playing item name and position
 */
@Composable
private fun PlaylistSummaryCard(
    items: List<PlaylistItemUi>,
    currentlyPlayingIndex: Int?,
    modifier: Modifier = Modifier,
) {
    val totalItems = items.size
    val activeItems = items.count { it.isVisible }
    val hiddenItems = items.count { !it.isVisible }
    val currentlyPlayingItem =
        if (currentlyPlayingIndex != null && currentlyPlayingIndex >= 0 &&
            currentlyPlayingIndex < items.size
        ) {
            items[currentlyPlayingIndex]
        } else {
            null
        }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Total items stat
                StatItem(
                    label = "Total",
                    value = totalItems.toString(),
                )
                // Active items stat
                StatItem(
                    label = "Active",
                    value = activeItems.toString(),
                )
                // Hidden items stat
                StatItem(
                    label = "Hidden",
                    value = hiddenItems.toString(),
                )
            }

            // Currently playing item
            currentlyPlayingItem?.let { item ->
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
                    thickness = 1.dp,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "Now Playing",
                            style = MaterialTheme.typography.labelMedium,
                            color =
                                MaterialTheme.colorScheme.onPrimaryContainer.copy(
                                    alpha = 0.7f,
                                ),
                        )
                        Text(
                            text = item.displayName,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    // Position indicator
                    val position = items.indexOf(item) + 1
                    Text(
                        text = "$position/$totalItems",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
    }
}

/**
 * Individual stat item in the summary card.
 */
@Composable
private fun StatItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
        )
    }
}

/**
 * List of playlist items with row numbers and status badges.
 */
@Composable
private fun PlaylistItemsList(
    items: List<PlaylistItemUi>,
    onItemClick: (PlaylistItemUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Find the most recently displayed item (currently displaying)
    val mostRecentlyDisplayedIndex =
        items
            .withIndex()
            .maxByOrNull { (_, item) ->
                item.renderedAt?.let { Instant.parse(it).epochSecond } ?: 0L
            }?.index

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Summary card at the top
        item {
            PlaylistSummaryCard(
                items = items,
                currentlyPlayingIndex = mostRecentlyDisplayedIndex,
            )
        }

        itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
            val rowNumber = index + 1
            val isCurrentlyDisplaying = index == mostRecentlyDisplayedIndex
            PlaylistItemCard(
                item = item,
                items = items,
                rowNumber = rowNumber,
                isCurrentlyDisplaying = isCurrentlyDisplaying,
                onClick = { onItemClick(item) },
            )
        }
    }
}

/**
 * Individual playlist item card with row number, status indicators, and improved spacing.
 *
 * Features:
 * - Row number display (1, 2, 3, etc.)
 * - "Currently displaying" badge for the most recently rendered item
 * - Material 3 chips for status indicators (mashup, never-rendered, etc.)
 * - Visual dividers between sections
 * - Improved typography hierarchy and spacing
 */
@Composable
private fun PlaylistItemCard(
    item: PlaylistItemUi,
    items: List<PlaylistItemUi>,
    rowNumber: Int,
    isCurrentlyDisplaying: Boolean,
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
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Header section with row number and "Currently displaying" badge or visibility status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    // Row number
                    Text(
                        text = "$rowNumber.",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    // Plugin name / display name
                    Text(
                        text = item.displayName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }

                // Status badge on the right
                if (isCurrentlyDisplaying) {
                    // Currently displaying badge - using theme secondary color
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                text = "Now Showing",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondary,
                            )
                        },
                        colors =
                            AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                            ),
                    )
                } else if (!item.isVisible) {
                    // Hidden indicator badge - only show if not visible
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                text = "Hidden",
                                style = MaterialTheme.typography.labelSmall,
                            )
                        },
                        leadingIcon = {
                            Icon(
                                painter =
                                    painterResource(R.drawable.baseline_visibility_off_24),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        },
                    )
                }
            }

            // Rendering status section
            item.renderedAt?.let { renderedAt ->
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f),
                    thickness = 1.dp,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        painter = painterResource(getClockLoaderIconForItem(item, items)),
                        contentDescription = "Rendered",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "Displayed ${formatRelativeTime(renderedAt)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Status chips section
            if (item.isMashup || item.isNeverRendered) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f),
                    thickness = 1.dp,
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Mashup chip
                    if (item.isMashup) {
                        SuggestionChip(
                            onClick = {},
                            label = {
                                Text(
                                    text = "Mashup Content",
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            },
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.widgets_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            },
                        )
                    }

                    // Never rendered chip
                    if (item.isNeverRendered) {
                        SuggestionChip(
                            onClick = {},
                            label = {
                                Text(
                                    text = "Not rendered",
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            },
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.info_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            },
                        )
                    }
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
