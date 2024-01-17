package com.example.whepexample

import android.os.Bundle
import androidx.activity.ComponentActivity

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.whepexample.ui.theme.WHEPExampleTheme
import org.webrtc.EglBase
import org.webrtc.MediaStream
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer

class NativeWebRTCActivity : ComponentActivity() {


    private lateinit var whepClient: WHEPClient
    private lateinit var surfaceViewRenderer: SurfaceViewRenderer


    private fun setupAudio() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(attributes)
            .build()
        audioManager.requestAudioFocus(focusRequest)
        AudioManager.AUDIOFOCUS_GAIN
        AudioManager.MODE_CURRENT

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val eglBaseContext = EglBase.create().eglBaseContext
        surfaceViewRenderer = SurfaceViewRenderer(this).apply {

            init(eglBaseContext, null)

            setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)

            setEnableHardwareScaler(true)
        }
        setupAudio()

        // disableVideo
        // disableAudio

        val config = WHEPClientConfig(eglBaseContext = eglBaseContext, disableVideo = true)
        var url: String = intent.getStringExtra("whep_url") ?: ""
        whepClient = WHEPClient(this, url, config)
        whepClient.setCallback(object : WHEPClientCallback {
            override fun onStreamAvailable(stream: MediaStream) {
                // Add the video track to the renderer
                stream.videoTracks.firstOrNull()?.addSink(surfaceViewRenderer)
            }
        })

        this.volumeControlStream = AudioManager.STREAM_MUSIC


        setContent {
            WHEPExampleTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    VideoComposable(surfaceViewRenderer)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        whepClient.cleanup()
    }
}