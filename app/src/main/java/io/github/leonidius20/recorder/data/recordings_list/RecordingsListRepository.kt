package io.github.leonidius20.recorder.data.recordings_list

import android.app.PendingIntent
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Named

@ViewModelScoped
class RecordingsListRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    @Named("io") private val ioDispatcher: CoroutineDispatcher,
) {

    data class Recording(
        val uri: Uri,
        val name: String,
        val duration: Int,
        val size: Int,
        val dateTaken: Long,
    )

    private val _recordings = MutableStateFlow<List<Recording>>(emptyList())
    val recordings = _recordings.asStateFlow()

    // todo Cache the result?

    suspend fun loadOrUpdateRecordingsIfNeeded() {

        // todo: check MediaStore generation and whatnot

        _recordings.value = getRecordings()
    }

    suspend fun getRecordings(): List<Recording> = withContext(ioDispatcher) {
        val recordings = mutableListOf<Recording>()

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_TAKEN,
            MediaStore.Audio.Media.IS_TRASHED,
        )

        val selection = "${MediaStore.Audio.Media.RELATIVE_PATH} == ?"
        val selectionArgs = arrayOf(
            "Recordings/RecordingStudio/",
        )

        // sort by date descending
        val sortOrder = "${MediaStore.Audio.Media.DATE_TAKEN} DESC"

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
            val dateTakenColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_TAKEN)

            while (cursor.moveToNext()) {
                // Get values of columns for a given video.
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val duration = cursor.getInt(durationColumn)
                val size = cursor.getInt(sizeColumn)
                val dateTaken = cursor.getLong(dateTakenColumn)

                // todo: it is the uri that are faulty probably, that's why we cannot delte jack shit
                //Uri.withAppendedPath()
                val contentUri: Uri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                // Stores column values and the contentUri in a local object
                // that represents the media file.
                recordings.add(Recording(contentUri, name, duration, size, dateTaken))
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

}