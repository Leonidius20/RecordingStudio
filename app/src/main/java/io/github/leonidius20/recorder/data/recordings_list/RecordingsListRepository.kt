package io.github.leonidius20.recorder.data.recordings_list

import android.app.PendingIntent
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class RecordingsListRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    @Named("io") private val ioDispatcher: CoroutineDispatcher,
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

    private val _recordings = MutableStateFlow<List<Recording>>(emptyList())
    val recordings = _recordings.asStateFlow()

    //private var latestMediaStoreVersion: String? = null
    //private var latestMediaStoreGeneration: Long? = null

    // todo Cache the result?

    suspend fun loadOrUpdateRecordingsIfNeeded() {
       // val newMediaStoreVersion = MediaStore.getVersion(context)
       // val newMediaStoreGeneration = MediaStore.getGeneration(context, MediaStore.VOLUME_EXTERNAL)
//
     //   if (newMediaStoreVersion != latestMediaStoreVersion) {
      //      latestMediaStoreVersion = newMediaStoreVersion

     //      _recordings.value = getRecordings()
     //   } else  {
            // mediastore version same, but something was added or modified
      //      MediaStore.getG
      //  }


        // todo: check MediaStore generation and whatnot

        _recordings.value = getRecordings()
    }

    suspend fun getRecordings(): List<Recording> = withContext(ioDispatcher) {
        val recordings = mutableListOf<Recording>()

        val dateColumn =
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
                MediaStore.Audio.Media.DATE_ADDED
            else
                MediaStore.Audio.Media.DATE_TAKEN

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            dateColumn,
            MediaStore.Audio.Media.MIME_TYPE,
        )

        val selectionColumn = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
            MediaStore.Audio.Media.DATA
        else
            MediaStore.Audio.Media.RELATIVE_PATH

        val selectionColumnValue = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val path = Environment.getExternalStorageDirectory().absolutePath + "/Music/RecordingStudio/" + "%"
            Log.d("RecListRepo", "Path: $path")
            path
        }
        else
            "Recordings/RecordingStudio/"


        val selection = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
            "$selectionColumn LIKE ?"
            else
            "$selectionColumn == ?"
        val selectionArgs = arrayOf(
            selectionColumnValue,
        )

        // sort by date descending
        val sortOrder = "$dateColumn DESC"

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->

            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val nameColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val durationColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val dateTakenColumn = cursor.getColumnIndexOrThrow(dateColumn)
            val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)

            while (cursor.moveToNext()) {
                // Get values of columns for a given video.
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val duration = cursor.getInt(durationColumn)
                val size = cursor.getInt(sizeColumn)
                val dateTaken = cursor.getLong(dateTakenColumn)
                val mimeType = cursor.getString(mimeTypeColumn)

                val contentUri: Uri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                // Stores column values and the contentUri in a local object
                // that represents the media file.
                recordings.add(Recording(id, contentUri, name, duration, size, dateTaken, mimeType))
            }

        }

        return@withContext recordings
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun requestTrashing(uris: List<Uri>): PendingIntent {
        return MediaStore.createTrashRequest(context.contentResolver, uris, true)
    }

    fun requestDeleting(uris: List<Uri>): PendingIntent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            MediaStore.createDeleteRequest(context.contentResolver, uris)
        } else {
            // todo: create pending intent to show a custom confirmation dialog
            TODO("VERSION.SDK_INT < R")
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
            selectionArgs)
    }

}