package io.github.leonidius20.recorder.ui.recordings_list.playback

import android.content.ComponentName
import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.leonidius20.recorder.data.playback.PlaybackService
import io.github.leonidius20.recorder.ui.recordings_list.RecordingsListFragment

/*class PlaybackControls(
    @ApplicationContext private val context: Context,
): DefaultLifecycleObserver {

    private var mediaController: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val factory = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture = factory
        factory.addListener( {
            mediaController = factory?.let {
                if (it.isDone)
                    it.get()
                else
                    null
            }

            (owner as RecordingsListFragment).attachPlayerToView(mediaController!!)

            viewModel.recordings.value!!.forEach { recording ->
                mediaController?.addMediaItem(
                    MediaItem.Builder().setUri(recording.uri).setMediaMetadata(
                        MediaMetadata.Builder().setDisplayTitle(recording.name).build()
                    ).build()
                )
            }

            mediaController?.prepare()


        }, MoreExecutors.directExecutor())
    }


}*/