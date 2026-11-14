package ink.trmnl.android.buddy.ui.recipesanalytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.AutoScrollCondition
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.Scroll
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.compose.cartesian.data.lineModel
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
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
    val uriHandler = LocalUriHandler.current

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
                actions = {
                    IconButton(
                        onClick = {
                            uriHandler.openUri("https://trmnl.com/analytics")
                        },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.open_in_new_24dp_e3e3e3_fill0_wght400_grad0_opsz24),
                            contentDescription = "Open Analytics Dashboard",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        val hasCreatorData =
            state.analytics.totalPlugins > 0 ||
                state.analytics.plugins.isNotEmpty() ||
                state.analytics.totalConnections > 0 ||
                state.analytics.totalPageviews > 0 ||
                state.pluginErrors.isNotEmpty() ||
                (state.uninstallFeedback?.total ?: 0) > 0

        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (!hasCreatorData &&
                !state.isLoadingCreatorInsights &&
                state.pluginErrorsError == null &&
                state.uninstallFeedbackError == null
            ) {
                item {
                    EmptyCreatorAnalyticsCard(
                        onExploreRecipesClick = { uriHandler.openUri("https://trmnl.com/recipes") },
                    )
                }
            } else {
                // Health Status Cards
                item {
                    HealthStatusSection(
                        healthyPercent = state.analytics.healthyPercent,
                        degradedPercent = state.analytics.degradedPercent,
                        erroringPercent = state.analytics.erroringPercent,
                    )
                }

                // Key Metrics Card
                item {
                    MetricsCard(
                        totalPlugins = state.analytics.totalPlugins,
                        totalConnections = state.analytics.totalConnections,
                        totalPageviews = state.analytics.totalPageviews,
                    )
                }

                // Growth Trend Chart
                if (state.analytics.growthData.isNotEmpty()) {
                    item {
                        GrowthChartCard(growthData = state.analytics.growthData)
                    }
                }

                // Plugins List
                if (state.analytics.plugins.isNotEmpty()) {
                    item {
                        PluginsListSection(plugins = state.analytics.plugins)
                    }
                }

                // Failing Installs (Author Plugin Errors)
                item {
                    PluginErrorsSection(
                        isLoading = state.isLoadingCreatorInsights,
                        errors = state.pluginErrors,
                        errorMessage = state.pluginErrorsError,
                        onRetry = {
                            state.eventSink(RecipesAnalyticsScreen.Event.RefreshCreatorInsights)
                        },
                    )
                }

                // Uninstall Feedback (Last 30 Days)
                item {
                    UninstallFeedbackSection(
                        isLoading = state.isLoadingCreatorInsights,
                        feedback = state.uninstallFeedback,
                        errorMessage = state.uninstallFeedbackError,
                        onRetry = {
                            state.eventSink(RecipesAnalyticsScreen.Event.RefreshCreatorInsights)
                        },
                    )
                }
            }

            // Bottom spacer
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
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
                text = formatAsPercentage(percentage),
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
 * Card displaying install growth trend using a Vico line chart.
 */
@Composable
private fun GrowthChartCard(
    growthData: List<GrowthDataPointUi>,
    modifier: Modifier = Modifier,
) {
    val firstValue = growthData.firstOrNull()?.value ?: 0
    val latestValue = growthData.lastOrNull()?.value ?: 0
    val delta = latestValue - firstValue

    Card(
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Growth Trend (Last ${growthData.size} Days)",
                    style = MaterialTheme.typography.titleMedium,
                )
                if (growthData.size > 1 && delta > 0) {
                    Text(
                        text = "+$delta",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

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
 * Vico line chart showing growth data points with auto-fitted Y-axis range and date labels.
 */
@Composable
private fun SimpleGrowthChart(
    growthData: List<GrowthDataPointUi>,
    modifier: Modifier = Modifier,
) {
    if (growthData.isEmpty()) return

    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(growthData) {
        modelProducer.runTransaction {
            lineModel {
                series(growthData.map { it.value.toFloat() })
            }
        }
    }

    val dataMin = remember(growthData) { growthData.minOf { it.value } }
    val dataMax = remember(growthData) { growthData.maxOf { it.value } }
    val rangeProvider =
        remember(dataMin, dataMax) {
            if (dataMin == dataMax) {
                CartesianLayerRangeProvider.fixed(
                    minY = (dataMin - 1).coerceAtLeast(0).toDouble(),
                    maxY = (dataMax + 1).toDouble(),
                )
            } else {
                val padding = ((dataMax - dataMin) * 0.15).coerceAtLeast(1.0)
                CartesianLayerRangeProvider.fixed(
                    minY = (dataMin - padding).coerceAtLeast(0.0),
                    maxY = dataMax + padding,
                )
            }
        }

    val primaryColor = MaterialTheme.colorScheme.primary
    val showPoints = growthData.size <= 31
    val lineLayer =
        rememberLineCartesianLayer(
            lineProvider =
                LineCartesianLayer.LineProvider.series(
                    LineCartesianLayer.Line(
                        fill = LineCartesianLayer.LineFill.single(Fill(primaryColor)),
                        areaFill =
                            LineCartesianLayer.AreaFill.single(
                                Fill(primaryColor.copy(alpha = 0.15f)),
                            ),
                        pointProvider =
                            if (showPoints) {
                                LineCartesianLayer.PointProvider.single(
                                    LineCartesianLayer.Point(
                                        component =
                                            rememberShapeComponent(
                                                fill = Fill(primaryColor),
                                                shape = CircleShape,
                                            ),
                                        size = 6.dp,
                                    ),
                                )
                            } else {
                                null
                            },
                    ),
                ),
            rangeProvider = rangeProvider,
        )

    CartesianChartHost(
        chart =
            rememberCartesianChart(
                lineLayer,
                startAxis =
                    VerticalAxis.rememberStart(
                        valueFormatter = { _, value, _ ->
                            value.toInt().toString()
                        },
                    ),
                bottomAxis =
                    HorizontalAxis.rememberBottom(
                        valueFormatter = { _, value, _ ->
                            val index = value.toInt()
                            if (index in growthData.indices) {
                                // Format "YYYY-MM-DD" as "MM/DD"
                                growthData[index].date.takeLast(5).replace('-', '/')
                            } else {
                                ""
                            }
                        },
                    ),
            ),
        modelProducer = modelProducer,
        modifier = modifier,
        scrollState =
            rememberVicoScrollState(
                scrollEnabled = growthData.size > 14,
                initialScroll = Scroll.Absolute.End,
                autoScrollCondition = AutoScrollCondition.OnModelGrowth,
            ),
        zoomState = rememberVicoZoomState(zoomEnabled = growthData.size > 14),
    )
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
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Your Published Recipes",
                style = MaterialTheme.typography.titleMedium,
            )
            OverallHealthStatus(plugins = plugins)
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            shape = RoundedCornerShape(12.dp),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                plugins.forEach { plugin ->
                    PluginListItem(plugin)
                }
            }
        }
    }
}

