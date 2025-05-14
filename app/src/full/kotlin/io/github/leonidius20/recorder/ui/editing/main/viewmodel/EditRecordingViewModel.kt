package io.github.leonidius20.recorder.ui.editing.main.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.leonidius20.recorder.ui.editing.main.view.EditRecordingFragmentArgs
import javax.inject.Inject

@HiltViewModel
class EditRecordingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    //init {
        val args = EditRecordingFragmentArgs.fromSavedStateHandle(savedStateHandle)
        val uri = args.fileToEdit
        val fileName = args.fileName
   // }

}