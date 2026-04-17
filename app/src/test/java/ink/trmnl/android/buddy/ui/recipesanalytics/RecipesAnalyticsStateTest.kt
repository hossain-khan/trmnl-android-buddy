package ink.trmnl.android.buddy.ui.recipesanalytics

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import org.junit.Test

class RecipesAnalyticsStateTest {
    @Test
    fun `isEmpty returns true when no plugins`() {
        val analytics =
            RecipesAnalyticsUi(
                totalPlugins = 0,
                totalConnections = 0,
                totalPageviews = 0,
                healthyPercent = 0.0,
                degradedPercent = 0.0,
                erroringPercent = 0.0,
                growthData = emptyList(),
                plugins = emptyList(),
            )

        assertThat(analytics.isEmpty()).isTrue()
    }

    @Test
    fun `isEmpty returns false when plugins exist`() {
        val analytics =
            RecipesAnalyticsUi(
                totalPlugins = 1,
                totalConnections = 10,
                totalPageviews = 5,
                healthyPercent = 100.0,
                degradedPercent = 0.0,
                erroringPercent = 0.0,
                growthData = emptyList(),
                plugins =
                    listOf(
                        PluginAnalyticsUi("Plugin A", "healthy", 10, 5),
                    ),
            )

        assertThat(analytics.isEmpty()).isFalse()
    }

    @Test
    fun `isHealthy returns true when healthy percent greater than 95`() {
        val analytics =
            RecipesAnalyticsUi(
                totalPlugins = 5,
                totalConnections = 100,
                totalPageviews = 50,
                healthyPercent = 98.5,
                degradedPercent = 1.2,
                erroringPercent = 0.3,
                growthData = emptyList(),
                plugins =
                    listOf(
                        PluginAnalyticsUi("Plugin A", "healthy", 10, 5),
                    ),
            )

        assertThat(analytics.isHealthy()).isTrue()
    }

    @Test
    fun `isHealthy returns false when healthy percent at or below 95`() {
        val analytics =
            RecipesAnalyticsUi(
                totalPlugins = 5,
                totalConnections = 100,
                totalPageviews = 50,
                healthyPercent = 90.0,
                degradedPercent = 5.0,
                erroringPercent = 5.0,
                growthData = emptyList(),
                plugins =
                    listOf(
                        PluginAnalyticsUi("Plugin A", "healthy", 10, 5),
                    ),
            )

        assertThat(analytics.isHealthy()).isFalse()
    }

    @Test
    fun `formatAsPercentage formats value correctly`() {
        assertThat(formatAsPercentage(98.5)).isEqualTo("98.5%")
        assertThat(formatAsPercentage(100.0)).isEqualTo("100.0%")
        assertThat(formatAsPercentage(0.0)).isEqualTo("0.0%")
        assertThat(formatAsPercentage(33.333)).isEqualTo("33.3%")
    }

    @Test
    fun `formatAsPercentageWhole formats value without decimals`() {
        assertThat(formatAsPercentageWhole(98.5)).isEqualTo("99%")
        assertThat(formatAsPercentageWhole(98.1)).isEqualTo("98%")
        assertThat(formatAsPercentageWhole(100.0)).isEqualTo("100%")
        assertThat(formatAsPercentageWhole(0.0)).isEqualTo("0%")
    }

    @Test
    fun `getHealthStatusLabel returns correct labels`() {
        assertThat(getHealthStatusLabel("healthy")).isEqualTo("Healthy")
        assertThat(getHealthStatusLabel("degraded")).isEqualTo("Degraded")
        assertThat(getHealthStatusLabel("erroring")).isEqualTo("Erroring")
        assertThat(getHealthStatusLabel("unknown")).isEqualTo("Unknown")
    }

    @Test
    fun `RecipesAnalyticsState Success isEmpty works correctly`() {
        val emptyState =
            RecipesAnalyticsState.Success(
                RecipesAnalyticsUi(
                    totalPlugins = 0,
                    totalConnections = 0,
                    totalPageviews = 0,
                    healthyPercent = 0.0,
                    degradedPercent = 0.0,
                    erroringPercent = 0.0,
                    growthData = emptyList(),
                    plugins = emptyList(),
                ),
            )

        assertThat(emptyState.isEmpty()).isTrue()
    }

