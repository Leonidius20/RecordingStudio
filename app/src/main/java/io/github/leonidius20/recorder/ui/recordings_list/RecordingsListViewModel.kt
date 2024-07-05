package io.github.leonidius20.recorder.ui.recordings_list

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.leonidius20.recorder.data.recordings_list.RecordingsListRepository
import javax.inject.Inject

@HiltViewModel
class RecordingsListViewModel @Inject constructor(
    private val repository: RecordingsListRepository,
) : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is dashboard Fragment"
    }
    val text: LiveData<String> = _text
}