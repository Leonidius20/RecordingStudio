package io.github.leonidius20.recorder

import android.view.View
import android.widget.Button
import androidx.annotation.DrawableRes
import androidx.test.espresso.matcher.BoundedMatcher
import org.hamcrest.Description

class ButtonIconTagMatcher {

    companion object {

        @JvmStatic
        fun withIcon(@DrawableRes iconResId: Int) = object : BoundedMatcher<View, Button>(Button::class.java) {

            override fun describeTo(description: Description?) {
                TODO("Not yet implemented")
            }

            override fun matchesSafely(item: Button): Boolean {
                return item.compoundDrawablesRelative[0].constantState ==
                        item.context.getDrawable(iconResId)?.constantState
            }

        }

    }

}