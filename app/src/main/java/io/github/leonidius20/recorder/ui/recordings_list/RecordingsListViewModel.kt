package io.github.leonidius20.recorder.ui.recordings_list

import android.content.Context
import android.text.format.Formatter
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.leonidius20.recorder.data.recordings_list.RecordingsListRepository
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
        // val dateTaken: String,
    )

    fun loadRecordings() {
        viewModelScope.launch {
            val result = withContext(ioDispatcher) {
                return@withContext repository.getRecordings().map {
                    RecordingUiModel(
                        it.name,
                        it.duration.toDuration(DurationUnit.MILLISECONDS).let {
                            String.format(locale, "%d:%02d:%02d", it.inWholeHours, it.inWholeMinutes, it.inWholeSeconds)
                        },
                        Formatter.formatFileSize(context, it.size.toLong()),
                        // dateFormat.format(Date(it.dateTaken)),
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

}