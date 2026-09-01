package io.github.leonidius20.recorder.ui.recordings_list.view

import android.content.Context
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.github.leonidius20.recorder.R
import io.github.leonidius20.recorder.ui.common.ifDifferentFrom

/**
 * this adapter supports selecting multiple items, removing and
 * changing their titles
 */
class RecordingsListAdapter(
    context: Context,
    private val onItemClicked: (id: Long) -> Unit,
    private val onItemLongClicked: (id: Long) -> Unit,
) : ListAdapter<RecordingUiModel, RecordingsListAdapter.ViewHolder>(
    RecordingsDiffUtilCallback()
) {

    /**
     * for when the element is neither selected nor is playing right now
     */
    private val regularIcon = ContextCompat.getDrawable(context, R.drawable.ic_microphone)
    private val playingIcon = ContextCompat.getDrawable(context, R.drawable.ic_audio_playing)
    private val selectedIcon = ContextCompat.getDrawable(context, R.drawable.ic_selected)

    fun setData(newData: ArrayList<RecordingUiModel>) {
        submitList(newData)
    }

    inner class ViewHolder(
        val root: RecordingListItemWrapper,
    ) : RecyclerView.ViewHolder(root) {

        private var isRecordSelected = false
        private var isRecordPlaying = false

        internal fun updateName(newName: String) {
            root.invokeWhenInflated {
                binding.txtHeadline.text = newName
            }
        }

        internal fun updateSelection(
            isSelected: Boolean
        ) {
            isRecordSelected = isSelected

            updateLeadIcon()
            updateViewSelection()
        }

        fun updatePlaybackStatus(isPlaying: Boolean) {
            isRecordPlaying = isPlaying

            updateTitleColor()
            updateLeadIcon()
            updateLeadIconTint()
        }

        fun updateDuration(newDuration: String) = root.invokeWhenInflated {
            binding.durationText.text = newDuration
        }

        fun updateSize(newSize: String) = root.invokeWhenInflated {
            binding.sizeText.text = newSize
        }

        // depends on selection state
        private fun updateViewSelection() = root.invokeWhenInflated {
            binding.root.isSelected = isRecordSelected
        }

        // depends on playback status
        private fun updateTitleColor() = root.invokeWhenInflated {
            binding.txtHeadline.setTextColor(
                ContextCompat.getColor(context,
                    if (isRecordPlaying) R.color.md_theme_primary
                    else R.color.md_theme_onSurface,
                )
            )
        }

        // depends on playback and selection status
        private fun updateLeadIcon() = root.invokeWhenInflated {
            binding.leadingIcon.setImageDrawable(
                if (isRecordPlaying) playingIcon
                else if (isRecordSelected) selectedIcon
                else regularIcon,
            )
        }

        // depends on playback status
        private fun updateLeadIconTint() = root.invokeWhenInflated {
            binding.leadingIcon.drawable.setTint(
                ContextCompat.getColor(context,
                    if (isRecordPlaying) R.color.md_theme_primary
                    else R.color.md_theme_onSurfaceVariant,
                )
            )
        }

        fun bind(recording: RecordingUiModel) {
            root.invokeWhenInflated {
                binding.root.setOnClickListener {
                    onItemClicked(recording.id)
                }
                binding.root.setOnLongClickListener {
                    onItemLongClicked(recording.id)
                    true
                }

                updateName(recording.name)
                updateSize(recording.size)
                updateDuration(recording.duration)
                this.binding.root.isSelected = recording.isSelected

                isRecordSelected = recording.isSelected
                isRecordPlaying = recording.isPlaying

                updateViewSelection()
                updateTitleColor()
                updateLeadIcon()
                updateLeadIconTint()
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val root = RecordingListItemWrapper(parent.context)
        root.inflateAsync(R.layout.recording_list_item)
        return ViewHolder(root)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val recording = getItem(position)
        holder.bind(recording)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            return onBindViewHolder(holder, position)
        }

        payloads.forEach { payload ->
            (payload as? RecordingChangePayload)?.let { payload ->
                payload.newName?.let {
                    holder.updateName(it)
                }

                payload.newIsSelected?.let {
                    holder.updateSelection(it)
                }

                payload.newIsPlaying?.let {
                    holder.updatePlaybackStatus(it)
                }

                payload.newSize?.let {
                    holder.updateSize(it)
                }

                payload.newDuration?.let {
                    holder.updateDuration(it)
                }
            }
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

    override fun getChangePayload(oldItem: RecordingUiModel, newItem: RecordingUiModel): Any {
        return RecordingChangePayload(
            newName = newItem.name.ifDifferentFrom(oldItem.name),
            newIsSelected = newItem.isSelected.ifDifferentFrom(oldItem.isSelected),
            newIsPlaying = newItem.isPlaying.ifDifferentFrom(oldItem.isPlaying),
            newSize = newItem.size.ifDifferentFrom(oldItem.size),
            newDuration = newItem.duration.ifDifferentFrom(oldItem.duration),
        )
    }

}

private data class RecordingChangePayload(
    val newName: String? = null,
    val newIsSelected: Boolean? = null,
    val newIsPlaying: Boolean? = null,
    val newSize: String? = null,
    val newDuration: String? = null,
)
