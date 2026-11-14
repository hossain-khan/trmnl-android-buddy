package ink.trmnl.android.buddy.ui.recipesanalytics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.eithernet.ApiResult
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import ink.trmnl.android.buddy.api.TrmnlApiService
import ink.trmnl.android.buddy.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first

/**
 * Presenter for [RecipesAnalyticsScreen].
 *
 * Displays pre-fetched analytics data and asynchronously loads creator insights
 * (failing plugin installs and 30-day uninstall feedback).
 */
@AssistedInject
class RecipesAnalyticsPresenter(
    @Assisted private val navigator: Navigator,
    @Assisted private val screen: RecipesAnalyticsScreen,
    private val apiService: TrmnlApiService,
    private val userPreferencesRepository: UserPreferencesRepository,
) : Presenter<RecipesAnalyticsScreen.State> {
    @Composable
    override fun present(): RecipesAnalyticsScreen.State {
        var isLoadingCreatorInsights by rememberRetained { mutableStateOf(false) }
        var pluginErrors by rememberRetained { mutableStateOf<List<AuthorPluginErrorUi>>(emptyList()) }
        var pluginErrorsError by rememberRetained { mutableStateOf<String?>(null) }
        var uninstallFeedback by rememberRetained { mutableStateOf<UninstallFeedbackUi?>(null) }
        var uninstallFeedbackError by rememberRetained { mutableStateOf<String?>(null) }
        var refreshTrigger by rememberRetained { mutableIntStateOf(0) }

        LaunchedEffect(refreshTrigger) {
            val token = userPreferencesRepository.userPreferencesFlow.first().apiToken
            if (token.isNullOrBlank()) {
                return@LaunchedEffect
            }

            isLoadingCreatorInsights = true
            pluginErrorsError = null
            uninstallFeedbackError = null

            val authHeader = "Bearer $token"
            coroutineScope {
                val errorsDeferred = async { apiService.getAuthorPluginErrors(authHeader) }
                val feedbackDeferred = async { apiService.getUninstallFeedback(authHeader) }

                when (val errorsResult = errorsDeferred.await()) {
                    is ApiResult.Success -> {
                        pluginErrors =
                            errorsResult.value.data.map { error ->
                                AuthorPluginErrorUi(
                                    pluginName =
                                        error.pluginName?.takeIf { it.isNotBlank() }
                                            ?: "Plugin #${error.subjectId}",
                                    kind = error.kind,
                                    owner = error.owner,
                                    message = error.message,
                                    count = error.count,
                                    lastSeen = error.lastSeen,
                                    hidden = error.hidden,
                                )
                            }
                    }

                    is ApiResult.Failure -> {
                        pluginErrorsError = formatInsightError(errorsResult, "failing installs")
                    }
                }

                when (val feedbackResult = feedbackDeferred.await()) {
                    is ApiResult.Success -> {
                        val data = feedbackResult.value.data
                        uninstallFeedback =
                            UninstallFeedbackUi(
                                windowDays = data.windowDays,
                                total = data.total,
                                typicalDaysInstalled = data.typicalDaysInstalled,
                                reasonBreakdown =
                                    data.reasonBreakdown.map { item ->
                                        UninstallReasonUi(
                                            reason = item.reason,
                                            count = item.count,
                                        )
                                    },
                                recentComments =
                                    data.recentComments.map { comment ->
                                        UninstallCommentUi(
                                            detail = comment.detail,
                                            pluginName = comment.pluginName,
                                            reasonLabel = comment.reasonLabel,
                                            kindLabel = comment.kindLabel,
                                            createdAt = comment.createdAt,
                                        )
                                    },
                            )
                    }

                    is ApiResult.Failure -> {
                        uninstallFeedbackError =
                            formatInsightError(feedbackResult, "uninstall feedback")
                    }
                }
            }

            isLoadingCreatorInsights = false
        }

        return RecipesAnalyticsScreen.State(
            analytics = screen.analytics,
            isLoadingCreatorInsights = isLoadingCreatorInsights,
            pluginErrors = pluginErrors,
            pluginErrorsError = pluginErrorsError,
            uninstallFeedback = uninstallFeedback,
            uninstallFeedbackError = uninstallFeedbackError,
        ) { event ->
            when (event) {
                RecipesAnalyticsScreen.Event.BackClicked -> {
                    navigator.pop()
                }

                RecipesAnalyticsScreen.Event.RefreshCreatorInsights -> {
                    refreshTrigger++
                }
            }
        }
    }

    private fun formatInsightError(
        failure: ApiResult.Failure<*>,
        label: String,
    ): String =
        when (failure) {
            is ApiResult.Failure.HttpFailure -> {
                if (failure.code == 401 || failure.code == 403) {
                    "Requires a scoped TRMNL API key (trmnl_…) with analytics access."
                } else {
                    "Unable to load $label (HTTP ${failure.code})."
                }
            }

            is ApiResult.Failure.NetworkFailure -> {
                "Network error while loading $label."
            }

            else -> {
                "Unable to load $label."
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
