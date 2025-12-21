// app/src/main/java/com/examapp/ui/exam/ExamSessionViewModel.kt
package com.examapp.ui.exam

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.examapp.data.models.ExamSession
import com.examapp.data.models.Question
import com.examapp.data.repository.ExamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExamSessionViewModel @Inject constructor(
    private val examRepository: ExamRepository
) : ViewModel() {

    private val _uiState = MutableLiveData<ExamSessionUiState>(ExamSessionUiState.Loading)
    val uiState: LiveData<ExamSessionUiState> = _uiState

    private val _currentQuestionIndex = MutableLiveData<Int>(0)
    val currentQuestionIndex: LiveData<Int> = _currentQuestionIndex

    private val _remainingTime = MutableLiveData<Long>()
    val remainingTime: LiveData<Long> = _remainingTime

    private var timerJob: Job? = null
    private var totalTimeSeconds: Long = 0
    private var startTime: Long = 0
    private var userAnswers = mutableMapOf<String, String>()

    private var examSession: ExamSession? = null
    private var questions: List<Question> = emptyList()

    fun startExam(examId: String) {
        viewModelScope.launch {
            _uiState.value = ExamSessionUiState.Loading

            try {
                // Start exam session
                val sessionResult = examRepository.startExamSession(examId)
                if (sessionResult.isFailure) {
                    _uiState.value = ExamSessionUiState.Error(
                        sessionResult.exceptionOrNull()?.message ?: "خطا در شروع آزمون"
                    )
                    return@launch
                }

                examSession = sessionResult.getOrNull()

                // Get exam questions
                val questionsResult = examRepository.getExamQuestions(examId)
                if (questionsResult.isFailure) {
                    _uiState.value = ExamSessionUiState.Error(
                        questionsResult.exceptionOrNull()?.message ?: "خطا در دریافت سوالات"
                    )
                    return@launch
                }

                questions = questionsResult.getOrNull() ?: emptyList()

                if (questions.isEmpty()) {
                    _uiState.value = ExamSessionUiState.Error("سوالی برای این آزمون یافت نشد")
                    return@launch
                }

                // Initialize timer
                totalTimeSeconds = (examSession?.timeLimit ?: 60) * 60L
                _remainingTime.value = totalTimeSeconds
                startTime = System.currentTimeMillis()

                // Start timer
                startTimer()

                // Update UI state with first question
                _uiState.value = ExamSessionUiState.Active(
                    examSession = examSession!!,
                    currentQuestion = questions[0].withUserAnswer(userAnswers[questions[0].id]),
                    totalQuestions = questions.size,
                    hasNext = questions.size > 1,
                    hasPrev = false
                )
                _currentQuestionIndex.value = 0

            } catch (e: Exception) {
                _uiState.value = ExamSessionUiState.Error("خطا در بارگذاری آزمون: ${e.message}")
            }
        }
    }

    private fun startTimer() {
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)

                val elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000
                val newRemainingTime = totalTimeSeconds - elapsedSeconds

                _remainingTime.value = newRemainingTime

                // Time's up
                if (newRemainingTime <= 0) {
                    submitExam()
                    break
                }
            }
        }
    }

    fun selectAnswer(answer: String) {
        val currentIndex = _currentQuestionIndex.value ?: 0
        if (currentIndex < questions.size) {
            val questionId = questions[currentIndex].id
            userAnswers[questionId] = answer

            // Update current question state
            val currentState = _uiState.value
            if (currentState is ExamSessionUiState.Active) {
                _uiState.value = currentState.copy(
                    currentQuestion = currentState.currentQuestion.copy(
                        userAnswer = answer
                    )
                )
            }
        }
    }

    fun goToQuestion(index: Int) {
        if (index in questions.indices) {
            _currentQuestionIndex.value = index

            val question = questions[index]
            val currentState = _uiState.value

            if (currentState is ExamSessionUiState.Active) {
                _uiState.value = currentState.copy(
                    currentQuestion = question.withUserAnswer(userAnswers[question.id]),
                    hasNext = index < questions.size - 1,
                    hasPrev = index > 0
                )
            }
        }
    }

    fun nextQuestion() {
        val currentIndex = _currentQuestionIndex.value ?: 0
        if (currentIndex < questions.size - 1) {
            goToQuestion(currentIndex + 1)
        }
    }

    fun previousQuestion() {
        val currentIndex = _currentQuestionIndex.value ?: 0
        if (currentIndex > 0) {
            goToQuestion(currentIndex - 1)
        }
    }

    fun submitExam() {
        viewModelScope.launch {
            timerJob?.cancel()
            _uiState.value = ExamSessionUiState.Submitting

            try {
                val session = examSession ?: run {
                    _uiState.value = ExamSessionUiState.Error("آزمون یافت نشد")
                    return@launch
                }

                // Prepare answers for submission
                val answers = questions.mapNotNull { question ->
                    userAnswers[question.id]?.let { userAnswer ->
                        mapOf(
                            "questionId" to question.id,
                            "answer" to userAnswer,
                            "questionType" to question.questionType
                        )
                    }
                }

                // Submit to server
                val submitResult = examRepository.submitExam(session.examId, answers)

                if (submitResult.isSuccess) {
                    _uiState.value = ExamSessionUiState.Completed(
                        examSession = session,
                        submittedAt = System.currentTimeMillis()
                    )
                } else {
                    _uiState.value = ExamSessionUiState.Error(
                        submitResult.exceptionOrNull()?.message ?: "خطا در ارسال آزمون"
                    )
                }

            } catch (e: Exception) {
                _uiState.value = ExamSessionUiState.Error("خطا در ارسال آزمون: ${e.message}")
            }
        }
    }

    fun pauseExam() {
        timerJob?.cancel()
        // Save current state to Room database
        // TODO: Implement offline save
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}

// UI State sealed class
sealed class ExamSessionUiState {
    data object Loading : ExamSessionUiState()
    data object Submitting : ExamSessionUiState()
    data class Active(
        val examSession: ExamSession,
        val currentQuestion: Question,
        val totalQuestions: Int,
        val hasNext: Boolean,
        val hasPrev: Boolean
    ) : ExamSessionUiState()
    data class Completed(
        val examSession: ExamSession,
        val submittedAt: Long
    ) : ExamSessionUiState()
    data class Error(val message: String) : ExamSessionUiState()
}

// Extension function for Question
private fun Question.withUserAnswer(userAnswer: String?): Question {
    return this.copy(userAnswer = userAnswer)
}

// Extension property for formatted time
val Long.formattedExamTime: String
    get() {
        val minutes = this / 60
        val seconds = this % 60
        return String.format("%02d:%02d", minutes, seconds)
    }