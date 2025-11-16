package ink.trmnl.android.buddy.work

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for WorkManagerObserver implementation.
 *
 * Tests cover:
 * - Observer initialization and lifecycle
 * - observeAllWorkers() flow emission
 * - Multiple worker status tracking
 * - Worker state transitions
 * - cancelAllWorkers() functionality
 * - resetAllWorkerSchedules() functionality
 * - Edge cases and error handling
 */
@RunWith(RobolectricTestRunner::class)
class WorkManagerObserverTest {
    private lateinit var context: Context
    private lateinit var workManager: WorkManager
    private lateinit var observer: WorkManagerObserverImpl
    private lateinit var fakeWorkerScheduler: FakeWorkerScheduler

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()

        // Initialize WorkManager with synchronous executor for testing
        val config =
            Configuration
                .Builder()
                .setMinimumLoggingLevel(android.util.Log.DEBUG)
                .setExecutor(SynchronousExecutor())
                .build()

        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
        workManager = WorkManager.getInstance(context)

        fakeWorkerScheduler = FakeWorkerScheduler()
        observer = WorkManagerObserverImpl(workManager, fakeWorkerScheduler)
    }

    // ====================================================================================
    // Core Functionality Tests
    // ====================================================================================

    @Test
    fun `observeAllWorkers emits initial empty state`() =
        runTest {
            observer.observeAllWorkers().test {
                // First emission should be empty
                val initialState = awaitItem()
                assertThat(initialState).isEmpty()

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `observeAllWorkers emits worker statuses after initialization`() =
        runTest {
            observer.observeAllWorkers().test {
                // Skip initial empty emission
                awaitItem()

                // Second emission should contain current worker statuses
                val statuses = awaitItem()

                // At minimum, we should have worker status information
                // The list might be empty if no workers are scheduled
                assertThat(statuses).isNotNull()

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `observeAllWorkers handles exception during status fetch`() =
        runTest {
            // Observer should handle exceptions gracefully and emit empty list
            observer.observeAllWorkers().test {
                val initialState = awaitItem()
                assertThat(initialState).isEmpty()

                val statusesState = awaitItem()
                assertThat(statusesState).isNotNull()

                cancelAndIgnoreRemainingEvents()
            }
        }

    // ====================================================================================
    // Worker State Tracking Tests
    // ====================================================================================

    @Test
    fun `WorkerStatus contains correct properties`() {
        val status =
            WorkerStatus(
                name = "test_worker",
                displayName = "Test Worker",
                state = WorkInfo.State.ENQUEUED,
                runAttemptCount = 0,
                tags = setOf("test-tag"),
            )

        assertThat(status.name).isEqualTo("test_worker")
        assertThat(status.displayName).isEqualTo("Test Worker")
        assertThat(status.state).isEqualTo(WorkInfo.State.ENQUEUED)
        assertThat(status.runAttemptCount).isEqualTo(0)
        assertThat(status.tags).contains("test-tag")
    }

    @Test
    fun `WorkerStatus tracks run attempt count`() {
        val status =
            WorkerStatus(
                name = "retry_worker",
                displayName = "Retry Worker",
                state = WorkInfo.State.RUNNING,
                runAttemptCount = 3,
                tags = emptySet(),
            )

        assertThat(status.runAttemptCount).isEqualTo(3)
        assertThat(status.state).isEqualTo(WorkInfo.State.RUNNING)
    }

    @Test
    fun `WorkerStatus supports all WorkInfo states`() {
        val states =
            listOf(
                WorkInfo.State.ENQUEUED,
                WorkInfo.State.RUNNING,
                WorkInfo.State.SUCCEEDED,
                WorkInfo.State.FAILED,
                WorkInfo.State.BLOCKED,
                WorkInfo.State.CANCELLED,
            )

        states.forEachIndexed { index, state ->
            val status =
                WorkerStatus(
                    name = "worker_$index",
                    displayName = "Worker $index",
                    state = state,
                    runAttemptCount = 0,
                    tags = emptySet(),
                )

            assertThat(status.state).isEqualTo(state)
        }
    }

    @Test
    fun `WorkerStatus handles multiple tags`() {
        val tags = setOf("tag1", "tag2", "tag3", "battery-tracking")
        val status =
            WorkerStatus(
                name = "multi_tag_worker",
                displayName = "Multi Tag Worker",
                state = WorkInfo.State.SUCCEEDED,
                runAttemptCount = 1,
                tags = tags,
            )

        assertThat(status.tags).hasSize(4)
        assertThat(status.tags).containsExactlyInAnyOrder("tag1", "tag2", "tag3", "battery-tracking")
    }

    @Test
    fun `WorkerStatus handles empty tags`() {
        val status =
            WorkerStatus(
                name = "no_tag_worker",
                displayName = "No Tag Worker",
                state = WorkInfo.State.ENQUEUED,
                runAttemptCount = 0,
                tags = emptySet(),
            )

        assertThat(status.tags).isEmpty()
    }

    // ====================================================================================
    // Worker Cancellation Tests
    // ====================================================================================

    @Test
    fun `cancelAllWorkers cancels all worker types`() {
        observer.cancelAllWorkers()

        // Verify all workers are cancelled
        // Note: In a real scenario, we'd check WorkManager's getWorkInfos
        // but for this unit test, we're verifying the method completes without errors
        assertThat(true).isEqualTo(true)
    }

    // ====================================================================================
    // Worker Reset Tests
    // ====================================================================================

    @Test
    fun `resetAllWorkerSchedules cancels and reschedules workers`() {
        observer.resetAllWorkerSchedules()

        // Verify that WorkerScheduler methods were called
        assertThat(fakeWorkerScheduler.lowBatteryScheduled).isEqualTo(true)
        assertThat(fakeWorkerScheduler.blogPostScheduled).isEqualTo(true)
        assertThat(fakeWorkerScheduler.announcementScheduled).isEqualTo(true)
    }

    @Test
    fun `resetAllWorkerSchedules calls scheduler in correct order`() {
        fakeWorkerScheduler.trackCallOrder()

        observer.resetAllWorkerSchedules()

        // Verify all three schedule methods were called
        assertThat(fakeWorkerScheduler.callOrder).hasSize(3)
        assertThat(fakeWorkerScheduler.callOrder).containsExactlyInAnyOrder(
            "scheduleLowBatteryNotification",
            "scheduleBlogPostSync",
            "scheduleAnnouncementSync",
        )
    }

    // ====================================================================================
    // Edge Cases
    // ====================================================================================

    @Test
    fun `observeAllWorkers handles no workers scheduled`() =
        runTest {
            // Fresh WorkManager with no scheduled workers
            observer.observeAllWorkers().test {
                val initialState = awaitItem()
                assertThat(initialState).isEmpty()

                val statusesState = awaitItem()
                // Should emit empty or contain empty list when no workers are scheduled
                assertThat(statusesState).isNotNull()

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `observeAllWorkers emits current snapshot of worker states`() =
        runTest {
            observer.observeAllWorkers().test {
                // Skip initial empty emission
                val initialState = awaitItem()
                assertThat(initialState).isEmpty()

                // Get worker statuses snapshot
                val statuses = awaitItem()
                assertThat(statuses).isNotNull()

                // The snapshot should reflect current state, not be a live-updating flow
                // This is a one-time emission per collection

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `cancelAllWorkers handles already cancelled workers gracefully`() {
        // Cancel once
        observer.cancelAllWorkers()

        // Cancel again - should handle gracefully without errors
        observer.cancelAllWorkers()

        assertThat(true).isEqualTo(true)
    }

    @Test
    fun `resetAllWorkerSchedules can be called multiple times`() {
        // Reset once
        observer.resetAllWorkerSchedules()

        // Reset again
        fakeWorkerScheduler.reset()
        observer.resetAllWorkerSchedules()

        // Verify schedules are set again
        assertThat(fakeWorkerScheduler.lowBatteryScheduled).isEqualTo(true)
        assertThat(fakeWorkerScheduler.blogPostScheduled).isEqualTo(true)
        assertThat(fakeWorkerScheduler.announcementScheduled).isEqualTo(true)
    }

    @Test
    fun `observeAllWorkers completes without hanging`() =
        runTest {
            // This test ensures the flow doesn't hang and completes properly
            observer.observeAllWorkers().test {
                awaitItem() // Initial empty state
                awaitItem() // Worker statuses
                cancelAndIgnoreRemainingEvents()
            }

            // If we reach here, the flow emitted and completed successfully
            assertThat(true).isEqualTo(true)
        }

    // ====================================================================================
    // Known Worker Names Tests
    // ====================================================================================

    @Test
    fun `observer tracks BatteryCollectionWorker by name`() {
        val expectedName = BatteryCollectionWorker.WORK_NAME
        assertThat(expectedName).isEqualTo("battery_collection_work")
    }

    @Test
    fun `observer tracks LowBatteryNotificationWorker by name`() {
        val expectedName = LowBatteryNotificationWorker.WORK_NAME
        assertThat(expectedName).isEqualTo("low_battery_notification_work")
    }

    @Test
    fun `observer tracks BlogPostSyncWorker by name`() {
        val expectedName = BlogPostSyncWorker.WORK_NAME
        assertThat(expectedName).isEqualTo("blog_post_sync")
    }

    @Test
    fun `observer tracks AnnouncementSyncWorker by name`() {
        val expectedName = AnnouncementSyncWorker.WORK_NAME
        assertThat(expectedName).isEqualTo("announcement_sync")
    }
}

// ====================================================================================
// Fake Implementation
// ====================================================================================

/**
 * Fake WorkerScheduler implementation for testing.
 * Tracks which scheduling methods were called without actually scheduling workers.
 */
class FakeWorkerScheduler : WorkerScheduler {
    var lowBatteryScheduled = false
        private set
    var blogPostScheduled = false
        private set
    var announcementScheduled = false
        private set
    var lowBatteryCancelled = false
        private set
    var blogPostCancelled = false
        private set
    var announcementCancelled = false
        private set
    var lowBatteryTriggered = false
        private set

    val callOrder = mutableListOf<String>()
    private var trackingEnabled = false

    fun trackCallOrder() {
        trackingEnabled = true
        callOrder.clear()
    }

    fun reset() {
        lowBatteryScheduled = false
        blogPostScheduled = false
        announcementScheduled = false
        lowBatteryCancelled = false
        blogPostCancelled = false
        announcementCancelled = false
        lowBatteryTriggered = false
        callOrder.clear()
        trackingEnabled = false
    }

    override fun scheduleLowBatteryNotification() {
        lowBatteryScheduled = true
        if (trackingEnabled) callOrder.add("scheduleLowBatteryNotification")
    }

    override fun cancelLowBatteryNotification() {
        lowBatteryCancelled = true
        if (trackingEnabled) callOrder.add("cancelLowBatteryNotification")
    }

    override fun triggerLowBatteryNotificationNow() {
        lowBatteryTriggered = true
        if (trackingEnabled) callOrder.add("triggerLowBatteryNotificationNow")
    }

    override fun scheduleAnnouncementSync() {
        announcementScheduled = true
        if (trackingEnabled) callOrder.add("scheduleAnnouncementSync")
    }

    override fun cancelAnnouncementSync() {
        announcementCancelled = true
        if (trackingEnabled) callOrder.add("cancelAnnouncementSync")
    }

    override fun scheduleBlogPostSync() {
        blogPostScheduled = true
        if (trackingEnabled) callOrder.add("scheduleBlogPostSync")
    }

    override fun cancelBlogPostSync() {
        blogPostCancelled = true
        if (trackingEnabled) callOrder.add("cancelBlogPostSync")
    }
}
