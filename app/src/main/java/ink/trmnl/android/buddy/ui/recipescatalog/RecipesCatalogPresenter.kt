package ink.trmnl.android.buddy.ui.recipescatalog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.Inject
import ink.trmnl.android.buddy.api.models.Recipe
import ink.trmnl.android.buddy.data.BookmarkRepository
import ink.trmnl.android.buddy.data.RecipesRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Delay in milliseconds for search query debouncing.
 * Prevents excessive API calls while user is typing.
 */
private const val SEARCH_DEBOUNCE_MS = 500L

/**
 * Default number of recipes per page.
 */
private const val DEFAULT_PER_PAGE = 25

/**
 * Presenter for RecipesCatalogScreen.
 *
 * Manages state for recipe browsing including:
 * - Search with debouncing (500ms delay)
 * - Sort by 5 different options
 * - Pagination with load more functionality
 * - Error handling and retry
 */
@Inject
class RecipesCatalogPresenter(
    @Assisted private val navigator: Navigator,
    private val recipesRepository: RecipesRepository,
    private val bookmarkRepository: BookmarkRepository,
) : Presenter<RecipesCatalogScreen.State> {
    @Composable
    override fun present(): RecipesCatalogScreen.State {
        var allRecipes by remember { mutableStateOf<List<Recipe>>(emptyList()) }
        var searchQuery by remember { mutableStateOf("") }
        var selectedSort by remember { mutableStateOf(SortOption.POPULARITY) }
        var availableCategories by remember { mutableStateOf<List<String>>(emptyList()) }
        var selectedCategories by remember { mutableStateOf<Set<String>>(emptySet()) }
        var showFilters by remember { mutableStateOf(false) }
        var isLoading by remember { mutableStateOf(false) }
        var isLoadingMore by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }
        var currentPage by remember { mutableStateOf(1) }
        var hasMorePages by remember { mutableStateOf(false) }
        var totalRecipes by remember { mutableStateOf(0) }

        // Collect bookmarked recipe IDs as state
        val bookmarkedRecipeIds by bookmarkRepository
            .getAllBookmarkedIds()
            .collectAsState(initial = emptySet())

        val coroutineScope = rememberCoroutineScope()
        var searchJob by remember { mutableStateOf<Job?>(null) }

        // Apply client-side category filtering to recipes
        val filteredRecipes =
            remember(allRecipes, selectedCategories) {
                filterRecipesByCategories(allRecipes, selectedCategories)
            }

        // Load categories on first composition
        LaunchedEffect(Unit) {
            val categoriesResult = recipesRepository.getCategories()
            categoriesResult.fold(
                onSuccess = { categories ->
                    availableCategories = categories
                },
                onFailure = { exception ->
                    Timber.e(exception, "Failed to fetch categories")
                    // Continue without categories - non-blocking error
                },
            )
        }

        // Load initial recipes on first composition
        LaunchedEffect(Unit) {
            fetchRecipes(
                repository = recipesRepository,
                search = null,
                sortBy = selectedSort.apiValue,
                page = 1,
                onLoadingStart = { isLoading = true },
                onLoadingEnd = { isLoading = false },
                onSuccess = { response ->
                    allRecipes = response.data
                    currentPage = response.currentPage
                    hasMorePages = response.nextPageUrl != null
                    totalRecipes = response.total
                    error = null
                },
                onError = { errorMessage ->
                    error = errorMessage
                },
            )
        }

        return RecipesCatalogScreen.State(
            recipes = filteredRecipes,
            bookmarkedRecipeIds = bookmarkedRecipeIds,
            searchQuery = searchQuery,
            selectedSort = selectedSort,
            availableCategories = availableCategories,
            selectedCategories = selectedCategories,
            showFilters = showFilters,
            isLoading = isLoading,
            isLoadingMore = isLoadingMore,
            error = error,
            currentPage = currentPage,
            hasMorePages = hasMorePages,
            totalRecipes = totalRecipes,
        ) { event ->
            when (event) {
                RecipesCatalogScreen.Event.BackClicked -> {
                    navigator.pop()
                }

                RecipesCatalogScreen.Event.BookmarksClicked -> {
                    navigator.goTo(ink.trmnl.android.buddy.ui.bookmarkedrecipes.BookmarkedRecipesScreen)
                }

                is RecipesCatalogScreen.Event.SearchQueryChanged -> {
                    searchQuery = event.query

                    // Cancel previous search job
                    searchJob?.cancel()

                    // Start new debounced search
                    searchJob =
                        coroutineScope.launch {
                            delay(SEARCH_DEBOUNCE_MS)
                            // Trigger search after debounce delay
                            fetchRecipes(
                                repository = recipesRepository,
                                search = if (event.query.isBlank()) null else event.query,
                                sortBy = selectedSort.apiValue,
                                page = 1,
                                onLoadingStart = { isLoading = true },
                                onLoadingEnd = { isLoading = false },
                                onSuccess = { response ->
                                    allRecipes = response.data
                                    currentPage = response.currentPage
                                    hasMorePages = response.nextPageUrl != null
                                    totalRecipes = response.total
                                    error = null
                                },
                                onError = { errorMessage ->
                                    error = errorMessage
                                },
                            )
                        }
                }

                RecipesCatalogScreen.Event.SearchClicked -> {
                    // Search is already triggered by debouncing, this is a no-op
                    // but kept for future extensibility
                }

                RecipesCatalogScreen.Event.ClearSearchClicked -> {
                    searchQuery = ""
                    searchJob?.cancel()

                    coroutineScope.launch {
                        fetchRecipes(
                            repository = recipesRepository,
                            search = null,
                            sortBy = selectedSort.apiValue,
                            page = 1,
                            onLoadingStart = { isLoading = true },
                            onLoadingEnd = { isLoading = false },
                            onSuccess = { response ->
                                allRecipes = response.data
                                currentPage = response.currentPage
                                hasMorePages = response.nextPageUrl != null
                                totalRecipes = response.total
                                error = null
                            },
                            onError = { errorMessage ->
                                error = errorMessage
                            },
                        )
                    }
                }

                is RecipesCatalogScreen.Event.SortSelected -> {
                    selectedSort = event.sort

                    coroutineScope.launch {
                        fetchRecipes(
                            repository = recipesRepository,
                            search = if (searchQuery.isBlank()) null else searchQuery,
                            sortBy = event.sort.apiValue,
                            page = 1,
                            onLoadingStart = { isLoading = true },
                            onLoadingEnd = { isLoading = false },
                            onSuccess = { response ->
                                allRecipes = response.data
                                currentPage = response.currentPage
                                hasMorePages = response.nextPageUrl != null
                                totalRecipes = response.total
                                error = null
                            },
                            onError = { errorMessage ->
                                error = errorMessage
                            },
                        )
                    }
                }

                is RecipesCatalogScreen.Event.RecipeClicked -> {
                    // For now, just log. Navigation to detail screen can be added later.
                    Timber.d("Recipe clicked: ${event.recipe.name} (ID: ${event.recipe.id})")
                }

                is RecipesCatalogScreen.Event.BookmarkClicked -> {
                    coroutineScope.launch {
                        try {
                            bookmarkRepository.toggleBookmark(event.recipe)
                            Timber.d("Bookmark toggled for recipe: ${event.recipe.name} (ID: ${event.recipe.id})")
                        } catch (e: Exception) {
                            Timber.e(
                                e,
                                "Failed to toggle bookmark for recipe: ${event.recipe.name}",
                            )
                        }
                    }
                }

                RecipesCatalogScreen.Event.LoadMoreClicked -> {
                    if (hasMorePages && !isLoadingMore) {
                        coroutineScope.launch {
                            fetchRecipes(
                                repository = recipesRepository,
                                search = if (searchQuery.isBlank()) null else searchQuery,
                                sortBy = selectedSort.apiValue,
                                page = currentPage + 1,
                                onLoadingStart = { isLoadingMore = true },
                                onLoadingEnd = { isLoadingMore = false },
                                onSuccess = { response ->
                                    allRecipes = allRecipes + response.data
                                    currentPage = response.currentPage
                                    hasMorePages = response.nextPageUrl != null
                                    totalRecipes = response.total
                                    error = null
                                },
                                onError = { errorMessage ->
                                    error = errorMessage
                                },
                            )
                        }
                    }
                }

                RecipesCatalogScreen.Event.RetryClicked -> {
                    coroutineScope.launch {
                        fetchRecipes(
                            repository = recipesRepository,
                            search = if (searchQuery.isBlank()) null else searchQuery,
                            sortBy = selectedSort.apiValue,
                            page = 1,
                            onLoadingStart = { isLoading = true },
                            onLoadingEnd = { isLoading = false },
                            onSuccess = { response ->
                                allRecipes = response.data
                                currentPage = response.currentPage
                                hasMorePages = response.nextPageUrl != null
                                totalRecipes = response.total
                                error = null
                            },
                            onError = { errorMessage ->
                                error = errorMessage
                            },
                        )
                    }
                }

                is RecipesCatalogScreen.Event.CategorySelected -> {
                    // Add category to selected set
                    selectedCategories = selectedCategories + event.category
                }

                is RecipesCatalogScreen.Event.CategoryDeselected -> {
                    // Remove category from selected set
                    selectedCategories = selectedCategories - event.category
                }

                RecipesCatalogScreen.Event.ClearCategoryFilters -> {
                    // Clear all category filters
                    selectedCategories = emptySet()
                }

                RecipesCatalogScreen.Event.ToggleFiltersClicked -> {
                    // Toggle filter visibility
                    showFilters = !showFilters
                }
            }
        }
    }

    @CircuitInject(RecipesCatalogScreen::class, AppScope::class)
    @AssistedFactory
    interface Factory {
        fun create(navigator: Navigator): RecipesCatalogPresenter
    }
}

