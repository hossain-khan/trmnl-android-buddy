package ink.trmnl.android.buddy.di

import ink.trmnl.android.buddy.data.ExampleEmailValidator
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

// Example of Metro providers that contribute to the app graph.
// You should delete this file and create your own providers.
@ContributesTo(AppScope::class)
interface ExampleAppBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun provideEmailValidator(): ExampleEmailValidator = ExampleEmailValidator()
}
