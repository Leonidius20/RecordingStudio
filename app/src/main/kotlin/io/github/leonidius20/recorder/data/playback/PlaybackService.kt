package io.github.leonidius20.recorder.data.playback

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PlaybackService: MediaSessionService() {

    private lateinit var mediaSession: MediaSession
    private lateinit var player: ExoPlayer

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        player = ExoPlayer.Builder(this).build()
        player.pauseAtEndOfMediaItems = true

        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaSession.run {
            player.release()
            release()
            // mediaSession = null
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = mediaSession

}