package io.github.leonidius20.recorder.ui.common

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.ImageButton
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
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

    private var adapter: SelectorViewPagerAdapter = SelectorViewPagerAdapter(context)

    private var listener: ((Int) -> Unit)? = null

    private val buttonEnabledColor = ContextCompat.getColor(
        context, R.color.md_theme_onSurface
    )
    private val buttonDisabledColor =
        (buttonEnabledColor and 0x00FFFFFF) or 0x59000000 // 35% opacity

    val callback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            listener?.invoke(position)

            updateButtonsOnSelectionChange(position)
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

    fun setValues(values: Array<String>, selectedIndex: Int) {
        valueText.unregisterOnPageChangeCallback(callback)
        adapter.setData(values)
        setSelected(selectedIndex)
        valueText.registerOnPageChangeCallback(callback)
    }

    fun setValues(list: List<String>, selectedIndex: Int) {
        setValues(list.toTypedArray(), selectedIndex)
        // todo: we have a crash when changing the dataset, it runs the listener
        // with indexes from the old dataset and we get index out of bounds, or,
        // we get wrong values sent to the listener. We gotta fix it somehow
        updateButtonsOnSelectionChange(selectedIndex)
    }

    fun setSelected(index: Int) {
        //valueText.unregisterOnPageChangeCallback(callback)
        valueText.setCurrentItem(index, false)
        //valueText.registerOnPageChangeCallback(callback)
    }

    fun setOnSelectionChangeListener(listener: (Int) -> Unit) {
        this.listener = listener

    }

    private fun updateButtonsOnSelectionChange(position: Int) {
        nextButton.apply {
            isEnabled = (position != adapter.data.size - 1)
            ImageViewCompat.setImageTintList(
                this, ColorStateList.valueOf(
                    if (isEnabled) buttonEnabledColor
                    else buttonDisabledColor
                )
            )
        }

        prevButton.apply {
            isEnabled = (position != 0)
            ImageViewCompat.setImageTintList(
                this, ColorStateList.valueOf(
                    if (isEnabled) buttonEnabledColor
                    else buttonDisabledColor
                )
            )
        }
    }


}