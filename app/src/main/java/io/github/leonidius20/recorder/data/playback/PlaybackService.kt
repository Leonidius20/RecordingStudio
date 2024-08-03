package io.github.leonidius20.recorder.data.playback

import android.content.Intent
import android.net.Uri
import android.os.IBinder
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
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

        // todo: receive directions from UI as to what file to jump to

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

    /**
     * this is temporary and should be replaced with a way to open the whole Recordings
     * folder at once and jump to appropriate file there (maybe taking position from
     * list adapter)
     */
    fun setFile(uri: Uri) {
        player.setMediaItem(
            MediaItem.fromUri(uri)
        )
        player.prepare()
    }

    inner class Binder: android.os.Binder() {

        val service = this@PlaybackService

    }

    private val binder = Binder()

    override fun onBind(intent: Intent?): IBinder? {
        return super.onBind(intent)
        TODO("not done")
    }


}