package ink.trmnl.android.buddy.ui.recipesanalytics

import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.Screen
import kotlinx.parcelize.Parcelize

/**
 * Screen for displaying recipes analytics dashboard.
 *
 * Shows aggregated statistics about published plugins including:
 * - Health status breakdown
 * - Connection and pageview metrics
 * - Growth trend over last 8 days
 * - Individual plugin statistics
 *
 * @param analytics Pre-fetched analytics data to display (no API calls made)
 */
@Parcelize
data class RecipesAnalyticsScreen(
    val analytics: RecipesAnalyticsUi,
) : Screen {
    /**
     * State for the recipes analytics screen.
     */
    data class State(
        val analytics: RecipesAnalyticsUi,
        val eventSink: (Event) -> Unit = {},
    ) : CircuitUiState

    /**
     * Events that can be triggered from the recipes analytics screen.
     */
    sealed interface Event : CircuitUiEvent {
        /** User tapped back to return to previous screen */
        data object BackClicked : Event
    }
}
