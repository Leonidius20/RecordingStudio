package io.github.leonidius20.recorder.domain.recordings_list

import android.net.Uri

data class Recording(
    val id: Long,
    val uri: Uri, // todo: use kotlin's uri?
    val name: String,
    val durationMs: Long,
    val size: Int, // todo use long
    val dateTaken: Long,
    val mimeType: String,
)