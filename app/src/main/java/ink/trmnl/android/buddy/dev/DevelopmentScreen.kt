package ink.trmnl.android.buddy.dev

import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.Screen
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
 */
@Parcelize
data object DevelopmentScreen : Screen {
    /**
     * UI state for the Development screen.
     */
    data class State(
        val notificationPermissionGranted: Boolean = false,
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

        // System events
        data object RequestNotificationPermission : Event

        data object OpenNotificationSettings : Event

        data object NavigateBack : Event
    }
}
