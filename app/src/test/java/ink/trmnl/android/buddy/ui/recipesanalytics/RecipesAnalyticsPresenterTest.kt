package ink.trmnl.android.buddy.ui.recipesanalytics

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import com.slack.eithernet.ApiResult
import ink.trmnl.android.buddy.api.models.ApiError
import ink.trmnl.android.buddy.api.models.ApiResponse
import ink.trmnl.android.buddy.api.models.AuthorPluginError
import ink.trmnl.android.buddy.api.models.UninstallComment
import ink.trmnl.android.buddy.api.models.UninstallFeedback
import ink.trmnl.android.buddy.api.models.UninstallReasonBreakdown
import ink.trmnl.android.buddy.data.preferences.UserPreferences
import ink.trmnl.android.buddy.fakes.FakeTrmnlApiService
import ink.trmnl.android.buddy.fakes.FakeUserPreferencesRepository
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
            val presenter =
                RecipesAnalyticsPresenter(
                    navigator = navigator,
                    screen = screen,
                    apiService = FakeTrmnlApiService(),
                    userPreferencesRepository = FakeUserPreferencesRepository(UserPreferences(apiToken = "")),
                )

            presenter.test {
                val state = awaitItem()
                assertThat(state.analytics).isEqualTo(analyticsUi)
                assertThat(state.analytics.totalPlugins).isEqualTo(15)
                assertThat(state.analytics.totalConnections).isEqualTo(350)
                assertThat(state.analytics.totalPageviews).isEqualTo(1200)
                assertThat(state.pluginErrors).isEmpty()
                assertThat(state.uninstallFeedback).isNull()
            }
        }

    @Test
    fun `BackClicked event pops navigator`() =
        runTest {
            val analyticsUi = createSampleAnalyticsUi()
            val screen = RecipesAnalyticsScreen(analytics = analyticsUi)
            val navigator = FakeNavigator(screen)
            val presenter =
                RecipesAnalyticsPresenter(
                    navigator = navigator,
                    screen = screen,
                    apiService = FakeTrmnlApiService(),
                    userPreferencesRepository = FakeUserPreferencesRepository(UserPreferences(apiToken = "")),
                )

            presenter.test {
                val state = awaitItem()

                state.eventSink(RecipesAnalyticsScreen.Event.BackClicked)

                val popped = navigator.awaitPop()
                assertThat(popped).isNotNull()
            }
        }

    @Test
    fun `present loads plugin errors and uninstall feedback when token is present`() =
        runTest {
            val analyticsUi = createSampleAnalyticsUi()
            val screen = RecipesAnalyticsScreen(analytics = analyticsUi)
            val navigator = FakeNavigator(screen)
            val fakeApi =
                FakeTrmnlApiService().apply {
                    getAuthorPluginErrorsResult =
                        ApiResult.success(
                            ApiResponse(
                                listOf(
                                    AuthorPluginError(
                                        kind = "recipe",
                                        subjectId = 42,
                                        pluginName = "Weather Radar",
                                        owner = "author",
                                        message = "Upstream HTTP 500",
                                        count = 3,
                                        lastSeen = "2026-11-12T09:30:00Z",
                                        hidden = false,
                                    ),
                                ),
                            ),
                        )
                    getUninstallFeedbackResult =
                        ApiResult.success(
                            ApiResponse(
                                UninstallFeedback(
                                    windowDays = 30,
                                    total = 12,
                                    reasonBreakdown =
                                        listOf(
                                            UninstallReasonBreakdown("Other", 7),
                                            UninstallReasonBreakdown("Just testing / temporary", 5),
                                        ),
                                    recentComments =
                                        listOf(
                                            UninstallComment(
                                                detail = "Needed hourly updates",
                                                pluginName = "Weather Radar",
                                                reasonLabel = "Other",
                                                kindLabel = "Recipe",
                                                createdAt = "2026-11-11T14:00:00Z",
                                            ),
                                        ),
                                    typicalDaysInstalled = 3,
                                ),
                            ),
                        )
                }
            val presenter =
                RecipesAnalyticsPresenter(
                    navigator = navigator,
                    screen = screen,
                    apiService = fakeApi,
                    userPreferencesRepository = FakeUserPreferencesRepository(UserPreferences(apiToken = "trmnl_test")),
                )

            presenter.test {
                // Drain until loading completes
                var state = awaitItem()
                while (state.isLoadingCreatorInsights || (state.pluginErrors.isEmpty() && state.uninstallFeedback == null)) {
                    state = awaitItem()
                }

                assertThat(state.isLoadingCreatorInsights).isFalse()
                assertThat(fakeApi.lastAuthorizationHeader).isEqualTo("Bearer trmnl_test")

                assertThat(state.pluginErrors).hasSize(1)
                assertThat(state.pluginErrors[0].pluginName).isEqualTo("Weather Radar")
                assertThat(state.pluginErrors[0].owner).isEqualTo("author")
                assertThat(state.pluginErrors[0].count).isEqualTo(3)
                assertThat(state.pluginErrorsError).isNull()

                val feedback = state.uninstallFeedback
                assertThat(feedback).isNotNull()
                assertThat(feedback!!.total).isEqualTo(12)
                assertThat(feedback.typicalDaysInstalled).isEqualTo(3)
                assertThat(feedback.reasonBreakdown).hasSize(2)
                assertThat(feedback.recentComments).hasSize(1)
                assertThat(state.uninstallFeedbackError).isNull()
            }
        }

    @Test
    fun `present handles 403 insufficient scope error gracefully`() =
        runTest {
            val analyticsUi = createSampleAnalyticsUi()
            val screen = RecipesAnalyticsScreen(analytics = analyticsUi)
            val navigator = FakeNavigator(screen)
            val fakeApi =
                FakeTrmnlApiService().apply {
                    getAuthorPluginErrorsResult =
                        ApiResult.httpFailure(403, ApiError("Insufficient scope"))
                    getUninstallFeedbackResult =
                        ApiResult.httpFailure(403, ApiError("Insufficient scope"))
                }
            val presenter =
                RecipesAnalyticsPresenter(
                    navigator = navigator,
                    screen = screen,
                    apiService = fakeApi,
                    userPreferencesRepository = FakeUserPreferencesRepository(UserPreferences(apiToken = "user_legacy")),
                )

            presenter.test {
                var state = awaitItem()
                while (state.isLoadingCreatorInsights || state.pluginErrorsError == null) {
                    state = awaitItem()
                }

                assertThat(state.isLoadingCreatorInsights).isFalse()
                assertThat(state.analytics).isEqualTo(analyticsUi)
                assertThat(state.pluginErrorsError).isEqualTo("Requires a scoped TRMNL API key (trmnl_…) with analytics access.")
                assertThat(state.uninstallFeedbackError).isEqualTo("Requires a scoped TRMNL API key (trmnl_…) with analytics access.")
            }
        }

    @Test
    fun `RefreshCreatorInsights event reloads creator insights after failure`() =
        runTest {
            val analyticsUi = createSampleAnalyticsUi()
            val screen = RecipesAnalyticsScreen(analytics = analyticsUi)
            val navigator = FakeNavigator(screen)
            val fakeApi =
                FakeTrmnlApiService().apply {
                    getAuthorPluginErrorsResult =
                        ApiResult.httpFailure(500, ApiError("Server Error"))
                    getUninstallFeedbackResult =
                        ApiResult.httpFailure(500, ApiError("Server Error"))
                }
            val presenter =
                RecipesAnalyticsPresenter(
                    navigator = navigator,
                    screen = screen,
                    apiService = fakeApi,
                    userPreferencesRepository = FakeUserPreferencesRepository(UserPreferences(apiToken = "trmnl_test")),
                )

            presenter.test {
                var state = awaitItem()
                while (state.isLoadingCreatorInsights || state.pluginErrorsError == null) {
                    state = awaitItem()
                }
                assertThat(state.pluginErrorsError).isNotNull()

                // Update fake responses and trigger refresh
                fakeApi.getAuthorPluginErrorsResult = ApiResult.success(ApiResponse(emptyList()))
                fakeApi.getUninstallFeedbackResult =
                    ApiResult.success(
                        ApiResponse(
                            UninstallFeedback(
                                windowDays = 30,
                                total = 0,
                                reasonBreakdown = emptyList(),
                                recentComments = emptyList(),
                                typicalDaysInstalled = null,
                            ),
                        ),
                    )
                state.eventSink(RecipesAnalyticsScreen.Event.RefreshCreatorInsights)

                state = awaitItem()
                while (state.isLoadingCreatorInsights || state.uninstallFeedback == null) {
                    state = awaitItem()
                }

                assertThat(state.pluginErrorsError).isNull()
                assertThat(state.uninstallFeedbackError).isNull()
                assertThat(state.pluginErrors).isEmpty()
                assertThat(state.uninstallFeedback.total).isEqualTo(0)
                cancelAndIgnoreRemainingEvents()
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
