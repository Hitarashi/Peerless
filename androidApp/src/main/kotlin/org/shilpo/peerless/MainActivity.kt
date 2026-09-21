package org.shilpo.peerless

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.shilpo.peerless.auth.AndroidContextProvider
import org.shilpo.peerless.auth.DeepLinkHandler
import org.shilpo.peerless.player.AndroidAudioContextHolder

class MainActivity : ComponentActivity() {
    private lateinit var windowPostureProvider: AndroidWindowPostureProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        AndroidAudioContextHolder.context = applicationContext
        AndroidContextProvider.context = applicationContext
        windowPostureProvider = AndroidWindowPostureProvider(this)

        intent?.dataString?.let { uri ->
            DeepLinkHandler.handleUri(uri)
        }

        setContent {
            App(windowPostureProvider = windowPostureProvider)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.dataString?.let { uri ->
            DeepLinkHandler.handleUri(uri)
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
