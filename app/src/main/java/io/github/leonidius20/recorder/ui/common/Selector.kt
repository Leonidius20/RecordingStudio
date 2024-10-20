package io.github.leonidius20.recorder.ui.common

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.ImageButton
import androidx.viewpager2.widget.ViewPager2
import io.github.leonidius20.recorder.R

class Selector @JvmOverloads constructor(
    private val context: Context,
    private val attrSet: AttributeSet? = null,
    defStyleAttr: Int = 0, defStyleRes: Int = 0
) : FrameLayout(context, attrSet, defStyleAttr, defStyleRes) {

    private val prevButton: ImageButton
    private val nextButton: ImageButton
    private val valueText: ViewPager2

    private val adapter = Adapter(context, arrayOf("cock", "balls"))

    private var listener: ((Int) -> Unit)? = null

    init {
        val childLinearLayout = LayoutInflater.from(context).inflate(
            R.layout.view_selector,
            this,
            false,
        )
        //childLinearLayout.layoutParams.width = this.layoutParams.width
        //childLinearLayout.layoutParams.height = this.layoutParams.height
        //inflate(context, R.layout.view_selector, this)

        this.addView(childLinearLayout)

        // inflate(context, R.layout.view_selector, this)
        valueText = findViewById<ViewPager2>(R.id.text_pager).apply {
            adapter = adapter

            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    listener?.invoke(position)
                }
            })
        }

        prevButton = findViewById<ImageButton>(R.id.button_prev).apply {
            setOnClickListener {
                val current = valueText.currentItem
                if (current > 0) {
                    valueText.setCurrentItem(current - 1, true)
                }
            }
        }

        nextButton = findViewById<ImageButton>(R.id.button_next).apply {
            setOnClickListener {
                val current = valueText.currentItem
                val total = adapter.data.size
                if (current < total - 1) {
                    valueText.setCurrentItem(current + 1, true)
                }
            }
        }
    }

    fun setValues(values: Array<String>) {
        adapter.setData(values)
    }

    fun setSelected(index: Int) {
        valueText.setCurrentItem(index, false)
    }

    fun setOnSelectionChangeListener(listener: (Int) -> Unit) {
        this.listener = listener
    }

}