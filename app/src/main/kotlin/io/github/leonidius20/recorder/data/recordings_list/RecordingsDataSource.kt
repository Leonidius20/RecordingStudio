package io.github.leonidius20.recorder.data.recordings_list

import android.database.Cursor
import android.net.Uri
import io.github.leonidius20.recorder.domain.recordings_list.Recording

interface RecordingsDataSource {

    fun getCursorForRecordingsFolder(): Cursor?

    suspend fun getRecordings(): List<Recording>

    fun createRecordingFile(name: String, mimeType: String): Uri

}