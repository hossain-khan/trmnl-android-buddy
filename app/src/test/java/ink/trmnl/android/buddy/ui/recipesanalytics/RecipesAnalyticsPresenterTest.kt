package ink.trmnl.android.buddy.ui.recipesanalytics

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Unit tests for [RecipesAnalyticsPresenter].
 */
class RecipesAnalyticsPresenterTest {
    @Test
    fun `present emits State with pre-fetched analytics data`() =
        runTest {
            val analyticsUi = createSampleAnalyticsUi()
            val screen = RecipesAnalyticsScreen(analytics = analyticsUi)
            val navigator = FakeNavigator(screen)
            val presenter = RecipesAnalyticsPresenter(navigator, screen)

            presenter.test {
                val state = awaitItem()
                assertThat(state.analytics).isEqualTo(analyticsUi)
                assertThat(state.analytics.totalPlugins).isEqualTo(15)
                assertThat(state.analytics.totalConnections).isEqualTo(350)
                assertThat(state.analytics.totalPageviews).isEqualTo(1200)
            }
        }

    @Test
    fun `BackClicked event pops navigator`() =
        runTest {
            val analyticsUi = createSampleAnalyticsUi()
            val screen = RecipesAnalyticsScreen(analytics = analyticsUi)
            val navigator = FakeNavigator(screen)
            val presenter = RecipesAnalyticsPresenter(navigator, screen)

            presenter.test {
                val state = awaitItem()

                state.eventSink(RecipesAnalyticsScreen.Event.BackClicked)

                val popped = navigator.awaitPop()
                assertThat(popped).isNotNull()
            }
        }

    private fun createSampleAnalyticsUi(): RecipesAnalyticsUi =
        RecipesAnalyticsUi(
            totalPlugins = 15,
            totalConnections = 350,
            totalPageviews = 1200,
            healthyPercent = 85.0,
            degradedPercent = 10.0,
            erroringPercent = 5.0,
            growthData =
                listOf(
                    GrowthDataPointUi(date = "2026-04-09", value = 10),
                    GrowthDataPointUi(date = "2026-04-10", value = 25),
                ),
            plugins =
                listOf(
                    PluginAnalyticsUi(
                        name = "Plugin One",
                        state = "healthy",
                        installs = 45,
                        forks = 8,
                    ),
                ),
        )
}
