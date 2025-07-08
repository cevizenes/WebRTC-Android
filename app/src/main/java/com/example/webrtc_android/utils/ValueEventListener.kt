package com.example.webrtc_android.utils

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

open class ValueEventListener: ValueEventListener {
    override fun onDataChange(p0: DataSnapshot) {
    }

    override fun onCancelled(p0: DatabaseError) {
    }
}