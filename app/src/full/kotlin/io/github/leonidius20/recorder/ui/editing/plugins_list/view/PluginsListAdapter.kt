package io.github.leonidius20.recorder.ui.editing.plugins_list.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.github.leonidius20.recorder.data.plugins.PluginModel
import io.github.leonidius20.recorder.databinding.PluginListItemBinding

class PluginsListAdapter(
    private val onItemClick: (PluginModel) -> Unit,
) : ListAdapter<PluginModel, PluginsListAdapter.ViewHolder>(
    DiffCallback()
) {

    inner class ViewHolder(
        private val binding: PluginListItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(plugin: PluginModel) {
            binding.plugin = plugin
            binding.root.setOnClickListener {
                onItemClick(plugin)
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(PluginListItemBinding
            .inflate(LayoutInflater.from(parent.context)))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    private class DiffCallback : DiffUtil.ItemCallback<PluginModel>() {

        override fun areItemsTheSame(oldItem: PluginModel, newItem: PluginModel) =
            oldItem == newItem // todo: change to comparing some ID

        override fun areContentsTheSame(oldItem: PluginModel, newItem: PluginModel) =
            oldItem == newItem

    }

}