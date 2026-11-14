package ink.trmnl.android.buddy.util

private const val LEGACY_API_TOKEN_PREFIX = "user_"

/**
 * Returns `true` if this TRMNL API token uses the legacy `user_` prefix.
 *
 * Legacy `user_` API keys predate capability-scoped keys (`trmnl_...`) and are not
 * supported by newer endpoints such as `/api/analytics` and device diagnostics.
 */
fun String?.isLegacyApiToken(): Boolean = this?.trim()?.startsWith(LEGACY_API_TOKEN_PREFIX) == true
