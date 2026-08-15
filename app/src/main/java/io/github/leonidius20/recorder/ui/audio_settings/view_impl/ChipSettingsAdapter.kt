package io.github.leonidius20.recorder.ui.audio_settings.view_impl

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import io.github.leonidius20.recorder.ui.common.ifDifferentFrom

// todo: rename into checkable setting or something,
//  lift from here. so as not to tie to UI logically
interface ChipSetting<T> {
    val id: Int

    val isSelected: Boolean

    // todo: create a mapper to get R.string value for this
    val displayName: String

    val option: T
}

// todo: T: ChipSetting
class ChipSettingsAdapter<T>(
    private val onClick: (option: T) -> Unit,
) : ListAdapter<ChipSetting<T>, ChipSettingsAdapter<T>.ViewHolder>(
    ChipSettingsDiffUtilCallback()
) {

    inner class ViewHolder(
        private val chip: Chip,
    ) : RecyclerView.ViewHolder(chip) {

        init {
            chip.apply {
                id = View.generateViewId() // todo: is it needed?
                isCheckedIconVisible = true
                isCheckable = true
                isClickable = true
            }
        }

        fun bind(item: ChipSetting<T>) {
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
            chip.isChecked = isSelected
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
         // todo inflate xml with margin end 8dp
        return ViewHolder(Chip(parent.context))
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


    class ChipSettingsDiffUtilCallback<T> : DiffUtil.ItemCallback<ChipSetting<T>>() {

        override fun areItemsTheSame(oldItem: ChipSetting<T>, newItem: ChipSetting<T>): Boolean {
            return oldItem.id == newItem.id // uri is the unique identifier
        }

        override fun areContentsTheSame(oldItem: ChipSetting<T>, newItem: ChipSetting<T>): Boolean {
            return oldItem.displayName == newItem.displayName &&
                    oldItem.isSelected == newItem.isSelected
        }

        override fun getChangePayload(oldItem: ChipSetting<T>, newItem: ChipSetting<T>): Any {
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
