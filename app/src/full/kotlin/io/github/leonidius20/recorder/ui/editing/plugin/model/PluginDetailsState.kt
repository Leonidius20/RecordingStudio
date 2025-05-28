package io.github.leonidius20.recorder.ui.editing.plugin.model

import android.view.View
import org.androidaudioplugin.PluginInformation

sealed interface PluginDetailsState {

    data object Connecting : PluginDetailsState

    data class Connected(
       // val connection: PluginServiceConnection,
        val scope: PluginDetailsScope,
        val info: PluginInformation,
        /**
         * Processing in progress. Lock UI
         */
        val isProcessing: Boolean = false,
        /**
         * true if a processed file has been generated and there have been
         * no plugin parameter changes since
         **/
        val isFileReady: Boolean = false,
    ) : PluginDetailsState

    // todo: move to some UiState class that is generated from state
    fun playBtnVisibility() =
        if (this is Connected && !this.isProcessing)
            View.VISIBLE else View.GONE

    fun saveBtnVisibility() =
        if (this is Connected && this.isFileReady)
            View.VISIBLE else View.GONE

}
