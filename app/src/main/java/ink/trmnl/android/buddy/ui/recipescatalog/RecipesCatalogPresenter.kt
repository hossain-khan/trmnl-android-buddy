package ink.trmnl.android.buddy.ui.recipescatalog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
) : Presenter<RecipesCatalogScreen.State> {
    @Composable
    override fun present(): RecipesCatalogScreen.State {
        var recipes by remember { mutableStateOf<List<Recipe>>(emptyList()) }
        var searchQuery by remember { mutableStateOf("") }
        var selectedSort by remember { mutableStateOf(SortOption.NEWEST) }
        var isLoading by remember { mutableStateOf(false) }
        var isLoadingMore by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }
        var currentPage by remember { mutableStateOf(1) }
        var hasMorePages by remember { mutableStateOf(false) }
        var totalRecipes by remember { mutableStateOf(0) }

        val coroutineScope = rememberCoroutineScope()
        var searchJob by remember { mutableStateOf<Job?>(null) }

        // Load initial recipes on first composition
        LaunchedEffect(Unit) {
            fetchRecipes(
                repository = recipesRepository,
                search = null,
                sortBy = selectedSort.apiValue,
                page = 1,
                append = false,
                onLoadingStart = { isLoading = true },
                onLoadingEnd = { isLoading = false },
                onSuccess = { response ->
                    recipes = response.data
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
            recipes = recipes,
            searchQuery = searchQuery,
            selectedSort = selectedSort,
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
                                append = false,
                                onLoadingStart = { isLoading = true },
                                onLoadingEnd = { isLoading = false },
                                onSuccess = { response ->
                                    recipes = response.data
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
                            append = false,
                            onLoadingStart = { isLoading = true },
                            onLoadingEnd = { isLoading = false },
                            onSuccess = { response ->
                                recipes = response.data
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
                            append = false,
                            onLoadingStart = { isLoading = true },
                            onLoadingEnd = { isLoading = false },
                            onSuccess = { response ->
                                recipes = response.data
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

                RecipesCatalogScreen.Event.LoadMoreClicked -> {
                    if (hasMorePages && !isLoadingMore) {
                        coroutineScope.launch {
                            fetchRecipes(
                                repository = recipesRepository,
                                search = if (searchQuery.isBlank()) null else searchQuery,
                                sortBy = selectedSort.apiValue,
                                page = currentPage + 1,
                                append = true,
                                onLoadingStart = { isLoadingMore = true },
                                onLoadingEnd = { isLoadingMore = false },
                                onSuccess = { response ->
                                    recipes = recipes + response.data
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
                            append = false,
                            onLoadingStart = { isLoading = true },
                            onLoadingEnd = { isLoading = false },
                            onSuccess = { response ->
                                recipes = response.data
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
        }
    }

    @CircuitInject(RecipesCatalogScreen::class, AppScope::class)
    @AssistedFactory
    interface Factory {
        fun create(navigator: Navigator): RecipesCatalogPresenter
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
    append: Boolean,
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
