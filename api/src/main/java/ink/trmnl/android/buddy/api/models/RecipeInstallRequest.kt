package ink.trmnl.android.buddy.api.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request payload for installing a recipe onto a specific device.
 *
 * @property deviceId Identifier of the TRMNL device whose playlist the installed recipe joins
 * @property inheritOauth For an oauth_choice recipe: whether to carry the author's OAuth connection
 */
@Serializable
data class RecipeInstallRequest(
    @SerialName("device_id")
    val deviceId: Int,
    @SerialName("inherit_oauth")
    val inheritOauth: Boolean? = null,
)