/**
 * Composable showing the overall health status of all published recipes.
 * Determines the worst-case health status among all plugins.
 */
@Composable
private fun OverallHealthStatus(
    plugins: List<PluginAnalyticsUi>,
    modifier: Modifier = Modifier,
) {
    // Determine overall status: error > degraded > healthy
    val overallState =
        when {
            plugins.any { it.state == "erroring" } -> "erroring"
            plugins.any { it.state == "degraded" } -> "degraded"
            else -> "healthy"
        }

    val (bgColor, fgColor, label) =
        when (overallState) {
            "healthy" -> {
                Triple(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    MaterialTheme.colorScheme.primary,
                    "Healthy",
                )
            }

            "degraded" -> {
                Triple(
                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                    MaterialTheme.colorScheme.tertiary,
                    "Degraded",
                )
            }

            else -> {
                Triple(
                    MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                    MaterialTheme.colorScheme.error,
                    "Erroring",
                )
            }
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
                    text = "Installs: ${plugin.installs}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Forks: ${plugin.forks}",
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
            "healthy" -> {
                Triple(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    MaterialTheme.colorScheme.primary,
                    "Healthy",
                )
            }

            "degraded" -> {
                Triple(
                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                    MaterialTheme.colorScheme.tertiary,
                    "Degraded",
                )
            }

            else -> {
                Triple(
                    MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                    MaterialTheme.colorScheme.error,
                    "Erroring",
                )
            }
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

/**
 * Empty state card shown when the user has not published any recipes or plugins yet.
 */
@Composable
private fun EmptyCreatorAnalyticsCard(
    onExploreRecipesClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(64.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(16.dp),
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.chef_hat_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(32.dp),
                )
            }

            Text(
                text = "No Creator Analytics Yet",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Text(
                text =
                    "Publish a recipe or plugin on TRMNL to track install health, " +
                        "8-day growth trends, failing installs, and user uninstall feedback.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )

            Button(
                onClick = onExploreRecipesClick,
                modifier = Modifier.padding(top = 4.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.open_in_new_24dp_e3e3e3_fill0_wght400_grad0_opsz24),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Explore TRMNL Recipes")
            }
        }
    }
}

/**
 * Section displaying failing plugin installs from `GET /api/analytics/errors`.
 */
@Composable
private fun PluginErrorsSection(
    isLoading: Boolean,
    errors: List<AuthorPluginErrorUi>,
    errorMessage: String?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Failing Installs",
                style = MaterialTheme.typography.titleMedium,
            )
            if (!isLoading && errorMessage == null) {
                val badgeText = if (errors.isEmpty()) "All Clear" else "${errors.size} Active"
                val badgeColor =
                    if (errors.isEmpty()) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = badgeColor,
                )
            }
        }

        when {
            isLoading && errors.isEmpty() && errorMessage == null -> {
                InsightLoadingCard(label = "Loading failing installs…")
            }

            errorMessage != null -> {
                InsightErrorCard(
                    message = errorMessage,
                    onRetry = onRetry,
                )
            }

            errors.isEmpty() -> {
                InsightEmptyCard(
                    iconRes = R.drawable.check_circle_24dp_e8eaed_fill0_wght400_grad0_opsz24,
                    iconTint = MaterialTheme.colorScheme.primary,
                    title = "No Failing Installs",
                    subtitle = "Your published plugins have no reported install failures.",
                )
            }

            else -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    errors.forEach { error ->
                        PluginErrorCard(error = error)
                    }
                }
            }
        }
    }
}

