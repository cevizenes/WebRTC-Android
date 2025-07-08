package com.example.webrtc_android.remote

import android.util.Log
import com.example.webrtc_android.utils.FirebaseFieldNames
import com.example.webrtc_android.utils.MatchState
import com.example.webrtc_android.utils.SharedPrefHelper
import com.example.webrtc_android.utils.SignalDataModel
import com.example.webrtc_android.utils.ValueEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseClient @Inject constructor(
    private val database: DatabaseReference,
    private val prefHelper: SharedPrefHelper,
    private val gson: Gson
) {

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun observeUserStatus(callback: (MatchState) -> Unit) {
        coroutineScope.launch {
            removeSelfData()
            updateSelfStatus(StatusDataModel(type = StatusDataModelTypes.LookingForMatch))

            val userId = prefHelper.getUserId()
            val statusRef = database.child(FirebaseFieldNames.USERS).child(userId)
                .child(FirebaseFieldNames.STATUS)

            statusRef.addValueEventListener(object : ValueEventListener() {
                override fun onDataChange(p0: DataSnapshot) {
                    p0.getValue(StatusDataModel::class.java)?.let { status ->
                        val newState = when (status.type) {
                            StatusDataModelTypes.LookingForMatch -> MatchState.LookingForMatchState
                            StatusDataModelTypes.OfferedMatch -> MatchState.OfferedMatchState(status.participant!!)
                            StatusDataModelTypes.ReceivedMatch -> MatchState.ReceivedMatchState(status.participant!!)
                            StatusDataModelTypes.IDLE -> MatchState.IDLE
                            StatusDataModelTypes.Connected -> MatchState.Connected
                            else -> null
                        }

                        newState?.let { callback(it) } ?: coroutineScope.launch {
                            updateSelfStatus(StatusDataModel(type = StatusDataModelTypes.LookingForMatch))
                            callback(MatchState.LookingForMatchState)
                        }
                    } ?: coroutineScope.launch {
                        updateSelfStatus(StatusDataModel(type = StatusDataModelTypes.LookingForMatch))
                        callback(MatchState.LookingForMatchState)
                    }
                }
            })
        }
    }

    fun observeIncomingSignals(callback: (SignalDataModel) -> Unit) {
        database.child(FirebaseFieldNames.USERS).child(prefHelper.getUserId())
            .child(FirebaseFieldNames.DATA).addValueEventListener(object : ValueEventListener() {
                override fun onDataChange(p0: DataSnapshot) {
                    super.onDataChange(p0)
                    runCatching {
                        gson.fromJson(p0.value.toString(), SignalDataModel::class.java)
                    }.onSuccess {
                        if (it != null) callback(it)
                    }.onFailure {
                        Log.d("TAG", "onDataChange: ${it.message}")
                    }
                }
            })
    }

    suspend fun updateParticipantDataModel(participantId: String, data: SignalDataModel) {
        database.child(FirebaseFieldNames.USERS).child(participantId).child(FirebaseFieldNames.DATA)
            .setValue(gson.toJson(data)).await()
    }

    suspend fun updateSelfStatus(status: StatusDataModel) {
        database.child(FirebaseFieldNames.USERS).child(prefHelper.getUserId())
            .child(FirebaseFieldNames.STATUS).setValue(status)
            .await() // Suspends until Firebase operation completes
    }

    suspend fun updateParticipantStatus(participantId: String, status: StatusDataModel) {
        database.child(FirebaseFieldNames.USERS).child(participantId)
            .child(FirebaseFieldNames.STATUS).setValue(status).await()
    }

    suspend fun findNextMatch() {
        removeSelfData()
        findAvailableParticipant { foundTarget ->
            Log.d("TAG", "findNextMatch: $foundTarget")
            foundTarget?.let { target ->
                database.child(FirebaseFieldNames.USERS).child(target)
                    .child(FirebaseFieldNames.STATUS).setValue(
                        StatusDataModel(
                            participant = prefHelper.getUserId(), type = StatusDataModelTypes.ReceivedMatch
                        )
                    )

                coroutineScope.launch {
                    updateSelfStatus(StatusDataModel(type = StatusDataModelTypes.OfferedMatch, participant = target))
                }
            }
        }
    }

    private fun findAvailableParticipant(callback: (String?) -> Unit) {
        database.child(FirebaseFieldNames.USERS).orderByChild("status/type")
            .equalTo(StatusDataModelTypes.LookingForMatch.name)
            .addListenerForSingleValueEvent(object : ValueEventListener() {
                override fun onDataChange(p0: DataSnapshot) {
                    var foundTarget: String? = null
                    p0.children.forEach { childSnapshot ->
                        if (childSnapshot.key != prefHelper.getUserId()) {
                            foundTarget = childSnapshot.key
                            return@forEach
                        }
                    }
                    callback(foundTarget)
                }

                override fun onCancelled(p0: DatabaseError) {
                    callback(null)
                }
            })
    }

    suspend fun removeSelfData() {
        database.child(FirebaseFieldNames.USERS).child(prefHelper.getUserId())
            .child(FirebaseFieldNames.DATA).removeValue().await()
    }

    fun clear() {
        coroutineScope.cancel()
    }
}