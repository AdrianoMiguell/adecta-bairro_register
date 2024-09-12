package com.miguelprojects.myapplication.repository

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.miguelprojects.myapplication.model.UserWorkspaceModel

class UserWorkspaceRepository {
    fun saveUserWorkspace(userWorkspace: UserWorkspaceModel, callback: (String) -> Unit) {
        val reference = FirebaseDatabase.getInstance().getReference("user_workspaces").push()
        userWorkspace.id = reference.key ?: ""
        reference.setValue(userWorkspace)
            .addOnCompleteListener { task ->
                callback(userWorkspace.id)
            }
            .addOnFailureListener { callback("") }
    }

    fun loadUserWorkspace(
        workspaceId: String,
        userId: String,
        callback: (UserWorkspaceModel) -> Unit
    ) {
        val reference: DatabaseReference =
            FirebaseDatabase.getInstance().getReference("user_workspaces")

        reference.orderByChild("workspace_id").equalTo(workspaceId).get()
            .addOnSuccessListener { dataSnapshot ->
                if (dataSnapshot.exists()) {
                    for (data in dataSnapshot.children) {
                        val userWorkspace = data.getValue(UserWorkspaceModel::class.java)
                        if (userWorkspace?.user_id == userId) {
                            callback(userWorkspace)
                            return@addOnSuccessListener
                        }
                    }
                    callback(UserWorkspaceModel())
                } else {
                    callback(UserWorkspaceModel())
                    Log.d("LoadUserWorkspace", "Userworkspace não encontrado")
                }
            }.addOnFailureListener { exception ->
                callback(UserWorkspaceModel())
                Log.d("LoadUserWorkspace", "Userworkspace não encontrado")
            }
    }

    fun loadListUserWorkspaces(userId: String, callback: (List<UserWorkspaceModel>) -> Unit) {
        val reference: DatabaseReference =
            FirebaseDatabase.getInstance().getReference("user_workspaces")
        val query = reference.orderByChild("user_id").equalTo(userId)

        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val userWorkspaces = mutableListOf<UserWorkspaceModel>()
                for (snapshot in dataSnapshot.children) {
                    val userWorkspace = snapshot.getValue(UserWorkspaceModel::class.java)
                    if (userWorkspace != null) {
                        userWorkspaces.add(userWorkspace)
                    }
                }
                callback(userWorkspaces)
            }

            override fun onCancelled(error: DatabaseError) {
                callback(emptyList())
            }
        })
    }


}
