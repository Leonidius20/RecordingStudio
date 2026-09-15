package io.github.leonidius20.recorder.common.ui

fun <T> T.ifDifferentFrom(other: T) =
    if (this != other) this else null
