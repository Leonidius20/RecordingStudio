package io.github.leonidius20.recorder.ui.common

import android.content.Context
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import io.github.leonidius20.recorder.R

class Selector(
    context: Context
) : FrameLayout(context) {

    private val prevButton: ImageButton
    private val nextButton: ImageButton
    private val valueText: TextView

    init {
        inflate(getContext(), R.layout.view_selector, this)
        prevButton = findViewById<ImageButton>(R.id.button_prev)
        nextButton = findViewById<ImageButton>(R.id.button_next)
        valueText = findViewById<TextView>(R.id.value_text)
    }

    fun setValues(values: Array<String>) {

    }

    fun setSelected(index: Int) {

    }

    fun setOnSelectionChangeListener(listener: (Int) -> Unit) {

    }

}