package ink.trmnl.android.buddy.ui.recipescatalog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.slack.circuit.codegen.annotations.CircuitInject
import dev.zacsweers.metro.AppScope
import ink.trmnl.android.buddy.R
import ink.trmnl.android.buddy.api.models.Recipe
import ink.trmnl.android.buddy.api.models.RecipeStats
import ink.trmnl.android.buddy.ui.components.TrmnlTitle
import ink.trmnl.android.buddy.ui.theme.TrmnlBuddyAppTheme

/**
 * UI content for RecipesCatalogScreen.
 *
 * Displays a searchable, sortable list of TRMNL community recipes with:
 * - Material 3 SearchBar for filtering recipes
 * - FilterChips for sort options
 * - LazyColumn of recipe cards
 * - Pagination with "Load More" button
 * - Loading, error, and empty states
 */
@OptIn(ExperimentalMaterial3Api::class)
@CircuitInject(RecipesCatalogScreen::class, AppScope::class)
@Composable
fun RecipesCatalogContent(
    state: RecipesCatalogScreen.State,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { TrmnlTitle("Recipes Catalog") },
                navigationIcon = {
                    IconButton(onClick = { state.eventSink(RecipesCatalogScreen.Event.BackClicked) }) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back_24dp_e3e3e3_fill0_wght400_grad0_opsz24),
                            contentDescription = "Back",
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { state.eventSink(RecipesCatalogScreen.Event.BookmarksClicked) }) {
                        Icon(
                            painter = painterResource(R.drawable.bookmark_star_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                            contentDescription = "View bookmarked recipes",
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
                    .padding(innerPadding),
        ) {
            // Search bar
            RecipesSearchBar(
                searchQuery = state.searchQuery,
                onQueryChange = { query ->
                    state.eventSink(RecipesCatalogScreen.Event.SearchQueryChanged(query))
                },
                onSearchClicked = {
                    state.eventSink(RecipesCatalogScreen.Event.SearchClicked)
                },
                onClearClicked = {
                    state.eventSink(RecipesCatalogScreen.Event.ClearSearchClicked)
                },
            )

            // Sort chips
            SortFilterRow(
                selectedSort = state.selectedSort,
                onSortSelected = { sort ->
                    state.eventSink(RecipesCatalogScreen.Event.SortSelected(sort))
                },
            )

            // Category filter chips
            if (state.availableCategories.isNotEmpty()) {
                CategoryFilterRow(
                    availableCategories = state.availableCategories,
                    selectedCategories = state.selectedCategories,
                    onCategorySelected = { category ->
                        state.eventSink(RecipesCatalogScreen.Event.CategorySelected(category))
                    },
                    onCategoryDeselected = { category ->
                        state.eventSink(RecipesCatalogScreen.Event.CategoryDeselected(category))
                    },
                    onClearAll = {
                        state.eventSink(RecipesCatalogScreen.Event.ClearCategoryFilters)
                    },
                )
            }

            // Content area
            when {
                state.isLoading -> {
                    LoadingState()
                }
                state.error != null && state.recipes.isEmpty() -> {
                    ErrorState(
                        errorMessage = state.error,
                        onRetryClick = {
                            state.eventSink(RecipesCatalogScreen.Event.RetryClicked)
                        },
                    )
                }
                state.recipes.isEmpty() && state.selectedCategories.isNotEmpty() -> {
                    // Empty filtered results - show message with selected categories
                    FilteredEmptyState(
                        selectedCategories = state.selectedCategories,
                        onClearFilters = {
                            state.eventSink(RecipesCatalogScreen.Event.ClearCategoryFilters)
                        },
                    )
                }
                state.recipes.isEmpty() -> {
                    EmptyState()
                }
                else -> {
                    RecipesList(
                        recipes = state.recipes,
                        bookmarkedRecipeIds = state.bookmarkedRecipeIds,
                        hasMorePages = state.hasMorePages,
                        isLoadingMore = state.isLoadingMore,
                        onRecipeClick = { recipe ->
                            state.eventSink(RecipesCatalogScreen.Event.RecipeClicked(recipe))
                        },
                        onBookmarkClick = { recipe ->
                            state.eventSink(RecipesCatalogScreen.Event.BookmarkClicked(recipe))
                        },
                        onLoadMoreClick = {
                            state.eventSink(RecipesCatalogScreen.Event.LoadMoreClicked)
                        },
                    )
                }
            }
        }
    }
}

/**
 * Material 3 SearchBar for recipe filtering.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecipesSearchBar(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    onSearchClicked: () -> Unit,
    onClearClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isActive by remember { mutableStateOf(false) }

    SearchBar(
        inputField = {
            SearchBarDefaults.InputField(
                query = searchQuery,
                onQueryChange = onQueryChange,
                onSearch = {
                    isActive = false
                    onSearchClicked()
                },
                expanded = isActive,
                onExpandedChange = { isActive = it },
                placeholder = { Text("Search recipes...") },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.search_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = onClearClicked) {
                            Icon(
                                painter = painterResource(R.drawable.close_24dp_e3e3e3_fill0_wght400_grad0_opsz24),
                                contentDescription = "Clear search",
                            )
                        }
                    }
                },
            )
        },
        windowInsets = WindowInsets(0, 0, 0, 0),
        expanded = isActive,
        onExpandedChange = { isActive = it },
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
    ) {
        // Search suggestions can be added here in future
    }
}

/**
 * Horizontal scrollable row of sort filter chips.
 */
