package org.shilpo.peerless

import android.app.Application
import org.shilpo.peerless.auth.AndroidContextProvider
import org.shilpo.peerless.player.AndroidAudioContextHolder

class PeerlessApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidAudioContextHolder.context = this
        AndroidContextProvider.context = this
    }
}
