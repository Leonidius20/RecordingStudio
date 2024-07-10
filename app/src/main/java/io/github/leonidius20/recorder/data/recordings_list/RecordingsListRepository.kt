package io.github.leonidius20.recorder.data.recordings_list

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject

@ViewModelScoped
class RecordingsListRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    data class Recording(
        val uri: Uri,
        val name: String,
        val duration: Int,
        val size: Int,
        val dateTaken: Long,
    )

    // todo Call the query() method in a worker thread.

    fun getRecordings(): List<Recording> {
        val recordings = mutableListOf<Recording>()

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_TAKEN,
        )

        val selection = "${MediaStore.Audio.Media.RELATIVE_PATH} == ?"
        val selectionArgs = arrayOf(
            "Recordings/RecordingStudio"
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

                val contentUri: Uri = ContentUris.withAppendedId(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                // Stores column values and the contentUri in a local object
                // that represents the media file.
                recordings.add(Recording(contentUri, name, duration, size, dateTaken))
            }

            recordings.forEach {
                Log.d("RecordingsListRepo", it.toString())
            }
        }

        return recordings
    }

}