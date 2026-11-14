package ink.trmnl.android.buddy.util

private const val LEGACY_API_TOKEN_PREFIX = "user_"

/**
 * Returns `true` if this TRMNL API token uses the legacy `user_` prefix.
 *
 * TRMNL distinguishes between two Account API key formats:
 * - **Scoped Account API Keys (`trmnl_...`)**: Generated under `Account > Developer`
 *   (`https://trmnl.com/account/developer/edit`) with explicit capability scopes
 *   (`read`, `content`, `devices`, `delete`, `profile`, `apps`).
 * - **Legacy Account API Keys (`user_...`)**: Older un-scoped account keys that only reach
 *   legacy endpoints and cannot access `/api/analytics*` or device diagnostics endpoints.
 *
 * See [TRMNL Account API Keys](https://help.trmnl.com/en/articles/11195228-account-api-keys).
 */
fun String?.isLegacyApiToken(): Boolean = this?.trim()?.startsWith(LEGACY_API_TOKEN_PREFIX) == true
