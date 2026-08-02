package io.github.leonidius20.recorder.ui.recordings_list.view

import android.annotation.SuppressLint
import android.app.Activity
import android.text.format.Formatter
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import com.arkivanov.essenty.lifecycle.Lifecycle
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.arkivanov.mvikotlin.core.binder.BinderLifecycleMode
import com.arkivanov.mvikotlin.core.utils.diff
import com.arkivanov.mvikotlin.core.view.BaseMviView
import com.arkivanov.mvikotlin.core.view.MviView
import com.arkivanov.mvikotlin.core.view.ViewRenderer
import com.arkivanov.mvikotlin.extensions.coroutines.bind
import com.arkivanov.mvikotlin.extensions.coroutines.events
import com.arkivanov.mvikotlin.extensions.coroutines.labels
import com.arkivanov.mvikotlin.extensions.coroutines.states
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.github.leonidius20.recorder.R
import io.github.leonidius20.recorder.RecorderApp
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

interface RecordingsListView : MviView<Model, Event> {

    data class Model(
        val recordings: ArrayList<RecordingUiModel>,
        val numberSelected: Int,
    )

    sealed interface Event {

        // todo: which recording? probably indexed or ui model supplied
        //  best not to use indices as they may change on list update from backend
        data class RecordingLongPressed(val id: Long) : Event

        data class RecordingClicked(val id: Long) : Event

        data object DisableSelectionMode : Event

    }

    fun handleLabel(label: Label)

}

class RecordingsListViewImpl(
    val binding: FragmentRecordingsListBinding,
    val requireActivity: () -> Activity,
) : BaseMviView<Model, Event>(), RecordingsListView {

    private var adapter: RecordingsListAdapter = RecordingsListAdapter(
        binding.root.context,
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
        binding.recordingList.adapter = adapter
    }

    override val renderer: ViewRenderer<Model> = diff {
        diff(Model::recordings, set = adapter::setData)

        diff(Model::numberSelected, set = { numberSelected ->
            if (numberSelected > 0) {
                isMultiSelection = numberSelected > 1

                if (actionMode == null) {
                    actionMode = requireActivity().startActionMode(actionModeCallback)
                }

                actionMode?.title = binding.root.context.getString(R.string.recs_list_action_mode_num_selected,
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
        /*when(label) {
            is Label.EnableSelectionMode -> {
                actionMode = requireActivity().startActionMode(actionModeCallback)
            }
            is Label.DisableSelectionMode -> {
                actionMode?.finish()
                actionMode = null
                isMultiSelection = false
            }

        }*/
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
                isPlaying = false,
            )
        }),
        numberSelected = selectedItems.size,
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
    }

}
