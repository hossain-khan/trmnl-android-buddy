package ink.trmnl.android.buddy.ui.recipescatalog

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import ink.trmnl.android.buddy.R
import ink.trmnl.android.buddy.api.models.Recipe
import ink.trmnl.android.buddy.api.models.RecipeStats
import ink.trmnl.android.buddy.ui.theme.TrmnlBuddyAppTheme

/**
 * List item component for displaying a single recipe.
 *
 * Shows recipe icon, name, statistics (installs and forks), and bookmark button.
 * Uses Material 3 Card and ListItem for consistent styling.
 *
 * @param recipe The recipe to display
 * @param isBookmarked Whether this recipe is currently bookmarked
 * @param onClick Callback when the item is clicked
 * @param onBookmarkClick Callback when the bookmark button is clicked
 * @param modifier Optional modifier for the component
 */
@Composable
fun RecipeListItem(
    recipe: Recipe,
    isBookmarked: Boolean,
    onClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        ListItem(
            leadingContent = {
                // Recipe icon using Coil for async image loading
                if (recipe.iconUrl != null) {
                    SubcomposeAsyncImage(
                        model = recipe.iconUrl,
                        contentDescription = null,
                        modifier =
                            Modifier
                                .size(40.dp)
                                .clip(MaterialTheme.shapes.small),
                        contentScale = ContentScale.Crop,
                        loading = {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        },
                        error = {
                            Icon(
                                painter = painterResource(R.drawable.widgets_24dp_e8eaed_fill0_wght200_grad0_opsz24),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                    )
                } else {
                    // Placeholder icon when iconUrl is null
                    Icon(
                        painter = painterResource(R.drawable.widgets_24dp_e8eaed_fill0_wght200_grad0_opsz24),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(40.dp),
                    )
                }
            },
            headlineContent = {
                Text(
                    text = recipe.name,
                    style = MaterialTheme.typography.titleMedium,
                )
            },
            supportingContent = {
                Text(
                    text = "${recipe.stats.installs} installs • ${recipe.stats.forks} forks",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            trailingContent = {
                // Animated bookmark button
                IconButton(onClick = onBookmarkClick) {
                    AnimatedContent(
                        targetState = isBookmarked,
                        transitionSpec = {
                            (
                                fadeIn(animationSpec = tween(220, delayMillis = 90)) +
                                    scaleIn(
                                        initialScale = 0.8f,
                                        animationSpec = tween(220, delayMillis = 90),
                                    )
                            ).togetherWith(
                                fadeOut(animationSpec = tween(90)) +
                                    scaleOut(
                                        targetScale = 0.8f,
                                        animationSpec = tween(90),
                                    ),
                            )
                        },
                        label = "bookmark_animation",
                    ) { bookmarked ->
                        Icon(
                            painter =
                                painterResource(
                                    if (bookmarked) {
                                        R.drawable.bookmark_added_24dp_e8eaed_fill0_wght400_grad0_opsz24
                                    } else {
                                        R.drawable.bookmark_add_24dp_e8eaed_fill0_wght400_grad0_opsz24
                                    },
                                ),
                            contentDescription = if (bookmarked) "Remove bookmark" else "Add bookmark",
                            tint =
                                if (bookmarked) {
                                    colorResource(R.color.trmnl_orange)
                                } else {
                                    MaterialTheme.colorScheme.secondary
                                },
                        )
                    }
                }
            },
            colors =
                ListItemDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            modifier = Modifier.clickable(onClick = onClick),
        )
    }
}

// ============================================
// Composable Previews
// ============================================

@PreviewLightDark
@Composable
private fun RecipeListItemPreview() {
    TrmnlBuddyAppTheme {
        RecipeListItem(
            recipe =
                Recipe(
                    id = 1,
                    name = "Weather Chum",
                    iconUrl = null,
                    screenshotUrl = null,
                    stats = RecipeStats(installs = 1230, forks = 1),
                ),
            isBookmarked = false,
            onClick = {},
            onBookmarkClick = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun RecipeListItemWithHighStatsPreview() {
    TrmnlBuddyAppTheme {
        RecipeListItem(
            recipe =
                Recipe(
                    id = 2,
                    name = "Matrix",
                    iconUrl = null,
                    screenshotUrl = null,
                    stats = RecipeStats(installs = 25, forks = 176),
                ),
            isBookmarked = false,
            onClick = {},
            onBookmarkClick = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun RecipeListItemBookmarkedPreview() {
    TrmnlBuddyAppTheme {
        RecipeListItem(
            recipe =
                Recipe(
                    id = 3,
                    name = "Bookmarked Recipe",
                    iconUrl = null,
                    screenshotUrl = null,
                    stats = RecipeStats(installs = 500, forks = 50),
                ),
            isBookmarked = true,
            onClick = {},
            onBookmarkClick = {},
        )
    }
}
