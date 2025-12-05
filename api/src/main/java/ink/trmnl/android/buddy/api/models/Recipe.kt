package ink.trmnl.android.buddy.api.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a TRMNL plugin recipe from the community catalog.
 *
 * Recipes are community-created plugins that can be installed on TRMNL devices.
 * Each recipe includes configuration fields, statistics, and metadata.
 *
 * **Note**: This API endpoint is in alpha testing and may be moved to `/api/recipes`
 * or `/api/plugins` before end of 2025.
 *
 * @property id Unique identifier for the recipe
 * @property name Display name of the recipe
 * @property iconUrl URL to the recipe's icon image (nullable)
 * @property screenshotUrl URL to the recipe's screenshot image (nullable)
 * @property authorBio Information about the recipe author (nullable)
 * @property customFields List of configuration fields for the recipe
 * @property stats Statistics about recipe usage (installs, forks)
 */
@Serializable
data class Recipe(
    val id: Int,
    val name: String,
    @SerialName("icon_url")
    val iconUrl: String? = null,
    @SerialName("screenshot_url")
    val screenshotUrl: String? = null,
    @SerialName("author_bio")
    val authorBio: AuthorBio? = null,
    @SerialName("custom_fields")
    val customFields: List<CustomField> = emptyList(),
    val stats: RecipeStats,
)

/**
 * Author biography information for a recipe.
 *
 * @property keyname Unique key identifier
 * @property name Display name
 * @property fieldType Type of field ("author_bio", etc.)
 * @property description Author biography text
 * @property category Comma-separated list of categories (e.g., "calendar,custom")
 */
@Serializable
data class AuthorBio(
    val keyname: String? = null,
    val name: String? = null,
    @SerialName("field_type")
    val fieldType: String? = null,
    val description: String? = null,
    val category: String? = null,
)

/**
 * Custom configuration field for a recipe.
 *
 * Defines user-configurable parameters for the recipe.
 *
 * @property keyname Unique key identifier for the field
 * @property name Display name of the field
 * @property fieldType Type of field (e.g., "string", "select", "author_bio")
 * @property description Field description
 * @property placeholder Placeholder text for input fields
 * @property helpText Additional help text
 * @property required Whether the field is required
 */
@Serializable
data class CustomField(
    val keyname: String,
    val name: String,
    @SerialName("field_type")
    val fieldType: String,
    val description: String? = null,
    val placeholder: String? = null,
    @SerialName("help_text")
    val helpText: String? = null,
    val required: Boolean? = null,
)

/**
 * Usage statistics for a recipe.
 *
 * @property installs Number of times the recipe has been installed
 * @property forks Number of times the recipe has been forked/copied
 */
@Serializable
data class RecipeStats(
    val installs: Int,
    val forks: Int,
)

/**
 * Response wrapper for a list of recipes with pagination metadata.
 *
 * @property data List of recipes
 * @property total Total number of recipes matching the query
 * @property from Starting index of the current page
 * @property to Ending index of the current page
 * @property perPage Number of items per page
 * @property currentPage Current page number
 * @property prevPageUrl URL to the previous page (or null if on first page)
 * @property nextPageUrl URL to the next page (or null if on last page)
 */
@Serializable
data class RecipesResponse(
    val data: List<Recipe>,
    val total: Int,
    val from: Int,
    val to: Int,
    @SerialName("per_page")
    val perPage: Int,
    @SerialName("current_page")
    val currentPage: Int,
    @SerialName("prev_page_url")
    val prevPageUrl: String?,
    @SerialName("next_page_url")
    val nextPageUrl: String?,
)

/**
 * Response wrapper for a single recipe.
 *
 * @property data The recipe data
 */
@Serializable
data class RecipeDetailResponse(
    val data: Recipe,
)
