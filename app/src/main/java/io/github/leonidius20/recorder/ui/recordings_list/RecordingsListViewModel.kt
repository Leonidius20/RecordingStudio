package io.github.leonidius20.recorder.ui.recordings_list

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Build
import android.text.format.Formatter
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.leonidius20.recorder.data.playback.PlaybackService
import io.github.leonidius20.recorder.data.recordings_list.RecordingsListRepository
import io.github.leonidius20.recorder.ui.common.millisecondsToStopwatchString
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Locale
import javax.inject.Inject
import javax.inject.Named
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@HiltViewModel
class RecordingsListViewModel @Inject constructor(
    private val repository: RecordingsListRepository,
    @ApplicationContext private val context: Context,
    @Named("io") private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val dateFormat =
        DateFormat.getDateInstance(DateFormat.DEFAULT, Locale.getDefault())

    private val locale = Locale.getDefault()

    data class RecordingUiModel(
        val name: String,
        val duration: String,
        val size: String,
        val uri: Uri,
        // val dateTaken: String,
    )

    /**
     * used in a dialog that is shown when user tries to rename a file
     */
    val renameFileNewName = MutableLiveData<String>()

    fun loadRecordings() {
        viewModelScope.launch {
            val result = withContext(ioDispatcher) {
                return@withContext repository.getRecordings().map {
                    RecordingUiModel(
                        it.name,
                        millisecondsToStopwatchString(it.duration),
                        Formatter.formatFileSize(context, it.size.toLong()),
                        // dateFormat.format(Date(it.dateTaken)),
                        it.uri,
                    )
                }.toTypedArray()
            }

            _recordings.value = result
        }
    }

    private val _recordings = MutableLiveData<Array<RecordingUiModel>>()

    val recordings: LiveData<Array<RecordingUiModel>>
        get() = _recordings


    fun rename(datasetPosition: Int) {
        val item = _recordings.value!![datasetPosition]
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun requestTrashing(positions: List<Int>): PendingIntent {
        return repository.requestTrashing(
            positions.map { position -> _recordings.value!![position].uri }
        )
    }

    fun requestDeleting(positions: List<Int>): PendingIntent {
        return repository.requestDeleting(
            positions.map { position -> _recordings.value!![position].uri }
        )
    }

}