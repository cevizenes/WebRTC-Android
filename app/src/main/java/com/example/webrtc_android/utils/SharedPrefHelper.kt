package com.example.webrtc_android.utils

import android.content.Context
import javax.inject.Inject
import androidx.core.content.edit

class SharedPrefHelper @Inject constructor(context: Context) {

    companion object {
        private const val PREF_NAME = "webrtc_android"
        private const val USER_ID_KEY = "user_id_key"
    }

    private val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun getUserId() : String {
        val userId = sharedPreferences.getString(USER_ID_KEY, null)
        return if (userId.isNullOrEmpty()) {
            val newUserId = java.util.UUID.randomUUID().toString().substring(0,6)
            saveUserId(newUserId)
            newUserId
        } else {
            userId
        }
    }

    private fun saveUserId(userId: String) {
        sharedPreferences.edit { putString(USER_ID_KEY, userId) }
    }
}