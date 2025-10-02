package ink.trmnl.android.buddy.di

import android.content.Context
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import ink.trmnl.android.buddy.BuildConfig
import ink.trmnl.android.buddy.api.TrmnlApiClient
import ink.trmnl.android.buddy.api.TrmnlApiService
import ink.trmnl.android.buddy.data.ExampleEmailValidator

// Example of Metro providers that contribute to the app graph.
// You should delete this file and create your own providers.
@ContributesTo(AppScope::class)
interface ExampleAppBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun provideEmailValidator(): ExampleEmailValidator = ExampleEmailValidator()

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
