package app.example.di

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlin.collections.get
import kotlin.reflect.KClass

/**
 * A custom [WorkerFactory] that uses Metro DI to create workers.
 *
 *
 * To register a worker, annotate the worker class with `@WorkerKey` and `@ContributesIntoMap` with
 * the appropriate scope, and create an `AssistedFactory` for the worker's parameters.
 *
 * ```kotlin
 * @WorkerKey(YourWorker::class)
 * @ContributesIntoMap(
 *     AppScope::class,
 *     binding = binding<WorkerInstanceFactory<*>>(),
 * )
 * @AssistedFactory
 * abstract class Factory : WorkerInstanceFactory<YourWorker>
 * ```
 */
@ContributesBinding(AppScope::class)
@Inject
class AppWorkerFactory(
    private val workerProviders: Map<KClass<out ListenableWorker>, WorkerInstanceFactory<*>>,
) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
    ): ListenableWorker? = workerProviders[Class.forName(workerClassName).kotlin]?.create(workerParameters)

    interface WorkerInstanceFactory<T : ListenableWorker> {
        fun create(params: WorkerParameters): T
    }
}
