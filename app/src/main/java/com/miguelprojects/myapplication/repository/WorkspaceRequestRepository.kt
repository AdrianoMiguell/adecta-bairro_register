package com.miguelprojects.myapplication.repository

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.miguelprojects.myapplication.model.WorkspaceModel
import com.miguelprojects.myapplication.model.WorkspaceRequestModel

class WorkspaceRequestRepository {
    fun sendWorkspaceRequest(
        inviteCode: String,
        userId: String,
        callback: (Boolean, String) -> Unit
    ) {
        val workspaceRef = FirebaseDatabase.getInstance().reference.child("workspaces")
        workspaceRef.orderByChild("inviteCode").equalTo(inviteCode)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val workspaceSnapshot = snapshot.children.first()
                        val workspaceId = workspaceSnapshot.key ?: return
                        val workspace = workspaceSnapshot.getValue(WorkspaceModel::class.java)

                        if (workspace != null) {
                            val creatorId = workspace.creator
                            if (userId == creatorId) {
                                callback(
                                    false,
                                    "Você não pode enviar uma solicitação para seu próprio workspace."
                                )
                                return
                            }

                            val requestsRef = FirebaseDatabase.getInstance().reference
                                .child("workspaceRequests")
                                .child(workspaceId)

                            // Usar uma chave composta para evitar duplicatas
                            val requestKey = "$workspaceId-$userId"

                            requestsRef.child(requestKey)
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(requestSnapshot: DataSnapshot) {
                                        if (requestSnapshot.exists()) {
                                            callback(
                                                false,
                                                "Você já enviou uma solicitação para este workspace."
                                            )
                                        } else {
                                            val request = mapOf(
                                                "userId" to userId,
                                                "status" to "pending"
                                            )

                                            requestsRef.child(requestKey).setValue(request)
                                                .addOnCompleteListener { task ->
                                                    if (task.isSuccessful) {
                                                        callback(
                                                            true,
                                                            "Solicitação enviada com sucesso."
                                                        )
                                                    } else {
                                                        callback(
                                                            false,
                                                            "Falha ao enviar a solicitação."
                                                        )
                                                    }
                                                }
                                        }
                                    }

                                    override fun onCancelled(error: DatabaseError) {
                                        callback(false, "Erro ao verificar solicitações")
                                    }
                                })
                        }
                    } else {
                        callback(false, "Código de convite inválido.")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    callback(false, "Erro. Processo cancelado inesperadamente.")
                }
            })
    }


    fun loadListWorkspaceRequests(
        workspaceId: String,
        callback: (List<WorkspaceRequestModel>) -> Unit
    ) {
        val reference =
            FirebaseDatabase.getInstance().getReference("workspaceRequests").child(workspaceId)
        reference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    val listWorkspaceRequest = mutableListOf<WorkspaceRequestModel>()
                    for (workspace in dataSnapshot.children) {
                        val request = workspace.getValue(WorkspaceRequestModel::class.java)
                        if (request != null) {
                            listWorkspaceRequest.add(request)
                        }
                    }
                    callback(listWorkspaceRequest)
                } else {
                    Log.d(
                        "LoadListWorkspaceRequest",
                        "No requests found for workspaceId: $workspaceId"
                    )
                    callback(emptyList())
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("LoadListWorkspaceRequest", "Error loading requests: ${error.message}")
                callback(emptyList())
            }
        })
    }

    //    fun allowUserRequest(
