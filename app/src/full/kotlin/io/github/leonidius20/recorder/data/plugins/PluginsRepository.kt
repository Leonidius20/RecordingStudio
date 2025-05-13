package io.github.leonidius20.recorder.data.plugins

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.androidaudioplugin.hosting.AudioPluginHostHelper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PluginsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    val list by lazy {
        AudioPluginHostHelper.queryAudioPluginServices(context).flatMap { it.plugins }.map {
            PluginModel(
                id = it.pluginId!!,
                name = it.displayName,
                allInfo = it,
            )
        }
    }

    fun getPluginDetails(id: String) = list.find { it.id == id }!!

}