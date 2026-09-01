package io.github.leonidius20.recorder.ui.editing.plugin.model

import org.androidaudioplugin.PluginInformation
import android.view.View
import io.github.leonidius20.recorder.R
import org.androidaudioplugin.ParameterInformation

data class PluginChainItem(
    val isExpanded: Boolean = false,
    val isConnected: Boolean = false,
    val info: PluginInformation,
    val params: List<ParameterInformation>,
) {

    fun progressBarVisibility() = !isConnected

    fun pluginName() = info.displayName

    fun expandContractBtnIcon() = if (isExpanded)
        R.drawable.ic_contract else R.drawable.ic_expand

    // todo: re-enable expaand-contract functionality
    fun paramsListVisibility() = true

}
