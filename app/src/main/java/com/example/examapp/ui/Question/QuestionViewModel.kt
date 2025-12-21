// app/src/main/java/com/examapp/ui/question/QuestionViewModel.kt
package com.examapp.ui.question

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.examapp.data.models.*
import com.examapp.data.repository.QuestionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QuestionViewModel @Inject constructor(
    private val questionRepository: QuestionRepository
) : ViewModel() {

    private val _questionsState = MutableStateFlow<QuestionsState>(QuestionsState.Loading)
    val questionsState: StateFlow<QuestionsState> = _questionsState.asStateFlow()

    private val _selectedQuestion = MutableStateFlow<Question?>(null)
    val selectedQuestion: StateFlow<Question?> = _selectedQuestion.asStateFlow()

    private val _questionOptions = MutableStateFlow<List<QuestionOption>>(emptyList())
    val questionOptions: StateFlow<List<QuestionOption>> = _questionOptions.asStateFlow()

    private val _filterState = MutableStateFlow(QuestionFilterState())
    val filterState: StateFlow<QuestionFilterState> = _filterState.asStateFlow()

    private val _paginationState = MutableStateFlow(PaginationState())
    val paginationState: StateFlow<PaginationState> = _paginationState.asStateFlow()

    init {
        loadQuestions()
    }

    fun loadQuestions(
        bookId: Int? = _filterState.value.bookId,
        chapterId: Int? = _filterState.value.chapterId,
        difficulty: DifficultyLevel? = _filterState.value.difficulty,
        questionType: QuestionType? = _filterState.value.questionType,
        page: Int? = null,
        limit: Int = 20
    ) {
        viewModelScope.launch {
            try {
                _questionsState.value = QuestionsState.Loading

                val result = questionRepository.getQuestions(
                    bookId = bookId,
                    chapterId = chapterId,
                    page = page,
                    difficulty = difficulty,
                    questionType = questionType,
                    limit = limit
                )

                if (result.isSuccess) {
                    val paginatedResponse = result.getOrNull()
                    paginatedResponse?.let { response ->
                        _questionsState.value = QuestionsState.Success(response.data)
                        _paginationState.value = PaginationState(
                            currentPage = response.currentPage,
                            totalPages = response.totalPages,
                            totalItems = response.totalItems,
                            hasNext = response.hasNext,
                            hasPrevious = response.hasPrevious
                        )
                    }
                } else {
                    _questionsState.value = QuestionsState.Error(
                        result.exceptionOrNull()?.message ?: "خطا در دریافت سوالات"
                    )
                }
            } catch (e: Exception) {
                _questionsState.value = QuestionsState.Error("خطا در اتصال: ${e.message}")
            }
        }
    }

    fun loadQuestionsByChapter(chapterId: Int, limit: Int = 50) {
        viewModelScope.launch {
            try {
                _questionsState.value = QuestionsState.Loading

                val result = questionRepository.getQuestionsByChapter(chapterId, limit)

                if (result.isSuccess) {
                    val questions = result.getOrNull() ?: emptyList()
                    _questionsState.value = QuestionsState.Success(questions)
                    _paginationState.value = PaginationState() // Reset pagination for chapter view
                } else {
                    _questionsState.value = QuestionsState.Error(
                        result.exceptionOrNull()?.message ?: "خطا در دریافت سوالات فصل"
                    )
                }
            } catch (e: Exception) {
                _questionsState.value = QuestionsState.Error("خطا در اتصال: ${e.message}")
            }
        }
    }

    fun loadQuestionsByPageRange(bookId: Int, startPage: Int, endPage: Int) {
        viewModelScope.launch {
            try {
                _questionsState.value = QuestionsState.Loading

                val result = questionRepository.getQuestionsByPageRange(bookId, startPage, endPage)

                if (result.isSuccess) {
                    val questions = result.getOrNull() ?: emptyList()
                    _questionsState.value = QuestionsState.Success(questions)
                } else {
                    _questionsState.value = QuestionsState.Error(
                        result.exceptionOrNull()?.message ?: "خطا در دریافت سوالات"
                    )
                }
            } catch (e: Exception) {
                _questionsState.value = QuestionsState.Error("خطا در اتصال: ${e.message}")
            }
        }
    }

    fun selectQuestion(question: Question) {
        _selectedQuestion.value = question
        loadQuestionOptions(question.id ?: 0)
    }

    fun clearSelectedQuestion() {
        _selectedQuestion.value = null
        _questionOptions.value = emptyList()
    }

    fun loadQuestionOptions(questionId: Int) {
        viewModelScope.launch {
            try {
                val result = questionRepository.getQuestionOptions(questionId)

                if (result.isSuccess) {
                    _questionOptions.value = result.getOrNull() ?: emptyList()
                }
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }

    fun loadQuestionDetails(questionId: Int) {
        viewModelScope.launch {
            try {
                val result = questionRepository.getQuestionById(questionId)

                if (result.isSuccess) {
                    _selectedQuestion.value = result.getOrNull()
                    loadQuestionOptions(questionId)
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun updateFilter(
        bookId: Int? = null,
        chapterId: Int? = null,
        difficulty: DifficultyLevel? = null,
        questionType: QuestionType? = null
    ) {
        _filterState.value = QuestionFilterState(
            bookId = bookId ?: _filterState.value.bookId,
            chapterId = chapterId ?: _filterState.value.chapterId,
            difficulty = difficulty ?: _filterState.value.difficulty,
            questionType = questionType ?: _filterState.value.questionType
        )
    }

    fun resetFilter() {
        _filterState.value = QuestionFilterState()
    }

    fun loadNextPage() {
        if (_paginationState.value.hasNext) {
            loadQuestions(page = _paginationState.value.currentPage + 1)
        }
    }

    fun loadPreviousPage() {
        if (_paginationState.value.hasPrevious) {
            loadQuestions(page = _paginationState.value.currentPage - 1)
        }
    }

    fun verifyQuestion(
        questionId: Int,
        isApproved: Boolean,
        feedback: String? = null
    ) {
        viewModelScope.launch {
            try {
                // TODO: Get teacher ID from AuthRepository
                val teacherId = 1 // Placeholder

                val result = questionRepository.verifyQuestion(
                    questionId = questionId,
                    teacherId = teacherId,
                    isApproved = isApproved,
                    feedback = feedback
                )

                if (result.isSuccess) {
                    // Update selected question if it's the same
                    _selectedQuestion.value?.let { question ->
                        if (question.id == questionId) {
                            _selectedQuestion.value = result.getOrNull()
                        }
                    }
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun cacheQuestion(question: Question) {
        questionRepository.cacheQuestion(question)
    }

    fun cacheOptions(questionId: Int, options: List<QuestionOption>) {
        questionRepository.cacheOptions(questionId, options)
    }

    fun getCachedQuestion(questionId: Int): Question? {
        return questionRepository.getCachedQuestion(questionId)
    }

    fun getCachedOptions(questionId: Int): List<QuestionOption>? {
        return questionRepository.getCachedOptions(questionId)
    }

    fun refreshQuestions() {
        loadQuestions(
            bookId = _filterState.value.bookId,
            chapterId = _filterState.value.chapterId,
            difficulty = _filterState.value.difficulty,
            questionType = _filterState.value.questionType
        )
    }
}

// State Classes
sealed class QuestionsState {
    data object Loading : QuestionsState()
    data class Success(val questions: List<Question>) : QuestionsState()
    data class Error(val message: String) : QuestionsState()
}

data class QuestionFilterState(
    val bookId: Int? = null,
    val chapterId: Int? = null,
    val difficulty: DifficultyLevel? = null,
    val questionType: QuestionType? = null
)

data class PaginationState(
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val totalItems: Int = 0,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false
)