package com.miguelprojects.myapplication.util

import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.GenericTypeIndicator

object WorkspaceManager {

    fun getNumberOfParticipants(workspaceId: String, callback: (Int) -> Unit) {
        val reference = FirebaseDatabase.getInstance().getReference("workspaces").child(workspaceId)
        reference.get().addOnSuccessListener { dataSnapshot ->
            if (dataSnapshot.exists()) {
                val userIdsMap = dataSnapshot.child("user_ids")
                    .getValue(object : GenericTypeIndicator<Map<String, Boolean>>() {})
                if (userIdsMap != null) {
                    val numberParticipants = userIdsMap.size
                    callback(numberParticipants)
                } else {
                    callback(0)
                }
            } else {
                callback(0)
            }
        }.addOnFailureListener {
            callback(0)
        }
    }

}