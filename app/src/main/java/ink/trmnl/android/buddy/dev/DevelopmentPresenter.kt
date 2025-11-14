package ink.trmnl.android.buddy.dev

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.Inject
import ink.trmnl.android.buddy.dev.DevelopmentScreen.Event
import ink.trmnl.android.buddy.notification.NotificationHelper
import ink.trmnl.android.buddy.work.AnnouncementSyncWorker
import ink.trmnl.android.buddy.work.BatteryCollectionWorker
import ink.trmnl.android.buddy.work.BlogPostSyncWorker
import ink.trmnl.android.buddy.work.LowBatteryNotificationWorker
import ink.trmnl.android.buddy.work.WorkManagerObserver
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Presenter for the Development screen.
 *
 * Handles:
 * - Notification permission state
 * - Manual notification triggering with test data
 * - One-time worker execution
 * - Navigation to system settings
 * - WorkManager worker status monitoring
 */
@Inject
class DevelopmentPresenter(
    @Assisted private val navigator: Navigator,
    private val workManagerObserver: WorkManagerObserver,
) : Presenter<DevelopmentScreen.State> {
    @OptIn(ExperimentalPermissionsApi::class)
    @Composable
    override fun present(): DevelopmentScreen.State {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        // Track notification permission state (Android 13+)
        val notificationPermissionGranted =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val permissionState = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
                permissionState.status.isGranted
            } else {
                true // Permission not required on older Android versions
            }

        var permissionRequested by remember { mutableStateOf(false) }

        // Observe worker statuses
        val workerStatuses by workManagerObserver.observeAllWorkers().collectAsState(initial = emptyList())

        return DevelopmentScreen.State(
            notificationPermissionGranted = notificationPermissionGranted,
            workerStatuses = workerStatuses,
        ) { event ->
            when (event) {
                // Notification testing with mock data
                is Event.TestLowBatteryNotification -> {
                    Timber.d("Testing low battery notification: ${event.deviceCount} devices, ${event.thresholdPercent}% threshold")
                    testLowBatteryNotification(context, event.deviceCount, event.thresholdPercent)
                }

                is Event.TestBlogPostNotification -> {
                    Timber.d("Testing blog post notification: ${event.postCount} new posts")
                    testBlogPostNotification(context, event.postCount)
                }

                is Event.TestAnnouncementNotification -> {
                    Timber.d("Testing announcement notification: ${event.announcementCount} new announcements")
                    testAnnouncementNotification(context, event.announcementCount)
                }

                // Worker triggers
                Event.TriggerLowBatteryWorker -> {
                    Timber.d("Triggering one-time LowBatteryNotificationWorker")
                    scope.launch {
                        triggerWorker<LowBatteryNotificationWorker>(context)
                    }
                }

                Event.TriggerBlogPostWorker -> {
                    Timber.d("Triggering one-time BlogPostSyncWorker")
                    scope.launch {
                        triggerWorker<BlogPostSyncWorker>(context)
                    }
                }

                Event.TriggerAnnouncementWorker -> {
                    Timber.d("Triggering one-time AnnouncementSyncWorker")
                    scope.launch {
                        triggerWorker<AnnouncementSyncWorker>(context)
                    }
                }

                Event.TriggerBatteryCollectionWorker -> {
                    Timber.d("Triggering one-time BatteryCollectionWorker")
                    scope.launch {
                        triggerWorker<BatteryCollectionWorker>(context)
                    }
                }

                // Worker management
                Event.CancelAllWorkers -> {
                    Timber.d("Cancelling all workers via DevelopmentPresenter")
                    scope.launch {
                        workManagerObserver.cancelAllWorkers()
                    }
                }

                Event.ResetWorkerSchedules -> {
                    Timber.d("Resetting all worker schedules via DevelopmentPresenter")
                    scope.launch {
                        workManagerObserver.resetAllWorkerSchedules()
                    }
                }

                // System actions
                Event.RequestNotificationPermission -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !permissionRequested) {
                        permissionRequested = true
                        // Permission request is handled by Accompanist in UI
                    }
                }

                Event.OpenNotificationSettings -> {
                    openNotificationSettings(context)
                }

                Event.NavigateBack -> {
                    navigator.pop()
                }
            }
        }
    }

    /**
     * Test low battery notification with mock device data.
     */
    private fun testLowBatteryNotification(
        context: Context,
        deviceCount: Int,
        thresholdPercent: Int,
    ) {
        val deviceNames =
            when (deviceCount) {
                1 -> listOf("Living Room Display")
                2 -> listOf("Living Room Display", "Kitchen Display")
                3 -> listOf("Living Room Display", "Kitchen Display", "Office Display")
                else -> List(deviceCount) { "Test Device ${it + 1}" }
            }

        NotificationHelper.showLowBatteryNotification(
            context = context,
            deviceNames = deviceNames,
            thresholdPercent = thresholdPercent,
        )
    }

    /**
     * Test blog post notification with mock count.
     */
    private fun testBlogPostNotification(
        context: Context,
        postCount: Int,
    ) {
        NotificationHelper.showBlogPostNotification(
            context = context,
            newPostsCount = postCount,
        )
    }

    /**
     * Test announcement notification with mock count.
     */
    private fun testAnnouncementNotification(
        context: Context,
        announcementCount: Int,
    ) {
        NotificationHelper.showAnnouncementNotification(
            context = context,
            newAnnouncementsCount = announcementCount,
        )
    }

    /**
     * Trigger a one-time worker execution.
     */
    private inline fun <reified T : androidx.work.ListenableWorker> triggerWorker(context: Context) {
        val workRequest = OneTimeWorkRequestBuilder<T>().build()
        WorkManager.getInstance(context).enqueue(workRequest)
        Timber.d("Enqueued one-time work request for ${T::class.simpleName}")
    }

    /**
     * Open system notification settings for the app.
     */
    private fun openNotificationSettings(context: Context) {
        val intent =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                }
            } else {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = android.net.Uri.parse("package:${context.packageName}")
                }
            }
        context.startActivity(intent)
    }

    @CircuitInject(DevelopmentScreen::class, AppScope::class)
    @AssistedFactory
    interface Factory {
        fun create(navigator: Navigator): DevelopmentPresenter
    }
}