//        workspaceId: String,
//        listUserIds: List<String>,
//        callback: (Boolean, String) -> Unit
//    ) {
//        val database = FirebaseDatabase.getInstance()
//        val workspaceRef = database.getReference("workspaces").child(workspaceId)
//        val requestsRef = database.getReference("workspaceRequests").child(workspaceId)
//
//        requestsRef.addListenerForSingleValueEvent(object : ValueEventListener {
//            override fun onDataChange(snapshot: DataSnapshot) {
//                if (snapshot.exists()) {
//                    val workspaceRequests = mutableListOf<WorkspaceRequestModel>()
//
////                    Adicionar requisições na lista
//                    for (requestData in snapshot.children) {
//                        val worksReq = requestData.getValue(WorkspaceRequestModel::class.java)
//                        if (worksReq != null && listUserIds.contains(worksReq.userId)) {
//                            workspaceRequests.add(worksReq)
//                        }
//                    }
//
//                    // Lê os dados do workspace
//                    workspaceRef.get().addOnSuccessListener { snapshot ->
//                        if (snapshot.exists()) {
//                            val workspace = snapshot.getValue(WorkspaceModel::class.java)
//                            if (workspace != null) {
//                                // Atualiza apenas os user_ids do workspace, mantendo os outros dados
//                                workspaceRequests.forEach { request ->
//                                    workspace.userIds[request.userId] = true
//                                }
//
//                                // Atualiza os dados específicos do workspace no Firebase
//                                val updates = hashMapOf<String, Any>(
//                                    "userIds" to workspace.userIds
//                                    // Adicione outros campos que você deseja manter aqui
//                                )
//
//                                // Atualiza os dados do workspace no Firebase
//                                workspaceRef.updateChildren(updates)
//                                    .addOnSuccessListener {
//                                        // Após atualizar com sucesso, exclui as requisições
//                                        workspaceRequests.forEach { request ->
//                                            requestsRef.child(request.id).removeValue()
//                                                .addOnSuccessListener {
//                                                    println("Requisição deletada com sucesso: ${request.id}")
//                                                }
//                                                .addOnFailureListener { error ->
//                                                    callback(
//                                                        false,
//                                                        "Erro ao deletar a requisição: $error"
//                                                    )
//                                                    println("Erro ao deletar a requisição: $error")
//                                                }
//                                        }
//
//                                        callback(
//                                            true,
//                                            "Usuários adicionados ao workspace com sucesso."
//                                        )
//                                    }
//                                    .addOnFailureListener { error ->
//                                        callback(false, "Erro ao atualizar o workspace: $error")
//                                        println("Erro ao atualizar o workspace: $error")
//                                    }
//                            } else {
//                                callback(false, "Workspace não encontrado.")
//                                println("Workspace não encontrado.")
//                            }
//                        } else {
//                            callback(false, "Workspace não encontrado.")
//                            println("Workspace não encontrado.")
//                        }
//                    }.addOnFailureListener { errorWork ->
//                        callback(false, "Erro ao ler os dados do workspace: $errorWork")
//                        println("Erro ao ler os dados do workspace: $errorWork")
//                    }
//                } else {
//                    callback(false, "Nenhuma requisição encontrada para os usuários fornecidos.")
//                    println("Nenhuma requisição encontrada para os usuários fornecidos.")
//                }
//            }
//
//            override fun onCancelled(error: DatabaseError) {
//                callback(false, "Erro! Processo cancelado: ${error.message}")
//                println("Erro! Processo cancelado: ${error.message}")
//            }
//        })
//    }
    fun allowUserRequest(
        workspaceId: String,
        listUserIds: List<String>,
        callback: (Boolean, String) -> Unit
    ) {
        val database = FirebaseDatabase.getInstance()
        val workspaceRef = database.getReference("workspaces").child(workspaceId)

        // Lê os dados do workspace
        workspaceRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val workspace = snapshot.getValue(WorkspaceModel::class.java)
                if (workspace != null) {
                    // Atualiza apenas os user_ids do workspace, mantendo os outros dados
                    listUserIds.forEach { userId ->
                        workspace.userIds[userId] = true
                    }

                    // Atualiza os dados específicos do workspace no Firebase
                    val updates = hashMapOf<String, Any>(
                        "userIds" to workspace.userIds
                    )

                    // Atualiza os dados do workspace no Firebase
                    workspaceRef.updateChildren(updates)
                        .addOnSuccessListener {
                            deleteUserRequests(
                                workspaceId,
                                listUserIds
                            ) { deleteSuccess, deleteMessage ->
                                if (deleteSuccess) {
                                    callback(
                                        true,
                                        "Usuários adicionados ao workspace com sucesso."
                                    )
                                } else {
                                    callback(
                                        false,
                                        "Erro ao execultar esta ação. Por favor, reporte esse problema!"
                                    )
                                    Log.d("deleteUserRequests", deleteMessage)
                                }
                            }
                        }
                        .addOnFailureListener { error ->
                            callback(false, "Erro ao atualizar o workspace: $error")
                            println("Erro ao atualizar o workspace: $error")
                        }
                } else {
                    callback(false, "Workspace não encontrado.")
                    println("Workspace não encontrado.")
                }
            } else {
                callback(false, "Workspace não encontrado.")
                println("Workspace não encontrado.")
            }
        }.addOnFailureListener { errorWork ->
            callback(false, "Erro ao ler os dados do workspace: $errorWork")
            println("Erro ao ler os dados do workspace: $errorWork")
        }
    }

    fun deleteUserRequests(
        workspaceId: String,
        listUserIds: List<String>,
        callback: (Boolean, String) -> Unit
    ) {
        val database = FirebaseDatabase.getInstance()
        val requestsRef = database.getReference("workspaceRequests").child(workspaceId)

        requestsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val workspaceRequests = mutableListOf<WorkspaceRequestModel>()

                    // Adicionar requisições na lista
                    for (requestData in snapshot.children) {
                        val worksReq = requestData.getValue(WorkspaceRequestModel::class.java)
                        if (worksReq != null && listUserIds.contains(worksReq.userId)) {
                            workspaceRequests.add(worksReq)
                        }
                    }

                    // Excluir as requisições
                    workspaceRequests.forEach { request ->
                        requestsRef.child(request.id).removeValue()
                            .addOnSuccessListener {
                                println("Requisição deletada com sucesso: ${request.id}")
                            }
                            .addOnFailureListener { error ->
                                callback(
                                    false,
                                    "Erro ao deletar a requisição: $error"
                                )
                                println("Erro ao deletar a requisição: $error")
                            }
                    }

                    callback(
                        true,
                        "Requisições deletadas com sucesso."
                    )
                } else {
                    callback(false, "Nenhuma requisição encontrada para os usuários fornecidos.")
                    println("Nenhuma requisição encontrada para os usuários fornecidos.")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                callback(false, "Erro! Processo cancelado: ${error.message}")
                println("Erro! Processo cancelado: ${error.message}")
            }
        })
    }


}