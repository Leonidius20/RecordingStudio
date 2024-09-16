package io.github.leonidius20.recorder.data.recordings_list

import android.app.PendingIntent
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.annotation.RequiresApi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import java.io.File
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

    /* TODO: reimplement with callbackFlow and
        https://developer.android.com/reference/android/database/ContentObserver
        MediaStore.addContentObserver(...)
     */

    val recordings = callbackFlow {

        val cursor = getCursorForRecordingsFolder()

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
        .map { getRecordings() }

    //private var latestMediaStoreVersion: String? = null
    //private var latestMediaStoreGeneration: Long? = null

    // todo Cache the result?

    private suspend fun loadOrUpdateRecordingsIfNeeded() {
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

        // _recordings.value = getRecordings()
    }

    private fun getCursorForRecordingsFolder(): Cursor? {
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

        // next two variables exist because RELATIVE_PATH is available only on
        // Android 10 and up, whereas the "Recordings" media folder is only
        // available on Android 12 and up. We have to handle all possible
        // combinations of path column with path value
        val androidApiLevel = Build.VERSION.SDK_INT
        val android10 = Build.VERSION_CODES.Q
        val android12 = Build.VERSION_CODES.S
        val pathColumnToValue =
            when(androidApiLevel) {
                in Int.MIN_VALUE until android10 -> {
                    MediaStore.Audio.Media.DATA to Environment.getExternalStorageDirectory().absolutePath + "/Music/RecordingStudio/" + "%"
                }
                in android10 until android12 -> {
                    // MediaStore.Audio.Media.RELATIVE_PATH to "Music/RecordingStudio"
                    MediaStore.Audio.Media.DATA to Environment.getExternalStorageDirectory().absolutePath + "/Music/RecordingStudio/" + "%"
                    // todo: i don't understand why RELATIVE_PATH doesn't work on Android 11
                    //  even though it is inserted with relative path just fine
                }
                in android12 until Int.MAX_VALUE -> {
                    MediaStore.Audio.Media.RELATIVE_PATH to "Recordings/RecordingStudio"
                }
                else -> { throw Error("Weird Android API level $androidApiLevel") }
            }


    /*
        val selectionColumn = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
            MediaStore.Audio.Media.DATA
        else
            MediaStore.Audio.Media.RELATIVE_PATH

        val selectionColumnValue =
            // from Android 5 up to Android 10 (exclusive)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                val path =
                    Environment.getExternalStorageDirectory().absolutePath + "/Music/RecordingStudio/" + "%"
                path
            // Android 10, Android 11 (less than 12 but no less than 10)
            } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                "Music/RecordingStudio"
            // Android 12 and up (no less than Android 12)
            } else
                "Recordings/RecordingStudio/"
        // todo: test this fix on an Android 11 emulator
        */

        /*val _selection = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
            "$selectionColumn LIKE ?"
        else
            "$selectionColumn == ?"
        val selectionArgs = arrayOf(
            selectionColumnValue,
        )*/

        val selection = when(pathColumnToValue.first) {
            MediaStore.Audio.Media.DATA -> {
                "${MediaStore.Audio.Media.DATA} LIKE ?"
            }
            MediaStore.Audio.Media.RELATIVE_PATH -> {
                "${MediaStore.Audio.Media.RELATIVE_PATH} == ?"
            }
            else -> { throw Error("unexpected path column") }
        }
        val selectionArgs = arrayOf(
            pathColumnToValue.second
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

    private suspend fun getRecordings(): List<Recording> = withContext(ioDispatcher) {
        val recordings = mutableListOf<Recording>()

        val dateColumn =
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
                MediaStore.Audio.Media.DATE_ADDED
            else
                MediaStore.Audio.Media.DATE_TAKEN

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
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val mediaFolder =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                        "Recordings" else "Music" // Recordings folder only appeared in Android 12
                put(MediaStore.MediaColumns.RELATIVE_PATH, "$mediaFolder/RecordingStudio")
            } else {
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

        }

        val uri =
            resolver.insert(//MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues
            )

        return uri!!
    }

}