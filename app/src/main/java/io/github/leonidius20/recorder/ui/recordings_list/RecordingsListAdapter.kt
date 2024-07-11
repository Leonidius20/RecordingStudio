package io.github.leonidius20.recorder.ui.recordings_list

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.github.leonidius20.recorder.R

class RecordingsListAdapter(
    private val recordings: Array<RecordingsListViewModel.RecordingUiModel>
): RecyclerView.Adapter<RecordingsListAdapter.ViewHolder>() {

    class ViewHolder(
        val root: RecordingListItemWrapper,
    ): RecyclerView.ViewHolder(root) {

        /*fun bind(recording: RecordingsListViewModel.RecordingUiModel) {
            binding.recording = recording
            binding.executePendingBindings()
        }*/

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val root = RecordingListItemWrapper(parent.context)
        root.inflateAsync(R.layout.recording_list_item2)
        return ViewHolder(root)
    }

    override fun getItemCount() = recordings.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.root.invokeWhenInflated {
            this.binding.recording = recordings[position]
        }
    }

}