@Composable
private fun SortFilterRow(
    selectedSort: SortOption,
    onSortSelected: (SortOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SortOption.entries.forEach { sortOption ->
            FilterChip(
                selected = selectedSort == sortOption,
                onClick = { onSortSelected(sortOption) },
                label = { Text(sortOption.displayName) },
            )
        }
    }
}

/**
 * Horizontal scrollable row of category filter chips.
 *
 * Shows all available categories with selection state. Adds a "Clear All" chip
 * when categories are selected for quick deselection.
 */
@Composable
private fun CategoryFilterRow(
    availableCategories: List<String>,
    selectedCategories: Set<String>,
    onCategorySelected: (String) -> Unit,
    onCategoryDeselected: (String) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Clear All chip (only shown when categories are selected)
        if (selectedCategories.isNotEmpty()) {
            FilterChip(
                selected = false,
                onClick = onClearAll,
                label = { Text("Clear All") },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.close_24dp_e3e3e3_fill0_wght400_grad0_opsz24),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
            )
        }

        // Category filter chips
        availableCategories.forEach { category ->
            val isSelected = category in selectedCategories
            FilterChip(
                selected = isSelected,
                onClick = {
                    if (isSelected) {
                        onCategoryDeselected(category)
                    } else {
                        onCategorySelected(category)
                    }
                },
                label = { Text(category.replaceFirstChar { it.uppercase() }) },
            )
        }
    }
}

/**
 * Lazy scrollable list of recipes with pagination.
 */
@Composable
private fun RecipesList(
    recipes: List<Recipe>,
    bookmarkedRecipeIds: Set<Int>,
    hasMorePages: Boolean,
    isLoadingMore: Boolean,
    onRecipeClick: (Recipe) -> Unit,
    onBookmarkClick: (Recipe) -> Unit,
    onLoadMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Alpha testing banner
        item {
            AlphaTestingBanner()
        }

        items(recipes, key = { it.id }) { recipe ->
            RecipeListItem(
                recipe = recipe,
                isBookmarked = bookmarkedRecipeIds.contains(recipe.id),
                onClick = { onRecipeClick(recipe) },
                onBookmarkClick = { onBookmarkClick(recipe) },
            )
        }

        if (hasMorePages) {
            item {
                LoadMoreButton(
                    isLoading = isLoadingMore,
                    onClick = onLoadMoreClick,
                )
            }
        }
    }
}

/**
 * Alpha testing banner to inform users about the feature status.
 */
@Composable
private fun AlphaTestingBanner(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.data_info_alert_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                text = "The recipe feature is in alpha testing state now and may be unstable.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
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
 * Empty state when no recipes are found.
 */
@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(R.drawable.recipe_24dp_e8eaed_fill0_wght400_grad0_opsz24),
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No recipes found",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "Try a different search term",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Empty state when no recipes match the selected category filters.
 *
 * Shows the selected categories and provides a button to clear filters.
 */
@Composable
private fun FilteredEmptyState(
    selectedCategories: Set<String>,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(R.drawable.recipe_24dp_e8eaed_fill0_wght400_grad0_opsz24),
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No recipes found",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "No recipes match the selected categories:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = selectedCategories.joinToString(", ") { it.replaceFirstChar { char -> char.uppercase() } },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp),
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onClearFilters) {
            Text("Clear Category Filters")
        }
    }
}

/**
 * Error state with retry button.
 */
@Composable
private fun ErrorState(
    errorMessage: String,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(16.dp),
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
            text = "Failed to load recipes",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetryClick) {
            Text("Retry")
        }
    }
}

// ============================================
// Composable Previews
// ============================================

// Shared sample recipe data for previews
private val SAMPLE_RECIPES =
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
private fun RecipesCatalogContentPreview() {
    TrmnlBuddyAppTheme {
        RecipesCatalogContent(
            state =
                RecipesCatalogScreen.State(
                    recipes = SAMPLE_RECIPES,
                    bookmarkedRecipeIds = setOf(1), // First recipe is bookmarked
                    searchQuery = "",
                    selectedSort = SortOption.NEWEST,
                    hasMorePages = true,
                    totalRecipes = 100,
                ),
        )
    }
}

@PreviewLightDark
@Composable
private fun RecipesCatalogLoadingPreview() {
    TrmnlBuddyAppTheme {
        RecipesCatalogContent(
            state =
                RecipesCatalogScreen.State(
                    isLoading = true,
                ),
        )
    }
}

@PreviewLightDark
@Composable
private fun RecipesCatalogEmptyPreview() {
    TrmnlBuddyAppTheme {
        RecipesCatalogContent(
            state =
                RecipesCatalogScreen.State(
                    recipes = emptyList(),
                    searchQuery = "nonexistent",
                ),
        )
    }
}

@PreviewLightDark
@Composable
private fun RecipesCatalogErrorPreview() {
    TrmnlBuddyAppTheme {
        RecipesCatalogContent(
            state =
                RecipesCatalogScreen.State(
                    error = "Network connection failed",
                ),
        )
    }
}
