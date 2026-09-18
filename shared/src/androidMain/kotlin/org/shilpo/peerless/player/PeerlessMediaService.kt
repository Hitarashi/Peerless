package org.shilpo.peerless.player

import android.content.Intent
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class PeerlessMediaService : MediaSessionService() {
    override fun onCreate() {
        super.onCreate()
        AndroidMediaSessionHolder.mediaSession?.let { runCatching { addSession(it) } }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        AndroidMediaSessionHolder.mediaSession?.let { session ->
            runCatching { addSession(session) }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return AndroidMediaSessionHolder.mediaSession
    }

    override fun onDestroy() {
        AndroidMediaSessionHolder.mediaSession?.let { runCatching { removeSession(it) } }
        super.onDestroy()
    }
}
