package ink.trmnl.android.buddy.data

import ink.trmnl.android.buddy.api.models.Recipe
import ink.trmnl.android.buddy.data.database.BookmarkedRecipeDao
import ink.trmnl.android.buddy.data.database.BookmarkedRecipeEntity
import ink.trmnl.android.buddy.di.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Repository for managing recipe bookmarks.
 *
 * Provides a clean API for adding, removing, and querying bookmarked recipes.
 * Handles conversion between domain models (Recipe) and database entities.
 */
interface BookmarkRepository {
    /**
     * Toggle bookmark status for a recipe.
     * If bookmarked, removes it. If not bookmarked, adds it.
     *
     * @param recipe The recipe to toggle
     */
    suspend fun toggleBookmark(recipe: Recipe)

    /**
     * Check if a recipe is bookmarked.
     *
     * @param recipeId The recipe ID to check
     * @return Flow of boolean indicating bookmark status
     */
    fun isBookmarked(recipeId: Int): Flow<Boolean>

    /**
     * Get all bookmarked recipes.
     *
     * @return Flow of bookmarked recipes list
     */
    fun getAllBookmarks(): Flow<List<Recipe>>

    /**
     * Get all bookmarked recipe IDs.
     *
     * @return Flow of bookmarked recipe ID set
     */
    fun getAllBookmarkedIds(): Flow<Set<Int>>
}

/**
 * Default implementation of BookmarkRepository using Room database.
 */
@ApplicationContext
class DefaultBookmarkRepository(
    private val bookmarkedRecipeDao: BookmarkedRecipeDao,
) : BookmarkRepository {
    override suspend fun toggleBookmark(recipe: Recipe) {
        // Check current status
        val isCurrentlyBookmarked = bookmarkedRecipeDao.isBookmarked(recipe.id).first()

        if (isCurrentlyBookmarked) {
            bookmarkedRecipeDao.deleteByRecipeId(recipe.id)
        } else {
            val entity =
                BookmarkedRecipeEntity(
                    recipeId = recipe.id,
                    recipeName = recipe.name,
                    recipeIconUrl = recipe.iconUrl,
                    installs = recipe.stats.installs,
                    forks = recipe.stats.forks,
                    bookmarkedAt = System.currentTimeMillis(),
                )
            bookmarkedRecipeDao.insert(entity)
        }
    }

    override fun isBookmarked(recipeId: Int): Flow<Boolean> = bookmarkedRecipeDao.isBookmarked(recipeId)

    override fun getAllBookmarks(): Flow<List<Recipe>> =
        bookmarkedRecipeDao.getAllBookmarks().map { entities ->
            entities.map { it.toRecipe() }
        }

    override fun getAllBookmarkedIds(): Flow<Set<Int>> = bookmarkedRecipeDao.getAllBookmarkedIds().map { it.toSet() }
}

/**
 * Convert BookmarkedRecipeEntity to Recipe domain model.
 */
private fun BookmarkedRecipeEntity.toRecipe(): Recipe =
    Recipe(
        id = recipeId,
        name = recipeName,
        iconUrl = recipeIconUrl,
        screenshotUrl = null,
        authorBio = null,
        customFields = emptyList(),
        stats =
            ink.trmnl.android.buddy.api.models.RecipeStats(
                installs = installs,
                forks = forks,
            ),
    )
