package ink.trmnl.android.buddy.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class WidgetPinHelperTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `isPinSupported returns false when launcher does not support pinning`() {
        assertThat(WidgetPinHelper.isPinSupported(context)).isFalse()
    }

    @Test
    fun `requestPinWidget returns false when launcher does not support pinning`() =
        runTest {
            val result =
                WidgetPinHelper.requestPinWidget(
                    context = context,
                    deviceFriendlyId = "ABC123",
                    deviceName = "Living Room",
                )
            assertThat(result).isFalse()
        }

    @Test
    fun `WidgetPinReceiver ignores intent without valid appWidgetId`() {
        val receiver = WidgetPinReceiver()
        val intent =
            Intent(context, WidgetPinReceiver::class.java).apply {
                putExtra(WidgetPinReceiver.EXTRA_DEVICE_FRIENDLY_ID, "ABC123")
            }

        // Should not throw when appWidgetId is missing
        receiver.onReceive(context, intent)
    }

    @Test
    fun `WidgetPinReceiver ignores intent without deviceFriendlyId`() {
        val receiver = WidgetPinReceiver()
        val intent =
            Intent(context, WidgetPinReceiver::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 42)
            }

        // Should not throw when deviceFriendlyId is missing
        receiver.onReceive(context, intent)
    }

    @Test
    fun `hasWidgetForDevice returns false when no widgets are pinned`() =
        runTest {
            assertThat(WidgetPinHelper.hasWidgetForDevice(context, "ABC123")).isFalse()
        }

    @Test
    fun `notifyDevicePinned emits deviceFriendlyId on pinnedDeviceEvents`() =
        runTest {
            WidgetPinHelper.pinnedDeviceEvents.test {
                WidgetPinHelper.notifyDevicePinned("ABC123")
                assertThat(awaitItem()).isEqualTo("ABC123")
                cancelAndIgnoreRemainingEvents()
            }
        }
}
