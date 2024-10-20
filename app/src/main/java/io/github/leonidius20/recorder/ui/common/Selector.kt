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

    private val adapter: SelectorViewPagerAdapter = SelectorViewPagerAdapter(context)

    private var listener: ((Int) -> Unit)? = null

    val callback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            listener?.invoke(position)
        }
    }

    init {
        val childLinearLayout = LayoutInflater.from(context).inflate(
            R.layout.view_selector,
            this,
            false,
        )
        this.addView(childLinearLayout)


        valueText = findViewById<ViewPager2>(R.id.text_pager).apply {

            adapter = this@Selector.adapter

            registerOnPageChangeCallback(callback)
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

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
    }

    fun setValues(values: Array<String>) {
        //valueText.unregisterOnPageChangeCallback(callback)
        adapter.setData(values)
        //valueText.registerOnPageChangeCallback(callback)
    }

    fun setSelected(index: Int) {
        //valueText.unregisterOnPageChangeCallback(callback)
        valueText.setCurrentItem(index, false)
        //valueText.registerOnPageChangeCallback(callback)
    }

    fun setOnSelectionChangeListener(listener: (Int) -> Unit) {
        this.listener = listener
    }

}