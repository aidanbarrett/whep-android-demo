package com.example.whepexample

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity

class NativeWebRTCActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val url: String = intent.getStringExtra("whep_url") ?: ""

        val serviceIntent = Intent(this, RadioPlaybackService::class.java).apply {
            putExtra("whep_url", url)
        }
        startForegroundService(serviceIntent)

    }

    override fun onDestroy() {
        super.onDestroy()
    }
}

