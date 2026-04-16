package ink.trmnl.android.buddy.ui.recipesanalytics

import androidx.compose.runtime.Composable
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.Inject

/**
 * Presenter for [RecipesAnalyticsScreen].
 *
 * Handles navigation events and passes the pre-fetched analytics data
 * directly to the UI without making additional API calls.
 */
@Inject
class RecipesAnalyticsPresenter(
    @Assisted private val navigator: Navigator,
    @Assisted private val screen: RecipesAnalyticsScreen,
) : Presenter<RecipesAnalyticsScreen.State> {
    @Composable
    override fun present(): RecipesAnalyticsScreen.State =
        RecipesAnalyticsScreen.State(
            analytics = screen.analytics,
        ) { event ->
            when (event) {
                RecipesAnalyticsScreen.Event.BackClicked -> {
                    navigator.pop()
                }
            }
        }

    @CircuitInject(RecipesAnalyticsScreen::class, AppScope::class)
    @AssistedFactory
    interface Factory {
        fun create(
            navigator: Navigator,
            screen: RecipesAnalyticsScreen,
        ): RecipesAnalyticsPresenter
    }
}
