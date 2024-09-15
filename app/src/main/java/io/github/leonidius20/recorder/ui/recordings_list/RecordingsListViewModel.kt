package io.github.leonidius20.recorder.ui.recordings_list

import android.app.PendingIntent
import android.content.Context
import android.net.Uri
import android.os.Build
import android.text.format.Formatter
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.leonidius20.recorder.data.recordings_list.RecordingsListRepository
import io.github.leonidius20.recorder.ui.common.millisecondsToStopwatchString
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Locale
import javax.inject.Inject
import javax.inject.Named

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
        val id: Long,
        val name: String,
        val duration: String,
        val size: String,
        val uri: Uri,
        // val dateTaken: String,
       // val mimeType: String,
        val isSelected: Boolean,
        val isPlaying: Boolean,
    )

    data class UiState(
        val recordings: ArrayList<RecordingUiModel>,

        // todo: lock recordings playback when recording is in progress, also pause any currently playing recording and acquire audio focus?
        val isRecInProgress: Boolean = false,
    ) {
        companion object { fun default() = UiState(ArrayList()) }

        val numItemsSelected = recordings.count { it.isSelected }

        val shouldShowActionMode = numItemsSelected != 0

        val itemIds
            get() = recordings.map { it.id }
    }

    /**
     * used in a dialog that is shown when user tries to rename a file
     */
    val renameFileNewName = MutableLiveData<String>()

    //init {
    //    loadRecordings()
    //}

    /*fun loadRecordings() {
        viewModelScope.launch {
            repository.loadOrUpdateRecordingsIfNeeded()

        }
    }*/

    private val recordingsList = repository.recordings
        .map { list ->

            ArrayList(list.map {
                RecordingUiModel(
                    it.id,
                    it.name,
                    millisecondsToStopwatchString(it.duration),
                    Formatter.formatFileSize(context, it.size.toLong()),
                    // dateFormat.format(Date(it.dateTaken)),
                    it.uri,// todo: think about how we can go about removing fields that have nothing to do with UI, like mime type
                    //it.mimeType,
                    isSelected = false,
                    isPlaying = false,
                )
            })


        }.flowOn(ioDispatcher)
        //.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    // todo: underlying recordings collection update is an Intent, that is originated from data layer


    init {
        // handle events (intents) originating from backend
        viewModelScope.launch {
            recordingsList.collect { recordings ->
                // todo: intent

                _state.value = _state.value.copy(
                    recordings = recordings
                )
            }
        }
    }


    // todo: combine recordings flow with selection info and currently playing info to create final ui state

    private val _state = MutableStateFlow<UiState>(UiState.default())
    val state = _state.asStateFlow()


    fun rename() {
        val newName = renameFileNewName.value!!
        val item = itemThatUserWantsToRename

        repository.rename(item.uri, item.id, newName, /*item.mimeType*/)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun requestTrashingSelected(): PendingIntent {
        return repository.requestTrashing(
            state.value.recordings.filter { it.isSelected }.map { it.uri }
        )
    }


    @RequiresApi(Build.VERSION_CODES.R)
    fun requestDeletingSelected(): PendingIntent {
        return repository.requestDeleting(
            state.value.recordings.filter { it.isSelected }.map { it.uri }
        )
    }

    /**
     * used on androids before R
     */
    fun legacyDeleteSelectedWithoutConfirmation() {
        repository.delete(
            state.value.recordings.filter { it.isSelected }.map { it.uri }
        )
    }

    private fun setItemSelected(index: Int, selected: Boolean) {
        // we are forcing recreation of list, even though we could maybe not recreate it by using Arrow or whatever
        _state.value = _state.value.copy(
            recordings = ArrayList(_state.value.recordings.toMutableList().apply {
                set(index, get(index).copy(isSelected = selected))
            })
        )
    }

    // todo: intent
    fun toggleSelection(index: Int) {
        Log.d("vm", "toggle selection @ index $index")
        setItemSelected(index, !_state.value.recordings[index].isSelected)
    }

    // todo: intent
    fun clearSelection() {
        _state.value = _state.value.copy(
            recordings = ArrayList(_state.value.recordings.map { recording ->
                if (recording.isSelected) {
                    recording.copy(isSelected = false)
                } else recording
            })
        )
    }

    // todo: refactor and remove
    private lateinit var itemThatUserWantsToRename: RecordingUiModel

    // todo: refactor and remove
    fun getFirstSelectedItemName(): String {

        itemThatUserWantsToRename = state.value.recordings.first { it.isSelected }

        return itemThatUserWantsToRename.name
    }

}