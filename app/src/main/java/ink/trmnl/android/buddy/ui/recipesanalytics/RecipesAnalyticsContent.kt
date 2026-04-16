package ink.trmnl.android.buddy.ui.recipesanalytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.slack.circuit.codegen.annotations.CircuitInject
import dev.zacsweers.metro.AppScope
import ink.trmnl.android.buddy.R
import ink.trmnl.android.buddy.ui.theme.TrmnlBuddyAppTheme

/**
 * UI content for [RecipesAnalyticsScreen].
 *
 * Displays a comprehensive Material 3 dashboard with:
 * - Health status cards (healthy, degraded, erroring percentages)
 * - Key metrics (plugins, connections, pageviews)
 * - Growth trend chart (last 8 days)
 * - List of published plugins with stats
 */
@OptIn(ExperimentalMaterial3Api::class)
@CircuitInject(RecipesAnalyticsScreen::class, AppScope::class)
@Composable
fun RecipesAnalyticsContent(
    state: RecipesAnalyticsScreen.State,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Recipes Analytics") },
                navigationIcon = {
                    IconButton(onClick = { state.eventSink(RecipesAnalyticsScreen.Event.BackClicked) }) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back_24dp_e3e3e3_fill0_wght400_grad0_opsz24),
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Health Status Cards
            HealthStatusSection(
                healthyPercent = state.analytics.healthyPercent,
                degradedPercent = state.analytics.degradedPercent,
                erroringPercent = state.analytics.erroringPercent,
            )

            // Key Metrics Card
            MetricsCard(
                totalPlugins = state.analytics.totalPlugins,
                totalConnections = state.analytics.totalConnections,
                totalPageviews = state.analytics.totalPageviews,
            )

            // Growth Trend Chart
            GrowthChartCard(growthData = state.analytics.growthData)

            // Plugins List
            if (state.analytics.plugins.isNotEmpty()) {
                PluginsListSection(plugins = state.analytics.plugins)
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/**
 * Section showing health status breakdown with three cards.
 */
@Composable
private fun HealthStatusSection(
    healthyPercent: Double,
    degradedPercent: Double,
    erroringPercent: Double,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Health Status",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 12.dp),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HealthStatusCard(
                label = "Healthy",
                percentage = healthyPercent,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            HealthStatusCard(
                label = "Degraded",
                percentage = degradedPercent,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.weight(1f),
            )
            HealthStatusCard(
                label = "Erroring",
                percentage = erroringPercent,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * Individual health status card showing percentage with color-coded styling.
 */
@Composable
private fun HealthStatusCard(
    label: String,
    percentage: Double,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.height(100.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = color.copy(alpha = 0.1f),
            ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = String.format("%.1f%%", percentage),
                style = MaterialTheme.typography.headlineSmall,
                color = color,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/**
 * Card showing key metrics: total plugins, connections, and pageviews.
 */
@Composable
private fun MetricsCard(
    totalPlugins: Int,
    totalConnections: Int,
    totalPageviews: Int,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Overview",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            MetricRow("Total Plugins", totalPlugins.toString())
            Spacer(modifier = Modifier.height(8.dp))
            MetricRow("Connections", totalConnections.toString())
            Spacer(modifier = Modifier.height(8.dp))
            MetricRow("Page Views", totalPageviews.toString())
        }
    }
}

/**
 * Row displaying a single metric with label and value.
 */
@Composable
private fun MetricRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )
    }
}

/**
 * Card displaying growth trend as a bar chart for last 8 days.
 */
@Composable
private fun GrowthChartCard(
    growthData: List<GrowthDataPointUi>,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Growth Trend (Last 8 Days)",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            SimpleGrowthChart(
                growthData = growthData,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(200.dp),
            )
        }
    }
}

/**
 * Simple bar chart showing growth data points with date labels.
 */
@Composable
private fun SimpleGrowthChart(
    growthData: List<GrowthDataPointUi>,
    modifier: Modifier = Modifier,
) {
    val maxValue = growthData.maxOfOrNull { it.value }?.toFloat() ?: 1f

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        growthData.forEach { point ->
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Bar
                Box(
                    modifier =
                        Modifier
                            .width(24.dp)
                            .fillMaxHeight(
                                fraction =
                                    if (maxValue > 0) point.value / maxValue else 0f,
                            ).background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp),
                            ),
                )

                // Date label
                Text(
                    text = point.date.takeLast(2), // Show day only
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 4.dp),
                    maxLines = 1,
                )
            }
        }
    }
}

