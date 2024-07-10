package io.github.leonidius20.recorder.ui.recordings_list

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.github.leonidius20.recorder.databinding.RecordingListItemBinding

class RecordingsListAdapter(
    private val recordings: Array<RecordingsListViewModel.RecordingUiModel>
): RecyclerView.Adapter<RecordingsListAdapter.ViewHolder>() {

    class ViewHolder(
        private val binding: RecordingListItemBinding
    ): RecyclerView.ViewHolder(binding.root) {

        fun bind(recording: RecordingsListViewModel.RecordingUiModel) {
            binding.recording = recording
            binding.executePendingBindings()
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = RecordingListItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent, false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount() = recordings.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(recordings[position])
    }

}