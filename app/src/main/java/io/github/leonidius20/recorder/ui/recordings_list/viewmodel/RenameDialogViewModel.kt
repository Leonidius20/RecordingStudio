package io.github.leonidius20.recorder.ui.recordings_list.viewmodel

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.leonidius20.recorder.data.recordings_list.RecordingsListRepository
import io.github.leonidius20.recorder.ui.recordings_list.view.RenameDialogFragmentArgs
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@HiltViewModel
class RenameDialogViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val recordingsListRepository: RecordingsListRepository,
) : ViewModel() {

    private val fileUri: Uri

    private val fileId: Long

    val fileName: MutableStateFlow<String>

    init {
        val args = RenameDialogFragmentArgs.fromSavedStateHandle(savedStateHandle)
        fileUri = args.fileToRename
        fileId = args.id
        fileName = MutableStateFlow(args.currentFileName)
    }

    fun rename() {
        recordingsListRepository.rename(fileUri, fileId, fileName.value)
    }

}