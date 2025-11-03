package ink.trmnl.android.buddy.data.database

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Fake implementation of [BookmarkedRecipeDao] for testing.
 *
 * Uses an in-memory map to store bookmarks without requiring a real database.
 */
class FakeBookmarkedRecipeDao : BookmarkedRecipeDao {
    private val bookmarks = MutableStateFlow<Map<Int, BookmarkedRecipeEntity>>(emptyMap())

    override suspend fun insert(bookmark: BookmarkedRecipeEntity) {
        bookmarks.value = bookmarks.value + (bookmark.recipeId to bookmark)
    }

    override suspend fun deleteByRecipeId(recipeId: Int) {
        bookmarks.value = bookmarks.value - recipeId
    }

    override fun getAllBookmarks(): Flow<List<BookmarkedRecipeEntity>> =
        bookmarks.map { map ->
            map.values.sortedByDescending { it.bookmarkedAt }
        }

    override fun isBookmarked(recipeId: Int): Flow<Boolean> = bookmarks.map { it.containsKey(recipeId) }

    override fun getAllBookmarkedIds(): Flow<List<Int>> = bookmarks.map { it.keys.toList() }

    override suspend fun deleteAll() {
        bookmarks.value = emptyMap()
    }
}
