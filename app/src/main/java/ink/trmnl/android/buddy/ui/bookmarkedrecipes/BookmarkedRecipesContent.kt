package ink.trmnl.android.buddy.ui.bookmarkedrecipes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import ink.trmnl.android.buddy.api.models.Recipe
import ink.trmnl.android.buddy.api.models.RecipeStats
import ink.trmnl.android.buddy.ui.components.TrmnlTitle
import ink.trmnl.android.buddy.ui.recipescatalog.RecipeListItem
import ink.trmnl.android.buddy.ui.theme.TrmnlBuddyAppTheme

/**
 * UI content for BookmarkedRecipesScreen.
 *
 * Displays a list of bookmarked recipes with:
 * - TopAppBar with back button
 * - LazyColumn of bookmarked recipe cards
 * - Loading state
 * - Empty state when no bookmarks
 */
@OptIn(ExperimentalMaterial3Api::class)
@CircuitInject(BookmarkedRecipesScreen::class, AppScope::class)
@Composable
fun BookmarkedRecipesContent(
    state: BookmarkedRecipesScreen.State,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { TrmnlTitle("Bookmarked Recipes") },
                navigationIcon = {
                    IconButton(onClick = { state.eventSink(BookmarkedRecipesScreen.Event.BackClicked) }) {
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
            state.bookmarkedRecipes.isEmpty() -> {
                EmptyState(modifier = Modifier.padding(innerPadding))
            }
            else -> {
                BookmarkedRecipesList(
                    recipes = state.bookmarkedRecipes,
                    onRecipeClick = { recipe ->
                        state.eventSink(BookmarkedRecipesScreen.Event.RecipeClicked(recipe))
                    },
                    onBookmarkClick = { recipe ->
                        state.eventSink(BookmarkedRecipesScreen.Event.BookmarkClicked(recipe))
                    },
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }
    }
}

/**
 * List of bookmarked recipes.
 */
@Composable
private fun BookmarkedRecipesList(
    recipes: List<Recipe>,
    onRecipeClick: (Recipe) -> Unit,
    onBookmarkClick: (Recipe) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(recipes, key = { it.id }) { recipe ->
            RecipeListItem(
                recipe = recipe,
                isBookmarked = true, // All items in this list are bookmarked
                onClick = { onRecipeClick(recipe) },
                onBookmarkClick = { onBookmarkClick(recipe) },
            )
        }
    }
}

/**
 * Loading state indicator.
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
 * Empty state when no bookmarks exist.
 */
@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(R.drawable.bookmark_add_24dp_e8eaed_fill0_wght400_grad0_opsz24),
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No bookmarked recipes",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "Bookmark recipes to see them here",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

// ============================================
// Composable Previews
// ============================================

private val SAMPLE_BOOKMARKED_RECIPES =
    listOf(
        Recipe(
            id = 1,
            name = "Weather Chum",
            iconUrl = null,
            screenshotUrl = null,
            stats = RecipeStats(installs = 1230, forks = 1),
        ),
        Recipe(
            id = 2,
            name = "Matrix",
            iconUrl = null,
            screenshotUrl = null,
            stats = RecipeStats(installs = 25, forks = 176),
        ),
    )

@PreviewLightDark
@Composable
private fun BookmarkedRecipesContentPreview() {
    TrmnlBuddyAppTheme {
        BookmarkedRecipesContent(
            state =
                BookmarkedRecipesScreen.State(
                    bookmarkedRecipes = SAMPLE_BOOKMARKED_RECIPES,
                ),
        )
    }
}

@PreviewLightDark
@Composable
private fun BookmarkedRecipesEmptyPreview() {
    TrmnlBuddyAppTheme {
        BookmarkedRecipesContent(
            state =
                BookmarkedRecipesScreen.State(
                    bookmarkedRecipes = emptyList(),
                ),
        )
    }
}

@PreviewLightDark
@Composable
private fun BookmarkedRecipesLoadingPreview() {
    TrmnlBuddyAppTheme {
        BookmarkedRecipesContent(
            state =
                BookmarkedRecipesScreen.State(
                    isLoading = true,
                ),
        )
    }
}
