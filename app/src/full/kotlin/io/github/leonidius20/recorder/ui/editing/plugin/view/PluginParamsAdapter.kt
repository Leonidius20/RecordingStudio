package io.github.leonidius20.recorder.ui.editing.plugin.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.github.leonidius20.recorder.R
import io.github.leonidius20.recorder.databinding.PluginParameterBinding
import org.androidaudioplugin.ParameterInformation

class PluginParamsAdapter(
    private val onParamChange: (paramId: UInt, newVal: Float) -> Unit,
) : RecyclerView.Adapter<PluginParamsAdapter.ViewHolder>() {

    private var parameters: List<ParameterInformation> = emptyList()

    inner class ViewHolder(
        private val binding: PluginParameterBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(param: ParameterInformation) {

            binding.slider.value = param.defaultValue.toFloat()

            binding.slider.addOnChangeListener { slider, newVal, fromUser ->
                if (fromUser) {
                    onParamChange(param.id.toUInt(), newVal)
                }
            }

            if (param.enumerations.isNotEmpty()) {
                val enums = (0 until param.enumerations.size).map { param.enumerations[it] }
                    .sortedBy { it.value }
                val minValue = enums.minBy { it.value }.value
                val maxValue = enums.maxBy { it.value }.value
                // a dirty hack to show the matching enum value from current value.
                // (cannot be "the nearest enum" as we round it.)
                // FIXME: we would need semantic definition for matching.
                val getMatchedEnum = { f:Float -> enums.lastOrNull { it.value <= f } ?: enums.first() }
                binding.slider.valueFrom = minValue.toFloat()
                binding.slider.valueTo = maxValue.toFloat()

                val paramValue = param.defaultValue // todo: ?? updating this??
                val en = getMatchedEnum(paramValue.toFloat())

                binding.title.text = if (en.name.length > 9) en.name.take(8) + ".." else en.name
                binding.root.setBackgroundColor(
                    binding.root.context.getColor(
                        R.color.md_theme_inversePrimary_mediumContrast))

            } else {
                binding.title.text = param.name
                binding.slider.valueFrom = param.minimumValue.toFloat()
                binding.slider.valueTo = param.maximumValue.toFloat()
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
