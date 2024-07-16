package io.github.leonidius20.recorder.ui.recordings_list

import android.content.res.Resources
import android.graphics.Color
import android.util.SparseBooleanArray
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.util.contains
import androidx.core.view.ViewCompat
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

            /*binding.root.backgroundTint (ContextCompat.getColor(context,
                if (isSelected(position)) {
                    R.color.md_theme_secondaryContainer
                } else android.R.color.black,
            )) // todo: proper highlighting*/

            if (isSelected(position)) {
                binding.leadingIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_selected))
               // binding.leadingIcon.setBackgroundColor(ContextCompat.getColor(context, R.color.md_theme_errorContainer_highContrast))
            } else {
                binding.leadingIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_microphone))
            }
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