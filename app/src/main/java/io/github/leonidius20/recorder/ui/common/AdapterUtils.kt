package io.github.leonidius20.recorder.ui.common

fun <T> T.ifDifferentFrom(other: T) =
    if (this != other) this else null
