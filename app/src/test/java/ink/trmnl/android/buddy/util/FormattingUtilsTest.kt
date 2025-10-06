package ink.trmnl.android.buddy.util

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test

class FormattingUtilsTest {
    @Test
    fun `formatRefreshRate formats seconds correctly`() {
        assertThat(formatRefreshRate(1)).isEqualTo("1s")
        assertThat(formatRefreshRate(30)).isEqualTo("30s")
        assertThat(formatRefreshRate(59)).isEqualTo("59s")
    }

    @Test
    fun `formatRefreshRate formats minutes correctly`() {
        assertThat(formatRefreshRate(60)).isEqualTo("1m")
        assertThat(formatRefreshRate(120)).isEqualTo("2m")
        assertThat(formatRefreshRate(300)).isEqualTo("5m")
        assertThat(formatRefreshRate(1800)).isEqualTo("30m")
        assertThat(formatRefreshRate(3599)).isEqualTo("59m")
    }

    @Test
    fun `formatRefreshRate formats hours correctly`() {
        assertThat(formatRefreshRate(3600)).isEqualTo("1h")
        assertThat(formatRefreshRate(7200)).isEqualTo("2h")
        assertThat(formatRefreshRate(86400)).isEqualTo("24h")
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
