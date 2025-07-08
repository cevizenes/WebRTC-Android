package com.example.webrtc_android.webrtc

import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
import org.webrtc.SessionDescription

class RTCClientImpl(
    connection: PeerConnection,
    private val transferListener: TransferDataToServerCallback,
) : RTCClient {

    private val mediaConstraints = MediaConstraints().apply {
        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
    }

    override val peerConnection: PeerConnection = connection

    override fun offer() {
        peerConnection.createOffer(object : SDPObserver() {
            override fun onCreateSuccess(p0: SessionDescription?) {
                super.onCreateSuccess(p0)
                peerConnection.setLocalDescription(object: SDPObserver() {}, p0)
                p0?.let {
                    transferListener.onOfferGenerated(it)
                }
            }
        }, mediaConstraints)
    }

    override fun answer() {
        peerConnection.createAnswer(object : SDPObserver() {
            override fun onCreateSuccess(p0: SessionDescription?) {
                super.onCreateSuccess(p0)
                peerConnection.setLocalDescription(object: SDPObserver() {
                    override fun onSetSuccess() {
                        super.onSetSuccess()
                        p0?.let {
                            transferListener.onAnswerGenerated(it)
                        }
                    }
                }, p0)
            }
        },mediaConstraints)
    }
    override fun onIceCandidateReceived(iceCandidate: IceCandidate) {
        peerConnection.addIceCandidate(iceCandidate)
    }

    override fun onLocalIceCandidateGenerated(iceCandidate: IceCandidate) {
        peerConnection.addIceCandidate(iceCandidate)
        transferListener.onIceGenerated(iceCandidate)
    }

    override fun onRemoteSessionReceived(sessionDescription: SessionDescription) {
        peerConnection.setRemoteDescription(SDPObserver(), sessionDescription)
    }

    override fun onDestroy() {
        runCatching {
            peerConnection.close()
        }
    }

    interface TransferDataToServerCallback {
        fun onIceGenerated(iceCandidate: IceCandidate)
        fun onOfferGenerated(sessionDescription: SessionDescription)
        fun onAnswerGenerated(sessionDescription: SessionDescription)
    }
}
