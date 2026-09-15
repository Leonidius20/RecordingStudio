package io.github.leonidius20.recorder.ui.common

import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.launch

abstract class RecStudioFragment : Fragment() {

    fun <T> Flow<T>.collectSinceStarted(flowCollector: FlowCollector<T>) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                this@collectSinceStarted.collect(flowCollector)
            }
        }
    }

}
