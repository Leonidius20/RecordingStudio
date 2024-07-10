package io.github.leonidius20.recorder.ui.common

import androidx.databinding.BindingAdapter
import net.nicbell.materiallists.ListItem

@BindingAdapter("app:headline")
fun ListItem.setHeadline(headline: String) {
    this.headline.text = headline
}

@BindingAdapter("app:supportText")
fun ListItem.setSupportText(supportText: String) {
    this.supportText.text = supportText
}