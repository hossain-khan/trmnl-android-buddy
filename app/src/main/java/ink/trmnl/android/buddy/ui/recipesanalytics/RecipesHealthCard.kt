package ink.trmnl.android.buddy.ui.recipesanalytics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ink.trmnl.android.buddy.R
import ink.trmnl.android.buddy.ui.theme.TrmnlBuddyAppTheme

/**
 * Data model for the RecipesHealthCard composable.
 *
 * @property isHealthy True when overall recipe health is good (>95% healthy plugins), false otherwise
 * @property unhealthyCount Number of plugins not in the "healthy" state
 * @property onCardClicked Callback invoked when the card is tapped
 */
data class RecipesHealthCardData(
    val isHealthy: Boolean,
    val unhealthyCount: Int = 0,
    val onCardClicked: () -> Unit,
)

/**
 * Converts [RecipesAnalyticsUi] into [RecipesHealthCardData] for display on the devices list.
 *
 * @param onCardClicked Callback invoked when the card is tapped
 */
fun RecipesAnalyticsUi.toHealthCardData(onCardClicked: () -> Unit): RecipesHealthCardData {
    val unhealthyCount = plugins.count { it.state != "healthy" }
    return RecipesHealthCardData(
        isHealthy = isHealthy(),
        unhealthyCount = unhealthyCount,
        onCardClicked = onCardClicked,
    )
}

/**
 * Compact card that shows an at-a-glance health summary for published recipes/plugins.
 *
 * The card displays:
 * - A chart icon on the left
 * - "All healthy" when [RecipesHealthCardData.isHealthy] is true, or "N unhealthy" otherwise
 * - A forward-arrow action icon on the right
 *
 * The card uses a subtle [fadeIn] animation when it first becomes visible so it appears
 * smoothly once analytics data finishes loading.
 *
 * @param data Card data including health status and click callback
 * @param modifier Modifier applied to the outer [AnimatedVisibility] wrapper
 */
@Composable
fun RecipesHealthCard(
    data: RecipesHealthCardData,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(),
        modifier = modifier,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
        ) {
            ListItem(
                modifier =
                    Modifier
                        .clickable(onClick = data.onCardClicked)
                        .padding(horizontal = 4.dp),
                colors =
                    ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                leadingContent = {
                    Icon(
                        painter = painterResource(R.drawable.chart_data_24dp_e3e3e3_fill0_wght400_grad0_opsz24),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
                headlineContent = {
                    Text(
                        text = "Recipes Health",
                        style = MaterialTheme.typography.titleSmall,
                    )
                },
                supportingContent = {
                    val hasUnhealthyPlugins = data.unhealthyCount > 0
                    val statusText =
                        if (hasUnhealthyPlugins) {
                            "${data.unhealthyCount} unhealthy"
                        } else {
                            "All healthy"
                        }
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyMedium,
                        color =
                            if (hasUnhealthyPlugins) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                    )
                },
                trailingContent = {
                    Icon(
                        painter = painterResource(R.drawable.arrow_forward_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                        contentDescription = "View recipes analytics",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )
        }
    }
}

// ========== Previews ==========

@PreviewLightDark
@Composable
private fun RecipesHealthCardHealthyPreview() {
    TrmnlBuddyAppTheme {
        RecipesHealthCard(
            data =
                RecipesHealthCardData(
                    isHealthy = true,
                    unhealthyCount = 0,
                    onCardClicked = {},
                ),
        )
    }
}

@PreviewLightDark
@Composable
private fun RecipesHealthCardUnhealthyPreview() {
    TrmnlBuddyAppTheme {
        RecipesHealthCard(
            data =
                RecipesHealthCardData(
                    isHealthy = false,
                    unhealthyCount = 2,
                    onCardClicked = {},
                ),
        )
    }
}
