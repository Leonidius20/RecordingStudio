package io.github.leonidius20.recorder.domain.recorder

import android.content.ContentValues
import android.content.Context
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped
import io.github.leonidius20.recorder.data.recordings_list.RecordingsListRepository
import io.github.leonidius20.recorder.data.settings.Container
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

interface OutputFile {

    fun open()

    fun close()

    fun updateMetadata(duration: Long)

}

class OutputFileAbstraction @AssistedInject constructor(
    @ApplicationContext private val context: Context,
    repo: RecordingsListRepository,
    @Assisted namePattern: String,
    @Assisted val format: Container,
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

    @AssistedFactory
    interface Factory {

        fun create(
            namePattern: String,
            format: Container,
        ) : OutputFileAbstraction

    }

}

interface OutputFileFactory {
    fun create(
        namePattern: String,
        format: Container,
    ) : OutputFile
}

@ServiceScoped
class OutputFileFactoryImpl @Inject constructor(
    val factory: OutputFileAbstraction.Factory,
) : OutputFileFactory {

    override fun create(namePattern: String, format: Container) =
        factory.create(namePattern, format)



}
