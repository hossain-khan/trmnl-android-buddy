package ink.trmnl.android.buddy.util

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test

class FormattingUtilsTest {
    @Test
    fun `formatRefreshRate maps to closest TRMNL option for exact matches`() {
        // Test exact matches to TRMNL options
        assertThat(formatRefreshRate(300)).isEqualTo("5m") // 5 mins
        assertThat(formatRefreshRate(600)).isEqualTo("10m") // 10 mins
        assertThat(formatRefreshRate(900)).isEqualTo("15m") // 15 mins
        assertThat(formatRefreshRate(1800)).isEqualTo("30m") // 30 mins
        assertThat(formatRefreshRate(2700)).isEqualTo("45m") // 45 mins
        assertThat(formatRefreshRate(3600)).isEqualTo("1h") // 60 mins (1 hour)
        assertThat(formatRefreshRate(5400)).isEqualTo("1.5h") // 90 mins (1.5 hours)
        assertThat(formatRefreshRate(7200)).isEqualTo("2h") // 120 mins (2 hours)
        assertThat(formatRefreshRate(14400)).isEqualTo("4h") // 240 mins (4 hours)
        assertThat(formatRefreshRate(86400)).isEqualTo("24h") // 1440 mins (24 hours)
    }

    @Test
    fun `formatRefreshRate maps to closest TRMNL option for near matches`() {
        // Test values that should map to closest option
        assertThat(formatRefreshRate(912)).isEqualTo("15m") // 15.2 mins → closest to 15m
        assertThat(formatRefreshRate(6812)).isEqualTo("2h") // 113.5 mins → closest to 120m (2h)
        assertThat(formatRefreshRate(1500)).isEqualTo("30m") // 25 mins → closest to 30m
        assertThat(formatRefreshRate(4000)).isEqualTo("1h") // 66.7 mins → closest to 60m (1h)
    }

    @Test
    fun `formatRefreshRate maps very small values to 5m`() {
        // Values below 5 minutes should map to 5m option
        assertThat(formatRefreshRate(1)).isEqualTo("5m")
        assertThat(formatRefreshRate(30)).isEqualTo("5m")
        assertThat(formatRefreshRate(60)).isEqualTo("5m") // 1 min
        assertThat(formatRefreshRate(120)).isEqualTo("5m") // 2 mins
    }

    @Test
    fun `formatRefreshRateExplanation formats seconds correctly`() {
        assertThat(formatRefreshRateExplanation(1))
            .isEqualTo("This device checks for new screen content every 1 seconds")
        assertThat(formatRefreshRateExplanation(30))
            .isEqualTo("This device checks for new screen content every 30 seconds")
        assertThat(formatRefreshRateExplanation(59))
            .isEqualTo("This device checks for new screen content every 59 seconds")
    }

    @Test
    fun `formatRefreshRateExplanation formats single minute correctly`() {
        assertThat(formatRefreshRateExplanation(60))
            .isEqualTo("This device checks for new screen content every 1 minute")
    }

    @Test
    fun `formatRefreshRateExplanation formats multiple minutes correctly`() {
        assertThat(formatRefreshRateExplanation(120))
            .isEqualTo("This device checks for new screen content every 2 minutes")
        assertThat(formatRefreshRateExplanation(300))
            .isEqualTo("This device checks for new screen content every 5 minutes")
        assertThat(formatRefreshRateExplanation(1800))
            .isEqualTo("This device checks for new screen content every 30 minutes")
        assertThat(formatRefreshRateExplanation(3599))
            .isEqualTo("This device checks for new screen content every 59 minutes")
    }

    @Test
    fun `formatRefreshRateExplanation formats single hour correctly`() {
        assertThat(formatRefreshRateExplanation(3600))
            .isEqualTo("This device checks for new screen content every 1 hour")
    }

    @Test
    fun `formatRefreshRateExplanation formats multiple hours correctly`() {
        assertThat(formatRefreshRateExplanation(7200))
            .isEqualTo("This device checks for new screen content every 2 hours")
        assertThat(formatRefreshRateExplanation(86400))
            .isEqualTo("This device checks for new screen content every 24 hours")
    }
}
