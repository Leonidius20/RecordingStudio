package io.github.leonidius20.recorder.ui.recordings_list.view

import android.text.format.Formatter
import com.arkivanov.essenty.lifecycle.Lifecycle
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.arkivanov.mvikotlin.core.binder.Binder
import com.arkivanov.mvikotlin.core.binder.BinderLifecycleMode
import com.arkivanov.mvikotlin.core.view.BaseMviView
import com.arkivanov.mvikotlin.core.view.MviView
import com.arkivanov.mvikotlin.extensions.coroutines.bind
import com.arkivanov.mvikotlin.extensions.coroutines.events
import com.arkivanov.mvikotlin.extensions.coroutines.states
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import dagger.hilt.android.scopes.ViewModelScoped
import io.github.leonidius20.recorder.RecorderApp
import io.github.leonidius20.recorder.data.recordings_list.RecordingsListRepository
import io.github.leonidius20.recorder.databinding.FragmentRecordingsListBinding
import io.github.leonidius20.recorder.ui.common.millisecondsToStopwatchString
import io.github.leonidius20.recorder.ui.recordings_list.view.RecordingsListView.Event
import io.github.leonidius20.recorder.ui.recordings_list.view.RecordingsListView.Model
import io.github.leonidius20.recorder.ui.recordings_list.viewmodel.CalculatorStoreFactory
import io.github.leonidius20.recorder.ui.recordings_list.viewmodel.RecordingsListStore.Intent
import io.github.leonidius20.recorder.ui.recordings_list.viewmodel.RecordingsListStore.State
import io.github.leonidius20.recorder.ui.recordings_list.viewmodel.RecordingsListViewModel.RecordingUiModel
import kotlinx.coroutines.flow.map

interface RecordingsListView : MviView<Model, Event> {

    data class Model(
        val recordings: ArrayList<RecordingUiModel>,
    )

    sealed interface Event {

        // todo: which recording? probably indexed or ui model supplied
        //  best not to use indices as they may change on list update from backend
        data class RecordingLongPressed(val index: Int) : Event

        data class RecordingClicked(val index: Int) : Event

    }

}

class RecordingsListViewImpl(
    val binding: FragmentRecordingsListBinding,
) : BaseMviView<Model, Event>(), RecordingsListView {

    private var adapter: RecordingsListAdapter = RecordingsListAdapter(
        binding.root.context,
        onItemClicked = { index ->
            dispatch(Event.RecordingClicked(index))
        }, onItemLongClicked = { index ->
            dispatch(Event.RecordingLongPressed(index))
        }
    )

    init {
        binding.recordingList.adapter = adapter
    }

    override fun render(model: Model) {
        super.render(model)

        adapter.setData(model.recordings)
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
                isSelected = false,
                isPlaying = false,
            )
        })
    )
}

// todo: is it legal to use state here???
internal val eventToIntent: Event.(state: State) -> Intent = {
    when(this) {
        // todo: could be select, could be deselect - who knows??? depends on state
        is Event.RecordingClicked -> {
            Intent.ClearSelection // todo
        }
        is Event.RecordingLongPressed -> {
            Intent.ClearSelection
        }
    }
}

class RecordingsListController(
    private val repository: RecordingsListRepository,
    lifecycle: Lifecycle,
) {
    private val store = CalculatorStoreFactory(repository).create()

    init {
        lifecycle.doOnDestroy(store::dispose)
    }


    fun onViewCreated(view: RecordingsListView, viewLifecycle: Lifecycle) {
        bind(viewLifecycle, BinderLifecycleMode.START_STOP) {
            store.states.map(stateToModel) bindTo view
            // Use store.labels to bind Labels to a consumer
            view.events.map { it.eventToIntent(store.state) } bindTo store
        }
    }

}