package io.github.leonidius20.recorder.ui.common

import android.widget.Button
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources

/**
 * @param tag a tag value (view.getTag()) used to mark the drawable of the button. This is used for
 * testing purposes because it is impossible to compare drawables in a test case.
 */
fun Button.setIcon(@DrawableRes iconResId: Int, tag: String) {
    setCompoundDrawablesWithIntrinsicBounds(
        AppCompatResources.getDrawable(context, iconResId), null,
        null, null)
    this.tag = tag
}