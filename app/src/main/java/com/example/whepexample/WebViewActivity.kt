package com.example.whepexample

import android.os.Bundle
import android.webkit.WebSettings
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.webrtc.SurfaceViewRenderer

class WebViewActivity:  ComponentActivity() {
    private lateinit var webView: MediaWebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview)

        webView = findViewById(R.id.audio_player)
        setupWebView()

        var url: String = intent.getStringExtra("whep_url") ?: ""
        loadWebPage(url)
    }

    private fun setupWebView() {
        val webSettings = webView.settings
        webSettings.javaScriptEnabled = true // Enable JavaScript
        webSettings.mediaPlaybackRequiresUserGesture = false // Autoplay audio
        webSettings.setPluginState(WebSettings.PluginState.ON);

        // Additional settings as needed
    }

    private fun loadWebPage(url: String) {
        var baseUrl: String = "https://whep-player-demo.vercel.app/?disableVideo=true&disableControls=true&url="
        webView.loadUrl(baseUrl.plus(url)) // Replace with your URL
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}

@Composable
fun VideoComposable(surfaceViewRenderer: SurfaceViewRenderer) {
    AndroidView(
        factory = { context ->
            surfaceViewRenderer
        },
        modifier = Modifier.fillMaxSize()
    )
}