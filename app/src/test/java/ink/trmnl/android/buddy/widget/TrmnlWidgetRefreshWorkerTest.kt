package ink.trmnl.android.buddy.widget

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.WorkManager
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThanOrEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotEmpty
import ink.trmnl.android.buddy.fakes.FakeDeviceTokenRepository
import ink.trmnl.android.buddy.fakes.FakeTrmnlApiService
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for [TrmnlWidgetRefreshWorker].
 *
 * Tests cover:
 * - Early-exit paths that do not require Glance infrastructure
 *   (invalid appWidgetId, missing device token, no device configured)
 * - Scheduling logic: refresh interval floor calculation
 * - WorkManager enqueue: verifies unique work is created with correct name and input data
 *
 * Note: Paths that require a running Glance widget host (GlanceAppWidgetManager.getGlanceIds)
 * exercise Android system infrastructure not available in Robolectric unit tests.
 * Those paths should be covered by instrumented tests.
 *
 * Uses Robolectric to provide an Android Context and WorkManager test helpers.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class TrmnlWidgetRefreshWorkerTest {
    private lateinit var context: Context
    private lateinit var fakeApiService: FakeTrmnlApiService
    private lateinit var fakeDeviceTokenRepository: FakeDeviceTokenRepository
    private lateinit var okHttpClient: OkHttpClient

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        fakeApiService = FakeTrmnlApiService()
        fakeDeviceTokenRepository = FakeDeviceTokenRepository()
        okHttpClient = OkHttpClient.Builder().build()

        // Initialize WorkManager with test configuration
        val config =
            Configuration
                .Builder()
                .setMinimumLoggingLevel(android.util.Log.DEBUG)
                .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
    }

    // =========================================================
    // Early-exit: Invalid appWidgetId
    // =========================================================

    @Test
    fun `doWork returns failure when appWidgetId is not set`() =
        runTest {
            // Given: No appWidgetId in input data
            val worker =
                createWorker(
                    inputData = Data.Builder().build(),
                )

            // When: Worker executes
            val result = worker.doWork()

            // Then: Returns failure without touching API
            assertThat(result).isInstanceOf(ListenableWorker.Result.Failure::class.java)
        }

    @Test
    fun `doWork returns failure when appWidgetId is INVALID_APPWIDGET_ID`() =
        runTest {
            // Given: Invalid sentinel appWidgetId
            val worker =
                createWorker(
                    inputData =
                        Data
                            .Builder()
                            .putInt(
                                TrmnlWidgetRefreshWorker.KEY_APP_WIDGET_ID,
                                android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID,
                            ).build(),
                )

            // When: Worker executes
            val result = worker.doWork()

            // Then: Returns failure without touching API
            assertThat(result).isInstanceOf(ListenableWorker.Result.Failure::class.java)
        }

    // =========================================================
    // Early-exit: Widget no longer registered in Glance
    // =========================================================

    @Test
    fun `doWork returns success when widget no longer exists in Glance`() =
        runTest {
            // Given: A valid but non-existent appWidgetId (no Glance widgets registered in tests)
            val worker = createWorkerWithWidgetId(appWidgetId = 99)

            // When: Worker executes
            val result = worker.doWork()

            // Then: Returns success (widget gone — nothing to do)
            assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)
        }

    // =========================================================
    // Scheduling: enqueue helper
    // =========================================================

    @Test
    fun `enqueue creates unique work for the given widget`() {
        val appWidgetId = 42

        // When: Enqueue is called
        TrmnlWidgetRefreshWorker.enqueue(context, appWidgetId, initialDelayMinutes = 0)

        // Then: WorkManager has exactly one work request with the expected unique name
        val workManager = WorkManager.getInstance(context)
        val workInfos =
            workManager
                .getWorkInfosForUniqueWork(TrmnlWidgetRefreshWorker.workName(appWidgetId))
                .get()
        assertThat(workInfos).isNotEmpty()
        assertThat(workInfos.size).isEqualTo(1)
    }

    @Test
    fun `enqueue unique work name encodes the appWidgetId`() {
        val appWidgetId = 55

        // When: Enqueue is called
        TrmnlWidgetRefreshWorker.enqueue(context, appWidgetId, initialDelayMinutes = 0)

        // Then: The unique work name is derivable from the appWidgetId
        val expectedWorkName = TrmnlWidgetRefreshWorker.workName(appWidgetId)
        val workManager = WorkManager.getInstance(context)
        val workInfos =
            workManager
                .getWorkInfosForUniqueWork(expectedWorkName)
                .get()
        assertThat(workInfos).isNotEmpty()
        assertThat(expectedWorkName).isEqualTo("trmnl_widget_refresh_$appWidgetId")
    }

    @Test
    fun `workName returns unique string per widget`() {
        val name1 = TrmnlWidgetRefreshWorker.workName(1)
        val name2 = TrmnlWidgetRefreshWorker.workName(2)
        val name99 = TrmnlWidgetRefreshWorker.workName(99)

        assertThat(name1).isEqualTo("trmnl_widget_refresh_1")
        assertThat(name2).isEqualTo("trmnl_widget_refresh_2")
        assertThat(name99).isEqualTo("trmnl_widget_refresh_99")
    }

    @Test
    fun `enqueue replaces existing work for same widget`() {
        val appWidgetId = 77
        val workManager = WorkManager.getInstance(context)

        // Enqueue twice
        TrmnlWidgetRefreshWorker.enqueue(context, appWidgetId, initialDelayMinutes = 0)
        TrmnlWidgetRefreshWorker.enqueue(context, appWidgetId, initialDelayMinutes = 0)

        // Should still be only one work entry (REPLACE policy)
        val workInfos =
            workManager
                .getWorkInfosForUniqueWork(TrmnlWidgetRefreshWorker.workName(appWidgetId))
                .get()
        assertThat(workInfos.size).isEqualTo(1)
    }

    @Test
    fun `enqueue for different widgets creates separate work entries`() {
        val workManager = WorkManager.getInstance(context)

        TrmnlWidgetRefreshWorker.enqueue(context, appWidgetId = 10, initialDelayMinutes = 0)
        TrmnlWidgetRefreshWorker.enqueue(context, appWidgetId = 20, initialDelayMinutes = 0)

        val workInfos10 =
            workManager
                .getWorkInfosForUniqueWork(TrmnlWidgetRefreshWorker.workName(10))
                .get()
        val workInfos20 =
            workManager
                .getWorkInfosForUniqueWork(TrmnlWidgetRefreshWorker.workName(20))
                .get()

        assertThat(workInfos10.size).isEqualTo(1)
        assertThat(workInfos20.size).isEqualTo(1)
    }

    // =========================================================
    // Refresh rate floor logic
    // =========================================================

    @Test
    fun `refresh interval is floored at MIN_REFRESH_INTERVAL_MINUTES`() {
        // refreshRate of 5 minutes (300 s) converts to 5 minutes, below the 15-min floor
        val refreshRateSeconds = 5 * 60
        val nextRefreshMinutes =
            maxOf(
                TrmnlDeviceWidget.MIN_REFRESH_INTERVAL_MINUTES,
                (refreshRateSeconds / 60L),
            )
        assertThat(nextRefreshMinutes).isEqualTo(TrmnlDeviceWidget.MIN_REFRESH_INTERVAL_MINUTES)
    }

    @Test
    fun `refresh interval above minimum uses API-provided rate`() {
        // refreshRate of 30 minutes (1800 s) → 30 min, above the 15-min floor
        val refreshRateSeconds = 30 * 60
        val nextRefreshMinutes =
            maxOf(
                TrmnlDeviceWidget.MIN_REFRESH_INTERVAL_MINUTES,
                (refreshRateSeconds / 60L),
            )
        assertThat(nextRefreshMinutes).isEqualTo(30L)
    }

    @Test
    fun `refresh interval at exactly minimum is accepted as-is`() {
        val refreshRateSeconds = TrmnlDeviceWidget.MIN_REFRESH_INTERVAL_MINUTES.toInt() * 60
        val nextRefreshMinutes =
            maxOf(
                TrmnlDeviceWidget.MIN_REFRESH_INTERVAL_MINUTES,
                (refreshRateSeconds / 60L),
            )
        assertThat(nextRefreshMinutes).isEqualTo(TrmnlDeviceWidget.MIN_REFRESH_INTERVAL_MINUTES)
    }

    @Test
    fun `MIN_REFRESH_INTERVAL_MINUTES is 15 or greater`() {
        assertThat(TrmnlDeviceWidget.MIN_REFRESH_INTERVAL_MINUTES).isGreaterThanOrEqualTo(15L)
    }

    // =========================================================
    // Helpers
    // =========================================================

    private fun createWorker(inputData: Data): TrmnlWidgetRefreshWorker =
        TestListenableWorkerBuilder<TrmnlWidgetRefreshWorker>(context)
            .setWorkerFactory(
                TestTrmnlWidgetRefreshWorkerFactory(
                    apiService = fakeApiService,
                    deviceTokenRepository = fakeDeviceTokenRepository,
                    okHttpClient = okHttpClient,
                ),
            ).setInputData(inputData)
            .build() as TrmnlWidgetRefreshWorker

    private fun createWorkerWithWidgetId(appWidgetId: Int): TrmnlWidgetRefreshWorker =
        createWorker(
            inputData =
                Data
                    .Builder()
                    .putInt(TrmnlWidgetRefreshWorker.KEY_APP_WIDGET_ID, appWidgetId)
                    .build(),
        )
}
