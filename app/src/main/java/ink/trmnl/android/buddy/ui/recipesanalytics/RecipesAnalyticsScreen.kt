package ink.trmnl.android.buddy.ui.recipesanalytics

import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.ParcelableScreen
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
) : ParcelableScreen {
    /**
     * State for the recipes analytics screen.
     */
    data class State(
        val analytics: RecipesAnalyticsUi,
        val isLoadingCreatorInsights: Boolean = false,
        val pluginErrors: List<AuthorPluginErrorUi> = emptyList(),
        val pluginErrorsError: String? = null,
        val uninstallFeedback: UninstallFeedbackUi? = null,
        val uninstallFeedbackError: String? = null,
        val eventSink: (Event) -> Unit = {},
    ) : CircuitUiState

    /**
     * Events that can be triggered from the recipes analytics screen.
     */
    sealed interface Event : CircuitUiEvent {
        /** User tapped back to return to previous screen */
        data object BackClicked : Event

        /** User tapped retry to reload creator insights */
        data object RefreshCreatorInsights : Event
    }
}

/**
 * UI model for a failing plugin install entry.
 */
data class AuthorPluginErrorUi(
    val pluginName: String,
    val kind: String,
    val owner: String,
    val message: String,
    val count: Int,
    val lastSeen: String,
    val hidden: Boolean,
)

/**
 * UI model for an uninstall reason breakdown row.
 */
data class UninstallReasonUi(
    val reason: String,
    val count: Int,
)

/**
 * UI model for an individual uninstall comment.
 */
data class UninstallCommentUi(
    val detail: String,
    val pluginName: String,
    val reasonLabel: String?,
    val kindLabel: String?,
    val createdAt: String,
)

/**
 * UI model for 30-day uninstall feedback summary.
 */
data class UninstallFeedbackUi(
    val windowDays: Int,
    val total: Int,
    val typicalDaysInstalled: Int?,
    val reasonBreakdown: List<UninstallReasonUi>,
    val recentComments: List<UninstallCommentUi>,
)
