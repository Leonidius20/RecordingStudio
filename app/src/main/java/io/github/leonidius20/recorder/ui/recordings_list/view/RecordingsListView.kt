package io.github.leonidius20.recorder.ui.recordings_list.view

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.os.Build
import android.provider.MediaStore
import android.text.format.Formatter
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.navigation.fragment.findNavController
import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import com.arkivanov.essenty.lifecycle.Lifecycle
import com.arkivanov.essenty.lifecycle.doOnStart
import com.arkivanov.essenty.lifecycle.doOnStop
import com.arkivanov.mvikotlin.core.binder.BinderLifecycleMode
import com.arkivanov.mvikotlin.core.instancekeeper.getStore
import com.arkivanov.mvikotlin.core.utils.diff
import com.arkivanov.mvikotlin.core.view.BaseMviView
import com.arkivanov.mvikotlin.core.view.MviView
import com.arkivanov.mvikotlin.core.view.ViewRenderer
import com.arkivanov.mvikotlin.extensions.coroutines.bind
import com.arkivanov.mvikotlin.extensions.coroutines.events
import com.arkivanov.mvikotlin.extensions.coroutines.labels
import com.arkivanov.mvikotlin.extensions.coroutines.states
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.github.leonidius20.recorder.R
import io.github.leonidius20.recorder.RecorderApp
import io.github.leonidius20.recorder.data.playback.PlaybackService
import io.github.leonidius20.recorder.databinding.FragmentRecordingsListBinding
import io.github.leonidius20.recorder.domain.recordings_list.Recording
import io.github.leonidius20.recorder.ui.common.millisecondsToStopwatchString
import io.github.leonidius20.recorder.ui.recordings_list.view.RecordingsListView.Event
import io.github.leonidius20.recorder.ui.recordings_list.view.RecordingsListView.Model
import io.github.leonidius20.recorder.ui.recordings_list.viewmodel.Label
import io.github.leonidius20.recorder.ui.recordings_list.viewmodel.RecordingsListStore.Intent
import io.github.leonidius20.recorder.ui.recordings_list.viewmodel.RecordingsListStore.State
import io.github.leonidius20.recorder.ui.recordings_list.viewmodel.RecordingsListStoreFactory
import kotlinx.coroutines.flow.map

interface RecordingsListView : MviView<Model, Event> {

    data class Model(
        // todo: maybe show those cool
        //  list loading visualizations. maybe in the player too.
        val recordings: ArrayList<RecordingUiModel>,
        val numberSelected: Int,
        val loading: Boolean,
    ) {
        val showEmptyListText get() = !loading && recordings.isEmpty()
    }

    sealed interface Event {

        data class RecordingLongPressed(val id: Long) : Event

        data class RecordingClicked(val id: Long) : Event

        data object DisableSelectionMode : Event

        data object MediaControllerConnected : Event

        data object MediaControllerDisconnected : Event

        data class OtherRecordingStartedPlaying(
            val id: Long,
            val index: Int,
        ) : Event

        data object PlaybackEnded : Event

        data object TrashSelectedClicked : Event

        data object DeleteSelectedClicked : Event

        data object RenameSelectedClicked : Event

        data object ShareSelectedClicked : Event

        data object FileDeletionFailure : Event

    }

    fun handleLabel(label: Label)

    fun connectToMediaPlayer()

    fun disconnectFromMediaPlayer()

}

