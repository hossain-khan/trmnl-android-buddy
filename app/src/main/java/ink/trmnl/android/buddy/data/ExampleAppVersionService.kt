package ink.trmnl.android.buddy.data

import android.content.Context
import dev.zacsweers.metro.Inject
import ink.trmnl.android.buddy.di.ApplicationContext

// Example service class that does not need DI module or binding
@Inject
class ExampleAppVersionService
    constructor(
        @ApplicationContext context: Context,
    ) {
        private val versionName: String = context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "Unknown"

        fun getApplicationVersion(): String = versionName
    }