@Composable
private fun PluginErrorCard(
    error: AuthorPluginErrorUi,
    modifier: Modifier = Modifier,
) {
    val ownerLabel =
        when (error.owner) {
            "author" -> "Author"
            "upstream" -> "Upstream"
            "installer_connection" -> "Connection"
            "installer_setup" -> "Setup"
            else -> error.owner
        }
    val ownerColor =
        when (error.owner) {
            "author" -> MaterialTheme.colorScheme.error
            "upstream" -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.secondary
        }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = error.pluginName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = ownerLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = ownerColor,
                        modifier =
                            Modifier
                                .background(
                                    color = ownerColor.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(6.dp),
                                ).padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                    Text(
                        text = "${error.count}×",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            Text(
                text = error.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                text = "Last seen: ${error.lastSeen.take(10)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Section displaying 30-day uninstall feedback from `GET /api/analytics/uninstall_feedback`.
 */
@Composable
private fun UninstallFeedbackSection(
    isLoading: Boolean,
    feedback: UninstallFeedbackUi?,
    errorMessage: String?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Uninstall Feedback (Last ${feedback?.windowDays ?: 30} Days)",
            style = MaterialTheme.typography.titleMedium,
        )

        when {
            isLoading && feedback == null && errorMessage == null -> {
                InsightLoadingCard(label = "Loading uninstall feedback…")
            }

            errorMessage != null -> {
                InsightErrorCard(
                    message = errorMessage,
                    onRetry = onRetry,
                )
            }

            feedback == null || feedback.total == 0 -> {
                InsightEmptyCard(
                    iconRes = R.drawable.heart_minus_24dp_e8eaed_fill0_wght400_grad0_opsz24,
                    iconTint = MaterialTheme.colorScheme.primary,
                    title = "No Uninstall Feedback",
                    subtitle =
                        "No uninstall feedback was recorded in the last " +
                            "${feedback?.windowDays ?: 30} days.",
                )
            }

            else -> {
                UninstallFeedbackCard(feedback = feedback)
            }
        }
    }
}

@Composable
private fun UninstallFeedbackCard(
    feedback: UninstallFeedbackUi,
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
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MetricRow("Total Uninstalls", feedback.total.toString())
            feedback.typicalDaysInstalled?.let { days ->
                MetricRow("Typical Days Installed", "$days days")
            }

            if (feedback.reasonBreakdown.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text(
                    text = "Reasons",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                val maxReasonCount = feedback.reasonBreakdown.maxOf { it.count }.coerceAtLeast(1)
                feedback.reasonBreakdown.forEach { item ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = item.reason,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = item.count.toString(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        LinearProgressIndicator(
                            progress = { item.count.toFloat() / maxReasonCount.toFloat() },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            if (feedback.recentComments.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text(
                    text = "Recent Comments",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                feedback.recentComments.forEach { comment ->
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(8.dp),
                                ).padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = comment.pluginName,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = comment.createdAt.take(10),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        comment.reasonLabel?.let { reason ->
                            Text(
                                text = reason,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Text(
                            text = comment.detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InsightLoadingCard(
    label: String,
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
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun InsightEmptyCard(
    iconRes: Int,
    iconTint: androidx.compose.ui.graphics.Color,
    title: String,
    subtitle: String,
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
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun InsightErrorCard(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
            ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.warning_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            TextButton(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

// ============================================
// Preview Composables
// ============================================

@PreviewLightDark
@Composable
private fun SimpleGrowthChartPreview() {
    TrmnlBuddyAppTheme {
        GrowthChartCard(
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
            modifier = Modifier.padding(16.dp),
        )
    }
}

@PreviewLightDark
@Composable
private fun SimpleGrowthChartFlatPreview() {
    TrmnlBuddyAppTheme {
        GrowthChartCard(
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
            modifier = Modifier.padding(16.dp),
        )
    }
}

@PreviewLightDark
@Composable
private fun SimpleGrowthChartSpikePreview() {
    TrmnlBuddyAppTheme {
        GrowthChartCard(
            growthData =
                listOf(
                    GrowthDataPointUi("2026-04-09", 1),
                    GrowthDataPointUi("2026-04-10", 2),
                    GrowthDataPointUi("2026-04-11", 3),
                    GrowthDataPointUi("2026-04-12", 5),
                    GrowthDataPointUi("2026-04-13", 12),
                    GrowthDataPointUi("2026-04-14", 8),
                    GrowthDataPointUi("2026-04-15", 15),
                    GrowthDataPointUi("2026-04-16", 4),
                ),
            modifier = Modifier.padding(16.dp),
        )
    }
}

@PreviewLightDark
@Composable
private fun SimpleGrowthChartMixedPreview() {
    TrmnlBuddyAppTheme {
        GrowthChartCard(
            growthData =
                listOf(
                    GrowthDataPointUi("2026-04-09", 5),
                    GrowthDataPointUi("2026-04-10", 0),
                    GrowthDataPointUi("2026-04-11", 3),
                    GrowthDataPointUi("2026-04-12", 10),
                    GrowthDataPointUi("2026-04-13", 2),
                    GrowthDataPointUi("2026-04-14", 7),
                    GrowthDataPointUi("2026-04-15", 1),
                    GrowthDataPointUi("2026-04-16", 6),
                ),
            modifier = Modifier.padding(16.dp),
        )
    }
}

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
                            healthyPercent = 98.5,
                            degradedPercent = 1.2,
                            erroringPercent = 0.3,
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
