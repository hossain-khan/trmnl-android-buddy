package ink.trmnl.android.buddy.util

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

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

    // ============================================================================
    // formatRelativeTime() tests
    // ============================================================================

    @Test
    fun `formatRelativeTime returns Never for null input`() {
        assertThat(formatRelativeTime(null)).isEqualTo("Never")
    }

    @Test
    fun `formatRelativeTime returns Unknown time for invalid ISO timestamp`() {
        assertThat(formatRelativeTime("invalid-timestamp")).isEqualTo("Unknown time")
        assertThat(formatRelativeTime("2026-02-30T25:70:00Z")).isEqualTo("Unknown time")
        assertThat(formatRelativeTime("not-a-date")).isEqualTo("Unknown time")
    }

    @Test
    fun `formatRelativeTime returns Just now for very recent time`() {
        // Less than 1 minute ago
        val thirtySecondsAgo = Instant.now().minus(30, ChronoUnit.SECONDS).toString()
        assertThat(formatRelativeTime(thirtySecondsAgo)).isEqualTo("Just now")

        val fiveSecondsAgo = Instant.now().minus(5, ChronoUnit.SECONDS).toString()
        assertThat(formatRelativeTime(fiveSecondsAgo)).isEqualTo("Just now")
    }

    @Test
    fun `formatRelativeTime returns Just now for future dates`() {
        // Future date (edge case handling)
        val futureTime = Instant.now().plus(10, ChronoUnit.MINUTES).toString()
        assertThat(formatRelativeTime(futureTime)).isEqualTo("Just now")
    }

    @Test
    fun `formatRelativeTime formats minutes correctly`() {
        // 1 minute ago (singular)
        val oneMinuteAgo = Instant.now().minus(1, ChronoUnit.MINUTES).toString()
        assertThat(formatRelativeTime(oneMinuteAgo)).isEqualTo("1 minute ago")

        // 5 minutes ago (plural)
        val fiveMinutesAgo = Instant.now().minus(5, ChronoUnit.MINUTES).toString()
        assertThat(formatRelativeTime(fiveMinutesAgo)).isEqualTo("5 minutes ago")

        // 45 minutes ago
        val fortyFiveMinutesAgo = Instant.now().minus(45, ChronoUnit.MINUTES).toString()
        assertThat(formatRelativeTime(fortyFiveMinutesAgo)).isEqualTo("45 minutes ago")

        // 59 minutes ago (just before 1 hour)
        val fiftyNineMinutesAgo = Instant.now().minus(59, ChronoUnit.MINUTES).toString()
        assertThat(formatRelativeTime(fiftyNineMinutesAgo)).isEqualTo("59 minutes ago")
    }

    @Test
    fun `formatRelativeTime formats hours correctly`() {
        // 1 hour ago (singular, no minutes)
        val oneHourAgo = Instant.now().minus(60, ChronoUnit.MINUTES).toString()
        assertThat(formatRelativeTime(oneHourAgo)).isEqualTo("1 hour ago")

        // 2 hours ago (plural, no minutes)
        val twoHoursAgo = Instant.now().minus(120, ChronoUnit.MINUTES).toString()
        assertThat(formatRelativeTime(twoHoursAgo)).isEqualTo("2 hours ago")

        // 5 hours ago
        val fiveHoursAgo = Instant.now().minus(5, ChronoUnit.HOURS).toString()
        assertThat(formatRelativeTime(fiveHoursAgo)).isEqualTo("5 hours ago")
    }

    @Test
    fun `formatRelativeTime formats hours and minutes correctly`() {
        // 1 hour and 1 minute ago (both singular)
        val oneHourOneMinuteAgo = Instant.now().minus(61, ChronoUnit.MINUTES).toString()
        assertThat(formatRelativeTime(oneHourOneMinuteAgo)).isEqualTo("1 hour and 1 minute ago")

        // 1 hour and 30 minutes ago
        val oneHourThirtyMinutesAgo = Instant.now().minus(90, ChronoUnit.MINUTES).toString()
        assertThat(formatRelativeTime(oneHourThirtyMinutesAgo)).isEqualTo("1 hour and 30 minutes ago")

        // 2 hours and 10 minutes ago (both plural)
        val twoHoursTenMinutesAgo = Instant.now().minus(130, ChronoUnit.MINUTES).toString()
        assertThat(formatRelativeTime(twoHoursTenMinutesAgo)).isEqualTo("2 hours and 10 minutes ago")

        // 3 hours and 45 minutes ago
        val threeHoursFortyFiveMinutesAgo = Instant.now().minus(225, ChronoUnit.MINUTES).toString()
        assertThat(formatRelativeTime(threeHoursFortyFiveMinutesAgo))
            .isEqualTo("3 hours and 45 minutes ago")

        // 23 hours and 59 minutes ago (just before 1 day)
        val almostOneDayAgo = Instant.now().minus(1439, ChronoUnit.MINUTES).toString()
        assertThat(formatRelativeTime(almostOneDayAgo)).isEqualTo("23 hours and 59 minutes ago")
    }

    @Test
    fun `formatRelativeTime formats days correctly`() {
        // 1 day ago (singular)
        val oneDayAgo = Instant.now().minus(1, ChronoUnit.DAYS).toString()
        assertThat(formatRelativeTime(oneDayAgo)).isEqualTo("1 day ago")

        // 2 days ago (plural)
        val twoDaysAgo = Instant.now().minus(2, ChronoUnit.DAYS).toString()
        assertThat(formatRelativeTime(twoDaysAgo)).isEqualTo("2 days ago")

        // 3 days ago
        val threeDaysAgo = Instant.now().minus(3, ChronoUnit.DAYS).toString()
        assertThat(formatRelativeTime(threeDaysAgo)).isEqualTo("3 days ago")

        // 7 days ago
        val sevenDaysAgo = Instant.now().minus(7, ChronoUnit.DAYS).toString()
        assertThat(formatRelativeTime(sevenDaysAgo)).isEqualTo("7 days ago")

        // 30 days ago
        val thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS).toString()
        assertThat(formatRelativeTime(thirtyDaysAgo)).isEqualTo("30 days ago")
    }

    @Test
    fun `formatRelativeTime handles real-world API timestamps`() {
        // Test with actual ISO 8601 format from TRMNL API
        // These are fixed timestamps, so we test the parsing works correctly
        // Note: These will show increasing age over time, but we're testing format parsing

        // Recent timestamp in proper ISO 8601 format
        val recentTimestamp = "2026-02-11T22:00:00.000Z"
        val result = formatRelativeTime(recentTimestamp)
        // Just verify it doesn't crash and returns a valid format (not "Unknown time")
        assertThat(result).isNotEqualTo("Unknown time")
        assertThat(result).isNotEqualTo("Never")
    }
}
