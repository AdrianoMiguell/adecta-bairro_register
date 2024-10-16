package com.miguelprojects.myapplication.util

import android.content.Intent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.GenericTypeIndicator
import com.miguelprojects.myapplication.ui.activitys.MainActivity
import com.miguelprojects.myapplication.viewmodel.WorkspaceViewModel

object WorkspaceManager {

    fun verifyExistsOnlineWorkspaceData(
        activity: AppCompatActivity,
        workspaceViewModel: WorkspaceViewModel,
        workspaceId: String,
        callback: (Boolean) -> Unit
    ) {
        workspaceViewModel.verifyExistsWorkspaceByOffId(workspaceId) { res, _ ->
            if (!res) {
                Toast.makeText(
                    activity,
                    "Sincronização dos dados em andamento!",
                    Toast.LENGTH_SHORT
                ).show()

                activity.startActivity(
                    Intent(
                        activity,
                        MainActivity::class.java
                    ).addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    )
                )
                activity.finish()
            }

            callback(res)
        }
    }

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