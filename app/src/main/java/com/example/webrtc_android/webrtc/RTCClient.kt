package com.example.webrtc_android.webrtc

import org.webrtc.IceCandidate
import org.webrtc.PeerConnection
import org.webrtc.SessionDescription

interface RTCClient {

    val peerConnection : PeerConnection
    fun onDestroy()
    fun offer()
    fun answer()
    fun onIceCandidateReceived(iceCandidate: IceCandidate)
    fun onLocalIceCandidateGenerated(iceCandidate: IceCandidate)
    fun onRemoteSessionReceived(sessionDescription: SessionDescription)

}