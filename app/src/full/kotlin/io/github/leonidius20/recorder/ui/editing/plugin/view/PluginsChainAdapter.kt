package io.github.leonidius20.recorder.ui.editing.plugin.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import io.github.leonidius20.recorder.databinding.PluginChainItemBinding
import io.github.leonidius20.recorder.ui.editing.plugin.model.PluginChainItem

class PluginsChainAdapter(
    private val toggleParamsVisibility: (pluginIndex: Int) -> Unit,
    private val changePluginParam:
        (pluginIndex: Int, paramIndex: UInt, newValue: Float) -> Unit,
) : RecyclerView.Adapter<PluginsChainAdapter.ViewHolder>() {

    private var chain = emptyList<PluginChainItem>()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ) = ViewHolder(
        PluginChainItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
    )

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        holder.bind(chain[position], position)
    }

    override fun getItemCount() = chain.size

    fun submitList(newChain: List<PluginChainItem>, view: RecyclerView) {
        chain = newChain
        view.post {
            notifyDataSetChanged()
        }
    }

    inner class ViewHolder(
        private val binding: PluginChainItemBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PluginChainItem, position: Int) {
            binding.pluginName.text = item.pluginName()
            binding.expandOrClosePluginParams.setImageResource(
                item.expandContractBtnIcon()
            )
            binding.progressCircle.isVisible = item.progressBarVisibility()
            binding.pluginParamsList.isVisible = item.paramsListVisibility()

            binding.expandOrClosePluginParams.setOnClickListener {
                toggleParamsVisibility(position)
            }
            val paramsAdapter = PluginParamsAdapter { paramIndex, newValue ->
                changePluginParam(position, paramIndex, newValue)
            }
            paramsAdapter.submitList(item.params)
            binding.pluginParamsList.adapter = paramsAdapter
        }

    }

}
