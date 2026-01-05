package com.example.promptdispenser.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.promptdispenser.data.PromptListEntity
import com.example.promptdispenser.data.PromptRepository
import kotlinx.coroutines.launch

class PromptViewModel(private val repository: PromptRepository) : ViewModel() {
    val allLists: LiveData<List<PromptListEntity>> = repository.allLists.asLiveData()

    fun insert(list: PromptListEntity) = viewModelScope.launch { repository.insert(list) }
    fun update(list: PromptListEntity) = viewModelScope.launch { repository.update(list) }
    fun delete(list: PromptListEntity) = viewModelScope.launch { repository.delete(list) }
}

class PromptViewModelFactory(private val repository: PromptRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PromptViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PromptViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}