class RecordingsListViewImpl @OptIn(UnstableApi::class) constructor(
    val binding: FragmentRecordingsListBinding,
    val fragment: Fragment,
    val requireActivity: () -> Activity = { fragment.requireActivity() },
) : BaseMviView<Model, Event>(), RecordingsListView {

    private val context get() = binding.root.context

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    private var adapter: RecordingsListAdapter = RecordingsListAdapter(
        context,
        onItemClicked = { id ->
            dispatch(Event.RecordingClicked(id))
        }, onItemLongClicked = { id ->
            dispatch(Event.RecordingLongPressed(id))
        }
    )

    private var actionMode: ActionMode? = null

    private var isMultiSelection: Boolean = false

    private val actionModeCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {

            if (isMultiSelection) {
                mode.menuInflater.inflate(
                    R.menu.recordings_list_multiple_recordings_context_menu,
                    menu
                )
            } else {
                mode.menuInflater.inflate(R.menu.recordings_list_one_recording_context_menu, menu)
            }

            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            // todo: invalidation happens on each toggling of selection
            // so we can add or remove menu elements here based on if it is
            // 1 element selected or multiple
            menu.clear()
            if (isMultiSelection) {
                mode.menuInflater.inflate(
                    R.menu.recordings_list_multiple_recordings_context_menu,
                    menu
                )
            } else {
                mode.menuInflater.inflate(R.menu.recordings_list_one_recording_context_menu, menu)
            }

            return true
        }

        @SuppressLint("NewApi") // the "trash" option requires api 30 but it isn't shown in the menu on lower apis
        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem): Boolean {
            when (item.itemId) {
                R.id.recordings_list_action_rename -> {
                    dispatch(Event.RenameSelectedClicked)
                }

                R.id.recordings_list_action_delete_forever -> {
                    dispatch(Event.DeleteSelectedClicked)
                }

                R.id.recordings_list_action_share -> {
                    dispatch(Event.ShareSelectedClicked)
                }

                R.id.recordings_list_action_trash -> {
                    dispatch(Event.TrashSelectedClicked)
                }
            }
            return true
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            dispatch(Event.DisableSelectionMode)
            actionMode = null
        }


    }

    private val trashRecordingsIntentLauncher = fragment.registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            dispatch(Event.DisableSelectionMode)
        } else {
            dispatch(Event.FileDeletionFailure)
        }
    }

    private val deleteRecordingsIntentLauncher =
        fragment.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                dispatch(Event.DisableSelectionMode)
            } else {
                dispatch(Event.FileDeletionFailure)
            }
        }

    init {
        binding.recordingList.setHasFixedSize(true) // supposedly improves performance
        binding.recordingList.adapter = adapter

        binding.playerView.showController()
    }

    override val renderer: ViewRenderer<Model> = diff {
        diff(Model::recordings, set = adapter::setData)

        diff(Model::showEmptyListText, set = {
            binding.emptyListText.isVisible = it
        })

        diff(Model::numberSelected, set = { numberSelected ->
            if (numberSelected > 0) {
                isMultiSelection = numberSelected > 1

                if (actionMode == null) {
                    actionMode = requireActivity().startActionMode(actionModeCallback)
                }

                actionMode?.title = context.getString(R.string.recs_list_action_mode_num_selected,
                    numberSelected)
                actionMode?.invalidate()
            } else {
                actionMode?.finish()
                actionMode = null
                isMultiSelection = false
            }
        })
    }

    override fun handleLabel(label: Label) {
        when (label) {
            is Label.UpdatePlayerItems -> {
                mediaController?.let { mediaController ->
                    mediaController.replaceMediaItems(
                        0, mediaController.mediaItemCount,
                        label.recordings.map { recording ->
                            MediaItem.Builder()
                                .setUri(recording.uri)
                                .setMediaId(recording.id.toString())
                                .setMediaMetadata(
                                    MediaMetadata.Builder().setDisplayTitle(recording.name).build()
                                ).build()
                        }
                    )
                }
            }
            is Label.Play -> {
                mediaController?.run {
                    seekTo(label.position, 0L)
                    if (!isPlaying) play()
                }
            }
            is Label.Trash -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val uris = label.recs.map { it.uri }
                    val intent = MediaStore.createTrashRequest(
                        context.contentResolver, uris, true)
                    trashRecordingsIntentLauncher.launch(
                        IntentSenderRequest.Builder(intent).build()
                    )
                }
            }
            is Label.Delete -> {
                val uris = label.recs.map { it.uri }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val intent = MediaStore.createDeleteRequest(context.contentResolver, uris)
                    deleteRecordingsIntentLauncher.launch(
                        IntentSenderRequest.Builder(intent).build()
                    )
                } else {
                    // todo: dialogFragment, lift strings
                    MaterialAlertDialogBuilder(context)
                        .setTitle("Deleting files")
                        .setMessage("Do you confirm deleting ${uris.size} selected file(s)?")
                        .setPositiveButton(android.R.string.yes) { _, _ ->
                            // todo: move this logic somewhere
                            uris.forEach { uri ->
                                context.contentResolver.delete(
                                    uri, null, null
                                )
                            }
                            dispatch(Event.DisableSelectionMode)
                        }
                        .setNegativeButton(android.R.string.no) { dialog, _ ->
                            dialog.dismiss()
                        }
                        .show()
                }
            }
            is Label.Rename -> {
                fragment.findNavController().navigate(
                    RecordingsListFragmentDirections
                        .actionNavigationRecordingsListToRenameDialogFragment(
                            fileToRename = label.rec.uri,
                            currentFileName = label.rec.name,
                            id = label.rec.id,
                        )
                )
                dispatch(Event.DisableSelectionMode)
            }
            is Label.Share -> {
                share(label.recs)
            }
            is Label.ShowMessage -> {
                val text = when(label) {
                    is Label.ShowMessage.FileDeletionFailed ->
                        context.getString(R.string.file_delete_failure)
                }

                Toast.makeText(context, text,
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun share(recs: List<Recording>) {
        if (recs.isEmpty()) return

        val shareIntent = if (recs.size == 1) {
            val rec = recs.first()

            android.content.Intent().apply {
                action = android.content.Intent.ACTION_SEND
                putExtra(android.content.Intent.EXTRA_STREAM, rec.uri)
                type = rec.mimeType
            }
        } else {
            val mime = if (recs.all { it.mimeType == recs.first().mimeType })
                recs.first().mimeType else "audio/*"

            android.content.Intent().apply {
                action = android.content.Intent.ACTION_SEND_MULTIPLE
                putParcelableArrayListExtra(android.content.Intent.EXTRA_STREAM,
                    ArrayList(recs.map { it.uri }))
                type = mime
            }
        }

        context.startActivity(android.content.Intent.createChooser(shareIntent, null))
    }

    override fun connectToMediaPlayer() {
        val sessionToken =
            SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val factory = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture = factory
        factory.addListener({
            mediaController = factory.let {
                if (it.isDone)
                    it.get()
                else
                    null
            }

            binding.playerView.player = mediaController

            dispatch(Event.MediaControllerConnected)

            mediaController?.addListener(object : Player.Listener {

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    mediaItem?.let {
                        mediaController?.currentMediaItemIndex?.let { index ->
                            dispatch(Event.OtherRecordingStartedPlaying(
                                id = mediaItem.mediaId.toLong(),
                                index = index,
                            ))
                        } ?: dispatch(Event.PlaybackEnded)
                    } ?: dispatch(Event.PlaybackEnded)
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (isPlaying) {
                        mediaController?.let { mediaController ->
                            val index = mediaController.currentMediaItemIndex

                            mediaController.currentMediaItem?.let { item ->
                                dispatch(Event.OtherRecordingStartedPlaying(
                                    id = item.mediaId.toLong(),
                                    index = index,
                                ))
                            }
                        }
                    }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        dispatch(Event.PlaybackEnded)
                    }
                }

            })

            mediaController?.prepare()


        }, MoreExecutors.directExecutor())
    }

    override fun disconnectFromMediaPlayer() {
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
        controllerFuture = null
        mediaController = null
        binding.playerView.player = null
        dispatch(Event.MediaControllerDisconnected)
    }

}

