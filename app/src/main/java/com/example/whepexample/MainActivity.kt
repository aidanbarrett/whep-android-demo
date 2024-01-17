package com.example.whepexample

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.activity.ComponentActivity


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val etUrlInput = findViewById<EditText>(R.id.etUrlInput)
        val btnNativeWebRTC = findViewById<Button>(R.id.btnNativeWebRTC)
        val btnWebView = findViewById<Button>(R.id.btnWebView)

        btnNativeWebRTC.setOnClickListener {
            val url = etUrlInput.text.toString()
            val intent = Intent(this, NativeWebRTCActivity::class.java).apply {
                putExtra("whep_url", url)
            }
            startActivity(intent)
        }

        btnWebView.setOnClickListener {
            val url = etUrlInput.text.toString()
            val intent = Intent(this, WebViewActivity::class.java).apply {
                putExtra("whep_url", url)
            }
            startActivity(intent)
        }
    }
}



