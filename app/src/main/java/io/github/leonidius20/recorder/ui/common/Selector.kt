package io.github.leonidius20.recorder.ui.common

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.ImageButton
import androidx.viewpager.widget.ViewPager
import io.github.leonidius20.recorder.R

class Selector(
    private val context: Context,
    private val attrSet: AttributeSet? = null,
) : FrameLayout(context, attrSet) {

    private val prevButton: ImageButton
    private val nextButton: ImageButton
    private val valueText: ViewPager

    private val adapter = SelectorViewPagerAdapter(context)

    private var listener: ((Int) -> Unit)? = null

    init {
        inflate(context, R.layout.view_selector, this)
        valueText = findViewById<ViewPager>(R.id.text_pager).apply {
            adapter = adapter
            this.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
                override fun onPageScrolled(
                    position: Int,
                    positionOffset: Float,
                    positionOffsetPixels: Int
                ) {}

                override fun onPageSelected(position: Int) {
                    listener?.invoke(position)
                }

                override fun onPageScrollStateChanged(state: Int) {}

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
                val total = adapter.count
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