internal val stateToModel: State.() -> Model = {
    Model(
        recordings = ArrayList(recordings.map {
            RecordingUiModel(
                it.id,
                it.name,
                millisecondsToStopwatchString(it.duration),
                // todo: remove context here
                Formatter.formatFileSize(RecorderApp.instance, it.size.toLong()),
                isSelected = selectedItems.contains(it.id),
                isPlaying = currentlyPlaying == it.id,
            )
        }),
        numberSelected = selectedItems.size,
        loading = isLoading,
    )
}

internal val eventToIntent: Event.() -> Intent = {
    when(this) {
        is Event.RecordingClicked -> {
            Intent.PlayOrToggleSelection(this.id)
        }
        is Event.RecordingLongPressed -> {
            Intent.ToggleSelection(this.id)
        }
        is Event.DisableSelectionMode -> {
            Intent.ClearSelection
        }
        is Event.MediaControllerConnected -> {
            Intent.ConnectPlayer
        }
        is Event.MediaControllerDisconnected -> Intent.DisconnectPlayer
        is Event.OtherRecordingStartedPlaying -> {
            Intent.OnPlayingRecordingChanged(id)
        }
        is Event.PlaybackEnded -> Intent.OnRecordingsPlaybackFinished
        is Event.TrashSelectedClicked -> Intent.TrashSelected
        is Event.DeleteSelectedClicked -> Intent.DeleteSelected
        is Event.RenameSelectedClicked -> Intent.RenameSelected
        is Event.ShareSelectedClicked -> Intent.ShareSelected
        is Event.FileDeletionFailure -> Intent.NotifyFileDeletionFailed
    }
}

class RecordingsListController @AssistedInject constructor(
    private val storeFactory: RecordingsListStoreFactory,
    @Assisted instanceKeeper: InstanceKeeper,
) {
    private val store = instanceKeeper.getStore {
        storeFactory.create()
    }

    @AssistedFactory
    interface Factory {
        fun create(
            instanceKeeper: InstanceKeeper,
        ): RecordingsListController
    }

    fun onViewCreated(view: RecordingsListView, viewLifecycle: Lifecycle) {
        bind(viewLifecycle, BinderLifecycleMode.START_STOP) {
            store.states.map(stateToModel) bindTo view
            view.events.map(eventToIntent) bindTo store
            store.labels bindTo view::handleLabel
        }

        viewLifecycle.doOnStart { view.connectToMediaPlayer() }

        viewLifecycle.doOnStop { view.disconnectFromMediaPlayer() }
    }

}