/**
 * Filter recipes based on selected categories.
 *
 * A recipe matches if its author_bio.category contains ANY of the selected categories.
 * Categories in author_bio are comma-separated (e.g., "calendar,custom").
 *
 * @param recipes List of recipes to filter
 * @param selectedCategories Set of selected category filters
 * @return Filtered list of recipes, or original list if no categories are selected
 */
private fun filterRecipesByCategories(
    recipes: List<Recipe>,
    selectedCategories: Set<String>,
): List<Recipe> {
    if (selectedCategories.isEmpty()) {
        return recipes
    }

    return recipes.filter { recipe ->
        val recipeCategories =
            recipe.authorBio
                ?.category
                ?.split(",")
                ?.map { it.trim() }
                ?.toSet()
                ?: emptySet()

        // Recipe matches if it has ANY of the selected categories
        recipeCategories.any { it in selectedCategories }
    }
}

/**
 * Fetch recipes from the repository.
 *
 * This is a suspend function that handles the API call and callbacks.
 */
private suspend fun fetchRecipes(
    repository: RecipesRepository,
    search: String?,
    sortBy: String,
    page: Int,
    onLoadingStart: () -> Unit,
    onLoadingEnd: () -> Unit,
    onSuccess: (ink.trmnl.android.buddy.api.models.RecipesResponse) -> Unit,
    onError: (String) -> Unit,
) {
    onLoadingStart()

    try {
        val result =
            repository.getRecipes(
                search = search,
                sortBy = sortBy,
                page = page,
                perPage = DEFAULT_PER_PAGE,
            )

        result.fold(
            onSuccess = { response ->
                onSuccess(response)
            },
            onFailure = { exception ->
                Timber.e(exception, "Failed to fetch recipes")
                onError(exception.message ?: "Failed to load recipes")
            },
        )
    } finally {
        onLoadingEnd()
    }
}