    @Test
    fun `RecipesAnalyticsState Success isHealthy works correctly`() {
        val healthyState =
            RecipesAnalyticsState.Success(
                RecipesAnalyticsUi(
                    totalPlugins = 5,
                    totalConnections = 100,
                    totalPageviews = 50,
                    healthyPercent = 99.0,
                    degradedPercent = 1.0,
                    erroringPercent = 0.0,
                    growthData = emptyList(),
                    plugins =
                        listOf(
                            PluginAnalyticsUi("Plugin A", "healthy", 10, 5),
                        ),
                ),
            )

        assertThat(healthyState.isHealthy()).isTrue()
    }

    @Test
    fun `RecipesAnalyticsState isLoading works correctly`() {
        val loadingState = RecipesAnalyticsState.Loading()
        val successState =
            RecipesAnalyticsState.Success(
                RecipesAnalyticsUi(
                    totalPlugins = 0,
                    totalConnections = 0,
                    totalPageviews = 0,
                    healthyPercent = 0.0,
                    degradedPercent = 0.0,
                    erroringPercent = 0.0,
                    growthData = emptyList(),
                    plugins = emptyList(),
                ),
            )

        assertThat(loadingState.isLoading()).isTrue()
        assertThat(successState.isLoading()).isFalse()
    }

    @Test
    fun `getDataOrNull returns data on Success`() {
        val analytics =
            RecipesAnalyticsUi(
                totalPlugins = 5,
                totalConnections = 100,
                totalPageviews = 50,
                healthyPercent = 99.0,
                degradedPercent = 1.0,
                erroringPercent = 0.0,
                growthData = emptyList(),
                plugins =
                    listOf(
                        PluginAnalyticsUi("Plugin A", "healthy", 10, 5),
                    ),
            )
        val successState = RecipesAnalyticsState.Success(analytics)

        assertThat(successState.getDataOrNull()).isEqualTo(analytics)
    }

    @Test
    fun `getDataOrNull returns previous data on Error`() {
        val previousAnalytics =
            RecipesAnalyticsUi(
                totalPlugins = 5,
                totalConnections = 100,
                totalPageviews = 50,
                healthyPercent = 99.0,
                degradedPercent = 1.0,
                erroringPercent = 0.0,
                growthData = emptyList(),
                plugins =
                    listOf(
                        PluginAnalyticsUi("Plugin A", "healthy", 10, 5),
                    ),
            )
        val errorState = RecipesAnalyticsState.Error("Network error", previousAnalytics)

        assertThat(errorState.getDataOrNull()).isEqualTo(previousAnalytics)
    }

    @Test
    fun `getDataOrNull returns null when no data available`() {
        val errorState = RecipesAnalyticsState.Error("Network error", null)
        val loadingState = RecipesAnalyticsState.Loading(null)

        assertThat(errorState.getDataOrNull()).isEqualTo(null)
        assertThat(loadingState.getDataOrNull()).isEqualTo(null)
    }

    @Test
    fun `normalizeHealthPercentages converts invalid percentages to valid sum of 100`() {
        // API returns: 121.33% + 1.0% + 0.33% = 122.66%
        val (healthy, degraded, erroring) =
            normalizeHealthPercentages(
                healthy = 121.3333333333333,
                degraded = 1.0,
                erroring = 0.3333333333333333,
            )

        // After normalization, should sum to 100%
        val sum = healthy + degraded + erroring
        assertThat(sum).isEqualTo(100.0)

        // Healthy is the dominant value, should be ~99%
        assertThat(healthy > 90.0).isTrue()
        // Degraded and erroring are much smaller
        assertThat(degraded < 10.0).isTrue()
        assertThat(erroring < 1.0).isTrue()
    }

    @Test
    fun `normalizeHealthPercentages handles already normalized percentages`() {
        val (healthy, degraded, erroring) =
            normalizeHealthPercentages(
                healthy = 99.0,
                degraded = 0.8,
                erroring = 0.2,
            )

        // Already normalized, should remain ~same
        val sum = healthy + degraded + erroring
        assertThat(sum).isEqualTo(100.0)
        assertThat(healthy).isEqualTo(99.0)
        assertThat(degraded).isEqualTo(0.8)
        assertThat(erroring).isEqualTo(0.2)
    }

    @Test
    fun `normalizeHealthPercentages handles zero percentages`() {
        val (healthy, degraded, erroring) =
            normalizeHealthPercentages(
                healthy = 0.0,
                degraded = 0.0,
                erroring = 0.0,
            )

        assertThat(healthy).isEqualTo(0.0)
        assertThat(degraded).isEqualTo(0.0)
        assertThat(erroring).isEqualTo(0.0)
    }
}
