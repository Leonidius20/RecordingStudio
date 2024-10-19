package io.github.leonidius20.recorder.ui.common

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.viewpager.widget.PagerAdapter

class SelectorViewPagerAdapter(
    private val context: Context,
) : PagerAdapter() {

    private var data: Array<String> = arrayOf()

    // string - title to show, any - tag
    fun setData(data: Array<String>) {
        this.data = data
    }

    override fun getCount() = data.size

    override fun isViewFromObject(view: View, obj: Any): Boolean {
        return view == obj
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val title = data[position]

        val view = TextView(context).apply {
            text = title
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT.toInt(),
                LinearLayout.LayoutParams.WRAP_CONTENT.toInt(),
            ).apply {
                gravity = Gravity.CENTER
            }
        }

        container.addView(view)

        return view // may return view but also any other key object really
    }

    override fun destroyItem(container: ViewGroup, position: Int, obj: Any) {
        container.removeView(obj as View)
    }

}