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
    fun `formatRefreshRateExplanation maps to closest TRMNL option for exact matches`() {
        // Test exact matches to TRMNL options
        assertThat(formatRefreshRateExplanation(300))
            .isEqualTo("This device checks for new screen content every 5 minutes")
        assertThat(formatRefreshRateExplanation(600))
            .isEqualTo("This device checks for new screen content every 10 minutes")
        assertThat(formatRefreshRateExplanation(900))
            .isEqualTo("This device checks for new screen content every 15 minutes")
        assertThat(formatRefreshRateExplanation(1800))
            .isEqualTo("This device checks for new screen content every 30 minutes")
        assertThat(formatRefreshRateExplanation(2700))
            .isEqualTo("This device checks for new screen content every 45 minutes")
        assertThat(formatRefreshRateExplanation(3600))
            .isEqualTo("This device checks for new screen content every 1 hour")
        assertThat(formatRefreshRateExplanation(5400))
            .isEqualTo("This device checks for new screen content every 90 minutes")
        assertThat(formatRefreshRateExplanation(7200))
            .isEqualTo("This device checks for new screen content every 2 hours")
        assertThat(formatRefreshRateExplanation(14400))
            .isEqualTo("This device checks for new screen content every 4 hours")
        assertThat(formatRefreshRateExplanation(86400))
            .isEqualTo("This device checks for new screen content every 24 hours")
    }

    @Test
    fun `formatRefreshRateExplanation maps to closest TRMNL option for near matches`() {
        // Test values that should map to closest option
        assertThat(formatRefreshRateExplanation(912))
            .isEqualTo("This device checks for new screen content every 15 minutes") // 15.2 mins → 15m
        assertThat(formatRefreshRateExplanation(6812))
            .isEqualTo("This device checks for new screen content every 2 hours") // 113.5 mins → 120m (2h)
        assertThat(formatRefreshRateExplanation(1500))
            .isEqualTo("This device checks for new screen content every 30 minutes") // 25 mins → 30m
        assertThat(formatRefreshRateExplanation(4000))
            .isEqualTo("This device checks for new screen content every 1 hour") // 66.7 mins → 60m (1h)
    }

    @Test
    fun `formatRefreshRateExplanation maps very small values to 5 minutes`() {
        // Values below 5 minutes should map to 5m option
        assertThat(formatRefreshRateExplanation(1))
            .isEqualTo("This device checks for new screen content every 5 minutes")
        assertThat(formatRefreshRateExplanation(30))
            .isEqualTo("This device checks for new screen content every 5 minutes")
        assertThat(formatRefreshRateExplanation(60))
            .isEqualTo("This device checks for new screen content every 5 minutes") // 1 min
        assertThat(formatRefreshRateExplanation(120))
            .isEqualTo("This device checks for new screen content every 5 minutes") // 2 mins
    }
}
