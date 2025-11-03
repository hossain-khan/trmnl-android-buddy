package ink.trmnl.android.buddy.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for bookmarked recipes operations.
 *
 * Provides methods to add, remove, and query bookmarked recipes.
 */
@Dao
interface BookmarkedRecipeDao {
    /**
     * Insert a bookmarked recipe.
     * Replaces existing bookmark if there's a conflict.
     *
     * @param bookmark The recipe bookmark to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bookmark: BookmarkedRecipeEntity)

    /**
     * Delete a bookmarked recipe by its recipe ID.
     *
     * @param recipeId The recipe ID to remove from bookmarks
     */
    @Query("DELETE FROM bookmarked_recipes WHERE recipe_id = :recipeId")
    suspend fun deleteByRecipeId(recipeId: Int)

    /**
     * Get all bookmarked recipes, ordered by bookmark time descending (newest first).
     *
     * @return Flow of bookmarked recipes list
     */
    @Query("SELECT * FROM bookmarked_recipes ORDER BY bookmarked_at DESC")
    fun getAllBookmarks(): Flow<List<BookmarkedRecipeEntity>>

    /**
     * Check if a recipe is bookmarked.
     *
     * @param recipeId The recipe ID to check
     * @return Flow of boolean indicating bookmark status
     */
    @Query("SELECT EXISTS(SELECT 1 FROM bookmarked_recipes WHERE recipe_id = :recipeId)")
    fun isBookmarked(recipeId: Int): Flow<Boolean>

    /**
     * Get all bookmarked recipe IDs.
     *
     * @return Flow of bookmarked recipe ID set
     */
    @Query("SELECT recipe_id FROM bookmarked_recipes")
    fun getAllBookmarkedIds(): Flow<List<Int>>

    /**
     * Delete all bookmarked recipes.
     */
    @Query("DELETE FROM bookmarked_recipes")
    suspend fun deleteAll()
}
