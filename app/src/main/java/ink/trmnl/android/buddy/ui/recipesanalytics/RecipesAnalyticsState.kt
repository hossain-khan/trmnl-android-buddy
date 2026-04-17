package ink.trmnl.android.buddy.ui.recipesanalytics

/**
 * Sealed class representing the different states of the Recipes Analytics screen.
 *
 * This wrapper provides type-safe state management with clear distinctions between
 * different UI states (loading, success, error).
 */
sealed class RecipesAnalyticsState {
    /**
     * Analytics data has been successfully loaded.
     *
     * @property data The loaded analytics data
     */
    data class Success(
        val data: RecipesAnalyticsUi,
    ) : RecipesAnalyticsState()

    /**
     * Analytics data is currently being loaded.
     *
     * @property previousData Previously loaded data (if available) to show while loading
     */
    data class Loading(
        val previousData: RecipesAnalyticsUi? = null,
    ) : RecipesAnalyticsState()

    /**
     * An error occurred while loading analytics data.
     *
     * @property message Human-readable error message
     * @property previousData Previously loaded data (if available) to show as fallback
     */
    data class Error(
        val message: String,
        val previousData: RecipesAnalyticsUi? = null,
    ) : RecipesAnalyticsState()
}

// ============================================
// Helper Extension Functions
// ============================================

/**
 * Check if the analytics data is empty (no plugins).
 *
 * Only works on Success state. Returns false for Loading/Error states.
 */
fun RecipesAnalyticsState.isEmpty(): Boolean = this is RecipesAnalyticsState.Success && this.data.isEmpty()

/**
 * Check if overall plugin health is good (> 95% healthy).
 *
 * Only works on Success state. Returns false for Loading/Error states.
 */
fun RecipesAnalyticsState.isHealthy(): Boolean = this is RecipesAnalyticsState.Success && this.data.isHealthy()

/**
 * Check if the analytics is currently loading.
 */
fun RecipesAnalyticsState.isLoading(): Boolean = this is RecipesAnalyticsState.Loading

/**
 * Get the data from any state, preferring current data over previous data.
 *
 * Useful for showing the best available data while loading or on error.
 */
fun RecipesAnalyticsState.getDataOrNull(): RecipesAnalyticsUi? =
    when (this) {
        is RecipesAnalyticsState.Success -> this.data
        is RecipesAnalyticsState.Loading -> this.previousData
        is RecipesAnalyticsState.Error -> this.previousData
    }

// ============================================
// RecipesAnalyticsUi Helper Extensions
// ============================================

/**
 * Check if the analytics data is empty (no plugins).
 */
fun RecipesAnalyticsUi.isEmpty(): Boolean = plugins.isEmpty()

/**
 * Check if overall plugin health is good (> 95% healthy).
 */
fun RecipesAnalyticsUi.isHealthy(): Boolean = healthyPercent > 95.0

/**
 * Format a percentage value for display.
 *
 * Example: 98.5 → "98.5%"
 *
 * @param value The percentage value (0-100)
 * @return Formatted percentage string
 */
fun formatAsPercentage(value: Double): String = String.format("%.1f%%", value)

/**
 * Format a percentage value for display with no decimal places.
 *
 * Example: 98.5 → "99%"
 *
 * @param value The percentage value (0-100)
 * @return Formatted percentage string with no decimals
 */
fun formatAsPercentageWhole(value: Double): String = String.format("%.0f%%", value)

/**
 * Get a display-friendly label for plugin health state.
 *
 * @param state The health state from API ("healthy", "degraded", "erroring")
 * @return Human-readable label
 */
fun getHealthStatusLabel(state: String): String =
    when (state) {
        "healthy" -> "Healthy"
        "degraded" -> "Degraded"
        "erroring" -> "Erroring"
        else -> "Unknown"
    }

/**
 * Normalizes health percentages to sum to 100%.
 *
 * **Why this is needed:**
 * The TRMNL API sometimes returns health percentages that don't sum to 100%.
 * For example: healthy=121.33%, degraded=1.0%, erroring=0.33% (total=122.67%)
 *
 * This is a temporary workaround until the backend is fixed.
 * The web dashboard already applies this normalization, so we do the same
 * in the Android app to keep the display consistent with the web version.
 *
 * **How it works:**
 * Each percentage is divided by the total sum and multiplied by 100 to get
 * the normalized value that represents its proportional share of 100%.
 *
 * @param healthy The healthy percentage from API
 * @param degraded The degraded percentage from API
 * @param erroring The erroring percentage from API
 * @return Triple of (normalizedHealthy, normalizedDegraded, normalizedErroring) that sum to 100%
 */
fun normalizeHealthPercentages(
    healthy: Double,
    degraded: Double,
    erroring: Double,
): Triple<Double, Double, Double> {
    val total = healthy + degraded + erroring
    return if (total > 0) {
        Triple(
            (healthy / total) * 100,
            (degraded / total) * 100,
            (erroring / total) * 100,
        )
    } else {
        Triple(0.0, 0.0, 0.0)
    }
}
