package com.example.whepexample

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle


class RadioPlaybackService : Service() {

    private lateinit var whepClient: WHEPClient
    private lateinit var mediaSession: MediaSessionCompat

    private val CHANNEL_ID = "WebRTCServiceChannel"
    private val NOTIFICATION_ID = 1
    private val EXTRA_WHEP_URL = "whep_url"

    override fun onCreate() {
        super.onCreate()
        Log.d("RadioPlaybackService", "Service created")
    }

    private fun startForegroundService() {
        val channelId = CHANNEL_ID
        val channelName = "Radio Service"
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel =
            NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
        notificationManager.createNotificationChannel(channel)
        Log.d("RadioPlaybackService", "Notification channel created")


        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Radio title")
            .setContentText("Running radio Service")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setStyle(
                MediaStyle().setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1)
            )
            .build()

        startForeground(NOTIFICATION_ID, notification)
        Log.d("RadioPlaybackService", "Foreground service started")
    }


    private fun startRadioClient(url: String) {
        val config = WHEPClientConfig(disableVideo = true)
        whepClient = WHEPClient(this, url, config)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val playbackStateBuilder = PlaybackStateCompat.Builder()
        val state = PlaybackStateCompat.STATE_PLAYING
        val position = 0L
        val playbackSpeed = 1f
        playbackStateBuilder.setState(state, position, playbackSpeed)

        mediaSession = MediaSessionCompat(this, "RadioPlaybackService")
        val stateActions = PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_STOP

        playbackStateBuilder.setActions(stateActions)
        mediaSession.setPlaybackState(playbackStateBuilder.build())


        val callback = object : MediaSessionCompat.Callback() {
            override fun onPause() {
                onDestroy()
            }
        }

        mediaSession.setCallback(callback)

        val url = intent?.getStringExtra(EXTRA_WHEP_URL) ?: ""
        if (url.isNotEmpty()) {
            startRadioClient(url)
            startForegroundService()
            Log.d("RadioPlaybackService", "Radio client initialized and audio setup completed")
        } else {
            Log.w("RadioPlaybackService", "No URL provided")
        }
        return START_STICKY

    }

    override fun onDestroy() {
        super.onDestroy()
        if (::whepClient.isInitialized) {
            whepClient.cleanup()
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        mediaSession.release()
        Log.d("RadioPlaybackService", "Service destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
