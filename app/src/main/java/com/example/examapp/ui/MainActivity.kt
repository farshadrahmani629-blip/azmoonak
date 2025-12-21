// app/src/main/java/com/examapp/ui/MainViewModel.kt
package com.examapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.examapp.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _userGreeting = MutableStateFlow("")
    val userGreeting: StateFlow<String> = _userGreeting.asStateFlow()

    init {
        refreshData()
    }

    fun refreshData() {
        viewModelScope.launch {
            try {
                _uiState.value = MainUiState(isLoading = true)

                val currentUser = authRepository.getCurrentUser()
                val greeting = if (currentUser != null) {
                    "سلام ${currentUser.firstName} ${currentUser.lastName}"
                } else {
                    "به آزمون‌ساز خوش آمدید"
                }

                _userGreeting.value = greeting
                _uiState.value = MainUiState(
                    isLoading = false,
                    isAuthenticated = currentUser != null,
                    user = currentUser
                )
            } catch (e: Exception) {
                _uiState.value = MainUiState(
                    isLoading = false,
                    error = "خطا در بارگذاری اطلاعات: ${e.message}"
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                authRepository.logout()
                refreshData()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "خطا در خروج: ${e.message}"
                )
            }
        }
    }
}

data class MainUiState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val user: com.examapp.data.models.User? = null,
    val error: String? = null
)