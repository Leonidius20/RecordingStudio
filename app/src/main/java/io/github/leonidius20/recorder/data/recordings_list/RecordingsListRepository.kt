package io.github.leonidius20.recorder.data.recordings_list

import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class RecordingsListRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    @Named("io") private val ioDispatcher: CoroutineDispatcher,
    private val dataSource: RecordingsDataSource,
) {

    data class Recording(
        val id: Long,
        val uri: Uri,
        val name: String,
        val duration: Int,
        val size: Int,
        val dateTaken: Long,
        val mimeType: String,
    )

    val recordings = callbackFlow {

        val cursor = dataSource.getCursorForRecordingsFolder()

        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {

            override fun onChange(selfChange: Boolean) {
                trySend(Unit)
            }

        }

        cursor?.registerContentObserver(observer)

        awaitClose {
            cursor?.apply {
                unregisterContentObserver(observer)
                close()
            }
        }

    }.onStart { emit(Unit) }
        .map { dataSource.getRecordings() }


    // todo Cache the result?

    @RequiresApi(Build.VERSION_CODES.R)
    fun requestTrashing(uris: List<Uri>): PendingIntent {
        return MediaStore.createTrashRequest(context.contentResolver, uris, true)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun requestDeleting(uris: List<Uri>): PendingIntent {
        return MediaStore.createDeleteRequest(context.contentResolver, uris)
    }

    /**
     * used on android versions lower than R
     */
    fun delete(uris: List<Uri>) {
        // Remove a specific media item.
        val resolver = context.contentResolver

        uris.forEach { uri ->
            resolver.delete(
                uri, null, null
            )
        }
    }

    fun rename(uri: Uri, id: Long, newName: String/*, mimeType: String*/) {
        // Updates an existing media item.
        val mediaId = id
        val resolver = context.contentResolver

        // When performing a single item update, prefer using the ID.
        val selection = "${MediaStore.Audio.Media._ID} = ?"

        // By using selection + args you protect against improper escaping of // values.
        val selectionArgs = arrayOf(mediaId.toString())

        // Update an existing recording.
        val updatedRecordingDetails = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, newName)
            // put(MediaStore.Audio.Media.MIME_TYPE, mimeType)
        }

        // Use the individual song's URI to represent the collection that's
        // updated.
        val numSongsUpdated = resolver.update(
            uri,
            updatedRecordingDetails,
            selection,
            selectionArgs
        )
    }

    fun createRecordingFile(name: String, mimeType: String): Uri {
        return dataSource.createRecordingFile(name, mimeType)
    }

}