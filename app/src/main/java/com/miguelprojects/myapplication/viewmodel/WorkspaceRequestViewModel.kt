package com.miguelprojects.myapplication.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.miguelprojects.myapplication.model.WorkspaceRequestModel
import com.miguelprojects.myapplication.repository.WorkspaceRequestRepository

class WorkspaceRequestViewModel(private val repository: WorkspaceRequestRepository) : ViewModel() {
    private val _workspaceRequestModel = MutableLiveData<WorkspaceRequestModel>()
    val workspaceRequestModel: LiveData<WorkspaceRequestModel> get() = _workspaceRequestModel

    private val _listWorkspaceRequestModel = MutableLiveData<List<WorkspaceRequestModel>>()
    val listWorkspaceRequestModel: LiveData<List<WorkspaceRequestModel>> get() = _listWorkspaceRequestModel

    // Construtor padrão público necessário para ViewModelProvider
    constructor() : this(WorkspaceRequestRepository()) {
        // Inicializações adicionais, se necessário
    }

    fun sendRequestJoin(inviteCode: String, userId: String, callback: (Boolean, String) -> Unit) {
        repository.sendWorkspaceRequest(inviteCode, userId) { res, message ->
            callback(res, message)
        }
    }

    fun loadRequestJoin(workspaceId: String) {
        repository.loadListWorkspaceRequests(workspaceId) { list ->
            _listWorkspaceRequestModel.value = list
            if (list.isNullOrEmpty()) {
                Log.d("Teste View Model Workspace Request", "Lista está nula ou vazia.")
            }
        }
    }

    fun allowUserRequest(
        workspaceId: String,
        listUserIds: List<String>,
        callback: (Boolean, String) -> Unit
    ) {
        repository.allowUserRequest(workspaceId, listUserIds) { res, message ->
            callback(res, message)
        }
    }

    fun deleteUserRequests(
        workspaceId: String,
        listUserIds: List<String>,
        callback: (Boolean, String) -> Unit
    ) {
        repository.deleteUserRequests(workspaceId, listUserIds) { res, message ->
            callback(res, message)
        }
    }
}