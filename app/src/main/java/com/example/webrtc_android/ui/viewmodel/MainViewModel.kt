package com.example.webrtc_android.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.webrtc_android.remote.FirebaseClient
import com.example.webrtc_android.utils.MatchState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.annotation.SuppressLint
import android.app.Application
import com.example.webrtc_android.remote.StatusDataModel
import com.example.webrtc_android.remote.StatusDataModelTypes
import com.example.webrtc_android.utils.ChatItems
import com.example.webrtc_android.utils.SignalDataModel
import com.example.webrtc_android.utils.SignalDataModelTypes
import com.example.webrtc_android.webrtc.PeerObserver
import com.example.webrtc_android.webrtc.RTCAudioManager
import com.example.webrtc_android.webrtc.RTCClient
import com.example.webrtc_android.webrtc.RTCClientImpl
import com.example.webrtc_android.webrtc.WebRTCFactory
import com.google.gson.Gson
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.SessionDescription
import org.webrtc.SurfaceViewRenderer

@SuppressLint("StaticFieldLeak")
@HiltViewModel
class MainViewModel @Inject constructor(
    private val firebaseClient: FirebaseClient,
    private val webRTCFactory: WebRTCFactory,
    private val application: Application,
    private val gson: Gson
) : ViewModel() {

    private val rtcAudioManager by lazy { RTCAudioManager.create(application) }
    private var rtcClient: RTCClient? = null
    private var remoteSurface: SurfaceViewRenderer? = null
    private var participantId: String = "test-participant"

    var matchState: MutableStateFlow<MatchState> = MutableStateFlow(MatchState.NewState)
        private set

    var chatList: MutableStateFlow<List<ChatItems>> = MutableStateFlow(mutableListOf())
    private fun addChatItem(newChatItem: ChatItems) {
        val currentList = chatList.value.toMutableList()
        currentList.add(newChatItem)
        chatList.value = currentList
    }

    private fun resetChatList() {
        chatList.value = mutableListOf()
    }

    fun sendChatItem(newChatItem: ChatItems) {
        addChatItem(newChatItem)
        viewModelScope.launch {
            firebaseClient.updateParticipantDataModel(
                participantId = participantId,
                data = SignalDataModel(type = SignalDataModelTypes.CHAT, data = newChatItem.text)
            )
        }
    }

    init {
        rtcAudioManager.setDefaultAudioDevice(RTCAudioManager.AudioDevice.SPEAKER_PHONE)
    }

    fun permissionsGranted() {
        firebaseClient.observeUserStatus { status ->
            matchState.value = status
            when (status) {
                is MatchState.ReceivedMatchState -> handleIncomingMatchCase(status)
                is MatchState.OfferedMatchState -> handleSentOffer(status)
                is MatchState.LookingForMatchState -> handleLookingForMatch()
                else -> Unit
            }
        }
        firebaseClient.observeIncomingSignals { signalDataModel ->
            Log.d("TAG", "incoming signal model: $signalDataModel")
            when (signalDataModel.type) {
                SignalDataModelTypes.OFFER -> handleReceivedOfferSdp(signalDataModel)
                SignalDataModelTypes.ANSWER -> handleReceivedAnswerSdp(signalDataModel)
                SignalDataModelTypes.ICE -> handleReceivedIceCandidate(signalDataModel)
                SignalDataModelTypes.CHAT -> handleReceivedChatItem(signalDataModel)

                null -> Unit
            }
        }

        findNextMatch()
    }

    private fun handleReceivedChatItem(signalDataModel: SignalDataModel) {
        addChatItem(
            ChatItems(
                text = signalDataModel.data.toString(), isMine = false
            )
        )
    }

    private fun handleLookingForMatch() {
        resetChatList()
        rtcClient?.onDestroy()
        viewModelScope.launch {
            firebaseClient.findNextMatch()
        }
    }

    private fun handleReceivedIceCandidate(signalDataModel: SignalDataModel) {
        runCatching {
            gson.fromJson(
                signalDataModel.data.toString(), IceCandidate::class.java
            )
        }.onSuccess {
            rtcClient?.onIceCandidateReceived(it)
        }.onFailure {
            Log.d("TAG", "handleReceivedIceCandidate: ${it.message}")
        }
    }

    private fun handleReceivedAnswerSdp(signalDataModel: SignalDataModel) {
        rtcClient?.onRemoteSessionReceived(
            SessionDescription(
                SessionDescription.Type.ANSWER, signalDataModel.data.toString()
            )
        )
    }

    private fun handleReceivedOfferSdp(signalDataModel: SignalDataModel) {
        setupRtcConnection(participantId)?.also {
            it.onRemoteSessionReceived(
                SessionDescription(
                    SessionDescription.Type.OFFER, signalDataModel.data.toString()
                )
            )
            it.answer()
        }
    }

    private fun handleSentOffer(status: MatchState.OfferedMatchState) {
        this.participantId = status.participant
    }

    private fun handleIncomingMatchCase(status: MatchState.ReceivedMatchState) {
        this.participantId = status.participant
        setupRtcConnection(participantId)?.also {
            it.offer()
        }
    }

    private fun setupRtcConnection(participant: String): RTCClient? {
        runCatching { rtcClient?.onDestroy() }
        rtcClient = null
        rtcClient = webRTCFactory.createRTCClient(observer = object : PeerObserver() {
            override fun onIceCandidate(p0: IceCandidate?) {
                super.onIceCandidate(p0)
                p0?.let {
                    rtcClient?.onLocalIceCandidateGenerated(it)
                }
            }

            override fun onAddStream(p0: MediaStream?) {
                super.onAddStream(p0)
                p0?.let {
                    runCatching {
                        remoteSurface?.let { surface ->
                            it.videoTracks[0]?.addSink(surface)
                        }
                    }
                }
            }

            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
                super.onConnectionChange(newState)
                if (newState == PeerConnection.PeerConnectionState.CONNECTED) {
                    viewModelScope.launch {
                        firebaseClient.updateSelfStatus(StatusDataModel(type = StatusDataModelTypes.Connected))
                        firebaseClient.removeSelfData()
                    }
                }
                Log.d("TAG", "onConnectionChange: $newState")
            }
        }, listener = object : RTCClientImpl.TransferDataToServerCallback {
            override fun onIceGenerated(iceCandidate: IceCandidate) {
                viewModelScope.launch {
                    firebaseClient.updateParticipantDataModel(
                        participantId = participant, data = SignalDataModel(
                            type = SignalDataModelTypes.ICE, data = gson.toJson(iceCandidate)
                        )
                    )
                }
            }

            override fun onOfferGenerated(sessionDescription: SessionDescription) {
                viewModelScope.launch {
                    firebaseClient.updateParticipantDataModel(
                        participantId = participant, data = SignalDataModel(
                            type = SignalDataModelTypes.OFFER, data = sessionDescription.description
                        )
                    )
                }
            }

            override fun onAnswerGenerated(sessionDescription: SessionDescription) {
                viewModelScope.launch {
                    firebaseClient.updateParticipantDataModel(
                        participantId = participant, data = SignalDataModel(
                            type = SignalDataModelTypes.ANSWER,
                            data = sessionDescription.description
                        )
                    )
                }
            }

        })
        return rtcClient
    }

    fun startLocalStream(surface: SurfaceViewRenderer) {
        webRTCFactory.prepareLocalStream(surface)
    }

    fun initRemoteSurfaceView(remoteSurface: SurfaceViewRenderer) {
        this.remoteSurface = remoteSurface
        webRTCFactory.initSurfaceView(remoteSurface)
    }

    private fun findNextMatch() {
        rtcClient?.onDestroy()
        viewModelScope.launch {
            if (matchState.value == MatchState.Connected) {
                firebaseClient.updateParticipantStatus(
                    participantId, StatusDataModel(type = StatusDataModelTypes.LookingForMatch)
                )
            }
            firebaseClient.updateSelfStatus(StatusDataModel(type = StatusDataModelTypes.LookingForMatch))
        }
    }

    override fun onCleared() {
        super.onCleared()
        remoteSurface?.release()
        remoteSurface = null
        firebaseClient.clear()
        webRTCFactory.onDestroy()
        rtcClient?.onDestroy()
    }
}