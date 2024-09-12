package com.miguelprojects.myapplication.factory

import WorkspaceRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.miguelprojects.myapplication.viewmodel.WorkspaceViewModel

class WorkspaceViewModelFactory(private val repository: WorkspaceRepository) :
    ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WorkspaceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WorkspaceViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}