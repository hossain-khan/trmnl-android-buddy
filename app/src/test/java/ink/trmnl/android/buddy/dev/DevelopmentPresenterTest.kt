package ink.trmnl.android.buddy.dev

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkInfo
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import ink.trmnl.android.buddy.work.WorkManagerObserver
import ink.trmnl.android.buddy.work.WorkerStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for DevelopmentPresenter.
 *
 * Tests cover:
 * - Initial state loading
 * - Worker status observation
 * - Notification test events
 * - Worker trigger events
 * - Worker management (cancel all, reset schedules)
 * - Navigation events
 *
 * Uses Robolectric to provide Android Context for Compose testing.
 * Uses a test wrapper to provide LocalContext to the Circuit composition.
 */
@RunWith(RobolectricTestRunner::class)
class DevelopmentPresenterTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `presenter loads with initial state`() =
        runTest {
            // Given
            val navigator = FakeNavigator(DevelopmentScreen)
            val workManagerObserver = FakeWorkManagerObserver()
            val presenter = createTestPresenter(navigator, workManagerObserver, context)

            // When/Then
            presenter.test {
                val initialState = awaitItem()

                assertThat(initialState.notificationPermissionGranted).isTrue() // Default on older Android
                assertThat(initialState.workerStatuses).isEmpty()
            }
        }

    @Test
    fun `test low battery notification event is handled`() =
        runTest {
            // Given
            val navigator = FakeNavigator(DevelopmentScreen)
            val workManagerObserver = FakeWorkManagerObserver()
            val presenter = createTestPresenter(navigator, workManagerObserver, context)

            // When/Then
            presenter.test {
                val state = awaitItem()

                // Trigger test notification - event is handled internally
                state.eventSink(DevelopmentScreen.Event.TestLowBatteryNotification(deviceCount = 2, thresholdPercent = 15))

                // State doesn't change for notification test events
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test blog post notification event is handled`() =
        runTest {
            // Given
            val navigator = FakeNavigator(DevelopmentScreen)
            val workManagerObserver = FakeWorkManagerObserver()
            val presenter = createTestPresenter(navigator, workManagerObserver, context)

            // When/Then
            presenter.test {
                val state = awaitItem()

                state.eventSink(DevelopmentScreen.Event.TestBlogPostNotification(postCount = 3))

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test announcement notification event is handled`() =
        runTest {
            // Given
            val navigator = FakeNavigator(DevelopmentScreen)
            val workManagerObserver = FakeWorkManagerObserver()
            val presenter = createTestPresenter(navigator, workManagerObserver, context)

            // When/Then
            presenter.test {
                val state = awaitItem()

                state.eventSink(DevelopmentScreen.Event.TestAnnouncementNotification(announcementCount = 1))

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `trigger low battery worker event is handled`() =
        runTest {
            // Given
            val navigator = FakeNavigator(DevelopmentScreen)
            val workManagerObserver = FakeWorkManagerObserver()
            val presenter = createTestPresenter(navigator, workManagerObserver, context)

            // When/Then
            presenter.test {
                val state = awaitItem()

                state.eventSink(DevelopmentScreen.Event.TriggerLowBatteryWorker)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `trigger blog post worker event is handled`() =
        runTest {
            // Given
            val navigator = FakeNavigator(DevelopmentScreen)
            val workManagerObserver = FakeWorkManagerObserver()
            val presenter = createTestPresenter(navigator, workManagerObserver, context)

            // When/Then
            presenter.test {
                val state = awaitItem()

                state.eventSink(DevelopmentScreen.Event.TriggerBlogPostWorker)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `trigger announcement worker event is handled`() =
        runTest {
            // Given
            val navigator = FakeNavigator(DevelopmentScreen)
            val workManagerObserver = FakeWorkManagerObserver()
            val presenter = createTestPresenter(navigator, workManagerObserver, context)

            // When/Then
            presenter.test {
                val state = awaitItem()

                state.eventSink(DevelopmentScreen.Event.TriggerAnnouncementWorker)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `trigger battery collection worker event is handled`() =
        runTest {
            // Given
            val navigator = FakeNavigator(DevelopmentScreen)
            val workManagerObserver = FakeWorkManagerObserver()
            val presenter = createTestPresenter(navigator, workManagerObserver, context)

            // When/Then
            presenter.test {
                val state = awaitItem()

                state.eventSink(DevelopmentScreen.Event.TriggerBatteryCollectionWorker)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `cancel all workers event calls WorkManagerObserver`() =
        runTest {
            // Given
            val navigator = FakeNavigator(DevelopmentScreen)
            val workManagerObserver = FakeWorkManagerObserver()
            val presenter = createTestPresenter(navigator, workManagerObserver, context)

            // When/Then
            presenter.test {
                val state = awaitItem()

                assertThat(workManagerObserver.cancelAllWorkersCalled).isFalse()

                state.eventSink(DevelopmentScreen.Event.CancelAllWorkers)

                assertThat(workManagerObserver.cancelAllWorkersCalled).isTrue()

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `reset worker schedules event calls WorkManagerObserver`() =
        runTest {
            // Given
            val navigator = FakeNavigator(DevelopmentScreen)
            val workManagerObserver = FakeWorkManagerObserver()
            val presenter = createTestPresenter(navigator, workManagerObserver, context)

            // When/Then
            presenter.test {
                val state = awaitItem()

                assertThat(workManagerObserver.resetWorkerSchedulesCalled).isFalse()

                state.eventSink(DevelopmentScreen.Event.ResetWorkerSchedules)

                assertThat(workManagerObserver.resetWorkerSchedulesCalled).isTrue()

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `request notification permission event is handled`() =
        runTest {
            // Given
            val navigator = FakeNavigator(DevelopmentScreen)
            val workManagerObserver = FakeWorkManagerObserver()
            val presenter = createTestPresenter(navigator, workManagerObserver, context)

            // When/Then
            presenter.test {
                val state = awaitItem()

                state.eventSink(DevelopmentScreen.Event.RequestNotificationPermission)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `navigate back event calls navigator pop`() =
        runTest {
            // Given
            val navigator = FakeNavigator(DevelopmentScreen)
            val workManagerObserver = FakeWorkManagerObserver()
            val presenter = createTestPresenter(navigator, workManagerObserver, context)

            // When/Then
            presenter.test {
                val state = awaitItem()

                state.eventSink(DevelopmentScreen.Event.NavigateBack)

                // Verify navigation occurred
                assertThat(navigator.awaitPop()).isNotNull()

                cancelAndIgnoreRemainingEvents()
            }
        }

    /**
     * Create a test presenter that wraps DevelopmentPresenter with LocalContext provided.
     * This is necessary because Circuit's test framework doesn't automatically provide
     * Android CompositionLocals, and DevelopmentPresenter requires LocalContext.
     */
    private fun createTestPresenter(
        navigator: Navigator,
        workManagerObserver: WorkManagerObserver,
        context: Context,
    ): Presenter<DevelopmentScreen.State> =
        object : Presenter<DevelopmentScreen.State> {
            private val delegate = DevelopmentPresenter(navigator, workManagerObserver)

            @Composable
            override fun present(): DevelopmentScreen.State {
                var result: DevelopmentScreen.State? = null
                CompositionLocalProvider(LocalContext provides context) {
                    result = delegate.present()
                }
                return checkNotNull(result) { "Delegate presenter failed to produce a state" }
            }
        }
}

/**
 * Fake implementation of WorkManagerObserver for testing.
 * Avoids mocking per project guidelines.
 */
private class FakeWorkManagerObserver : WorkManagerObserver {
    private val workerStatusesFlow = MutableStateFlow<List<WorkerStatus>>(emptyList())

    var cancelAllWorkersCalled = false
        private set

    var resetWorkerSchedulesCalled = false
        private set

    override fun observeAllWorkers(): Flow<List<WorkerStatus>> = workerStatusesFlow

    override fun cancelAllWorkers() {
        cancelAllWorkersCalled = true
    }

    override fun resetAllWorkerSchedules() {
        resetWorkerSchedulesCalled = true
    }

    /**
     * Helper method to update worker statuses for testing.
     */
    fun updateWorkerStatuses(statuses: List<WorkerStatus>) {
        workerStatusesFlow.value = statuses
    }
}
