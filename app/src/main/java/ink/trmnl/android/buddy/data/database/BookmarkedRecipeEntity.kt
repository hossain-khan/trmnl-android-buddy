package ink.trmnl.android.buddy.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Database entity for storing bookmarked recipes.
 *
 * This entity tracks which recipes the user has bookmarked for quick access.
 * Each bookmark stores the recipe ID and when it was bookmarked.
 *
 * @property recipeId Unique identifier of the recipe (primary key)
 * @property recipeName Display name of the recipe
 * @property recipeIconUrl URL to the recipe's icon image (nullable)
 * @property installs Number of times the recipe has been installed
 * @property forks Number of times the recipe has been forked
 * @property bookmarkedAt Unix timestamp in milliseconds when this recipe was bookmarked
 */
@Entity(tableName = "bookmarked_recipes")
data class BookmarkedRecipeEntity(
    @PrimaryKey
    @ColumnInfo(name = "recipe_id")
    val recipeId: Int,
    @ColumnInfo(name = "recipe_name")
    val recipeName: String,
    @ColumnInfo(name = "recipe_icon_url")
    val recipeIconUrl: String?,
    @ColumnInfo(name = "installs")
    val installs: Int,
    @ColumnInfo(name = "forks")
    val forks: Int,
    @ColumnInfo(name = "bookmarked_at")
    val bookmarkedAt: Long,
)
