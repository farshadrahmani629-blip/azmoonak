// app/src/main/java/com/examapp/ui/main/MainViewModel.kt
package com.examapp.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.examapp.data.models.Exam
import com.examapp.data.models.ExamResult
import com.examapp.data.models.Question
import com.examapp.data.repository.ExamRepository
import com.examapp.data.repository.QuestionRepository
import com.examapp.data.repository.ResultRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val questionRepository: QuestionRepository,
    private val examRepository: ExamRepository,
    private val resultRepository: ResultRepository
) : ViewModel() {

    // ------------ LiveDataها ------------

    private val _totalQuestions = MutableLiveData<Int>(0)
    val totalQuestions: LiveData<Int> = _totalQuestions

    private val _totalExams = MutableLiveData<Int>(0)
    val totalExams: LiveData<Int> = _totalExams

    private val _totalResults = MutableLiveData<Int>(0)
    val totalResults: LiveData<Int> = _totalResults

    private val _averageScore = MutableLiveData<Double>(0.0)
    val averageScore: LiveData<Double> = _averageScore

    private val _recentExams = MutableLiveData<List<Exam>>(emptyList())
    val recentExams: LiveData<List<Exam>> = _recentExams

    private val _recentResults = MutableLiveData<List<ExamResult>>(emptyList())
    val recentResults: LiveData<List<ExamResult>> = _recentResults

    private val _availableSubjects = MutableLiveData<List<String>>(emptyList())
    val availableSubjects: LiveData<List<String>> = _availableSubjects

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _successMessage = MutableLiveData<String?>()
    val successMessage: LiveData<String?> = _successMessage

    // ------------ State Management ------------

    sealed class MainViewState {
        data object Loading : MainViewState()
        data object Success : MainViewState()
        data class Error(val message: String) : MainViewState()
        data object Empty : MainViewState()
    }

    private val _viewState = MutableLiveData<MainViewState>(MainViewState.Loading)
    val viewState: LiveData<MainViewState> = _viewState

    // ------------ Init ------------

    init {
        loadDashboardData()
        loadAvailableSubjects()
    }

    // ------------ Dashboard Data ------------

    fun loadDashboardData() {
        viewModelScope.launch {
            _viewState.value = MainViewState.Loading
            _isLoading.value = true

            try {
                // بارگذاری همزمان داده‌ها با async
                val questionsDeferred = async {
                    questionRepository.getTotalQuestionCount()
                }
                val examsDeferred = async {
                    examRepository.getTotalExamCount() to examRepository.getRecentExams(5)
                }
                val resultsDeferred = async {
                    resultRepository.getTotalResultCount() to resultRepository.getAverageScore()
                }
                val topResultsDeferred = async {
                    resultRepository.getTopResults(5)
                }

                // جمع‌آوری نتایج
                _totalQuestions.value = questionsDeferred.await()

                val (examCount, recentExamsList) = examsDeferred.await()
                _totalExams.value = examCount
                _recentExams.value = recentExamsList ?: emptyList()

                val (resultCount, avgScore) = resultsDeferred.await()
                _totalResults.value = resultCount
                _averageScore.value = avgScore

                _recentResults.value = topResultsDeferred.await()

                // بررسی وضعیت داده‌ها
                val isEmpty = examCount == 0 && resultCount == 0
                _viewState.value = if (isEmpty) {
                    MainViewState.Empty
                } else {
                    MainViewState.Success
                }

                _errorMessage.value = null

            } catch (e: Exception) {
                _errorMessage.value = "خطا در بارگذاری داده‌ها: ${e.message}"
                _viewState.value = MainViewState.Error(e.message ?: "خطای ناشناخته")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshData() {
        loadDashboardData()
        loadAvailableSubjects()
    }

    // ------------ Subjects ------------

    private fun loadAvailableSubjects() {
        viewModelScope.launch {
            try {
                val subjects = questionRepository.getAllSubjects()
                _availableSubjects.value = subjects
            } catch (e: Exception) {
                _errorMessage.value = "خطا در دریافت موضوعات: ${e.message}"
            }
        }
    }

    // ------------ Quick Exam Creation ------------

    fun createQuickExam(
        subject: String,
        grade: String,
        questionCount: Int = 20,
        timeLimit: Int = 60
    ): LiveData<Result<Exam>> {
        val result = MutableLiveData<Result<Exam>>()

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val exam = examRepository.createQuickExam(
                    subject = subject,
                    grade = grade,
                    questionCount = questionCount,
                    timeLimit = timeLimit
                )

                if (exam != null) {
                    result.value = Result.success(exam)
                    _successMessage.value = "آزمون '${exam.title}' با موفقیت ایجاد شد"

                    // به‌روزرسانی لیست آزمون‌ها
                    _recentExams.value = listOf(exam) + (_recentExams.value ?: emptyList()).take(4)
                    _totalExams.value = (_totalExams.value ?: 0) + 1
                } else {
                    result.value = Result.failure(Exception("آزمون ایجاد نشد"))
                    _errorMessage.value = "خطا در ایجاد آزمون"
                }
            } catch (e: Exception) {
                result.value = Result.failure(e)
                _errorMessage.value = "خطا در ایجاد آزمون: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }

        return result
    }

    // ------------ Search ------------

    fun searchQuestions(keyword: String): LiveData<Result<List<Question>>> {
        val result = MutableLiveData<Result<List<Question>>>()

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val questions = questionRepository.searchQuestions(keyword)
                result.value = Result.success(questions)
                _errorMessage.value = null

                if (questions.isEmpty()) {
                    _successMessage.value = "هیچ نتیجه‌ای برای '$keyword' یافت نشد"
                } else {
                    _successMessage.value = "${questions.size} نتیجه برای '$keyword' یافت شد"
                }
            } catch (e: Exception) {
                result.value = Result.failure(e)
                _errorMessage.value = "خطا در جستجو: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }

        return result
    }

    // ------------ Import Questions ------------

    fun importQuestions(questions: List<Question>): LiveData<Result<Int>> {
        val result = MutableLiveData<Result<Int>>()

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val insertedCount = questionRepository.importQuestionsFromList(questions)
                result.value = Result.success(insertedCount)

                if (insertedCount > 0) {
                    _successMessage.value = "$insertedCount سوال با موفقیت وارد شد"
                    // به‌روزرسانی آمار
                    _totalQuestions.value = (_totalQuestions.value ?: 0) + insertedCount
                } else {
                    _errorMessage.value = "هیچ سوالی وارد نشد"
                }
            } catch (e: Exception) {
                result.value = Result.failure(e)
                _errorMessage.value = "خطا در وارد کردن سوالات: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }

        return result
    }

    // ------------ Data Management ------------

    fun clearAllData(): LiveData<Result<Boolean>> {
        val result = MutableLiveData<Result<Boolean>>()

        viewModelScope.launch {
            _isLoading.value = true
            try {
                // تایید کاربر قبل از پاک کردن
                // در اینجا فقط عملیات پاک کردن را انجام می‌دهیم
                val questionsDeleted = questionRepository.deleteAllQuestions() > 0
                val examsDeleted = examRepository.deleteAllExams() > 0
                val resultsDeleted = resultRepository.deleteAllResults() > 0

                val success = questionsDeleted && examsDeleted && resultsDeleted
                result.value = Result.success(success)

                if (success) {
                    _successMessage.value = "تمام داده‌ها با موفقیت پاک شدند"

                    // به‌روزرسانی LiveDataها
                    _totalQuestions.value = 0
                    _totalExams.value = 0
                    _totalResults.value = 0
                    _averageScore.value = 0.0
                    _recentExams.value = emptyList()
                    _recentResults.value = emptyList()
                    _viewState.value = MainViewState.Empty
                } else {
                    _errorMessage.value = "خطا در پاک کردن برخی داده‌ها"
                }
            } catch (e: Exception) {
                result.value = Result.failure(e)
                _errorMessage.value = "خطا در پاک کردن داده‌ها: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }

        return result
    }

    // ------------ Stats ------------

    data class DashboardStats(
        val totalQuestions: Int,
        val totalExams: Int,
        val totalResults: Int,
        val averageScore: Double,
        val recentExamsCount: Int,
        val recentResultsCount: Int,
        val isLoading: Boolean,
        val hasData: Boolean
    )

    fun getDashboardStats(): DashboardStats {
        return DashboardStats(
            totalQuestions = _totalQuestions.value ?: 0,
            totalExams = _totalExams.value ?: 0,
            totalResults = _totalResults.value ?: 0,
            averageScore = _averageScore.value ?: 0.0,
            recentExamsCount = _recentExams.value?.size ?: 0,
            recentResultsCount = _recentResults.value?.size ?: 0,
            isLoading = _isLoading.value ?: false,
            hasData = (_totalExams.value ?: 0) > 0
        )
    }

    // ------------ Helper Functions ------------

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }

    fun getQuickExamSubjects(): List<String> {
        return listOf("ریاضی", "علوم", "فارسی", "انگلیسی", "اجتماعی")
    }

    fun getQuickExamGrades(): List<String> {
        return (1..12).map { "پایه $it" }
    }

    // ------------ Extension Functions ------------

    private fun ExamRepository.createQuickExam(
        subject: String,
        grade: String,
        questionCount: Int,
        timeLimit: Int
    ): Exam? {
        // این تابع باید در Repository پیاده‌سازی شود
        // اینجا فقط یک شبیه‌سازی است
        return Exam(
            id = System.currentTimeMillis().toString(),
            title = "آزمون فوری $subject",
            subject = subject,
            grade = grade.toIntOrNull() ?: 6,
            totalQuestions = questionCount,
            examDuration = timeLimit,
            status = com.examapp.data.models.ExamStatus.ACTIVE,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }

    // ------------ Result Wrapper ------------

    sealed class Result<out T> {
        data class Success<out T>(val data: T) : Result<T>()
        data class Failure(val exception: Exception) : Result<Nothing>()

        companion object {
            fun <T> success(data: T): Result<T> = Success(data)
            fun failure(exception: Exception): Result<Nothing> = Failure(exception)
        }

        fun getOrNull(): T? = when (this) {
            is Success -> data
            is Failure -> null
        }

        fun isSuccess(): Boolean = this is Success
        fun isFailure(): Boolean = this is Failure
    }
}

// ------------ Extension Properties ------------

val MainViewModel.DashboardStats.formattedAverageScore: String
    get() = "%.1f".format(this.averageScore)

val MainViewModel.DashboardStats.hasRecentActivity: Boolean
    get() = this.recentExamsCount > 0 || this.recentResultsCount > 0

val MainViewModel.DashboardStats.completionRate: Double
    get() {
        val total = this.totalExams
        val completed = this.totalResults
        return if (total > 0) {
            (completed.toDouble() / total) * 100
        } else {
            0.0
        }
    }

// ------------ Extension for List<Exam> ------------

val List<Exam>.recentFormatted: String
    get() = when (this.size) {
        0 -> "بدون آزمون اخیر"
        1 -> "۱ آزمون اخیر"
        else -> "${this.size} آزمون اخیر"
    }

val List<ExamResult>.averageScoreFormatted: String
    get() = if (this.isNotEmpty()) {
        val average = this.map { it.score ?: 0 }.average()
        "%.1f".format(average)
    } else {
        "۰.۰"
    }