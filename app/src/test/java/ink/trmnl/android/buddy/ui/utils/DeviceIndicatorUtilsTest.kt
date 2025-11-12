package ink.trmnl.android.buddy.ui.utils

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import org.junit.Test

/**
 * Unit tests for DeviceIndicatorUtils battery alert logic.
 */
class DeviceIndicatorUtilsTest {
    @Test
    fun `isLowBatteryAlert returns true when notifications enabled and battery below threshold`() {
        val result =
            isLowBatteryAlert(
                percentCharged = 15.0,
                thresholdPercent = 20,
                isNotificationEnabled = true,
            )

        assertThat(result).isTrue()
    }

    @Test
    fun `isLowBatteryAlert returns false when notifications disabled`() {
        val result =
            isLowBatteryAlert(
                percentCharged = 15.0,
                thresholdPercent = 20,
                isNotificationEnabled = false,
            )

        assertThat(result).isFalse()
    }

    @Test
    fun `isLowBatteryAlert returns false when battery above threshold`() {
        val result =
            isLowBatteryAlert(
                percentCharged = 85.0,
                thresholdPercent = 20,
                isNotificationEnabled = true,
            )

        assertThat(result).isFalse()
    }

    @Test
    fun `isLowBatteryAlert returns false when battery exactly at threshold`() {
        val result =
            isLowBatteryAlert(
                percentCharged = 20.0,
                thresholdPercent = 20,
                isNotificationEnabled = true,
            )

        assertThat(result).isFalse()
    }

    @Test
    fun `isLowBatteryAlert returns true when battery just below threshold`() {
        val result =
            isLowBatteryAlert(
                percentCharged = 19.9,
                thresholdPercent = 20,
                isNotificationEnabled = true,
            )

        assertThat(result).isTrue()
    }

    @Test
    fun `isLowBatteryAlert handles high threshold values`() {
        val result =
            isLowBatteryAlert(
                percentCharged = 45.0,
                thresholdPercent = 50,
                isNotificationEnabled = true,
            )

        assertThat(result).isTrue()
    }

    @Test
    fun `isLowBatteryAlert handles low threshold values`() {
        val result =
            isLowBatteryAlert(
                percentCharged = 3.0,
                thresholdPercent = 5,
                isNotificationEnabled = true,
            )

        assertThat(result).isTrue()
    }

    @Test
    fun `isLowBatteryAlert returns false for zero battery with notifications disabled`() {
        val result =
            isLowBatteryAlert(
                percentCharged = 0.0,
                thresholdPercent = 20,
                isNotificationEnabled = false,
            )

        assertThat(result).isFalse()
    }
}
