package com.miguelprojects.myapplication.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.miguelprojects.myapplication.model.UserWorkspaceModel
import com.miguelprojects.myapplication.repository.UserWorkspaceRepository

class UserWorkspaceViewModel(val repository: UserWorkspaceRepository): ViewModel() {
    private val _userWorkspaceModel = MutableLiveData<UserWorkspaceModel>()
    val userWorkspaceModel: LiveData<UserWorkspaceModel> get() = _userWorkspaceModel
    private val _listUserWorkspaceModel = MutableLiveData<List<UserWorkspaceModel>>()
    val listUserWorkspaceModel: LiveData<List<UserWorkspaceModel>> get() = _listUserWorkspaceModel

    constructor(): this (UserWorkspaceRepository()) {}

    fun saveData(userWorkspace: UserWorkspaceModel, callback: (String) -> Unit) {
        repository.saveUserWorkspace(userWorkspace) { userWorkspaceId ->
            if(userWorkspaceId.isNotEmpty()) {
                callback(userWorkspaceId)
                Log.d("UserWorkspaceViewModel", "UserWorkspaceViewModel ID not null: $userWorkspaceId ")
            } else {
                callback("")
                Log.d("UserWorkspaceViewModel", "UserWorkspaceViewModel ID null")
            }
        }
    }

    fun loadData(workspaceId: String, userId: String) {
        repository.loadUserWorkspace(workspaceId, userId) { userWorkspace ->
            _userWorkspaceModel.value = userWorkspace
        }
    }

    fun loadListData(userId: String) {
        repository.loadListUserWorkspaces(userId) { listUserWorkspace ->
            _listUserWorkspaceModel.value = listUserWorkspace
        }
    }

    fun getData(): LiveData<UserWorkspaceModel> {
        return userWorkspaceModel
    }

    fun getListData(): LiveData<List<UserWorkspaceModel>> {
        return listUserWorkspaceModel
    }
}