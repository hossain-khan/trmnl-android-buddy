package ink.trmnl.android.buddy.di

import android.content.Context
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import ink.trmnl.android.buddy.BuildConfig
import ink.trmnl.android.buddy.api.TrmnlApiClient
import ink.trmnl.android.buddy.api.TrmnlApiService

@ContributesTo(AppScope::class)
interface AppBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun provideTrmnlApiService(
        @ApplicationContext context: Context,
    ): TrmnlApiService {
        // Create API service with debug logging in debug builds
        val isDebug = BuildConfig.DEBUG
        return TrmnlApiClient.create(isDebug = isDebug)
    }
}
