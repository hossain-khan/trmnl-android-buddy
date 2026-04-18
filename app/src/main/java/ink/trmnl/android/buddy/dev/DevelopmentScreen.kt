package ink.trmnl.android.buddy.dev

import androidx.work.WorkInfo
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.Screen
import ink.trmnl.android.buddy.ui.recipesanalytics.RecipesAnalyticsState
import ink.trmnl.android.buddy.work.WorkerStatus
import kotlinx.parcelize.Parcelize

/**
 * Development screen for testing and debugging features.
 *
 * **DEBUG BUILDS ONLY** - This screen should not be accessible in release builds.
 *
 * Features:
 * - Test all notification types with various parameters
 * - Trigger workers manually (one-time requests)
 * - Test notification channels and permissions
 * - Quick access to development flags (AppDevConfig)
 * - Monitor WorkManager worker status and execution
 */
@Parcelize
data object DevelopmentScreen : Screen {
    /**
     * UI state for the Development screen.
     */
    data class State(
        val notificationPermissionGranted: Boolean = false,
        val workerStatuses: List<WorkerStatus> = emptyList(),
        val analyticsState: RecipesAnalyticsState = RecipesAnalyticsState.Loading(),
        val currentAnalyticsScenario: AnalyticsScenario? = null,
        val eventSink: (Event) -> Unit = {},
    ) : CircuitUiState

    /**
     * Events that can be triggered from the Development screen.
     */
    sealed interface Event : CircuitUiEvent {
        // Notification testing events
        data class TestLowBatteryNotification(
            val deviceCount: Int,
            val thresholdPercent: Int,
        ) : Event

        data class TestBlogPostNotification(
            val postCount: Int,
        ) : Event

        data class TestAnnouncementNotification(
            val announcementCount: Int,
        ) : Event

        // Worker trigger events
        data object TriggerLowBatteryWorker : Event

        data object TriggerBlogPostWorker : Event

        data object TriggerAnnouncementWorker : Event

        data object TriggerBatteryCollectionWorker : Event

        // Worker management events
        data object CancelAllWorkers : Event

        data object ResetWorkerSchedules : Event

        // Analytics simulation events
        data class SimulateRecipesAnalytics(
            val scenario: AnalyticsScenario,
        ) : Event

        data object ClearAnalyticsCache : Event

        // System events
        data object RequestNotificationPermission : Event

        data object OpenNotificationSettings : Event

        data object NavigateBack : Event
    }

    /**
     * Analytics test scenarios for simulating different UI states.
     * Allows testing RecipesHealthCard and RecipeHealthCardSection with various data conditions.
     */
    sealed interface AnalyticsScenario {
        /**
         * User has no published recipes - analytics section should not appear.
         * Used to test visibility toggle of RecipeHealthCardSection.
         */
        data object NoRecipes : AnalyticsScenario

        /**
         * All published recipes are healthy (health percentage > 95%).
         * RecipesHealthCard should show "All healthy" status.
         */
        data object AllHealthy : AnalyticsScenario

        /**
         * All published recipes are unhealthy (health percentage ≤ 95%).
         * RecipesHealthCard should show warning indicator.
         */
        data object AllUnhealthy : AnalyticsScenario

        /**
         * Mix of healthy and unhealthy recipes.
         * RecipesHealthCard should show count of unhealthy recipes.
         *
         * @param unhealthyCount Number of unhealthy recipes to simulate (must be > 0)
         */
        data class PartiallyHealthy(
            val unhealthyCount: Int,
        ) : AnalyticsScenario

        /**
         * Simulate data loading state.
         * RecipesHealthCard should show loading skeleton/placeholder.
         */
        data object Loading : AnalyticsScenario

        /**
         * Simulate API error during analytics fetch.
         * RecipesHealthCard should handle error gracefully.
         */
        data object Error : AnalyticsScenario

        /**
         * Large number of recipes with mixed health states.
         * Useful for testing performance and overflow scenarios.
         *
         * @param recipeCount Total number of recipes to simulate
         * @param unhealthyPercent Percentage of recipes that are unhealthy (0-100)
         */
        data class LargeDataset(
            val recipeCount: Int,
            val unhealthyPercent: Int,
        ) : AnalyticsScenario
    }
}
