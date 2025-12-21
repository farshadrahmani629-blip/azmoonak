// app/src/main/java/com/examapp/ui/results/ResultViewModelFactory.kt
package com.examapp.ui.results

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.examapp.data.repository.ResultRepository

class ResultViewModelFactory(
    private val resultRepository: ResultRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ResultViewModel::class.java)) {
            return ResultViewModel(resultRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}