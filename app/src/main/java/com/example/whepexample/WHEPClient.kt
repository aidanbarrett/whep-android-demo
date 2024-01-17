package com.example.whepexample

import android.content.Context
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.webrtc.AudioTrack
import org.webrtc.DataChannel
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.MediaStreamTrack
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.RtpTransceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.VideoDecoderFactory
import org.webrtc.VideoTrack
import java.net.HttpURLConnection
import java.net.URL


data class WHEPClientConfig(
    val iceServers: List<PeerConnection.IceServer>? = null,
    val bundlePolicy: PeerConnection.BundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE,
    val eglBaseContext: EglBase.Context? = null, // If video is required
    val disableAudio: Boolean = false,
    val disableVideo: Boolean = false,
    val maxRetries: Int = 3
)



interface WHEPClientCallback {
    fun onStreamAvailable(stream: MediaStream)
}


class WHEPClient(private val context: Context, private val endpoint: String, private val config: WHEPClientConfig) {
    private var peerConnection: PeerConnection? = null
    private var iceGatheringComplete = CompletableDeferred<String?>()
    private var mediaStream: MediaStream? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private var callback: WHEPClientCallback? = null



    init {
        if (config.disableAudio && config.disableVideo) {
            throw IllegalArgumentException("Cannot disable both audio and video")
        }
        println("initializing")
        println("Initializing PeerConnection")
        initializePeerConnection()

        println("Setting up Transceivers")
        setupTransceivers()

        println("Assigning Observers")
    }

    private fun initializePeerConnection() {
        // Initialize WebRTC
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .createInitializationOptions()
        )

        val decoderFactory: VideoDecoderFactory =
            DefaultVideoDecoderFactory(config?.eglBaseContext)


        // Create a PeerConnectionFactory
        val options = PeerConnectionFactory.Options()
        val peerConnectionFactory =
            PeerConnectionFactory.builder()
                .setOptions(options)
                .setVideoDecoderFactory(decoderFactory)
                .createPeerConnectionFactory()



        mediaStream = peerConnectionFactory.createLocalMediaStream("TEST_RADIO_STREAM")

