package ink.trmnl.android.buddy.api.models

/**
 * Response wrapper for the `/categories` API endpoint.
 *
 * Returns a list of valid plugin categories that can be used to improve
 * search exposure and filtering for Public or Recipe style plugins.
 *
 * Example response:
 * ```json
 * {
 *   "data": [
 *     "analytics",
 *     "art",
 *     "calendar",
 *     "comics",
 *     "crm",
 *     "custom",
 *     "discovery",
 *     "ecommerce",
 *     "education",
 *     "email",
 *     "entertainment",
 *     "environment",
 *     "finance",
 *     "games",
 *     "humor",
 *     "images",
 *     "kpi",
 *     "life",
 *     "marketing",
 *     "nature",
 *     "news",
 *     "personal",
 *     "productivity",
 *     "programming",
 *     "sales",
 *     "sports",
 *     "travel"
 *   ]
 * }
 * ```
 *
 * @see ApiResponse
 */
typealias CategoriesResponse = ApiResponse<List<String>>
