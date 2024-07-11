package io.github.leonidius20.recorder.ui.recordings_list

import android.content.Context
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import io.github.leonidius20.recorder.databinding.RecordingListItem2Binding
import io.github.leonidius20.recorder.databinding.RecordingListItemBinding
import tech.okcredit.layout_inflator.OkLayoutInflater

/**
 * needed for async-ly inflating list items in recyclerview
 */
class RecordingListItemWrapper(
    context: Context,
) : FrameLayout(
    context,
    null,
    0,
    0
) {
    init {
        layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
    }

    private var isInflated = false
    private var pendingActions: MutableList<RecordingListItemWrapper.() -> Unit> = ArrayList()

    lateinit var binding: RecordingListItem2Binding

    fun inflateAsync(layoutResId: Int) {
        OkLayoutInflater(context).inflate(layoutResId, this) { inflatedView ->
            this.binding = RecordingListItem2Binding.bind(inflatedView)
            addView(inflatedView)
            isInflated = true
            pendingActions.forEach { action -> action() }
            pendingActions.clear()
        }
    }

    fun invokeWhenInflated(action: RecordingListItemWrapper.() -> Unit) {
        if (isInflated) {
            action()
        } else {
            pendingActions.add(action)
        }
    }
}