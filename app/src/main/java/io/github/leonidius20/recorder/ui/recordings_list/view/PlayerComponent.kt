package io.github.leonidius20.recorder.ui.recordings_list.view

import android.content.ComponentName
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import io.github.leonidius20.recorder.data.playback.PlaybackService
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map

/**
 * integrates the player with the UI
 */
class PlayerComponent : DefaultLifecycleObserver {

    private var mediaController: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null


    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)

        // todo: can we use application context instead?
        val context = (owner as RecordingsListFragment).requireContext()
        val sessionToken =
            SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val factory = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture = factory
        factory.addListener({
            mediaController = factory.let {
                if (it.isDone)
                    it.get()
                else
                    null
            }

            binding.playerView.player = mediaController

            viewModel.state
                .distinctUntilChangedBy { it.itemIds }
                .map { it.recordings }
                .collectSinceStarted { recordings ->

                    mediaController?.replaceMediaItems(0, mediaController!!.mediaItemCount,
                        recordings.map { recording ->
                            MediaItem.Builder()
                                .setUri(recording.uri)
                                .setMediaId(recording.id.toString())
                                .setMediaMetadata(
                                    MediaMetadata.Builder().setDisplayTitle(recording.name).build()
                                ).build()
                        }
                    )
                }

            mediaController?.addListener(object : Player.Listener {

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    adapter.setPlaying(mediaController!!.currentMediaItemIndex)
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (isPlaying) {
                        adapter.setPlaying(mediaController!!.currentMediaItemIndex)
                    }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        adapter.resetPlayingItemHighlighting()
                    }
                }

            })

            mediaController?.prepare()


        }, MoreExecutors.directExecutor())
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        MediaController.releaseFuture(controllerFuture!!)
        controllerFuture = null
        mediaController = null
    }

}