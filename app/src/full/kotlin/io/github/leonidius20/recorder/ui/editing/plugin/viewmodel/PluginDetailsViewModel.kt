package io.github.leonidius20.recorder.ui.editing.plugin.viewmodel

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.leonidius20.recorder.data.plugins.PluginsRepository
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
) : ViewModel() {

    private val pluginId = PluginDetailsFragmentArgs
        .fromSavedStateHandle(savedStateHandle).pluginId

    private val client = AudioPluginClientBase(context)

    private val _uiState = MutableStateFlow<PluginDetailsState>(
        PluginDetailsState.Connecting)
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val plugin = pluginsRepository.getPluginDetails(pluginId)
            val connection = client.connectToPluginService(plugin.allInfo.packageName)
            _uiState.value = PluginDetailsState.Connected(
                connection, plugin.allInfo
            )
            // todo: show the plugin ui? or at least ports
        }
    }

}