/**
 * Section displaying list of published plugins with their stats.
 */
@Composable
private fun PluginsListSection(
    plugins: List<PluginAnalyticsUi>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Your Plugins",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 12.dp),
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            shape = RoundedCornerShape(12.dp),
        ) {
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                items(plugins) { plugin ->
                    PluginListItem(plugin)
                }
            }
        }
    }
}

/**
 * List item showing a single plugin with its stats and health status.
 */
@Composable
private fun PluginListItem(
    plugin: PluginAnalyticsUi,
    modifier: Modifier = Modifier,
) {
    ListItem(
        modifier = modifier,
        headlineContent = {
            Text(
                text = plugin.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
        },
        supportingContent = {
            Row(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "📥 ${plugin.installs}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "🔀 ${plugin.forks}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        trailingContent = {
            HealthBadge(plugin.state)
        },
        colors =
            ListItemDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
    )
}

/**
 * Badge showing plugin health status with color-coded styling.
 */
@Composable
private fun HealthBadge(
    state: String,
    modifier: Modifier = Modifier,
) {
    val (bgColor, fgColor, label) =
        when (state) {
            "healthy" ->
                Triple(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    MaterialTheme.colorScheme.primary,
                    "Healthy",
                )
            "degraded" ->
                Triple(
                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                    MaterialTheme.colorScheme.tertiary,
                    "Degraded",
                )
            else ->
                Triple(
                    MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                    MaterialTheme.colorScheme.error,
                    "Erroring",
                )
        }

    AssistChip(
        onClick = {},
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        modifier = modifier,
        colors =
            AssistChipDefaults.assistChipColors(
                containerColor = bgColor,
                labelColor = fgColor,
            ),
        enabled = false,
    )
}

// ============================================
// Preview Composables
// ============================================

@PreviewLightDark
@Composable
private fun RecipesAnalyticsContentPreview() {
    TrmnlBuddyAppTheme {
        RecipesAnalyticsContent(
            state =
                RecipesAnalyticsScreen.State(
                    analytics =
                        RecipesAnalyticsUi(
                            totalPlugins = 9,
                            totalConnections = 423,
                            totalPageviews = 28,
                            healthyPercent = 121.33,
                            degradedPercent = 0.67,
                            erroringPercent = 0.33,
                            growthData =
                                listOf(
                                    GrowthDataPointUi("2026-04-09", 0),
                                    GrowthDataPointUi("2026-04-10", 0),
                                    GrowthDataPointUi("2026-04-11", 0),
                                    GrowthDataPointUi("2026-04-12", 0),
                                    GrowthDataPointUi("2026-04-13", 2),
                                    GrowthDataPointUi("2026-04-14", 1),
                                    GrowthDataPointUi("2026-04-15", 3),
                                    GrowthDataPointUi("2026-04-16", 0),
                                ),
                            plugins =
                                listOf(
                                    PluginAnalyticsUi(
                                        "Calendar XL",
                                        "healthy",
                                        0,
                                        43,
                                    ),
                                    PluginAnalyticsUi(
                                        "Google Photos",
                                        "healthy",
                                        1,
                                        124,
                                    ),
                                    PluginAnalyticsUi(
                                        "Max Payne Quotes",
                                        "degraded",
                                        27,
                                        1,
                                    ),
                                ),
                        ),
                ),
        )
    }
}

@PreviewLightDark
@Composable
private fun RecipesAnalyticsContentNoPluginsPreview() {
    TrmnlBuddyAppTheme {
        RecipesAnalyticsContent(
            state =
                RecipesAnalyticsScreen.State(
                    analytics =
                        RecipesAnalyticsUi(
                            totalPlugins = 0,
                            totalConnections = 0,
                            totalPageviews = 0,
                            healthyPercent = 0.0,
                            degradedPercent = 0.0,
                            erroringPercent = 0.0,
                            growthData =
                                listOf(
                                    GrowthDataPointUi("2026-04-09", 0),
                                    GrowthDataPointUi("2026-04-10", 0),
                                    GrowthDataPointUi("2026-04-11", 0),
                                    GrowthDataPointUi("2026-04-12", 0),
                                    GrowthDataPointUi("2026-04-13", 0),
                                    GrowthDataPointUi("2026-04-14", 0),
                                    GrowthDataPointUi("2026-04-15", 0),
                                    GrowthDataPointUi("2026-04-16", 0),
                                ),
                            plugins = emptyList(),
                        ),
                ),
        )
    }
}
