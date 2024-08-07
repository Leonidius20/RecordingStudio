package io.github.leonidius20.recorder.ui.recordings_list

import android.util.SparseBooleanArray
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.util.contains
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import io.github.leonidius20.recorder.R
import io.github.leonidius20.recorder.ui.common.breakIntoRangesDescending

/**
 * this adapter supports selecting multiple items, removing and (in future) changing their titles
 */
class RecordingsListAdapter(
    private val onItemClicked: (Int) -> Unit,
    private val onItemLongClicked: (Int) -> Unit,
): RecyclerView.Adapter<RecordingsListAdapter.ViewHolder>() {

    private var recordings = ArrayList<RecordingsListViewModel.RecordingUiModel>()

    private val selectedItems = SparseBooleanArray()

    fun setData(
        newData: ArrayList<RecordingsListViewModel.RecordingUiModel>
    ) {
        val callback = RecordingsDiffUtilCallback(
            oldList = recordings,
            newList = newData
        )
        val diff = DiffUtil.calculateDiff(callback)
        recordings = newData

        diff.dispatchUpdatesTo(this)
    }

    class ViewHolder(
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
            // we have to do it here because it doesn't work when we
            // add those listeners to the wrapper
            binding.root.setOnClickListener(holder)
            binding.root.setOnLongClickListener(holder)

            this.binding.recording = recordings[position]
            this.binding.root.isSelected = isSelected(position)

            if (isSelected(position)) {
                binding.leadingIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_selected))
               // binding.leadingIcon.setBackgroundColor(ContextCompat.getColor(context, R.color.md_theme_errorContainer_highContrast))
            } else {
                binding.leadingIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_microphone))
            }
        }
    }

    private fun isSelected(position: Int) = selectedItems.contains(position)

    fun toggleSelection(position: Int) {
        if (selectedItems.get(position, false)) {
            selectedItems.delete(position)
        } else {
            selectedItems.put(position, true)
        }
        notifyItemChanged(position)
    }

    fun getSelectedItemsPositions(): List<Int> {
        return (0 until selectedItems.size()).map {
            selectedItems.keyAt(it)
        }
    }

    fun clearAllSelection() {
        val selection = getSelectedItemsPositions()
        selectedItems.clear()
        selection.forEach { position -> notifyItemChanged(position) }
    }

    fun getSelectedItemsCount() = selectedItems.size()

    private fun removeItem(position: Int) {
        recordings.removeAt(position)
        notifyItemRemoved(position)
    }

    /**
     * this of course breaks the single source of truth (it should
     * be MediaStore but re-querying it after every deletion is potentially
     * too slow. todo however we can make an in-mem cache a SSOT. Start by populating it from mediastore, then after making recording add it to cached version. We can also ssve this data to disk and only rescan Mediastore of needed as per https://developer.android.com/training/data-storage/shared/media#check-for-updates. there we can use diffutil
     */
    fun removeItems(positions: List<Int>) {
        val ranges = breakIntoRangesDescending(positions)

        ranges.forEach { range ->
            if (range.size == 1) {
                removeItem(position = range.first())
            } else {
                removeRange(fromPosition = range.first(), count = range.size)
            }
        }
    }

    private fun removeRange(fromPosition: Int, count: Int) {
        // cut range out of list
        recordings.removeAtRange(fromPosition, count)

        notifyItemRangeRemoved(fromPosition, count)
    }

    private fun <T> MutableList<T>.removeAtRange(fromIndex: Int, count: Int) {
        this.removeAll(
            this.slice(fromIndex until fromIndex + count).toSet()
        )
    }

    fun replaceItemAt(position: Int, with: RecordingsListViewModel.RecordingUiModel) {
        recordings[position] = with
        notifyItemChanged(position)
    }

    val currentData
        get() = recordings

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
        return oldItem.uri == newItem.uri // uri is the unique identifier
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]
        return oldItem == newItem // here we compare all fields including name, duration
    }


}