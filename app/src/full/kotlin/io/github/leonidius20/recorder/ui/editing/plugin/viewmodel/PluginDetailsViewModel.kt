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
import io.github.leonidius20.recorder.data.plugins.PluginModel
import io.github.leonidius20.recorder.data.plugins.PluginsRepository
import io.github.leonidius20.recorder.data.recordings_list.RecordingsListRepository
import io.github.leonidius20.recorder.data.settings.Container
import io.github.leonidius20.recorder.ui.editing.plugin.model.PluginChainItem
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

    // todo: expose flow of PluginChainItem-s. Also update parameter lists in them
    // when parameter is changed

    private val args = PluginDetailsFragmentArgs
        .fromSavedStateHandle(savedStateHandle)

    private val pluginId = args.pluginId

    private val fileName = args.fileName
    private val fileUri = args.fileUri

    private val client = AudioPluginClientBase(context)

    private val _uiState = MutableStateFlow<PluginDetailsState>(
        PluginDetailsState.Connecting)
    val uiState = _uiState.asStateFlow()

    private val _pluginChain = MutableStateFlow<List<PluginChainItem>>(emptyList())
    val pluginChain = _pluginChain.asStateFlow()

    private lateinit var descriptor: ParcelFileDescriptor
    private lateinit var cacheOutFile: File

    private lateinit var scope: PluginDetailsScope

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
            scope = PluginDetailsScope.create(plugin.allInfo, context, client,
                fileUri, fileName, descriptor.fd) // todo: should we use detachFd()?

            _uiState.value = PluginDetailsState.Connected(
                scope, plugin.allInfo
            )

            _pluginChain.value = pluginChain.value.toMutableList().apply {
                add(PluginChainItem(
                    isConnected = true,
                    isExpanded = true,
                    info = plugin.allInfo,
                    params = scope.getParameters(pluginIndex = 0).toList(),
                ))
            }
        }
    }

    fun changeParam(id: UInt, value: Float, pluginIndex: Int) {
        val state = uiState.value as PluginDetailsState.Connected
        state.scope.setParameterValue(id, value, pluginIndex)
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
            // todo: recreate and set new file descriptor
            //FileOutputStream(cacheOutFile, false).channel.apply {
            //    truncate(0)
            //    position(0)
            //}

            process(state)

            _uiState.value = state.copy(isProcessing = false, isFileReady = true)

            startPlayingFile()
        }
    }

    fun togglePluginExpandedState(pluginIndex: Int) {
        _pluginChain.value = pluginChain.value.toMutableList().apply {
            this[pluginIndex] = this[pluginIndex].copy(
                isExpanded = !this[pluginIndex].isExpanded
            )
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

    fun addPlugin(pluginInfo: PluginModel) = viewModelScope.launch {
        val state = uiState.value as PluginDetailsState.Connected
        val index = state.scope.addPlugin(pluginInfo.allInfo)

        _pluginChain.value = pluginChain.value.toMutableList().apply {
            add(PluginChainItem(
                isConnected = true,
                isExpanded = false,
                info = pluginInfo.allInfo,
                params = scope.getParameters(pluginIndex = index).toList(),
            ))
        }

        Toast.makeText(context, "added plugin ${pluginInfo.name} index $index" , Toast.LENGTH_SHORT).show()
    }

}
