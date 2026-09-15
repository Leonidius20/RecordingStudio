package io.github.leonidius20.recorder.ui.recordings_list.view

import android.content.Context
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import io.github.leonidius20.recorder.R
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
        layoutParams = LayoutParams(MATCH_PARENT,
            // we need proper size. If we use wrap_content, that would be 0dp
            // until the actual layout is inflated, and recyclerview will create
            // a bunch of items to fill the screen that we don't need
            resources.getDimensionPixelSize(R.dimen.rec_list_item_height))
    }

    private var isInflated = false
    private var pendingActions: MutableList<RecordingListItemWrapper.() -> Unit> = ArrayList()

    lateinit var binding: RecordingListItemBinding

    fun inflateAsync(layoutResId: Int) {
        OkLayoutInflater(context).inflate(layoutResId, this) { inflatedView ->
            this.binding = RecordingListItemBinding.bind(inflatedView)
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
