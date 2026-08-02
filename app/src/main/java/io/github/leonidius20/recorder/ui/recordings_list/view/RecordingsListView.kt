package io.github.leonidius20.recorder.ui.recordings_list.view

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.text.format.Formatter
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import androidx.core.view.isVisible
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.arkivanov.essenty.lifecycle.Lifecycle
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.arkivanov.essenty.lifecycle.doOnStart
import com.arkivanov.essenty.lifecycle.doOnStop
import com.arkivanov.mvikotlin.core.binder.BinderLifecycleMode
import com.arkivanov.mvikotlin.core.utils.diff
import com.arkivanov.mvikotlin.core.view.BaseMviView
import com.arkivanov.mvikotlin.core.view.MviView
import com.arkivanov.mvikotlin.core.view.ViewRenderer
import com.arkivanov.mvikotlin.extensions.coroutines.bind
import com.arkivanov.mvikotlin.extensions.coroutines.events
import com.arkivanov.mvikotlin.extensions.coroutines.labels
import com.arkivanov.mvikotlin.extensions.coroutines.states
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.github.leonidius20.recorder.R
import io.github.leonidius20.recorder.RecorderApp
import io.github.leonidius20.recorder.data.playback.PlaybackService
import io.github.leonidius20.recorder.databinding.FragmentRecordingsListBinding
import io.github.leonidius20.recorder.ui.common.millisecondsToStopwatchString
import io.github.leonidius20.recorder.ui.recordings_list.view.RecordingsListView.Event
import io.github.leonidius20.recorder.ui.recordings_list.view.RecordingsListView.Model
import io.github.leonidius20.recorder.ui.recordings_list.viewmodel.Label
import io.github.leonidius20.recorder.ui.recordings_list.viewmodel.RecordingsListStore.Intent
import io.github.leonidius20.recorder.ui.recordings_list.viewmodel.RecordingsListStore.State
import io.github.leonidius20.recorder.ui.recordings_list.viewmodel.RecordingsListStoreFactory
import io.github.leonidius20.recorder.ui.recordings_list.viewmodel.RecordingsListViewModel.RecordingUiModel
import kotlinx.coroutines.flow.map
import kotlin.collections.map

interface RecordingsListView : MviView<Model, Event> {

    data class Model(
        // todo: add a Loading state. show a progress bar or those cool
        //  list loading visualizations. maybe in the player too.
        val recordings: ArrayList<RecordingUiModel>,
        val numberSelected: Int,
    ) {
        val showEmptyListText get() = recordings.isEmpty()
    }

    sealed interface Event {

        // todo: which recording? probably indexed or ui model supplied
        //  best not to use indices as they may change on list update from backend
        data class RecordingLongPressed(val id: Long) : Event

        data class RecordingClicked(val id: Long, val index: Int) : Event

        data object DisableSelectionMode : Event

        data object MediaControllerConnected : Event

        data object MediaControllerDisconnected : Event

        data class OtherRecordingStartedPlaying(
            val id: Long,
            val index: Int,
        ) : Event

        data object PlaybackEnded : Event

    }

    fun handleLabel(label: Label)

    fun connectToMediaPlayer() // todo: remove?

    fun disconnectFromMediaPlayer()

}

class RecordingsListViewImpl(
    val binding: FragmentRecordingsListBinding,
    val requireActivity: () -> Activity,
) : BaseMviView<Model, Event>(), RecordingsListView {

    private val context get() = binding.root.context

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    private var adapter: RecordingsListAdapter = RecordingsListAdapter(
        context,
        onItemClicked = { id, position ->
            dispatch(Event.RecordingClicked(id, position))
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

            // todo: this is temporary, remove once sharing is implemented
            menu.removeItem(R.id.recordings_list_action_share)

            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            // todo: invalidation happends on each toggling of selection
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

            // todo: this is temporary, remove once sharing is implemented
            menu.removeItem(R.id.recordings_list_action_share)

            return true
        }

        @SuppressLint("NewApi") // the "trash" option requires api 30 but it isn't shown in the menu on lower apis
        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem): Boolean {
            when (item.itemId) {
                R.id.recordings_list_action_rename -> {
                    // todo:
                    //rename()
                }

                R.id.recordings_list_action_delete_forever -> {
                    //delete()
                }

                R.id.recordings_list_action_share -> {
                    // todo
                }

                R.id.recordings_list_action_trash -> {
                    //trash()
                }
            }
            return true
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            dispatch(Event.DisableSelectionMode)
            actionMode = null
        }


    }

    init {
        binding.recordingList.setHasFixedSize(true) // supposedly improves performance
        binding.recordingList.adapter = adapter
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
                mediaController?.replaceMediaItems(
                    0, mediaController!!.mediaItemCount,
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
            is Label.Play -> {
                mediaController?.run {
                    seekTo(label.position, 0L)
                    if (!isPlaying) play()
                }
            }
        }
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
                        dispatch(Event.OtherRecordingStartedPlaying(
                            id = mediaItem.mediaId.toLong(),
                            index = mediaController!!.currentMediaItemIndex,
                        ))
                    } ?: dispatch(Event.PlaybackEnded)
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (isPlaying) {
                        val index = mediaController!!.currentMediaItemIndex
                        val item = mediaController!!.currentMediaItem!!
                        dispatch(Event.OtherRecordingStartedPlaying(
                            id = item.mediaId.toLong(),
                            index = index,
                        ))
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
                // dateFormat.format(Date(it.dateTaken)),
                it.uri,// todo: think about how we can go about removing fields that have nothing to do with UI, like mime type
                //it.mimeType,
                // todo: also inplement selection here
                isSelected = selectedItems.contains(it.id),
                isPlaying = currentlyPlaying == it.id,
            )
        }),
        numberSelected = selectedItems.size,
    )
}

internal val eventToIntent: Event.() -> Intent = {
    when(this) {
        is Event.RecordingClicked -> {
            Intent.PlayOrToggleSelection(this.index, this.id)
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
    }
}

class RecordingsListController @AssistedInject constructor(
    private val storeFactory: RecordingsListStoreFactory,
    @Assisted lifecycle: Lifecycle,
) {
    private val store = storeFactory.create()

    init {
        lifecycle.doOnDestroy(store::dispose)
    }

    @AssistedFactory
    interface Factory {
        fun create(lifecycle: Lifecycle): RecordingsListController
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
