package io.github.leonidius20.recorder.ui.editing.plugin.viewmodel

import android.content.Context
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.core.net.toUri
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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.androidaudioplugin.hosting.AudioPluginClientBase
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class PluginDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val pluginsRepository: PluginsRepository,
    @ApplicationContext private val context: Context,
    private val recordingsRepo: RecordingsListRepository,
    @Named("cpu") private val defaultDispatcher: CoroutineDispatcher,
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
    private lateinit var cacheOutFile: File

    init {
        viewModelScope.launch {
            cacheOutFile = File(context.cacheDir, "effect-cache.wav")
            if (cacheOutFile.exists()) cacheOutFile.delete()
            cacheOutFile.createNewFile()

            val outFile = /*recordingsRepo.createRecordingFile(
                System.currentTimeMillis().toString(),
                Container.WAV.mimeType
            )*/ cacheOutFile.toUri()

            descriptor = context.contentResolver.openFileDescriptor(
                outFile, "rw"
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
        _uiState.value = state.copy(isFileReady = false)
    }

    fun toggleProcessing() = viewModelScope.launch {
        val state = uiState.value as PluginDetailsState.Connected
        //if (state.scope.isProcessing) {
        //    state.scope.pauseProcessing()
        //    descriptor.close()

        if (state.isFileReady) {
            startPlayingFile()
        } else {
            _uiState.value = state.copy(isProcessing = true)

            // clear out file
            FileOutputStream(cacheOutFile).close()

            process(state)

            _uiState.value = state.copy(isProcessing = false, isFileReady = true)

            startPlayingFile()
        }
    }

    private suspend fun process(state: PluginDetailsState.Connected) = withContext(defaultDispatcher) {
        state.scope.playPreloadedAudio()
        state.scope.startProcessing()
        state.scope.pauseProcessing()
    }

    private fun startPlayingFile() {
        // todo:
    }

    fun saveFile() {
        val outFile = recordingsRepo.createRecordingFile(
            System.currentTimeMillis().toString(),
            Container.WAV.mimeType
        )

        context.contentResolver.openOutputStream(outFile)!!.use { out ->
            cacheOutFile.inputStream().use { input ->
                input.copyTo(out)
            }
        }

        Toast.makeText(context, "Save success", Toast.LENGTH_SHORT).show()
        // descriptor.close()
    }

    override fun onCleared() {
        super.onCleared()
        try {
            descriptor.close()
        } catch (e: IOException) {
            Timber.e(e, "Failed to close descriptor when clearing vm")
        }
    }

}
