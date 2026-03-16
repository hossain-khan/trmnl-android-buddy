package ink.trmnl.android.buddy.api.models

/**
 * Response wrapper for the `/me` API endpoint.
 *
 * The TRMNL API wraps responses in a `data` field for consistency.
 *
 * @see ApiResponse
 */
typealias UserResponse = ApiResponse<User>
