package ink.trmnl.android.buddy.api.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response returned by the TRMNL API after successfully installing a recipe.
 *
 * @property data The installed recipe data wrapper
 */
@Serializable
data class RecipeInstallResponse(
    @SerialName("data")
    val data: RecipeInstallData,
)

/**
 * Data payload containing the created plugin setting and install strategy.
 *
 * @property pluginSetting The newly created plugin setting instance
 * @property installMethod How the recipe was installed (e.g. "simple_install", "read_only_fork", "oauth_choice")
 */
@Serializable
data class RecipeInstallData(
    @SerialName("plugin_setting")
    val pluginSetting: PluginSetting? = null,
    @SerialName("install_method")
    val installMethod: String? = null,
)
