package io.github.leonidius20.recorder.domain.recorder

import android.content.Context
import android.os.ParcelFileDescriptor
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.leonidius20.recorder.data.recordings_list.RecordingsListRepository
import io.github.leonidius20.recorder.data.settings.Container
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OutputFileAbstraction @AssistedInject constructor(
    @ApplicationContext private val context: Context,
    repo: RecordingsListRepository,
    @Assisted namePattern: String,
    @Assisted val format: Container,
) {

    private val dateFormat = SimpleDateFormat(namePattern, Locale.getDefault())

    private val fileName = dateFormat.format(Date(System.currentTimeMillis()))

    // http://androidxref.com/4.4.4_r1/xref/frameworks/base/media/java/android/media/MediaFile.java#174
    val fileUri = repo.createRecordingFile(fileName, format.mimeType)

    lateinit var descriptor: ParcelFileDescriptor

    fun open() {
        descriptor = context.contentResolver.openFileDescriptor(fileUri, "rw")!!
    }

    fun close() {
        descriptor.close()
    }

    @AssistedFactory
    interface Factory {

        fun create(
            namePattern: String,
            format: Container,
        ) : OutputFileAbstraction

    }

}
