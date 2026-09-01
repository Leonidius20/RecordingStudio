package io.github.leonidius20.recorder.domain.recorder

import android.content.ContentValues
import android.content.Context
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.leonidius20.recorder.data.recordings_list.RecordingsListRepository
import io.github.leonidius20.recorder.entities.audio_settings.Container
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class OutputFileImpl(
    private val context: Context,
    repo: RecordingsListRepository,
    namePattern: String,
    val format: Container,
) : OutputFile {

    private val dateFormat = SimpleDateFormat(namePattern, Locale.getDefault())

    private val fileName = dateFormat.format(Date(System.currentTimeMillis()))

    // http://androidxref.com/4.4.4_r1/xref/frameworks/base/media/java/android/media/MediaFile.java#174
    val fileUri = repo.createRecordingFile(fileName, format.mimeType)

    lateinit var descriptor: ParcelFileDescriptor

    override fun open() {
        descriptor = context.contentResolver.openFileDescriptor(fileUri, "rw")!!
    }

    override fun close() {
        descriptor.close()
    }

    override fun updateMetadata(duration: Long) {
        context.contentResolver.update(fileUri, ContentValues().apply {
            put(MediaStore.MediaColumns.SIZE, descriptor.statSize)
            put(MediaStore.MediaColumns.DURATION, duration)
        }, null, null)
    }

}

class OutputFileFactoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val repo: RecordingsListRepository,
) : OutputFileFactory {

    override fun create(namePattern: String, format: Container) =
        OutputFileImpl(
            context, repo, namePattern, format
        )

}
