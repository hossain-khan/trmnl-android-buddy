package ink.trmnl.android.buddy.dev

import ink.trmnl.android.buddy.ui.recipesanalytics.GrowthDataPointUi
import ink.trmnl.android.buddy.ui.recipesanalytics.PluginAnalyticsUi
import ink.trmnl.android.buddy.ui.recipesanalytics.RecipesAnalyticsUi

/**
 * Mock data generator for recipes analytics testing in development builds.
 *
 * Provides realistic test data for different analytics scenarios to test
 * UI rendering, state transitions, and edge cases.
 */
object RecipesAnalyticsMockData {
    /**
     * Generate mock analytics data for a given scenario.
     *
     * @param scenario The test scenario to generate data for
     * @return RecipesAnalyticsUi with mock data, or null for empty scenarios
     */
    fun generateMockAnalytics(scenario: DevelopmentScreen.AnalyticsScenario): RecipesAnalyticsUi? =
        when (scenario) {
            DevelopmentScreen.AnalyticsScenario.NoRecipes -> null

            DevelopmentScreen.AnalyticsScenario.AllHealthy ->
                createAnalytics(
                    pluginCount = 5,
                    healthyPercent = 100.0,
                    degradedPercent = 0.0,
                    erroringPercent = 0.0,
                )

            DevelopmentScreen.AnalyticsScenario.AllUnhealthy ->
                createAnalytics(
                    pluginCount = 5,
                    healthyPercent = 0.0,
                    degradedPercent = 50.0,
                    erroringPercent = 50.0,
                )

            is DevelopmentScreen.AnalyticsScenario.PartiallyHealthy -> {
                val pluginCount = scenario.unhealthyCount + 3
                val healthyPercent = ((pluginCount - scenario.unhealthyCount) * 100.0) / pluginCount
                val degradedPercent = (scenario.unhealthyCount * 50.0) / pluginCount
                val erroringPercent = (scenario.unhealthyCount * 50.0) / pluginCount
                createAnalytics(
                    pluginCount = pluginCount,
                    healthyPercent = healthyPercent,
                    degradedPercent = degradedPercent,
                    erroringPercent = erroringPercent,
                )
            }

            DevelopmentScreen.AnalyticsScenario.Loading -> null // Handled separately in presenter

            DevelopmentScreen.AnalyticsScenario.Error -> null // Handled separately in presenter

            is DevelopmentScreen.AnalyticsScenario.LargeDataset -> {
                val unhealthyCount = (scenario.recipeCount * scenario.unhealthyPercent) / 100
                val healthyPercent = ((scenario.recipeCount - unhealthyCount) * 100.0) / scenario.recipeCount
                val degradedPercent = (unhealthyCount * 50.0) / scenario.recipeCount
                val erroringPercent = (unhealthyCount * 50.0) / scenario.recipeCount
                createAnalytics(
                    pluginCount = scenario.recipeCount,
                    healthyPercent = healthyPercent,
                    degradedPercent = degradedPercent,
                    erroringPercent = erroringPercent,
                )
            }
        }

    /**
     * Create a complete RecipesAnalyticsUi object with the given parameters.
     */
    private fun createAnalytics(
        pluginCount: Int,
        healthyPercent: Double,
        degradedPercent: Double,
        erroringPercent: Double,
    ): RecipesAnalyticsUi {
        // Calculate integer thresholds to avoid Int to Double comparison issues
        val healthyCount = (pluginCount * healthyPercent / 100).toInt()
        val degradedCount = (pluginCount * (healthyPercent + degradedPercent) / 100).toInt()

        val plugins =
            (1..pluginCount).map { index ->
                val state =
                    when {
                        index <= healthyCount -> "healthy"
                        index <= degradedCount -> "degraded"
                        else -> "erroring"
                    }
                PluginAnalyticsUi(
                    name = "Recipe $index",
                    state = state,
                    installs = (100..500).random(),
                    forks = (10..50).random(),
                )
            }

        return RecipesAnalyticsUi(
            totalPlugins = pluginCount,
            totalConnections = (50..500).random(),
            totalPageviews = (1000..10000).random(),
            healthyPercent = healthyPercent.coerceIn(0.0, 100.0),
            degradedPercent = degradedPercent.coerceIn(0.0, 100.0),
            erroringPercent = erroringPercent.coerceIn(0.0, 100.0),
            growthData = generateMockGrowthData(),
            plugins = plugins,
        )
    }

    /**
     * Generate mock growth data for the last 8 days.
     */
    private fun generateMockGrowthData(): List<GrowthDataPointUi> {
        val today = java.time.LocalDate.now()
        return (7 downTo 0).map { daysAgo ->
            val date = today.minusDays(daysAgo.toLong())
            GrowthDataPointUi(
                date = date.toString(),
                value = (10..100).random(),
            )
        }
    }
}
