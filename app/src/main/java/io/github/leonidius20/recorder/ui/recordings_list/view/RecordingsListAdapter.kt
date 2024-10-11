package io.github.leonidius20.recorder.ui.recordings_list.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.github.leonidius20.recorder.R
import io.github.leonidius20.recorder.ui.recordings_list.viewmodel.RecordingsListViewModel.RecordingUiModel

/**
 * this adapter supports selecting multiple items, removing and (in future) changing their titles
 */
class RecordingsListAdapter(
    private val context: Context,
    private val onItemClicked: (Int) -> Unit,
    private val onItemLongClicked: (Int) -> Unit,
) : ListAdapter<RecordingUiModel, RecordingsListAdapter.ViewHolder>(
    RecordingsDiffUtilCallback()
) {

    /**
     * for when the element is neither selected nor is playing right now
     */
    private val regularIcon = ContextCompat.getDrawable(context, R.drawable.ic_microphone)
    private val playingIcon = ContextCompat.getDrawable(context, R.drawable.ic_audio_playing)
    private val selectedIcon = ContextCompat.getDrawable(context, R.drawable.ic_selected)

    // private val selectedItems = SparseBooleanArray()

    /**
     * item that is currently playing and should be marked appropriately
     */
    private var playingItem: Int? = null // todo: save in viewmodel

    fun setData(newData: ArrayList<RecordingUiModel>) {
        submitList(newData)
    }

    inner class ViewHolder(
        val root: RecordingListItemWrapper,
        val onItemClicked: (Int) -> Unit,
        val onItemLongClicked: (Int) -> Unit,
    ) : RecyclerView.ViewHolder(root), View.OnClickListener, View.OnLongClickListener {

        override fun onClick(v: View) {
            onItemClicked(position)
        }

        override fun onLongClick(v: View): Boolean {
            onItemLongClicked(position)
            return true
        }

        internal fun updateName(newName: String) {
            root.invokeWhenInflated {
                binding.txtHeadline.text = newName
            }
        }

        internal fun updateSelection(isSelected: Boolean) {
            root.invokeWhenInflated {
                binding.leadingIcon.setImageDrawable(
                    if (isSelected)
                        selectedIcon
                    else if (playingItem == position)
                        playingIcon
                    else
                        regularIcon
                )
                binding.root.isSelected = isSelected
            }
        }

        fun updatePlaybackStatus(isSelected: Boolean, isPlaying: Boolean) {
            root.invokeWhenInflated {
                binding.txtHeadline.setTextColor(
                    resources.getColor(
                        if (isPlaying) R.color.md_theme_primary
                        else R.color.md_theme_onSurface,
                    )
                )

                binding.leadingIcon.setImageDrawable(
                    if (isPlaying) playingIcon
                    else if (isSelected) selectedIcon
                    else regularIcon,
                )

                binding.leadingIcon.drawable.setTint(
                    resources.getColor(
                        if (isPlaying) R.color.md_theme_primary
                        else R.color.md_theme_onSurfaceVariant,
                    )
                )
            }
        }

        /*fun bind(recording: RecordingsListViewModel.RecordingUiModel) {
            binding.recording = recording
            binding.executePendingBindings()
        }*/

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val root = RecordingListItemWrapper(parent.context)
        root.inflateAsync(R.layout.recording_list_item2)
        return ViewHolder(root, onItemClicked, onItemLongClicked)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.root.invokeWhenInflated {
            val recording = getItem(position)
            // we have to do it here because it doesn't work when we
            // add those listeners to the wrapper
            binding.root.setOnClickListener(holder)
            binding.root.setOnLongClickListener(holder)

            this.binding.recording = recording
            this.binding.root.isSelected = recording.isSelected

            if (recording.isSelected) {
                binding.leadingIcon.setImageDrawable(selectedIcon)
                // binding.leadingIcon.setBackgroundColor(ContextCompat.getColor(context, R.color.md_theme_errorContainer_highContrast))
            } else {
                binding.leadingIcon.setImageDrawable(regularIcon)
            }

            // todo: optimize update...() or bind...() functions
            holder.updatePlaybackStatus(
                isPlaying = position == playingItem,
                isSelected = recording.isSelected
            )
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            return onBindViewHolder(holder, position)
        }

        payloads.forEach { payload ->
            when (payload) {
                is RecordingChangePayload.Name -> {
                    holder.updateName(payload.newName)
                }

                is RecordingChangePayload.Selection -> {
                    holder.updateSelection(payload.isSelected)
                }

                is RecordingChangePayload.Playback -> {
                    holder.updatePlaybackStatus(
                        isPlaying = payload.isPlaying,
                        isSelected = getItem(position).isSelected
                    )
                }
            }
        }
    }

    fun setPlaying(position: Int) {
        if (position == playingItem) return

        resetPlayingItemHighlighting()

        playingItem = position

        notifyItemChanged(position, RecordingChangePayload.Playback(true))
    }

    fun resetPlayingItemHighlighting() {
        if (playingItem != null) {
            notifyItemChanged(playingItem!!, RecordingChangePayload.Playback(false))
            playingItem = null
        }
    }

}

class RecordingsDiffUtilCallback : DiffUtil.ItemCallback<RecordingUiModel>() {

    override fun areItemsTheSame(oldItem: RecordingUiModel, newItem: RecordingUiModel): Boolean {
        return oldItem.id == newItem.id // uri is the unique identifier
    }

    override fun areContentsTheSame(oldItem: RecordingUiModel, newItem: RecordingUiModel): Boolean {
        return oldItem == newItem // here we compare all fields including name, duration
    }

    override fun getChangePayload(oldItem: RecordingUiModel, newItem: RecordingUiModel): Any? {
        if (oldItem.name != newItem.name) {
            return RecordingChangePayload.Name(newItem.name)
        }

        if (oldItem.isSelected != newItem.isSelected) {
            return RecordingChangePayload.Selection(newItem.isSelected)
        }

        if (oldItem.isPlaying != newItem.isPlaying) {
            return RecordingChangePayload.Playback(newItem.isPlaying)
        }

        return super.getChangePayload(oldItem, newItem)
    }

}

private sealed interface RecordingChangePayload {

    data class Name(val newName: String) : RecordingChangePayload

    data class Selection(val isSelected: Boolean) : RecordingChangePayload

    data class Playback(val isPlaying: Boolean) : RecordingChangePayload

}