package io.github.leonidius20.recorder.data.plugins

import org.androidaudioplugin.PluginInformation

data class PluginModel(
    val id: String,
    val name: String,

    val allInfo: PluginInformation, // todo: maybe remove
)
