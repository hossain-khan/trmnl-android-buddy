package ink.trmnl.android.buddy.data

import ink.trmnl.android.buddy.api.models.Recipe
import ink.trmnl.android.buddy.data.database.FakeBookmarkedRecipeDao

/**
 * Fake implementation of [BookmarkRepository] for testing.
 */
class FakeBookmarkRepository : BookmarkRepository {
    private val dao = FakeBookmarkedRecipeDao()
    private val repository = DefaultBookmarkRepository(dao)

    override suspend fun toggleBookmark(recipe: Recipe) = repository.toggleBookmark(recipe)

    override fun isBookmarked(recipeId: Int) = repository.isBookmarked(recipeId)

    override fun getAllBookmarks() = repository.getAllBookmarks()

    override fun getAllBookmarkedIds() = repository.getAllBookmarkedIds()
}
