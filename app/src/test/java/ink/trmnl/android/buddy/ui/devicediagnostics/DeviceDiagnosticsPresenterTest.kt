package ink.trmnl.android.buddy.ui.devicediagnostics

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import com.slack.eithernet.ApiResult
import ink.trmnl.android.buddy.api.models.ApiError
import ink.trmnl.android.buddy.api.models.DeviceForecast
import ink.trmnl.android.buddy.api.models.DeviceForecastResponse
import ink.trmnl.android.buddy.api.models.DeviceLogCursor
import ink.trmnl.android.buddy.api.models.DeviceLogsData
import ink.trmnl.android.buddy.api.models.DeviceLogsResponse
import ink.trmnl.android.buddy.api.models.DeviceTimeline
import ink.trmnl.android.buddy.api.models.DeviceTimelineResponse
import ink.trmnl.android.buddy.api.models.TimelineCheckIn
import ink.trmnl.android.buddy.api.models.TimelineStep
import ink.trmnl.android.buddy.data.preferences.UserPreferences
import ink.trmnl.android.buddy.fakes.FakeTrmnlApiService
import ink.trmnl.android.buddy.fakes.FakeUserPreferencesRepository
import ink.trmnl.android.buddy.ui.accesstoken.AccessTokenScreen
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.junit.Test
import java.io.IOException

