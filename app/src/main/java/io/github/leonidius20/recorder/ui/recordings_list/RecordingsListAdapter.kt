package io.github.leonidius20.recorder.ui.recordings_list

import android.graphics.Color
import android.util.SparseBooleanArray
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.util.contains
import androidx.recyclerview.widget.RecyclerView
import io.github.leonidius20.recorder.R

class RecordingsListAdapter(
    private val recordings: Array<RecordingsListViewModel.RecordingUiModel>,
    private val onItemClicked: (Int) -> Unit,
    private val onItemLongClicked: (Int) -> Unit,
): RecyclerView.Adapter<RecordingsListAdapter.ViewHolder>() {

    val selectedItems = SparseBooleanArray(recordings.size)

    class ViewHolder(
        val root: RecordingListItemWrapper,
        val onItemClicked: (Int) -> Unit,
        val onItemLongClicked: (Int) -> Unit,
    ): RecyclerView.ViewHolder(root), View.OnClickListener, View.OnLongClickListener {

        override fun onClick(v: View) {
            Toast.makeText(v.context, "short click position ${position}", Toast.LENGTH_SHORT).show()
            onItemClicked(position)
        }

        override fun onLongClick(v: View): Boolean {
            onItemLongClicked(position)
            Toast.makeText(v.context, "long click position ${position}", Toast.LENGTH_SHORT).show()
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
                binding.txtHeadline.setTextColor(Color.RED)
            } else binding.txtHeadline.setTextColor(Color.BLACK) // todo: proper highlighting
        }
    }

    fun isSelected(position: Int) = selectedItems.contains(position)

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

}