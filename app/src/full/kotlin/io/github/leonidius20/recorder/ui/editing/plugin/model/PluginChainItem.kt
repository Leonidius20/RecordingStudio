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

    fun progressBarVisibility() =
        if (isConnected) View.GONE else View.VISIBLE

    fun pluginName() = info.displayName

    fun expandContractBtnIcon() = if (isExpanded)
        R.drawable.ic_contract else R.drawable.ic_expand

    fun paramsListVisibility() = if (isExpanded && isConnected)
        View.VISIBLE else View.GONE

}
