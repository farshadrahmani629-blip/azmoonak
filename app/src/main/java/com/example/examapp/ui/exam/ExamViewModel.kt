// app/src/main/java/com/examapp/ui/exam/ExamViewModel.kt
package com.examapp.ui.exam

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.examapp.data.models.*
import com.examapp.data.repository.ExamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExamViewModel @Inject constructor(
    private val examRepository: ExamRepository
) : ViewModel() {

    private val _examsState = MutableStateFlow<ExamsState>(ExamsState.Loading)
    val examsState: StateFlow<ExamsState> = _examsState.asStateFlow()

    private val _examGenerationState = MutableStateFlow<ExamGenerationState>(ExamGenerationState.Idle)
    val examGenerationState: StateFlow<ExamGenerationState> = _examGenerationState.asStateFlow()

    private val _currentExam = MutableStateFlow<Exam?>(null)
    val currentExam: StateFlow<Exam?> = _currentExam.asStateFlow()

    private val _examQuestions = MutableStateFlow<List<Question>>(emptyList())
    val examQuestions: StateFlow<List<Question>> = _examQuestions.asStateFlow()

    private val _examResults = MutableStateFlow<ExamResultsState>(ExamResultsState.Loading)
    val examResults: StateFlow<ExamResultsState> = _examResults.asStateFlow()

    private val _examFilter = MutableStateFlow<ExamStatus?>(null)
    val examFilter: StateFlow<ExamStatus?> = _examFilter.asStateFlow()

    private val _userAnswers = MutableStateFlow<Map<String, String>>(emptyMap())
    val userAnswers: StateFlow<Map<String, String>> = _userAnswers.asStateFlow()

    init {
        loadUserExams()
        loadUserResults()
    }

    fun loadUserExams(status: ExamStatus? = null) {
        viewModelScope.launch {
            try {
                _examsState.value = ExamsState.Loading
                _examFilter.value = status

                // Get user ID from AuthRepository
                val userId = getCurrentUserId()
                if (userId == null) {
                    _examsState.value = ExamsState.Error("لطفاً وارد شوید")
                    return@launch
                }

                val result = examRepository.getUserExams(userId, status)

                if (result.isSuccess) {
                    val exams = result.getOrNull() ?: emptyList()
                    _examsState.value = ExamsState.Success(exams)
                } else {
                    _examsState.value = ExamsState.Error(
                        result.exceptionOrNull()?.message ?: "خطا در دریافت آزمون‌ها"
                    )
                }
            } catch (e: Exception) {
                _examsState.value = ExamsState.Error("خطا در اتصال: ${e.message}")
            }
        }
    }

    fun generateExam(request: ExamRequest) {
        viewModelScope.launch {
            try {
                _examGenerationState.value = ExamGenerationState.Loading

                val result = examRepository.generateExam(request)

                if (result.isSuccess) {
                    val exam = result.getOrNull()
                    _currentExam.value = exam
                    _examGenerationState.value = ExamGenerationState.Success(exam)
                } else {
                    _examGenerationState.value = ExamGenerationState.Error(
                        result.exceptionOrNull()?.message ?: "خطا در ایجاد آزمون"
                    )
                }
            } catch (e: Exception) {
                _examGenerationState.value = ExamGenerationState.Error("خطا در اتصال: ${e.message}")
            }
        }
    }

    fun startExam(examId: String) {
        viewModelScope.launch {
            try {
                val result = examRepository.startExam(examId)

                if (result.isSuccess) {
                    val exam = result.getOrNull()
                    _currentExam.value = exam
                    loadExamQuestions(examId)
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun loadExamQuestions(examId: String) {
        viewModelScope.launch {
            try {
                val result = examRepository.getExamQuestions(examId)

                if (result.isSuccess) {
                    _examQuestions.value = result.getOrNull() ?: emptyList()
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun submitExam(answers: List<StudentAnswer>) {
        viewModelScope.launch {
            try {
                val examId = _currentExam.value?.id ?: run {
                    _examGenerationState.value = ExamGenerationState.Error("آزمون انتخاب نشده")
                    return@launch
                }

                val result = examRepository.submitExam(examId, answers)

                if (result.isSuccess) {
                    val examResult = result.getOrNull()
                    _examGenerationState.value = ExamGenerationState.Submitted(examResult)
                    loadUserResults() // Refresh results
                } else {
                    _examGenerationState.value = ExamGenerationState.Error(
                        result.exceptionOrNull()?.message ?: "خطا در ثبت آزمون"
                    )
                }
            } catch (e: Exception) {
                _examGenerationState.value = ExamGenerationState.Error("خطا در اتصال: ${e.message}")
            }
        }
    }

    fun loadUserResults() {
        viewModelScope.launch {
            try {
                _examResults.value = ExamResultsState.Loading

                val userId = getCurrentUserId()
                if (userId == null) {
                    _examResults.value = ExamResultsState.Error("لطفاً وارد شوید")
                    return@launch
                }

                val result = examRepository.getUserResults(userId)

                if (result.isSuccess) {
                    val results = result.getOrNull() ?: emptyList()
                    _examResults.value = ExamResultsState.Success(results)
                } else {
                    _examResults.value = ExamResultsState.Error(
                        result.exceptionOrNull()?.message ?: "خطا در دریافت نتایج"
                    )
                }
            } catch (e: Exception) {
                _examResults.value = ExamResultsState.Error("خطا در اتصال: ${e.message}")
            }
        }
    }

    fun getExamByCode(examCode: String) {
        viewModelScope.launch {
            try {
                val result = examRepository.getExamByCode(examCode)

                if (result.isSuccess) {
                    _currentExam.value = result.getOrNull()
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun clearCurrentExam() {
        _currentExam.value = null
        _examQuestions.value = emptyList()
        _examGenerationState.value = ExamGenerationState.Idle
        _userAnswers.value = emptyMap()
    }

    fun refreshExams() {
        loadUserExams(_examFilter.value)
    }

    fun refreshResults() {
        loadUserResults()
    }

    fun saveAnswer(questionId: String, answer: String) {
        val currentAnswers = _userAnswers.value.toMutableMap()
        currentAnswers[questionId] = answer
        _userAnswers.value = currentAnswers
    }

    fun getAnswer(questionId: String): String? {
        return _userAnswers.value[questionId]
    }

    // Helper function to get current user ID (to be replaced with actual AuthRepository)
    private suspend fun getCurrentUserId(): String? {
        // TODO: Replace with actual AuthRepository call
        return "user_123" // Mock user ID for now
    }

    // Extension properties
    val Exam.isActive: Boolean
        get() = this.status == ExamStatus.ACTIVE

    val Exam.isCompleted: Boolean
        get() = this.status == ExamStatus.COMPLETED

    val Exam.isScheduled: Boolean
        get() = this.status == ExamStatus.SCHEDULED
}

// State Classes
sealed class ExamsState {
    data object Loading : ExamsState()
    data class Success(val exams: List<Exam>) : ExamsState()
    data class Error(val message: String) : ExamsState()
}

sealed class ExamGenerationState {
    data object Idle : ExamGenerationState()
    data object Loading : ExamGenerationState()
    data class Success(val exam: Exam?) : ExamGenerationState()
    data class Submitted(val result: ExamResult?) : ExamGenerationState()
    data class Error(val message: String) : ExamGenerationState()
}

sealed class ExamResultsState {
    data object Loading : ExamResultsState()
    data class Success(val results: List<ExamResult>) : ExamResultsState()
    data class Error(val message: String) : ExamResultsState()
}

// Data class for student answers
data class StudentAnswer(
    val questionId: String,
    val answer: String,
    val questionType: String
)