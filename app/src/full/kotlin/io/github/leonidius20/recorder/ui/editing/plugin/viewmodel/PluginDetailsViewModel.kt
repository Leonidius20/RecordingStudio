package io.github.leonidius20.recorder.ui.editing.plugin.viewmodel

import android.content.Context
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.leonidius20.recorder.data.plugins.PluginsRepository
import io.github.leonidius20.recorder.data.recordings_list.RecordingsListRepository
import io.github.leonidius20.recorder.data.settings.Container
import io.github.leonidius20.recorder.ui.editing.plugin.model.PluginDetailsScope
import io.github.leonidius20.recorder.ui.editing.plugin.model.PluginDetailsState
import io.github.leonidius20.recorder.ui.editing.plugin.view.PluginDetailsFragmentArgs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.androidaudioplugin.hosting.AudioPluginClientBase
import javax.inject.Inject

@HiltViewModel
class PluginDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val pluginsRepository: PluginsRepository,
    @ApplicationContext private val context: Context,
    private val recordingsRepo: RecordingsListRepository,
) : ViewModel() {

    private val args = PluginDetailsFragmentArgs
        .fromSavedStateHandle(savedStateHandle)

    private val pluginId = args.pluginId

    private val fileName = args.fileName
    private val fileUri = args.fileUri

    private val client = AudioPluginClientBase(context)

    private val _uiState = MutableStateFlow<PluginDetailsState>(
        PluginDetailsState.Connecting)
    val uiState = _uiState.asStateFlow()

    private lateinit var descriptor: ParcelFileDescriptor

    init {
        viewModelScope.launch {
            val outFile = recordingsRepo.createRecordingFile(
                System.currentTimeMillis().toString(),
                Container.WAV.mimeType
            )

            descriptor = context.contentResolver.openFileDescriptor(
                outFile, "w"
            )!!

            val plugin = pluginsRepository.getPluginDetails(pluginId)
            val scope = PluginDetailsScope.create(plugin.allInfo, context, client,
                fileUri, fileName, descriptor.fd) // todo: should we use detachFd()?

            _uiState.value = PluginDetailsState.Connected(
                scope, plugin.allInfo
            )
        }
    }

    fun changeParam(id: UInt, value: Float) {
        val state = uiState.value as PluginDetailsState.Connected
        state.scope.setParameterValue(id, value)
    }

    fun toggleProcessing() {
        val state = uiState.value as PluginDetailsState.Connected
        if (state.scope.isProcessing) {
            state.scope.pauseProcessing()
            descriptor.close()
        } else {
            // should move this to BG thread. startProcessing()
            // processes the whole file on the main thead
            state.scope.playPreloadedAudio()
            state.scope.startProcessing()

            Toast.makeText(context, "Done (startProcessing() existed)", Toast.LENGTH_SHORT).show()
        }
    }

    fun playExample() {
        val state = uiState.value as PluginDetailsState.Connected
        state.scope.playPreloadedAudio()
    }

    /**
     * var isActivated by remember { mutableStateOf(false) }
     *         Button(onClick = {
     *             isActivated = !isActivated
     *             if (isActivated)
     *                 scope.startProcessing()
     *             else
     *                 scope.pauseProcessing()
     *         }) {
     *             Text(text = if (isActivated) "Pause" else "Start")
     *         }
     *         Button(onClick = { scope.playPreloadedAudio() }) {
     *             Text(text = "Play Audio")
     *         }
     */

}
