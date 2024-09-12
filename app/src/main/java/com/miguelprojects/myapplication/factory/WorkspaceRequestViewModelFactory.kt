package com.miguelprojects.myapplication.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.miguelprojects.myapplication.repository.WorkspaceRequestRepository
import com.miguelprojects.myapplication.viewmodel.WorkspaceRequestViewModel

class WorkspaceRequestViewModelFactory(private val repository: WorkspaceRequestRepository) :
    ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WorkspaceRequestViewModel::class.java)) {
            return WorkspaceRequestViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}