package app.example.data

import android.content.Context
import app.example.di.ApplicationContext
import dev.zacsweers.metro.Inject

// Example service class that does not need DI module or binding
@Inject
class ExampleAppVersionService
    constructor(
        @ApplicationContext context: Context,
    ) {
        private val versionName: String = context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "Unknown"

        fun getApplicationVersion(): String = versionName
    }
