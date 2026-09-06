package io.github.leonidius20.recorder.ui.audio_settings.view_impl

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import io.github.leonidius20.recorder.R
import io.github.leonidius20.recorder.audio_config.domain.impl.options.DiscreteAudioSetting
import io.github.leonidius20.recorder.ui.common.ifDifferentFrom

// todo: T: ChipSetting
class ChipSettingsAdapter<T>(
    private val onClick: (option: T) -> Unit,
) : ListAdapter<DiscreteAudioSetting<T>, ChipSettingsAdapter<T>.ViewHolder>(
    ChipSettingsDiffUtilCallback()
) {

    init {
        setHasStableIds(true)
    }

    inner class ViewHolder(
        private val chip: Chip,
    ) : RecyclerView.ViewHolder(chip), CompoundButton.OnCheckedChangeListener {

        init {
            chip.apply {
                id = View.generateViewId() // todo: is it needed?
                isCheckedIconVisible = true
                isCheckable = true
                isClickable = true
                setOnCheckedChangeListener(this@ViewHolder)
            }
        }

        override fun onCheckedChanged(chip: CompoundButton, checked: Boolean) {
            // make sure you cannot de-select a chip by clicking it
            // (simulate ChipGroup with selection required)
            if (!checked) {
                chip.apply {
                    setOnCheckedChangeListener(null)
                    chip.isChecked = true
                    setOnCheckedChangeListener(this@ViewHolder)
                }
            }
        }

        fun bind(item: DiscreteAudioSetting<T>) {
            updateTitle(item.displayName)
            updateSelectionState(item.isSelected)
            chip.setOnClickListener {
                onClick(item.option)
            }
        }

        fun updateTitle(newTitle: String) {
            chip.text = newTitle
        }

        fun updateSelectionState(isSelected: Boolean) {
            chip.apply {
                setOnCheckedChangeListener(null)
                chip.isChecked = isSelected
                setOnCheckedChangeListener(this@ViewHolder)
            }
        }

    }

    override fun getItemId(position: Int): Long {
        val item = getItem(position)
        return item.id.toLong()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.chip_setting, parent, false)
                    as Chip
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: List<Any>) {
        if (payloads.isEmpty()) {
            return onBindViewHolder(holder, position)
        }

        payloads.mapNotNull { it as? ChipSettingChangePayload }.forEach { payload ->
            payload.newName?.let { holder.updateTitle(it) }

            payload.newIsSelected?.let { holder.updateSelectionState(it) }
        }
    }


    class ChipSettingsDiffUtilCallback<T> : DiffUtil.ItemCallback<DiscreteAudioSetting<T>>() {

        override fun areItemsTheSame(oldItem: DiscreteAudioSetting<T>, newItem: DiscreteAudioSetting<T>): Boolean {
            return oldItem.id == newItem.id // uri is the unique identifier
        }

        override fun areContentsTheSame(oldItem: DiscreteAudioSetting<T>, newItem: DiscreteAudioSetting<T>): Boolean {
            return oldItem.displayName == newItem.displayName &&
                    oldItem.isSelected == newItem.isSelected
        }

        override fun getChangePayload(oldItem: DiscreteAudioSetting<T>, newItem: DiscreteAudioSetting<T>): Any {
            return ChipSettingChangePayload(
                newName = newItem.displayName.ifDifferentFrom(oldItem.displayName),
                newIsSelected = newItem.isSelected.ifDifferentFrom(oldItem.isSelected),
            )
        }

    }

    private data class ChipSettingChangePayload(
        val newName: String? = null,
        val newIsSelected: Boolean? = null,
    )

}
