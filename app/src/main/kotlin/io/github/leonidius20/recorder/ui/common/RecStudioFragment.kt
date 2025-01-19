package io.github.leonidius20.recorder.ui.common

import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch

abstract class RecStudioFragment : Fragment() {


    fun <T> Flow<T>.collectDistinctSinceStarted(flowCollector: FlowCollector<T>) {
        distinctUntilChanged().collectSinceStarted(flowCollector)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun <T, P> Flow<T>.collectDistinctSinceStarted(property: (T) -> P, flowCollector: FlowCollector<P>) {
        mapLatest(property).collectDistinctSinceStarted(flowCollector)
    }

    fun <T> Flow<T>.collectSinceStarted(flowCollector: FlowCollector<T>) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                this@collectSinceStarted.collect(flowCollector)
            }
        }
    }

}