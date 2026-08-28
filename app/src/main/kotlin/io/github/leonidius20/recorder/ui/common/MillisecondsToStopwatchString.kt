package io.github.leonidius20.recorder.ui.common

import kotlin.time.DurationUnit
import kotlin.time.toDuration

fun millisecondsToStopwatchString(milliseconds: Long): String {
    return secondsToStopwatchString(milliseconds / 1000)
}

fun secondsToStopwatchString(seconds: Long): String {
    return seconds.toDuration(DurationUnit.SECONDS)
        .toComponents { hours, minutes, seconds, _ ->
            if (hours == 0L) {
                "%02d:%02d".format(minutes, seconds)
            } else {
                "%d:%02d:%02d".format(hours, minutes, seconds)
            }
        }
}
