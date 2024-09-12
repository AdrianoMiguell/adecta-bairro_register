package com.miguelprojects.myapplication.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.miguelprojects.myapplication.repository.CitizenRepository
import com.miguelprojects.myapplication.viewmodel.CitizenViewModel

class CitizenViewModelFactory(private val repository: CitizenRepository) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CitizenViewModel::class.java)) {
            return CitizenViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
