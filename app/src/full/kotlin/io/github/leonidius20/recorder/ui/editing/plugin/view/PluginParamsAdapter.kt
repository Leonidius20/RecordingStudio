package io.github.leonidius20.recorder.ui.editing.plugin.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.github.leonidius20.recorder.databinding.PluginParameterBinding
import org.androidaudioplugin.ParameterInformation

class PluginParamsAdapter() : RecyclerView.Adapter<PluginParamsAdapter.ViewHolder>() {

    private var parameters: List<ParameterInformation> = emptyList()

    class ViewHolder(
        private val binding: PluginParameterBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(param: ParameterInformation) {
            binding.title.text = param.name
            if (param.enumerations.isNotEmpty()) {

            } else {

            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(PluginParameterBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        ))
    }

    override fun getItemCount() = parameters.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(parameters[position])
    }

    fun submitList(params: List<ParameterInformation>) {
        this.parameters = params
        notifyDataSetChanged()
    }

}
