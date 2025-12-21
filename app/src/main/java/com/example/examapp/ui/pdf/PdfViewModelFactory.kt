// app/src/main/java/com/examapp/ui/pdf/PdfViewModelFactory.kt
package com.examapp.ui.pdf

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.examapp.data.repository.ResultRepository

class PdfViewModelFactory(
    private val resultRepository: ResultRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PdfViewModel::class.java)) {
            return PdfViewModel(resultRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}