package io.github.leonidius20.recorder.ui.recordings_list

import android.content.Context
import android.text.format.Formatter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.leonidius20.recorder.data.recordings_list.RecordingsListRepository
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Locale
import javax.inject.Inject
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@HiltViewModel
class RecordingsListViewModel @Inject constructor(
    private val repository: RecordingsListRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    // todo: launch coroutine

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
            // todo
        }
    }

    // todo: only do the loading on fragment attach, not every time the view model is created
    val recordings = repository.getRecordings().map {
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