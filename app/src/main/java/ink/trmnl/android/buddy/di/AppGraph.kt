package ink.trmnl.android.buddy.di

import android.app.Activity
import android.content.Context
import androidx.work.WorkManager
import com.slack.circuit.foundation.Circuit
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provider
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import ink.trmnl.android.buddy.data.preferences.UserPreferencesRepository
import ink.trmnl.android.buddy.work.WorkerScheduler
import kotlin.reflect.KClass

/**
 * Metro dependency graph for the application, which includes all necessary bindings and providers.
 *
 * See https://zacsweers.github.io/metro/
 */
@DependencyGraph(scope = AppScope::class)
@SingleIn(AppScope::class)
interface AppGraph {
    val activityProviders: Map<KClass<out Activity>, Provider<Activity>>
    val circuit: Circuit
    val workManager: WorkManager
    val workerFactory: AppWorkerFactory
    val userPreferencesRepository: UserPreferencesRepository
    val workerScheduler: WorkerScheduler

    @Provides
    fun providesWorkManager(
        @ApplicationContext context: Context,
    ): WorkManager = WorkManager.getInstance(context)

    // https://zacsweers.github.io/metro/dependency-graphs/#provides
    @DependencyGraph.Factory
    interface Factory {
        fun create(
            @ApplicationContext @Provides context: Context,
        ): AppGraph
    }
}