class DeviceDiagnosticsPresenterTest {
    @Test
    fun `presenter loads forecast on start and updates when hours change`() =
        runTest {
            val screen = DeviceDiagnosticsScreen(deviceId = 42, deviceName = "Office TRMNL")
            val navigator = FakeNavigator(screen)
            val apiService =
                FakeTrmnlApiService().apply {
                    getDeviceForecastResult =
                        ApiResult.success(
                            DeviceForecastResponse(
                                data =
                                    DeviceForecast(
                                        steps =
                                            listOf(
                                                TimelineStep(
                                                    at = "2026-11-13T05:15:00Z",
                                                    name = "Weather",
                                                    refreshRateSeconds = 900,
                                                ),
                                            ),
                                    ),
                            ),
                        )
                }
            val prefsRepo = FakeUserPreferencesRepository(UserPreferences(apiToken = "user_test"))
            val presenter = DeviceDiagnosticsPresenter(screen, navigator, apiService, prefsRepo)

            presenter.test {
                var state: DeviceDiagnosticsScreen.State
                do {
                    state = awaitItem()
                } while (state.isForecastLoading)

                assertThat(state.selectedTab).isEqualTo(DeviceDiagnosticsScreen.DiagnosticsTab.FORECAST)
                assertThat(state.forecastSteps).hasSize(1)
                assertThat(state.forecastSteps.first().name).isEqualTo("Weather")
                assertThat(apiService.lastForecastHours).isEqualTo(12)

                state.eventSink(DeviceDiagnosticsScreen.Event.ForecastHoursSelected(24))
                do {
                    state = awaitItem()
                } while (state.isForecastLoading || state.forecastHours != 24)

                assertThat(apiService.lastForecastHours).isEqualTo(24)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `presenter loads timeline when Timeline tab is selected`() =
        runTest {
            val screen = DeviceDiagnosticsScreen(deviceId = 42, deviceName = "Office TRMNL")
            val navigator = FakeNavigator(screen)
            val apiService =
                FakeTrmnlApiService().apply {
                    getDeviceForecastResult = ApiResult.success(DeviceForecastResponse(DeviceForecast()))
                    getDeviceTimelineResult =
                        ApiResult.success(
                            DeviceTimelineResponse(
                                data =
                                    DeviceTimeline(
                                        date = "2026-11-13",
                                        today = "2026-11-13",
                                        zone = "UTC",
                                        available = true,
                                        checkIn = TimelineCheckIn(state = "on_time"),
                                    ),
                            ),
                        )
                }
            val prefsRepo = FakeUserPreferencesRepository(UserPreferences(apiToken = "user_test"))
            val presenter = DeviceDiagnosticsPresenter(screen, navigator, apiService, prefsRepo)

            presenter.test {
                var state: DeviceDiagnosticsScreen.State
                do {
                    state = awaitItem()
                } while (state.isForecastLoading)

                state.eventSink(DeviceDiagnosticsScreen.Event.TabSelected(DeviceDiagnosticsScreen.DiagnosticsTab.TIMELINE))
                do {
                    state = awaitItem()
                } while (state.selectedTab != DeviceDiagnosticsScreen.DiagnosticsTab.TIMELINE || state.isTimelineLoading ||
                    state.timeline == null
                )

                assertThat(state.timeline?.checkIn?.state).isEqualTo("on_time")
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `presenter loads logs, filters by level, and supports cursor pagination`() =
        runTest {
            val screen = DeviceDiagnosticsScreen(deviceId = 42, deviceName = "Office TRMNL")
            val navigator = FakeNavigator(screen)
            val apiService =
                FakeTrmnlApiService().apply {
                    getDeviceForecastResult = ApiResult.success(DeviceForecastResponse(DeviceForecast()))
                    getDeviceLogsResult =
                        ApiResult.success(
                            DeviceLogsResponse(
                                data =
                                    DeviceLogsData(
                                        logs =
                                            listOf(
                                                buildJsonObject {
                                                    put("event_id", JsonPrimitive("evt_1"))
                                                    put("ts", JsonPrimitive("2026-11-13T05:00:00Z"))
                                                    put("level", JsonPrimitive("error"))
                                                    put("source", JsonPrimitive("device"))
                                                    put("message", JsonPrimitive("HTTP 502"))
                                                },
                                            ),
                                        more = true,
                                        oldestCursor = DeviceLogCursor(ts = "2026-11-13T05:00:00Z", eventId = "evt_1"),
                                    ),
                            ),
                        )
                }
            val prefsRepo = FakeUserPreferencesRepository(UserPreferences(apiToken = "user_test"))
            val presenter = DeviceDiagnosticsPresenter(screen, navigator, apiService, prefsRepo)

            presenter.test {
                var state: DeviceDiagnosticsScreen.State
                do {
                    state = awaitItem()
                } while (state.isForecastLoading)

                state.eventSink(DeviceDiagnosticsScreen.Event.TabSelected(DeviceDiagnosticsScreen.DiagnosticsTab.LOGS))
                do {
                    state = awaitItem()
                } while (state.selectedTab != DeviceDiagnosticsScreen.DiagnosticsTab.LOGS || state.isLogsLoading || state.logs.isEmpty())

                assertThat(state.logs).hasSize(1)
                assertThat(state.logs.first().message).isEqualTo("HTTP 502")
                assertThat(state.hasMoreLogs).isTrue()

                apiService.getDeviceLogsResult =
                    ApiResult.success(
                        DeviceLogsResponse(
                            data =
                                DeviceLogsData(
                                    logs =
                                        listOf(
                                            buildJsonObject {
                                                put("event_id", JsonPrimitive("evt_0"))
                                                put("ts", JsonPrimitive("2026-11-13T04:00:00Z"))
                                                put("level", JsonPrimitive("error"))
                                                put("message", JsonPrimitive("Older log"))
                                            },
                                        ),
                                    more = false,
                                    oldestCursor = null,
                                ),
                        ),
                    )

                state.eventSink(DeviceDiagnosticsScreen.Event.LoadMoreLogs)
                do {
                    state = awaitItem()
                } while (state.isLoadingMoreLogs || state.logs.size < 2)

                assertThat(state.logs).hasSize(2)
                assertThat(state.hasMoreLogs).isFalse()
                assertThat(apiService.lastLogsBeforeEventId).isEqualTo("evt_1")

                state.eventSink(DeviceDiagnosticsScreen.Event.LogLevelSelected(DeviceDiagnosticsScreen.LogLevelFilter.ERROR))
                do {
                    state = awaitItem()
                } while (
                    state.selectedLogLevel != DeviceDiagnosticsScreen.LogLevelFilter.ERROR ||
                    state.isLogsLoading ||
                    apiService.lastLogsLevel != "error"
                )

                assertThat(apiService.lastLogsLevel).isEqualTo("error")
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `presenter surfaces network error on forecast failure and pops on back`() =
        runTest {
            val screen = DeviceDiagnosticsScreen(deviceId = 42, deviceName = "Office TRMNL")
            val navigator = FakeNavigator(screen)
            val apiService =
                FakeTrmnlApiService().apply {
                    getDeviceForecastResult = ApiResult.networkFailure(IOException("Offline"))
                }
            val prefsRepo = FakeUserPreferencesRepository(UserPreferences(apiToken = "user_test"))
            val presenter = DeviceDiagnosticsPresenter(screen, navigator, apiService, prefsRepo)

            presenter.test {
                var state: DeviceDiagnosticsScreen.State
                do {
                    state = awaitItem()
                } while (state.isForecastLoading)

                assertThat(state.forecastError).isNotNull()
                assertThat(state.forecastSteps).hasSize(0)

                state.eventSink(DeviceDiagnosticsScreen.Event.BackClicked)
                navigator.awaitPop()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `presenter reports missing token when apiToken is null`() =
        runTest {
            val screen = DeviceDiagnosticsScreen(deviceId = 42, deviceName = "Office TRMNL")
            val navigator = FakeNavigator(screen)
            val apiService = FakeTrmnlApiService()
            val prefsRepo = FakeUserPreferencesRepository(UserPreferences(apiToken = null))
            val presenter = DeviceDiagnosticsPresenter(screen, navigator, apiService, prefsRepo)

            presenter.test {
                var state: DeviceDiagnosticsScreen.State
                do {
                    state = awaitItem()
                } while (state.isForecastLoading)

                assertThat(state.forecastError).isEqualTo(
                    DeviceDiagnosticsScreen.DiagnosticsError.General("Missing API token. Please sign in again."),
                )
                assertThat(state.logsError).isNull()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `presenter maps HTTP 403 legacy key error to InsufficientKeyCapabilities and navigates to AccessTokenScreen`() =
        runTest {
            val screen = DeviceDiagnosticsScreen(deviceId = 42, deviceName = "Office TRMNL")
            val navigator = FakeNavigator(screen)
            val legacyKeyMessage =
                "This is a legacy API key, which reaches only the endpoints it had before API keys held capabilities. " +
                    "Create an API key with the capabilities you need at https://trmnl.com/account."
            val apiService =
                FakeTrmnlApiService().apply {
                    getDeviceForecastResult = ApiResult.success(DeviceForecastResponse(DeviceForecast()))
                    getDeviceTimelineResult = ApiResult.apiFailure(ApiError(legacyKeyMessage))
                }
            val prefsRepo = FakeUserPreferencesRepository(UserPreferences(apiToken = "user_legacy"))
            val presenter = DeviceDiagnosticsPresenter(screen, navigator, apiService, prefsRepo)

            presenter.test {
                var state: DeviceDiagnosticsScreen.State
                do {
                    state = awaitItem()
                } while (state.isForecastLoading)

                state.eventSink(DeviceDiagnosticsScreen.Event.TabSelected(DeviceDiagnosticsScreen.DiagnosticsTab.TIMELINE))
                do {
                    state = awaitItem()
                } while (state.selectedTab != DeviceDiagnosticsScreen.DiagnosticsTab.TIMELINE || state.isTimelineLoading ||
                    state.timelineError == null
                )

                assertThat(state.timelineError).isInstanceOf<DeviceDiagnosticsScreen.DiagnosticsError.InsufficientKeyCapabilities>()
                val capError = state.timelineError as DeviceDiagnosticsScreen.DiagnosticsError.InsufficientKeyCapabilities
                assertThat(capError.message).isEqualTo(legacyKeyMessage)
                assertThat(capError.accountUrl).isEqualTo(DeviceDiagnosticsScreen.TRMNL_ACCOUNT_URL)

                state.eventSink(DeviceDiagnosticsScreen.Event.UpdateApiKeyClicked)
                assertThat(navigator.awaitNextScreen()).isEqualTo(AccessTokenScreen)
                cancelAndIgnoreRemainingEvents()
            }
        }
}
