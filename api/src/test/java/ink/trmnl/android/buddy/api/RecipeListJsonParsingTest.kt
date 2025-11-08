package ink.trmnl.android.buddy.api

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import ink.trmnl.android.buddy.api.models.RecipesResponse
import kotlinx.serialization.json.Json
import org.junit.Test

/**
 * Test to validate parsing of the actual recipe_list.json file.
 *
 * This test ensures that the Recipe model can successfully parse the real-world
 * API response, including recipes with complex custom_fields that contain options
 * as both strings and key-value objects.
 *
 * This is a regression test for the parsing error:
 * "Unexpected JSON token at offset 4917: Expected beginning of the string, but got {"
 */
class RecipeListJsonParsingTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Test
    fun `parse actual recipe_list json file successfully`() {
        // Given: Load actual recipe_list.json from test resources
        val jsonContent = javaClass.classLoader
            ?.getResourceAsStream("recipe_list.json")
            ?.bufferedReader()
            ?.use { it.readText() }
            ?: throw IllegalStateException("Could not load recipe_list.json")

        // When: Parse the JSON
        val response = json.decodeFromString<RecipesResponse>(jsonContent)

        // Then: Verify pagination metadata
        assertThat(response.total).isEqualTo(490)
        assertThat(response.from).isEqualTo(1)
        assertThat(response.to).isEqualTo(25)
        assertThat(response.perPage).isEqualTo(25)
        assertThat(response.currentPage).isEqualTo(1)
        assertThat(response.prevPageUrl).isNull()
        assertThat(response.nextPageUrl).isEqualTo("/recipes?page=2&sort_by=newest")

        // Verify data array
        assertThat(response.data).hasSize(25)

        // Verify first recipe (DN Latest News)
        val firstRecipe = response.data[0]
        assertThat(firstRecipe.id).isEqualTo(176747)
        assertThat(firstRecipe.name).isEqualTo("DN Latest News")
        assertThat(firstRecipe.iconUrl).isNotNull()
        assertThat(firstRecipe.screenshotUrl).isNotNull()
        assertThat(firstRecipe.stats.installs).isEqualTo(1)
        assertThat(firstRecipe.stats.forks).isEqualTo(0)

        // Verify author bio exists
        assertThat(firstRecipe.authorBio).isNotNull()
        assertThat(firstRecipe.authorBio?.keyname).isEqualTo("author_bio")
        assertThat(firstRecipe.authorBio?.name).isEqualTo("About This Plugin")
        assertThat(firstRecipe.authorBio?.fieldType).isEqualTo("author_bio")

        // Verify custom fields are parsed (even with complex options that we ignore)
        assertThat(firstRecipe.customFields).hasSize(1)

        // Verify third recipe (Top Android Apps) which has custom_fields with complex options
        val androidAppsRecipe = response.data[2]
        assertThat(androidAppsRecipe.id).isEqualTo(176578)
        assertThat(androidAppsRecipe.name).isEqualTo("Top Android Apps")
        assertThat(androidAppsRecipe.customFields.size).isEqualTo(4)

        // Verify custom fields are accessible
        val showField = androidAppsRecipe.customFields[1]
        assertThat(showField.keyname).isEqualTo("show")
        assertThat(showField.fieldType).isEqualTo("select")
        // Note: options and default fields are not exposed in DTO (unused in app)

        // Verify other recipes parse successfully
        assertThat(response.data[3].name).isEqualTo("10 PRINT")
        assertThat(response.data[4].name).isEqualTo("RetroAchievements: Recent Games")
        assertThat(response.data[5].name).isEqualTo("Canadian Holidays")
        assertThat(response.data[6].name).isEqualTo("Top iOS Apps")
        assertThat(response.data[7].name).isEqualTo("Flip Clock")
    }
}