        // Create PeerConnection.RTCConfiguration with STUN server
        val defaultIceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.cloudflare.com:3478").createIceServer()
        )
        val iceServers = config.iceServers.takeUnless { it.isNullOrEmpty() } ?: defaultIceServers

        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            bundlePolicy = config.bundlePolicy
        }

        // Create PeerConnection
        peerConnection = peerConnectionFactory?.createPeerConnection(rtcConfig, object : PeerConnection.Observer {

            override fun onIceGatheringChange(iceGatheringState: PeerConnection.IceGatheringState?) {
                println(iceGatheringState)
                if (iceGatheringState == PeerConnection.IceGatheringState.COMPLETE) {
                    iceGatheringComplete.complete(peerConnection?.localDescription?.description)
                }
            }

            override fun onTrack(rtpTransceiver: RtpTransceiver?) {
                rtpTransceiver?.receiver?.track()?.let { track ->
                    when (track.kind()) {
                        "video" -> if (!config.disableVideo) addTrackToStream(track)
                        "audio" -> if (!config.disableAudio) addTrackToStream(track)
                        else -> println("Received unknown track kind: ${track.kind()}")
                    }
                }
            }

            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
                // Handle connection state change
            }

            override fun onSignalingChange(signalingState: PeerConnection.SignalingState?) {}

            override fun onIceConnectionChange(iceConnectionState: PeerConnection.IceConnectionState?) {}

            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}

            override fun onIceConnectionReceivingChange(receiving: Boolean) {}

            override fun onIceCandidate(candidate: IceCandidate?) {}

            override fun onAddStream(stream: MediaStream?) {}

            override fun onRemoveStream(stream: MediaStream?) {}

            override fun onDataChannel(dataChannel: DataChannel?) {}

            override fun onRenegotiationNeeded() {
                coroutineScope.launch {
                    negotiateConnectionWithClientOffer()
                }
            }

            override fun onAddTrack(receiver: RtpReceiver?, mediaStreams: Array<out MediaStream>?) {}
        })
    }

    private fun setupTransceivers() {
        if (!config.disableVideo) {
            val videoTransceiverInit =
                RtpTransceiver.RtpTransceiverInit(RtpTransceiver.RtpTransceiverDirection.RECV_ONLY)
            peerConnection?.addTransceiver(
                MediaStreamTrack.MediaType.MEDIA_TYPE_VIDEO,
                videoTransceiverInit
            )
        }

        if (!config.disableAudio) {
            val audioTransceiverInit =
                RtpTransceiver.RtpTransceiverInit(RtpTransceiver.RtpTransceiverDirection.RECV_ONLY)
            peerConnection?.addTransceiver(
                MediaStreamTrack.MediaType.MEDIA_TYPE_AUDIO,
                audioTransceiverInit
            )
        }
    }

    private fun addTrackToStream(track: MediaStreamTrack) {
        if (mediaStream === null) {
            throw Exception("MediaStream not initialised")
        }
        when (track) {
            is AudioTrack -> mediaStream?.addTrack(track)
            is VideoTrack -> {
                println("video track being added")
                mediaStream?.addTrack(track)
                mediaStream?.let { nonNullMediaStream ->
                    notifyStreamAvailable(nonNullMediaStream)
                } ?: run {
                    println("Error: MediaStream is null when trying to add VideoTrack.")
                }
            }
            else -> println("Unknown track type: ${track.kind()}")
        }
    }


    private suspend fun waitToCompleteICEGathering(): String? {
        return try {
            withTimeoutOrNull(5000) {
                iceGatheringComplete.await()
            } ?: peerConnection?.localDescription?.description
        } catch (e: Exception) {
            peerConnection?.localDescription?.description
        }
    }


    private suspend fun negotiateConnectionWithClientOffer(): String? {
        println("Negotiating connection")
        val offerCreationDeferred = CompletableDeferred<SessionDescription>()
        val setLocalDescDeferred = CompletableDeferred<Unit>()
        val setRemoteDescDeferred = CompletableDeferred<Unit>()

        // Create Offer
        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sessionDescription: SessionDescription?) {
                sessionDescription?.let {
                    offerCreationDeferred.complete(it)
                }
            }

            override fun onSetSuccess() {}
            override fun onCreateFailure(s: String?) {
                offerCreationDeferred.completeExceptionally(Exception(s))
            }

            override fun onSetFailure(s: String?) {}
        }, MediaConstraints())

        println("Creating Offer")
        val offer = offerCreationDeferred.await()
        println("Offer created")
        peerConnection?.setLocalDescription(object : SdpObserver {
            override fun onSetSuccess() {
                setLocalDescDeferred.complete(Unit)
            }

            override fun onCreateSuccess(sessionDescription: SessionDescription?) {}
            override fun onCreateFailure(s: String?) {}
            override fun onSetFailure(s: String?) {
                setLocalDescDeferred.completeExceptionally(Exception(s))
            }
        }, offer)

        println("Setting local description")
        setLocalDescDeferred.await()
        println("Local description set")

        println("Gathering ICE candidates")
        val initialisedOffer = waitToCompleteICEGathering()
            ?: throw Exception("Failed to gather ICE candidates for offer")

        println("Gathering ICE candidates complete")
            println("Exchanging offer")

        val resultDeferred = CompletableDeferred<String?>()
        coroutineScope.launch(Dispatchers.IO) {

            val response = withContext(Dispatchers.IO) {
                postSDPOffer(endpoint, initialisedOffer)
            }
            println("Response received")

            when (response.responseCode) {
                201 -> {
                    val answerSDP = withContext(Dispatchers.IO) {
                        response.inputStream.bufferedReader().use { it.readText() }
                    }
                    withContext(Dispatchers.Main) {
                        peerConnection?.setRemoteDescription(
                            object : SdpObserver {
                                override fun onSetSuccess() {
                                    setRemoteDescDeferred.complete(Unit)
                                }

                                override fun onCreateSuccess(sessionDescription: SessionDescription?) {}
                                override fun onCreateFailure(s: String?) {}
                                override fun onSetFailure(s: String?) {
                                    setRemoteDescDeferred.completeExceptionally(Exception(s))
                                }
                            },
                            SessionDescription(SessionDescription.Type.ANSWER, answerSDP)
                        )
                    }

                    println("Answer received, setting remote description")
                    setRemoteDescDeferred.await() // Wait for the remote description to be set
                    println("Answer set")
                    resultDeferred.complete(response.getHeaderField("Location"))
                }

                403 -> {
                    println("Token is invalid")
                    throw Error("Unauthorized")
                }

                405 -> {
                    println("Must be returned for future WHEP spec updates")
                }

                else -> {
                    val errorMessage =
                        response.errorStream.bufferedReader().use { it.readText() }
                    println(errorMessage)
                }
            }

        }
        return resultDeferred.await()
    }

    private fun postSDPOffer(endpoint: String, data: String): HttpURLConnection {
        val url = URL(endpoint)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/sdp")
        connection.doOutput = true
        connection.outputStream.use { os ->
            os.write(data.toByteArray())
        }
        return connection
    }

    fun cleanup() {
        coroutineScope.cancel()
        peerConnection?.close()
    }

    fun setCallback(callback: WHEPClientCallback) {
        this.callback = callback
    }

    private fun notifyStreamAvailable(stream: MediaStream) {
        callback?.onStreamAvailable(stream)
    }
}

fun delay(ms: Long) {
    delay(ms)
}