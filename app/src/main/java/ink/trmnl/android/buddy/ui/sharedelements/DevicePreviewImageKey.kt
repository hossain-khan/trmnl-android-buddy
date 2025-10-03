package ink.trmnl.android.buddy.ui.sharedelements

import com.slack.circuit.sharedelements.SharedTransitionKey

/**
 * Shared element transition keys for device-related UI elements.
 * Used to match shared elements across different screens for smooth transitions.
 */
data class DevicePreviewImageKey(
    val deviceId: String,
) : SharedTransitionKey
