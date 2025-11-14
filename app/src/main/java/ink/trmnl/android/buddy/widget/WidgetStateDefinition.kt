package ink.trmnl.android.buddy.widget

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import androidx.glance.state.GlanceStateDefinition
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.io.OutputStream

/**
 * State definition for the device widget using DataStore.
 * Manages widget state persistence using kotlinx.serialization.
 */
object WidgetStateDefinition : GlanceStateDefinition<WidgetState> {
    private const val WIDGET_PREFS_NAME = "device_widget_prefs"

    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    override suspend fun getDataStore(
        context: Context,
        fileKey: String,
    ): DataStore<WidgetState> = context.widgetStateDataStore

    override fun getLocation(
        context: Context,
        fileKey: String,
    ): File = context.dataDir.resolve("datastore/$WIDGET_PREFS_NAME")

    private val Context.widgetStateDataStore: DataStore<WidgetState> by dataStore(
        fileName = WIDGET_PREFS_NAME,
        serializer = WidgetStateSerializer,
    )

    private object WidgetStateSerializer : Serializer<WidgetState> {
        override val defaultValue: WidgetState
            get() = WidgetState()

        override suspend fun readFrom(input: InputStream): WidgetState =
            try {
                json.decodeFromString(
                    WidgetState.serializer(),
                    input.readBytes().decodeToString(),
                )
            } catch (e: SerializationException) {
                throw CorruptionException("Cannot read widget state", e)
            }

        override suspend fun writeTo(
            t: WidgetState,
            output: OutputStream,
        ) {
            output.write(
                json.encodeToString(WidgetState.serializer(), t).encodeToByteArray(),
            )
        }
    }
}
