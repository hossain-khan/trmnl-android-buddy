package ink.trmnl.android.buddy.ui.calendarsync

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import ink.trmnl.android.buddy.calendar.models.SyncCalendar
import ink.trmnl.android.buddy.fakes.FakeCalendarSyncRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Unit tests for [CalendarSyncPresenter].
 *
 * Tests cover:
 * - Initial state composition (sync enabled, last sync time, error states)
 * - Calendar loading via [CalendarSyncScreen.Event.RefreshCalendarsClicked]
 * - Calendar selection toggle updates
 * - Sync enabled auto-management based on calendar selection
 * - Sync Now operation (success and error cases)
 * - Disconnect flow with confirmation
 * - Back navigation
 * - Error handling when calendar loading fails
 *
 * All assertions use AssertK (no JUnit assertions).
 */
class CalendarSyncScreenTest {
    @Test
    fun `presenter loads initial sync state from repository`() =
        runTest {
            // Given
            val repository =
                FakeCalendarSyncRepository(
                    initialSyncEnabled = true,
                    initialLastSyncTime = 1_000_000L,
                    initialLastSyncError = null,
                )
            val screen = CalendarSyncScreen
            val navigator = FakeNavigator(screen)
            val presenter = CalendarSyncPresenter(navigator, repository)

            // When/Then
            presenter.test {
                // Wait for LaunchedEffect to run
                var state = awaitItem()
                // Poll until isSyncEnabled is updated by LaunchedEffect
                while (!state.isSyncEnabled) {
                    state = awaitItem()
                }

                assertThat(state.isSyncEnabled).isTrue()
                assertThat(state.lastSyncTime).isEqualTo(1_000_000L)
                assertThat(state.lastSyncError).isNull()
                assertThat(state.syncStatus).isEqualTo(CalendarSyncScreen.SyncStatus.Idle)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `presenter sets sync status to error when initial sync error exists`() =
        runTest {
            // Given
            val repository =
                FakeCalendarSyncRepository(
                    initialLastSyncError = "Connection timeout",
                )
            val screen = CalendarSyncScreen
            val navigator = FakeNavigator(screen)
            val presenter = CalendarSyncPresenter(navigator, repository)

            // When/Then
            presenter.test {
                var state = awaitItem()
                // Wait for LaunchedEffect to run
                while (state.syncStatus != CalendarSyncScreen.SyncStatus.Error) {
                    state = awaitItem()
                }

                assertThat(state.syncStatus).isEqualTo(CalendarSyncScreen.SyncStatus.Error)
                assertThat(state.lastSyncError).isEqualTo("Connection timeout")

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `presenter loads calendars when RefreshCalendarsClicked event is sent`() =
        runTest {
            // Given
            val calendars =
                listOf(
                    SyncCalendar(
                        calendarId = 1L,
                        calendarName = "Personal",
                        accountName = "user@gmail.com",
                        accountType = "com.google",
                        ownerAccount = "user@gmail.com",
                    ),
                    SyncCalendar(
                        calendarId = 2L,
                        calendarName = "Work",
                        accountName = "user@company.com",
                        accountType = "com.microsoft.exchange",
                        ownerAccount = "user@company.com",
                    ),
                )
            val repository = FakeCalendarSyncRepository(initialCalendars = calendars)
            val screen = CalendarSyncScreen
            val navigator = FakeNavigator(screen)
            val presenter = CalendarSyncPresenter(navigator, repository)

            // When/Then
            presenter.test {
                val initialState = awaitItem()
                assertThat(initialState.calendars).isEmpty()

                // Trigger calendar load
                initialState.eventSink(CalendarSyncScreen.Event.RefreshCalendarsClicked)

                // Wait for loading to complete
                var state = awaitItem()
                while (state.isLoading) {
                    state = awaitItem()
                }

                assertThat(state.calendars.size).isEqualTo(2)
                assertThat(state.calendars[0].calendarName).isEqualTo("Personal")
                assertThat(state.calendars[1].calendarName).isEqualTo("Work")
                assertThat(repository.getAvailableCalendarsCallCount).isEqualTo(1)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `presenter handles calendar load error gracefully`() =
        runTest {
            // Given
            val repository = FakeCalendarSyncRepository(shouldThrowOnGetCalendars = true)
            val screen = CalendarSyncScreen
            val navigator = FakeNavigator(screen)
            val presenter = CalendarSyncPresenter(navigator, repository)

            // When/Then
            presenter.test {
                val initialState = awaitItem()

                // Trigger calendar load that will fail
                initialState.eventSink(CalendarSyncScreen.Event.RefreshCalendarsClicked)

                // Wait for loading to complete
                var state = awaitItem()
                while (state.isLoading) {
                    state = awaitItem()
                }

                assertThat(state.isLoading).isFalse()
                assertThat(state.calendars).isEmpty()
                assertThat(state.errorMessage).isNotNull()

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `presenter toggles calendar selection and updates repository`() =
        runTest {
            // Given
            val calendars =
                listOf(
                    SyncCalendar(
                        calendarId = 1L,
                        calendarName = "Personal",
                        accountName = "user@gmail.com",
                        accountType = "com.google",
                        ownerAccount = "user@gmail.com",
                        isSelected = false,
                    ),
                )
            val repository = FakeCalendarSyncRepository(initialCalendars = calendars)
            val screen = CalendarSyncScreen
            val navigator = FakeNavigator(screen)
            val presenter = CalendarSyncPresenter(navigator, repository)

            // When/Then
            presenter.test {
                val initialState = awaitItem()

                // Load calendars first
                initialState.eventSink(CalendarSyncScreen.Event.RefreshCalendarsClicked)
                var state = awaitItem()
                while (state.isLoading) {
                    state = awaitItem()
                }

                // Toggle calendar selection
                state.eventSink(
                    CalendarSyncScreen.Event.CalendarToggled(
                        calendarId = 1L,
                        selected = true,
                    ),
                )

                // Wait for state to update
                state = awaitItem()
                while (!state.isSyncEnabled) {
                    state = awaitItem()
                }

                assertThat(state.calendars[0].isSelected).isTrue()
                assertThat(state.isSyncEnabled).isTrue()
                assertThat(repository.updateCalendarSelectionCallCount).isEqualTo(1)
                assertThat(repository.lastUpdatedCalendarIds).isEqualTo(listOf(1L))
                assertThat(repository.setSyncEnabledCallCount).isEqualTo(1)
                assertThat(repository.lastSyncEnabledValue).isEqualTo(true)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `presenter disables sync when all calendars are deselected`() =
        runTest {
            // Given
            val calendars =
                listOf(
                    SyncCalendar(
                        calendarId = 1L,
                        calendarName = "Personal",
                        accountName = "user@gmail.com",
                        accountType = "com.google",
                        ownerAccount = "user@gmail.com",
                        isSelected = true,
                    ),
                )
            val repository =
                FakeCalendarSyncRepository(
                    initialCalendars = calendars,
                    initialSyncEnabled = true,
                )
            val screen = CalendarSyncScreen
            val navigator = FakeNavigator(screen)
            val presenter = CalendarSyncPresenter(navigator, repository)

            // When/Then
            presenter.test {
                val initialState = awaitItem()

                // Load calendars
                initialState.eventSink(CalendarSyncScreen.Event.RefreshCalendarsClicked)
                var state = awaitItem()
                while (state.isLoading) {
                    state = awaitItem()
                }

                // Deselect the calendar
                state.eventSink(
                    CalendarSyncScreen.Event.CalendarToggled(
                        calendarId = 1L,
                        selected = false,
                    ),
                )

                // Wait for state to update - wait until isSyncEnabled becomes false
                state = awaitItem()
                while (state.isSyncEnabled) {
                    state = awaitItem()
                }

                assertThat(state.isSyncEnabled).isFalse()
                assertThat(repository.lastSyncEnabledValue).isEqualTo(false)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `presenter handles SyncNow success`() =
        runTest {
            // Given
            val repository =
                FakeCalendarSyncRepository(
                    initialSyncEnabled = true,
                )
            val screen = CalendarSyncScreen
            val navigator = FakeNavigator(screen)
            val presenter = CalendarSyncPresenter(navigator, repository)

            // When/Then
            presenter.test {
                // Wait for initial state
                var state = awaitItem()
                while (!state.isSyncEnabled) {
                    state = awaitItem()
                }

                // Trigger sync
                state.eventSink(CalendarSyncScreen.Event.SyncNowClicked)

                // Wait for sync to complete
                state = awaitItem()
                while (state.syncStatus == CalendarSyncScreen.SyncStatus.Syncing) {
                    state = awaitItem()
                }

                assertThat(state.syncStatus).isEqualTo(CalendarSyncScreen.SyncStatus.Idle)
                assertThat(state.lastSyncError).isNull()
                assertThat(repository.recordSyncSuccessCallCount).isEqualTo(1)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `presenter handles SyncNow failure and records error`() =
        runTest {
            // Given
            val repository =
                FakeCalendarSyncRepository(
                    initialSyncEnabled = true,
                    shouldThrowOnGetEvents = true,
                )
            val screen = CalendarSyncScreen
            val navigator = FakeNavigator(screen)
            val presenter = CalendarSyncPresenter(navigator, repository)

            // When/Then
            presenter.test {
                var state = awaitItem()
                while (!state.isSyncEnabled) {
                    state = awaitItem()
                }

                // Trigger sync that will fail
                state.eventSink(CalendarSyncScreen.Event.SyncNowClicked)

                // Wait for sync to complete (error state)
                state = awaitItem()
                while (state.syncStatus == CalendarSyncScreen.SyncStatus.Syncing) {
                    state = awaitItem()
                }

                assertThat(state.syncStatus).isEqualTo(CalendarSyncScreen.SyncStatus.Error)
                assertThat(state.errorMessage).isNotNull()
                assertThat(repository.recordSyncErrorCallCount).isEqualTo(1)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `presenter shows disconnect dialog when DisconnectClicked`() =
        runTest {
            // Given
            val repository = FakeCalendarSyncRepository()
            val screen = CalendarSyncScreen
            val navigator = FakeNavigator(screen)
            val presenter = CalendarSyncPresenter(navigator, repository)

            // When/Then
            presenter.test {
                val state = awaitItem()

                assertThat(state.showDisconnectConfirmDialog).isFalse()

                state.eventSink(CalendarSyncScreen.Event.DisconnectClicked)

                val updatedState = awaitItem()
                assertThat(updatedState.showDisconnectConfirmDialog).isTrue()

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `presenter dismisses disconnect dialog when DisconnectDismissed`() =
        runTest {
            // Given
            val repository = FakeCalendarSyncRepository()
            val screen = CalendarSyncScreen
            val navigator = FakeNavigator(screen)
            val presenter = CalendarSyncPresenter(navigator, repository)

            // When/Then
            presenter.test {
                val state = awaitItem()

                // Open dialog
                state.eventSink(CalendarSyncScreen.Event.DisconnectClicked)
                var updatedState = awaitItem()
                assertThat(updatedState.showDisconnectConfirmDialog).isTrue()

                // Dismiss dialog
                updatedState.eventSink(CalendarSyncScreen.Event.DisconnectDismissed)
                updatedState = awaitItem()
                assertThat(updatedState.showDisconnectConfirmDialog).isFalse()

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `presenter disconnects and navigates back when DisconnectConfirmed`() =
        runTest {
            // Given
            val repository =
                FakeCalendarSyncRepository(
                    initialSyncEnabled = true,
                    initialLastSyncTime = 1_000_000L,
                )
            val screen = CalendarSyncScreen
            val navigator = FakeNavigator(screen)
            val presenter = CalendarSyncPresenter(navigator, repository)

            // When/Then
            presenter.test {
                val state = awaitItem()

                // Open dialog
                state.eventSink(CalendarSyncScreen.Event.DisconnectClicked)
                val dialogState = awaitItem()

                // Confirm disconnect
                dialogState.eventSink(CalendarSyncScreen.Event.DisconnectConfirmed)

                // Verify navigation back
                assertThat(navigator.awaitPop()).isNotNull()
                assertThat(repository.wasDisconnected).isTrue()

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `presenter handles back button navigation`() =
        runTest {
            // Given
            val repository = FakeCalendarSyncRepository()
            val screen = CalendarSyncScreen
            val navigator = FakeNavigator(screen)
            val presenter = CalendarSyncPresenter(navigator, repository)

            // When/Then
            presenter.test {
                val state = awaitItem()

                state.eventSink(CalendarSyncScreen.Event.BackClicked)

                assertThat(navigator.awaitPop()).isNotNull()

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `presenter initial state has correct defaults`() =
        runTest {
            // Given
            val repository = FakeCalendarSyncRepository()
            val screen = CalendarSyncScreen
            val navigator = FakeNavigator(screen)
            val presenter = CalendarSyncPresenter(navigator, repository)

            // When/Then
            presenter.test {
                val state = awaitItem()

                assertThat(state.isLoading).isFalse()
                assertThat(state.calendars).isEmpty()
                assertThat(state.showDisconnectConfirmDialog).isFalse()
                assertThat(state.errorMessage).isNull()
                assertThat(state.syncStatus).isEqualTo(CalendarSyncScreen.SyncStatus.Idle)

                cancelAndIgnoreRemainingEvents()
            }
        }
}
