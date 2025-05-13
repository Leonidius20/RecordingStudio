package io.github.leonidius20.recorder.ui.editing.plugin.model

import org.androidaudioplugin.PluginInformation
import org.androidaudioplugin.hosting.PluginServiceConnection

sealed interface PluginDetailsState {

    data object Connecting : PluginDetailsState

    data class Connected(
        val connection: PluginServiceConnection,
        val info: PluginInformation,
    ) : PluginDetailsState

}