// app/src/main/java/com/examapp/ui/results/ResultViewModel.kt
package com.examapp.ui.results

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.examapp.data.models.Result
import com.examapp.data.repository.ResultRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ResultViewModel(
    private val resultRepository: ResultRepository
) : ViewModel() {

    private val _allResults = MutableLiveData<List<Result>>()
    val allResults: LiveData<List<Result>> = _allResults

    private val _selectedResult = MutableLiveData<Result?>()
    val selectedResult: LiveData<Result?> = _selectedResult

    private val _chartData = MutableLiveData<List<Pair<String, Float>>>()
    val chartData: LiveData<List<Pair<String, Float>>> = _chartData

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // بارگذاری همه نتایج
    fun loadAllResults() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val results = resultRepository.getAllResults()
                _allResults.value = results
                prepareChartData(results)
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "خطا در بارگذاری نتایج: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // بارگذاری نتیجه خاص
    fun loadResultById(resultId: Int) {
        viewModelScope.launch {
            try {
                val result = resultRepository.getResultById(resultId)
                _selectedResult.value = result
            } catch (e: Exception) {
                _errorMessage.value = "خطا در بارگذاری نتیجه: ${e.message}"
            }
        }
    }

    // آماده‌سازی داده‌های نمودار
    private fun prepareChartData(results: List<Result>) {
        if (results.isEmpty()) return

        val chartPoints = mutableListOf<Pair<String, Float>>()

        // اگر کمتر از 5 نتیجه داریم، همه را نشان می‌دهیم
        if (results.size <= 5) {
            results.forEachIndexed { index, result ->
                val label = "آزمون ${index + 1}"
                chartPoints.add(label to result.score)
            }
        } else {
            // آخرین 10 نتیجه
            val recentResults = results.takeLast(10)
            recentResults.forEachIndexed { index, result ->
                val dateFormat = SimpleDateFormat("MM/dd", Locale.getDefault())
                val label = dateFormat.format(Date(result.date))
                chartPoints.add(label to result.score)
            }
        }

        _chartData.value = chartPoints
    }

    // حذف نتیجه
    fun deleteResult(resultId: Int) {
        viewModelScope.launch {
            try {
                resultRepository.deleteResult(resultId)
                // آپدیت لیست
                loadAllResults()
            } catch (e: Exception) {
                _errorMessage.value = "خطا در حذف نتیجه: ${e.message}"
            }
        }
    }

    // فیلتر نتایج بر اساس تاریخ
    fun filterResultsByDate(startDate: Date, endDate: Date) {
        viewModelScope.launch {
            try {
                val results = resultRepository.getResultsBetweenDates(startDate, endDate)
                _allResults.value = results
                prepareChartData(results)
            } catch (e: Exception) {
                _errorMessage.value = "خطا در فیلتر نتایج: ${e.message}"
            }
        }
    }

    // گرفتن آمار کلی
    fun getStatistics(): Map<String, Any> {
        val results = _allResults.value ?: return emptyMap()

        if (results.isEmpty()) {
            return mapOf(
                "totalExams" to 0,
                "averageScore" to 0f,
                "bestScore" to 0f,
                "worstScore" to 0f,
                "totalCorrect" to 0,
                "totalQuestions" to 0
            )
        }

        val totalExams = results.size
        val averageScore = results.map { it.score }.average().toFloat()
        val bestScore = results.maxByOrNull { it.score }?.score ?: 0f
        val worstScore = results.minByOrNull { it.score }?.score ?: 0f
        val totalCorrect = results.sumOf { it.correctAnswers }
        val totalQuestions = results.sumOf { it.totalQuestions }

        return mapOf(
            "totalExams" to totalExams,
            "averageScore" to averageScore,
            "bestScore" to bestScore,
            "worstScore" to worstScore,
            "totalCorrect" to totalCorrect,
            "totalQuestions" to totalQuestions,
            "accuracy" to if (totalQuestions > 0) {
                (totalCorrect.toFloat() / totalQuestions) * 100
            } else 0f
        )
    }

    // گرفتن تاریخ‌های قابل نمایش
    fun getFormattedDate(date: Date): String {
        val dateFormat = SimpleDateFormat("yyyy/MM/dd - HH:mm", Locale("fa", "IR"))
        return dateFormat.format(date)
    }

    // ارزیابی عملکرد
    fun getPerformanceEvaluation(score: Float): String {
        return when {
            score >= 90 -> "عالی 🎉"
            score >= 75 -> "خوب 👍"
            score >= 50 -> "متوسط 😊"
            else -> "نیاز به تمرین بیشتر 📚"
        }
    }

    // پیشنهاد بهبود
    fun getImprovementSuggestion(score: Float): String {
        return when {
            score >= 90 -> "شما عملکرد بسیار خوبی داشتید. به مطالعه ادامه دهید!"
            score >= 75 -> "خوب است. روی نقاط ضعف تمرکز کنید."
            score >= 50 -> "نیاز به تمرین بیشتر دارید. آزمون‌های بیشتری بدهید."
            else -> "پیشنهاد می‌کنیم مطالب را دوباره مرور کنید."
        }
    }
}