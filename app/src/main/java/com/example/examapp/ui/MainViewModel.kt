// app/src/main/java/com/examapp/ui/MainViewModel.kt
package com.examapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.examapp.data.models.Book
import com.examapp.data.models.User
import com.examapp.data.repository.AuthRepository
import com.examapp.data.repository.BookRepository
import com.examapp.data.repository.ExamRepository
import com.examapp.data.repository.QuestionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val bookRepository: BookRepository,
    private val examRepository: ExamRepository,
    private val questionRepository: QuestionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<MainUiState>(MainUiState.Loading)
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _userGreeting = MutableStateFlow("")
    val userGreeting: StateFlow<String> = _userGreeting.asStateFlow()

    init {
        checkUserStatus()
        loadInitialData()
    }

    private fun checkUserStatus() {
        val currentUser = authRepository.getCurrentUser()
        _userGreeting.value = if (currentUser != null) {
            "سلام ${currentUser.firstName} ${currentUser.lastName}"
        } else {
            "خوش آمدید! لطفاً وارد شوید"
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            try {
                _uiState.value = MainUiState.Loading

                // بارگذاری اولیه کتاب‌ها
                val booksResult = bookRepository.getAllBooks()
                if (booksResult.isSuccess) {
                    _uiState.value = MainUiState.Success(
                        books = booksResult.getOrNull() ?: emptyList(),
                        isLoggedIn = authRepository.isLoggedIn(),
                        user = authRepository.getCurrentUser()
                    )
                } else {
                    _uiState.value = MainUiState.Error(
                        message = booksResult.exceptionOrNull()?.message ?: "خطا در بارگذاری داده‌ها",
                        isLoggedIn = authRepository.isLoggedIn()
                    )
                }
            } catch (e: Exception) {
                _uiState.value = MainUiState.Error(
                    message = "خطا در اتصال: ${e.message}",
                    isLoggedIn = authRepository.isLoggedIn()
                )
            }
        }
    }

    fun refreshData() {
        loadInitialData()
    }

    fun logout() {
        viewModelScope.launch {
            try {
                authRepository.logout()
                checkUserStatus()
                _uiState.value = MainUiState.Success(
                    books = emptyList(),
                    isLoggedIn = false,
                    user = null
                )
            } catch (e: Exception) {
                _uiState.value = MainUiState.Error(
                    message = "خطا در خروج: ${e.message}",
                    isLoggedIn = authRepository.isLoggedIn()
                )
            }
        }
    }

    fun getCurrentUser(): User? {
        return authRepository.getCurrentUser()
    }

    fun isLoggedIn(): Boolean {
        return authRepository.isLoggedIn()
    }

    fun getUserStats(): Map<String, Any> {
        // اینجا می‌توانید آمار کاربر را محاسبه کنید
        return mapOf(
            "totalExams" to 0,
            "completedExams" to 0,
            "averageScore" to 0f,
            "rank" to 0
        )
    }
}

sealed class MainUiState {
    data object Loading : MainUiState()
    data class Success(
        val books: List<Book>,
        val isLoggedIn: Boolean,
        val user: User? = null
    ) : MainUiState()
    data class Error(
        val message: String,
        val isLoggedIn: Boolean
    ) : MainUiState()
}