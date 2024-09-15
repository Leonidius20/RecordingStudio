package io.github.leonidius20.recorder.ui.recordings_list.view

import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import io.github.leonidius20.recorder.R
import io.github.leonidius20.recorder.ui.recordings_list.viewmodel.RecordingsListViewModel
import io.github.leonidius20.recorder.ui.recordings_list.viewmodel.RecordingsListViewModel.RecordingUiModel

/**
 * this adapter supports selecting multiple items, removing and (in future) changing their titles
 */
class RecordingsListAdapter(
    private val onItemClicked: (Int) -> Unit,
    private val onItemLongClicked: (Int) -> Unit,
): RecyclerView.Adapter<RecordingsListAdapter.ViewHolder>() {

    // todo: replace with viewModel.recordings livedata?
    private var recordings = ArrayList<RecordingUiModel>()

    // private val selectedItems = SparseBooleanArray()

    /**
     * item that is currently playing and should be marked appropriately
     */
    private var playingItem: Int? = null // todo: save in viewmodel

    fun setData(
        newData: ArrayList<RecordingUiModel>
    ) {
        val callback = RecordingsDiffUtilCallback(
            oldList = recordings,
            newList = newData
        )
        val diff = DiffUtil.calculateDiff(callback)
        recordings = newData

        diff.dispatchUpdatesTo(this)
    }

    inner class ViewHolder(
        val root: RecordingListItemWrapper,
        val onItemClicked: (Int) -> Unit,
        val onItemLongClicked: (Int) -> Unit,
    ): RecyclerView.ViewHolder(root), View.OnClickListener, View.OnLongClickListener {

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
                binding.leadingIcon.setImageResource(
                    if (isSelected)
                        R.drawable.ic_selected
                    else if (playingItem == position)
                        R.drawable.ic_audio_playing
                    else
                        R.drawable.ic_microphone
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

                binding.leadingIcon.setImageResource(
                    if (isPlaying) R.drawable.ic_audio_playing
                    else if (isSelected) R.drawable.ic_selected
                    else R.drawable.ic_microphone,
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

    override fun getItemCount() = recordings.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.root.invokeWhenInflated {
            val recording = recordings[position]
            // we have to do it here because it doesn't work when we
            // add those listeners to the wrapper
            binding.root.setOnClickListener(holder)
            binding.root.setOnLongClickListener(holder)

            this.binding.recording = recording
            this.binding.root.isSelected = recording.isSelected

            if (recording.isSelected) {
                binding.leadingIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_selected))
               // binding.leadingIcon.setBackgroundColor(ContextCompat.getColor(context, R.color.md_theme_errorContainer_highContrast))
            } else {
                binding.leadingIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_microphone))
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
            when(payload) {
                is RecordingChangePayload.Name -> {
                    holder.updateName(payload.newName)
                }
                is RecordingChangePayload.Selection -> {
                    holder.updateSelection(payload.isSelected)
                }
                is RecordingChangePayload.Playback -> {
                    holder.updatePlaybackStatus(
                        isPlaying = payload.isPlaying,
                        isSelected = recordings[position].isSelected
                    )
                }
            }
        }
    }

    // private fun isSelected(position: Int) = selectedItems.contains(position)

    /*fun toggleSelection(position: Int) {
        val wasSelected = selectedItems.get(position, false)

        if (wasSelected) {
            selectedItems.delete(position)
        } else {
            selectedItems.put(position, true)
        }
        notifyItemChanged(position, RecordingChangePayload.Selection(!wasSelected))
        // todo: checked vs unchecked icon (as well as Playing icon in future)
        // can be changed using payloads
        // same for recording name
    }

    fun getSelectedItemsPositions(): List<Int> {
        return (0 until selectedItems.size()).map {
            selectedItems.keyAt(it)
        }
    }

    fun clearAllSelection() {
        val selection = getSelectedItemsPositions()
        selectedItems.clear()
        selection.forEach { position -> notifyItemChanged(position, RecordingChangePayload.Selection(false)) }
    }

    fun getSelectedItemsCount() = selectedItems.size() */

    private fun removeItem(position: Int) {
        recordings.removeAt(position)
        notifyItemRemoved(position)
    }

    /**
     * this of course breaks the single source of truth (it should
     * be MediaStore but re-querying it after every deletion is potentially
     * too slow. todo however we can make an in-mem cache a SSOT. Start by populating it from mediastore, then after making recording add it to cached version. We can also ssve this data to disk and only rescan Mediastore of needed as per https://developer.android.com/training/data-storage/shared/media#check-for-updates. there we can use diffutil
     */
    /*fun removeItems(positions: List<Int>) {
        val ranges = breakIntoRangesDescending(positions)

        ranges.forEach { range ->
            if (range.size == 1) {
                removeItem(position = range.first())
            } else {
                removeRange(fromPosition = range.first(), count = range.size)
            }
        }
    }*/

    /*private fun removeRange(fromPosition: Int, count: Int) {
        // cut range out of list
        recordings.removeAtRange(fromPosition, count)

        notifyItemRangeRemoved(fromPosition, count)
    }*/

    /*private fun <T> MutableList<T>.removeAtRange(fromIndex: Int, count: Int) {
        this.removeAll(
            this.slice(fromIndex until fromIndex + count).toSet()
        )
    }

    fun replaceItemAt(position: Int, with: RecordingsListViewModel.RecordingUiModel) {
        recordings[position] = with
        notifyItemChanged(position)
    }

    fun renameItemAt(position: Int, newName: String) {
        recordings[position] = recordings[position].copy(name = newName)
        notifyItemChanged(position, RecordingChangePayload.Name(newName))
    }*/

    val currentData
        get() = recordings

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

class RecordingsDiffUtilCallback(
    private val oldList: ArrayList<RecordingsListViewModel.RecordingUiModel>,
    private val newList: ArrayList<RecordingsListViewModel.RecordingUiModel>,
): DiffUtil.Callback() {

    override fun getOldListSize() = oldList.size

    override fun getNewListSize() = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]
        Log.d("adapter", "are items the same ${oldItem.id == newItem.id}")
        return oldItem.id == newItem.id // uri is the unique identifier
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]
        Log.d("adapter", "are contents the same ${oldItem == newItem}")
        return oldItem == newItem // here we compare all fields including name, duration
    }

    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]

        Log.d("adapter", "getting change payload")

        if (oldItem.name != newItem.name) {
            return RecordingChangePayload.Name(newItem.name)
        }

        if (oldItem.isSelected != newItem.isSelected) {
            Log.d("adapter", "selection change payload")
            return RecordingChangePayload.Selection(newItem.isSelected)
        }

        if (oldItem.isPlaying != newItem.isPlaying) {
            return RecordingChangePayload.Playback(newItem.isPlaying)
        }

        return super.getChangePayload(oldItemPosition, newItemPosition)
    }

}

private sealed interface RecordingChangePayload {

    data class Name(val newName: String): RecordingChangePayload

    data class Selection(val isSelected: Boolean): RecordingChangePayload

    data class Playback(val isPlaying: Boolean): RecordingChangePayload

}