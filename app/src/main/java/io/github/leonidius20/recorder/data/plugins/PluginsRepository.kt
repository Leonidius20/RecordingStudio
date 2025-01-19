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

    fun getList() =
        AudioPluginHostHelper.queryAudioPluginServices(context).flatMap { it.plugins }.map {
            PluginModel(
                name = it.displayName
            )
        }

}