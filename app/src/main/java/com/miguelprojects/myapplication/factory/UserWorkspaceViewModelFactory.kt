package com.miguelprojects.myapplication.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.miguelprojects.myapplication.repository.UserWorkspaceRepository
import com.miguelprojects.myapplication.viewmodel.UserWorkspaceViewModel

class UserWorkspaceViewModelFactory(private val repository: UserWorkspaceRepository) :
    ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UserWorkspaceViewModel::class.java)) {
            return UserWorkspaceViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }

}


//class UserViewModelFactory(private val repository: UserRepository) : ViewModelProvider.Factory {
//    override fun <T : ViewModel> create(modelClass: Class<T>): T {
//        if (modelClass.isAssignableFrom(UserViewModel::class.java)) {
//            return UserViewModel(repository) as T
//        }
//        throw IllegalArgumentException("Unknown ViewModel class")
//    }
//}