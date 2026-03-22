package ink.trmnl.android.buddy.api.models

/**
 * Response wrapper for the `/plugin_settings` API endpoint.
 *
 * Returns a list of plugin settings matching the given query parameters.
 * Used in the calendar sync 3-step workflow to retrieve the calendar plugin
 * setting ID needed for syncing events.
 *
 * @see ApiResponse
 * @see PluginSetting
 */
typealias PluginSettingsResponse = ApiResponse<List<PluginSetting>>
