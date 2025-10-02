package ink.trmnl.android.buddy.api

import retrofit2.http.GET

/**
 * TRMNL API Service interface.
 *
 * Defines all available API endpoints for interacting with the TRMNL server.
 * Full API documentation: https://usetrmnl.com/api-docs/index.html
 */
interface TrmnlApiService {
    
    /**
     * Placeholder method - will be populated with actual API endpoints
     * based on TRMNL API documentation.
     *
     * TODO: Review API docs at https://usetrmnl.com/api-docs/index.html
     *       and add actual endpoints
     */
    @GET("status")
    suspend fun getStatus(): String
}
