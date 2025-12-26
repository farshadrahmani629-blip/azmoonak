package com.examapp.ui.exam

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.examapp.data.models.*
import com.examapp.data.remote.*
import com.examapp.data.repository.ExamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ExamViewModel @Inject constructor(
    private val repository: ExamRepository
) : ViewModel() {

    // ==================== UI State ====================
    sealed class ExamUiState {
        object Loading : ExamUiState()
        object Ready : ExamUiState()
        object Active : ExamUiState()
        data class Completed(val message: String) : ExamUiState()
        data class Error(val message: String) : ExamUiState()
    }

    private val _uiState = MutableLiveData<ExamUiState>(ExamUiState.Loading)
    val uiState: LiveData<ExamUiState> = _uiState

    // ==================== Data ====================
    private val _examData = MutableLiveData<ExamRemote?>()
    val examData: LiveData<ExamRemote?> = _examData

    private val _questions = MutableLiveData<List<QuestionRemote>>(emptyList())
    val questions: LiveData<List<QuestionRemote>> = _questions

    private val _currentQuestion = MutableLiveData<QuestionRemote?>()
    val currentQuestion: LiveData<QuestionRemote?> = _currentQuestion

    // ==================== Exam State ====================
    private val _remainingTime = MutableLiveData<Long>(0L)
    val remainingTime: LiveData<Long> = _remainingTime

    private val _progress = MutableLiveData<Float>(0f)
    val progress: LiveData<Float> = _progress

    private val _answeredCount = MutableLiveData<Int>(0)
    val answeredCount: LiveData<Int> = _answeredCount

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // ==================== Local State ====================
    private var currentQuestionIndex = 0
    private var currentExamId: Int? = null
    private val userAnswers = mutableMapOf<Int, UserAnswerRemote>()
    private var isExamStarted = false
    private var examStartTime: Long = 0

    // ==================== Public Methods ====================

    /**
     * بارگذاری آزمون بر اساس ID
     */
    fun loadExam(examId: Int) {
        currentExamId = examId
        viewModelScope.launch {
            try {
                _uiState.value = ExamUiState.Loading

                // 1. دریافت اطلاعات آزمون
                when (val examResult = repository.getExamById(examId)) {
                    is Result.Success -> {
                        _examData.value = examResult.data

                        // 2. دریافت سوالات آزمون
                        when (val questionsResult = repository.getExamQuestions(examId)) {
                            is Result.Success -> {
                                val questionsList = questionsResult.data
                                _questions.value = questionsList

                                if (questionsList.isNotEmpty()) {
                                    _currentQuestion.value = questionsList[0]
                                    updateProgress()
                                    _uiState.value = ExamUiState.Ready
                                } else {
                                    _errorMessage.value = "آزمون سوالی ندارد"
                                    _uiState.value = ExamUiState.Error("آزمون سوالی ندارد")
                                }
                            }
                            is Result.Failure -> {
                                _errorMessage.value = "خطا در دریافت سوالات: ${questionsResult.exception.message}"
                                _uiState.value = ExamUiState.Error("خطا در دریافت سوالات")
                            }
                        }
                    }
                    is Result.Failure -> {
                        _errorMessage.value = "خطا در دریافت آزمون: ${examResult.exception.message}"
                        _uiState.value = ExamUiState.Error("خطا در دریافت آزمون")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "خطا در بارگذاری آزمون")
                _errorMessage.value = "خطا در بارگذاری آزمون: ${e.message}"
                _uiState.value = ExamUiState.Error("خطا در بارگذاری آزمون")
            }
        }
    }

    /**
     * شروع آزمون
     */
    fun startExam() {
        isExamStarted = true
        examStartTime = System.currentTimeMillis()
        _uiState.value = ExamUiState.Active

        // شروع تایمر
        startTimer()
    }

    /**
     * رفتن به سوال بعدی
     */
    fun goToNextQuestion() {
        val currentQuestions = _questions.value ?: return
        if (currentQuestionIndex < currentQuestions.size - 1) {
            currentQuestionIndex++
            _currentQuestion.value = currentQuestions[currentQuestionIndex]
            updateProgress()
        }
    }

    /**
     * رفتن به سوال قبلی
     */
    fun goToPreviousQuestion() {
        if (currentQuestionIndex > 0) {
            currentQuestionIndex--
            val currentQuestions = _questions.value ?: return
            _currentQuestion.value = currentQuestions[currentQuestionIndex]
            updateProgress()
        }
    }

    /**
     * رفتن به سوال مشخص
     */
    fun goToQuestion(index: Int) {
        val currentQuestions = _questions.value ?: return
        if (index in currentQuestions.indices) {
            currentQuestionIndex = index
            _currentQuestion.value = currentQuestions[index]
            updateProgress()
        }
    }

    /**
     * ذخیره پاسخ سوال فعلی
     */
    fun saveCurrentAnswer(selectedOption: String? = null, descriptiveAnswer: String? = null) {
        val question = _currentQuestion.value ?: return

        val answer = UserAnswerRemote(
            questionId = question.id,
            selectedOption = selectedOption,
            descriptiveAnswer = descriptiveAnswer,
            isFlagged = false
        )

        userAnswers[question.id] = answer
        updateProgress()
    }

    /**
     * علامت‌گذاری سوال فعلی
     */
    fun toggleFlagCurrentQuestion() {
        val question = _currentQuestion.value ?: return
        val currentAnswer = userAnswers[question.id]

        val updatedAnswer = UserAnswerRemote(
            questionId = question.id,
            selectedOption = currentAnswer?.selectedOption,
            descriptiveAnswer = currentAnswer?.descriptiveAnswer,
            isFlagged = currentAnswer?.isFlagged != true
        )

        userAnswers[question.id] = updatedAnswer
    }

    /**
     * ارسال آزمون
     */
    fun submitExam() {
        val examId = currentExamId ?: return
        val answers = userAnswers.values.toList()
        val timeSpentSeconds = calculateTimeSpent()

        viewModelScope.launch {
            try {
                _uiState.value = ExamUiState.Loading

                when (val result = repository.submitExam(examId, answers, timeSpentSeconds)) {
                    is Result.Success -> {
                        val submitResponse = result.data
                        _uiState.value = ExamUiState.Completed(
                            "آزمون با موفقیت ثبت شد. نمره شما: ${submitResponse.score}/${submitResponse.totalScore}"
                        )

                        // برای نسخه Pro: تولید PDF
                        if (repository.isProVersion()) {
                            generateResultPdf(submitResponse)
                        }
                    }
                    is Result.Failure -> {
                        _errorMessage.value = "خطا در ثبت آزمون: ${result.exception.message}"
                        _uiState.value = ExamUiState.Error("خطا در ثبت آزمون")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "خطا در ثبت آزمون")
                _errorMessage.value = "خطا در ثبت آزمون: ${e.message}"
                _uiState.value = ExamUiState.Error("خطا در ثبت آزمون")
            }
        }
    }

    /**
     * دانلود آزمون (برای نسخه Pro)
     */
    fun downloadExam() {
        val examId = currentExamId ?: return

        if (!repository.isProVersion()) {
            _errorMessage.value = "فقط نسخه Pro امکان دانلود دارد"
            return
        }

        viewModelScope.launch {
            try {
                _uiState.value = ExamUiState.Loading

                when (val result = repository.downloadExam(examId)) {
                    is Result.Success -> {
                        if (result.data) {
                            _errorMessage.value = "آزمون با موفقیت دانلود شد"
                        }
                    }
                    is Result.Failure -> {
                        _errorMessage.value = "خطا در دانلود: ${result.exception.message}"
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "خطا در دانلود آزمون")
                _errorMessage.value = "خطا در دانلود: ${e.message}"
            } finally {
                // بازگشت به حالت قبلی
                val currentState = _uiState.value
                if (currentState is ExamUiState.Loading) {
                    _uiState.value = ExamUiState.Ready
                }
            }
        }
    }

    /**
     * تولید PDF نتیجه
     */
    private fun generateResultPdf(submitResponse: SubmitExamResponse) {
        viewModelScope.launch {
            try {
                val exam = _examData.value ?: return@launch

                when (val result = repository.generateAnswerSheetPdf(exam, submitResponse)) {
                    is Result.Success -> {
                        Timber.d("PDF نتیجه با موفقیت ایجاد شد: ${result.data.path}")
                    }
                    is Result.Failure -> {
                        Timber.e(result.exception, "خطا در ایجاد PDF نتیجه")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "خطا در تولید PDF")
            }
        }
    }

    // ==================== Helper Methods ====================

    /**
     * شروع تایمر
     */
    private fun startTimer() {
        // اینجا می‌توانید از Timer یا Coroutine timer استفاده کنید
        // برای سادگی، فقط یک نمونه ساده:
        val examDuration = _examData.value?.durationMinutes ?: 60
        val totalSeconds = examDuration * 60L
        _remainingTime.value = totalSeconds
    }

    /**
     * محاسبه زمان سپری شده
     */
    private fun calculateTimeSpent(): Int {
        return if (examStartTime > 0) {
            ((System.currentTimeMillis() - examStartTime) / 1000).toInt()
        } else {
            0
        }
    }

    /**
     * به‌روزرسانی پیشرفت
     */
    private fun updateProgress() {
        val currentQuestions = _questions.value ?: emptyList()
        val totalQuestions = currentQuestions.size
        val answered = userAnswers.size

        if (totalQuestions > 0) {
            _progress.value = (answered.toFloat() / totalQuestions) * 100
            _answeredCount.value = answered
        } else {
            _progress.value = 0f
            _answeredCount.value = 0
        }
    }

    /**
     * دریافت وضعیت پاسخ سوال
     */
    fun getQuestionStatus(questionId: Int): String {
        return when {
            userAnswers.containsKey(questionId) -> "پاسخ داده شده"
            userAnswers[questionId]?.isFlagged == true -> "علامت‌گذاری شده"
            else -> "بی‌پاسخ"
        }
    }

    /**
     * دریافت پاسخ ذخیره شده برای سوال
     */
    fun getSavedAnswer(questionId: Int): UserAnswerRemote? {
        return userAnswers[questionId]
    }

    /**
     * بررسی آیا سوال آخر است
     */
    fun isLastQuestion(): Boolean {
        val currentQuestions = _questions.value ?: return false
        return currentQuestionIndex == currentQuestions.size - 1
    }

    /**
     * بررسی آیا سوال اول است
     */
    fun isFirstQuestion(): Boolean = currentQuestionIndex == 0

    /**
     * دریافت شماره سوال فعلی
     */
    fun getCurrentQuestionNumber(): Int = currentQuestionIndex + 1

    /**
     * دریافت تعداد کل سوالات
     */
    fun getTotalQuestions(): Int = _questions.value?.size ?: 0

    /**
     * دریافت تعداد سوالات پاسخ داده شده
     */
    fun getAnsweredQuestionsCount(): Int = userAnswers.size

    /**
     * ریست وضعیت آزمون
     */
    fun resetExam() {
        currentQuestionIndex = 0
        userAnswers.clear()
        isExamStarted = false
        examStartTime = 0
        _progress.value = 0f
        _answeredCount.value = 0
        _remainingTime.value = 0L
        _currentQuestion.value = _questions.value?.getOrNull(0)
        _uiState.value = ExamUiState.Ready
    }
}

// ==================== Result Wrapper ====================
/**
 * wrapper برای Result (در صورت عدم وجود در پروژه)
 */
sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Failure(val exception: Exception) : Result<Nothing>()

    val isSuccess: Boolean get() = this is Success<T>
    val isFailure: Boolean get() = this is Failure
}