package io.github.leonidius20.recorder.data.recordings_list

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.leonidius20.recorder.data.recordings_list.RecordingsListRepository.Recording
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class RecordingsDataSourceAndroid5to9 @Inject constructor(
    @ApplicationContext private val context: Context,
    @Named("io") private val ioDispatcher: CoroutineDispatcher,
): RecordingsDataSource {

    override fun getCursorForRecordingsFolder(): Cursor? {
        val dateColumn = MediaStore.Audio.Media.DATE_ADDED

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            dateColumn,
            MediaStore.Audio.Media.MIME_TYPE,
        )

        val selection = "${MediaStore.Audio.Media.DATA} LIKE ?"
        val selectionArgs = arrayOf(
            Environment.getExternalStorageDirectory().absolutePath + "/Music/RecordingStudio/" + "%",
        )

        // sort by date descending
        val sortOrder = "$dateColumn DESC"

        val cursor = context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )

        return cursor
    }

    override suspend fun getRecordings() = withContext(ioDispatcher) {
        val recordings = mutableListOf<Recording>()

        val dateColumn = MediaStore.Audio.Media.DATE_ADDED

        getCursorForRecordingsFolder()?.use { cursor ->

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

    override fun createRecordingFile(name: String, mimeType: String): Uri {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)

            // "RELATIVE_PATH" only appeared in android 10
            val folderPath =
                Environment.getExternalStorageDirectory().absolutePath + "/Music/RecordingStudio/"
            val fullFileName =
                "$name.${MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)}"
            put(
                MediaStore.MediaColumns.DATA,
                folderPath + fullFileName
            )
            val folder = File(folderPath)
            if (!folder.exists()) folder.mkdirs()
        }

        val uri =
            resolver.insert(//MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues
            )

        return uri!!
    }

}