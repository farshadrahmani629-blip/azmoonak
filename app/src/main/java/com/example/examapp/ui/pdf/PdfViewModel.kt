// app/src/main/java/com/examapp/ui/pdf/PdfViewModel.kt
package com.examapp.ui.pdf

import android.content.Context
import android.os.Environment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.examapp.data.models.Result
import com.examapp.data.repository.ResultRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ViewModel برای مدیریت عملیات PDF
 * شامل: ایجاد PDF، ذخیره، اشتراک‌گذاری و نمایش
 */
class PdfViewModel(
    private val resultRepository: ResultRepository
) : ViewModel() {

    private val _isGeneratingPdf = MutableLiveData(false)
    val isGeneratingPdf: LiveData<Boolean> = _isGeneratingPdf

    private val _pdfGenerationProgress = MutableLiveData(0)
    val pdfGenerationProgress: LiveData<Int> = _pdfGenerationProgress

    private val _pdfFilePath = MutableLiveData<String?>()
    val pdfFilePath: LiveData<String?> = _pdfFilePath

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _pdfContent = MutableLiveData<String>()
    val pdfContent: LiveData<String> = _pdfContent

    /**
     * ایجاد PDF برای یک نتیجه خاص
     */
    fun generateSingleResultPdf(context: Context, resultId: Int) {
        _isGeneratingPdf.value = true
        _pdfGenerationProgress.value = 10

        viewModelScope.launch {
            try {
                // بارگذاری نتیجه از دیتابیس
                val result = withContext(Dispatchers.IO) {
                    resultRepository.getResultById(resultId)
                }

                _pdfGenerationProgress.value = 30

                // ایجاد محتوای PDF
                val pdfContentText = createSingleResultPdfContent(result)
                _pdfContent.value = pdfContentText

                _pdfGenerationProgress.value = 50

                // ذخیره PDF در فایل
                val filePath = savePdfToFile(context, pdfContentText, "result_${result.id}")

                _pdfGenerationProgress.value = 80

                // به‌روزرسانی مسیر فایل
                _pdfFilePath.value = filePath
                _errorMessage.value = null

                _pdfGenerationProgress.value = 100

            } catch (e: Exception) {
                _errorMessage.value = "خطا در ایجاد PDF: ${e.message}"
            } finally {
                _isGeneratingPdf.value = false
            }
        }
    }

    /**
     * ایجاد PDF برای همه نتایج
     */
    fun generateAllResultsPdf(context: Context) {
        _isGeneratingPdf.value = true
        _pdfGenerationProgress.value = 10

        viewModelScope.launch {
            try {
                // بارگذاری همه نتایج
                val allResults = withContext(Dispatchers.IO) {
                    resultRepository.getAllResults()
                }

                _pdfGenerationProgress.value = 30

                if (allResults.isEmpty()) {
                    _errorMessage.value = "هیچ نتیجه‌ای برای ایجاد PDF وجود ندارد"
                    _isGeneratingPdf.value = false
                    return@launch
                }

                // ایجاد محتوای PDF
                val pdfContentText = createAllResultsPdfContent(allResults)
                _pdfContent.value = pdfContentText

                _pdfGenerationProgress.value = 50

                // ذخیره PDF در فایل
                val filePath = savePdfToFile(context, pdfContentText, "all_results_${System.currentTimeMillis()}")

                _pdfGenerationProgress.value = 80

                // به‌روزرسانی مسیر فایل
                _pdfFilePath.value = filePath
                _errorMessage.value = null

                _pdfGenerationProgress.value = 100

            } catch (e: Exception) {
                _errorMessage.value = "خطا در ایجاد PDF: ${e.message}"
            } finally {
                _isGeneratingPdf.value = false
            }
        }
    }

    /**
     * ایجاد محتوای PDF برای یک نتیجه
     */
    private fun createSingleResultPdfContent(result: Result): String {
        val dateFormat = SimpleDateFormat("yyyy/MM/dd - HH:mm", Locale("fa", "IR"))
        val formattedDate = dateFormat.format(Date(result.date))

        val timeTakenMinutes = result.timeTaken / (1000 * 60)
        val timeTakenSeconds = (result.timeTaken / 1000) % 60

        return """
            <html dir="rtl" lang="fa">
            <head>
                <meta charset="UTF-8">
                <title>نتیجه آزمون</title>
                <style>
                    body { font-family: 'Tahoma', sans-serif; padding: 20px; }
                    .header { text-align: center; margin-bottom: 30px; }
                    .title { font-size: 24px; color: #6200EE; font-weight: bold; }
                    .subtitle { font-size: 18px; color: #757575; margin-top: 10px; }
                    .section { margin-bottom: 25px; border: 1px solid #E0E0E0; padding: 15px; border-radius: 10px; }
                    .section-title { font-size: 18px; color: #6200EE; margin-bottom: 10px; font-weight: bold; }
                    .info-row { display: flex; justify-content: space-between; margin-bottom: 8px; }
                    .info-label { font-weight: bold; color: #424242; }
                    .info-value { color: #757575; }
                    .score-box { text-align: center; padding: 20px; background: #F3E5F5; border-radius: 10px; margin: 20px 0; }
                    .score-value { font-size: 36px; color: #6200EE; font-weight: bold; }
                    .score-label { font-size: 16px; color: #757575; }
                    .performance { text-align: center; margin: 20px 0; padding: 15px; background: #E8F5E9; border-radius: 10px; }
                    .footer { text-align: center; margin-top: 40px; color: #9E9E9E; font-size: 12px; }
                    table { width: 100%; border-collapse: collapse; margin-top: 15px; }
                    th { background: #6200EE; color: white; padding: 10px; text-align: right; }
                    td { padding: 10px; border-bottom: 1px solid #E0E0E0; text-align: right; }
                </style>
            </head>
            <body>
                <div class="header">
                    <div class="title">گزارش نتیجه آزمون</div>
                    <div class="subtitle">اپلیکیشن آزمون فارسی پایه چهارم</div>
                </div>
                
                <div class="score-box">
                    <div class="score-value">${String.format("%.1f", result.score)}%</div>
                    <div class="score-label">نمره نهایی</div>
                </div>
                
                <div class="section">
                    <div class="section-title">📋 اطلاعات آزمون</div>
                    <div class="info-row">
                        <span class="info-label">عنوان آزمون:</span>
                        <span class="info-value">${result.examTitle ?: "بدون عنوان"}</span>
                    </div>
                    <div class="info-row">
                        <span class="info-label">تاریخ و زمان:</span>
                        <span class="info-value">$formattedDate</span>
                    </div>
                    <div class="info-row">
                        <span class="info-label">زمان مصرف شده:</span>
                        <span class="info-value">${String.format("%02d:%02d", timeTakenMinutes, timeTakenSeconds)}</span>
                    </div>
                </div>
                
                <div class="section">
                    <div class="section-title">📊 آمار پاسخ‌ها</div>
                    <div class="info-row">
                        <span class="info-label">تعداد کل سوالات:</span>
                        <span class="info-value">${result.totalQuestions}</span>
                    </div>
                    <div class="info-row">
                        <span class="info-label">پاسخ‌های صحیح:</span>
                        <span class="info-value">${result.correctAnswers}</span>
                    </div>
                    <div class="info-row">
                        <span class="info-label">پاسخ‌های نادرست:</span>
                        <span class="info-value">${result.totalQuestions - result.correctAnswers}</span>
                    </div>
                    <div class="info-row">
                        <span class="info-label">درصد دقت:</span>
                        <span class="info-value">${String.format("%.1f", (result.correctAnswers.toFloat() / result.totalQuestions) * 100)}%</span>
                    </div>
                </div>
                
                <div class="performance">
                    <div style="font-size: 18px; font-weight: bold; margin-bottom: 10px;">📈 ارزیابی عملکرد</div>
                    <div style="font-size: 16px; color: #424242;">
                        ${getPerformanceEvaluation(result.score)}
                    </div>
                    <div style="font-size: 14px; color: #757575; margin-top: 10px;">
                        ${getImprovementSuggestion(result.score)}
                    </div>
                </div>
                
                ${if (result.userAnswers?.isNotEmpty() == true) createAnswersTable(result) else ""}
                
                <div class="footer">
                    <p>این گزارش به صورت خودکار توسط اپلیکیشن ExamApp ایجاد شده است.</p>
                    <p>تاریخ ایجاد: ${SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale("fa", "IR")).format(Date())}</p>
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    /**
     * ایجاد محتوای PDF برای همه نتایج
     */
    private fun createAllResultsPdfContent(results: List<Result>): String {
        val dateFormat = SimpleDateFormat("yyyy/MM/dd - HH:mm", Locale("fa", "IR"))

        // محاسبه آمار کلی
        val totalExams = results.size
        val averageScore = results.map { it.score }.average()
        val bestScore = results.maxByOrNull { it.score }?.score ?: 0f
        val totalCorrect = results.sumOf { it.correctAnswers }
        val totalQuestions = results.sumOf { it.totalQuestions }
        val accuracy = if (totalQuestions > 0) (totalCorrect.toFloat() / totalQuestions) * 100 else 0f

        return """
            <html dir="rtl" lang="fa">
            <head>
                <meta charset="UTF-8">
                <title>گزارش کلی نتایج</title>
                <style>
                    body { font-family: 'Tahoma', sans-serif; padding: 20px; }
                    .header { text-align: center; margin-bottom: 30px; }
                    .title { font-size: 24px; color: #6200EE; font-weight: bold; }
                    .subtitle { font-size: 18px; color: #757575; margin-top: 10px; }
                    .stats-container { display: flex; flex-wrap: wrap; justify-content: space-between; margin: 20px 0; }
                    .stat-box { width: 48%; background: #F5F5F5; padding: 15px; border-radius: 10px; margin-bottom: 15px; text-align: center; }
                    .stat-value { font-size: 24px; color: #6200EE; font-weight: bold; }
                    .stat-label { font-size: 14px; color: #757575; margin-top: 5px; }
                    table { width: 100%; border-collapse: collapse; margin-top: 20px; }
                    th { background: #6200EE; color: white; padding: 12px; text-align: right; }
                    td { padding: 10px; border-bottom: 1px solid #E0E0E0; text-align: right; }
                    .row-even { background: #FAFAFA; }
                    .score-excellent { color: #4CAF50; font-weight: bold; }
                    .score-good { color: #2196F3; font-weight: bold; }
                    .score-average { color: #FF9800; font-weight: bold; }
                    .score-poor { color: #F44336; font-weight: bold; }
                    .footer { text-align: center; margin-top: 40px; color: #9E9E9E; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="header">
                    <div class="title">گزارش کلی نتایج آزمون‌ها</div>
                    <div class="subtitle">اپلیکیشن آزمون فارسی پایه چهارم</div>
                </div>
                
                <div class="stats-container">
                    <div class="stat-box">
                        <div class="stat-value">$totalExams</div>
                        <div class="stat-label">تعداد کل آزمون‌ها</div>
                    </div>
                    <div class="stat-box">
                        <div class="stat-value">${String.format("%.1f", averageScore)}%</div>
                        <div class="stat-label">میانگین نمره</div>
                    </div>
                    <div class="stat-box">
                        <div class="stat-value">${String.format("%.1f", bestScore)}%</div>
                        <div class="stat-label">بهترین نمره</div>
                    </div>
                    <div class="stat-box">
                        <div class="stat-value">${String.format("%.1f", accuracy)}%</div>
                        <div class="stat-label">دقت کلی</div>
                    </div>
                </div>
                
                <table>
                    <thead>
                        <tr>
                            <th>ردیف</th>
                            <th>عنوان آزمون</th>
                            <th>تاریخ</th>
                            <th>نمره</th>
                            <th>صحیح/کل</th>
                            <th>زمان</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${results.mapIndexed { index, result ->
            val rowClass = if (index % 2 == 0) "row-even" else ""
            val scoreClass = when {
                result.score >= 90 -> "score-excellent"
                result.score >= 75 -> "score-good"
                result.score >= 50 -> "score-average"
                else -> "score-poor"
            }
            val timeTaken = "${result.timeTaken / (1000 * 60)}:${String.format("%02d", (result.timeTaken / 1000) % 60)}"

            """
                            <tr class="$rowClass">
                                <td>${index + 1}</td>
                                <td>${result.examTitle ?: "بدون عنوان"}</td>
                                <td>${dateFormat.format(Date(result.date))}</td>
                                <td class="$scoreClass">${String.format("%.1f", result.score)}%</td>
                                <td>${result.correctAnswers}/${result.totalQuestions}</td>
                                <td>$timeTaken</td>
                            </tr>
                            """
        }.joinToString("")}
                    </tbody>
                </table>
                
                <div style="margin-top: 30px; padding: 15px; background: #E3F2FD; border-radius: 10px;">
                    <div style="font-size: 16px; font-weight: bold; color: #1976D2; margin-bottom: 10px;">📊 تحلیل کلی عملکرد</div>
                    <div style="color: #424242;">
                        ${getOverallPerformanceAnalysis(results)}
                    </div>
                </div>
                
                <div class="footer">
                    <p>این گزارش شامل ${results.size} نتیجه آزمون می‌باشد.</p>
                    <p>تاریخ ایجاد: ${SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale("fa", "IR")).format(Date())}</p>
                    <p>اپلیکیشن ExamApp - فارسی پایه چهارم</p>
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    /**
     * ایجاد جدول پاسخ‌ها
     */
    private fun createAnswersTable(result: Result): String {
        val userAnswers = result.userAnswers ?: emptyMap()
        return """
            <div style="margin-top: 25px;">
                <div style="font-size: 18px; color: #6200EE; font-weight: bold; margin-bottom: 15px;">📝 جزئیات پاسخ‌ها</div>
                <table>
                    <thead>
                        <tr>
                            <th>شماره سوال</th>
                            <th>پاسخ کاربر</th>
                            <th>پاسخ صحیح</th>
                            <th>وضعیت</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${userAnswers.entries.mapIndexed { index, entry ->
            // TODO: Get correct answers from result
            val isCorrect = true // Placeholder
            val status = if (isCorrect) "✅ صحیح" else "❌ نادرست"
            val statusColor = if (isCorrect) "#4CAF50" else "#F44336"

            """
                            <tr style="${if (index % 2 == 0) "background: #FAFAFA;" else ""}">
                                <td>${index + 1}</td>
                                <td>${entry.value}</td>
                                <td>${entry.value}</td>
                                <td style="color: $statusColor; font-weight: bold;">$status</td>
                            </tr>
                            """
        }.joinToString("")}
                    </tbody>
                </table>
            </div>
        """
    }

    /**
     * ذخیره PDF در فایل
     */
    private suspend fun savePdfToFile(context: Context, htmlContent: String, fileName: String): String {
        return withContext(Dispatchers.IO) {
            // ایجاد پوشه ذخیره‌سازی
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val appDir = File(downloadsDir, "ExamApp")
            if (!appDir.exists()) {
                appDir.mkdirs()
            }

            // ایجاد فایل PDF
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val pdfFile = File(appDir, "${fileName}_${timestamp}.html") // فعلاً HTML، بعداً به PDF تبدیل می‌کنیم

            // نوشتن محتوا در فایل
            pdfFile.writeText(htmlContent, Charsets.UTF_8)

            // بازگشت مسیر فایل
            pdfFile.absolutePath
        }
    }

    /**
     * ارزیابی عملکرد بر اساس نمره
     */
    private fun getPerformanceEvaluation(score: Float): String {
        return when {
            score >= 90 -> "🎉 عملکرد عالی! شما تسلط کاملی بر مطالب دارید."
            score >= 75 -> "👍 عملکرد خوب! با کمی تمرین می‌توانید عالی باشید."
            score >= 50 -> "😊 عملکرد متوسط! نیاز به مرور و تمرین بیشتر دارید."
            else -> "📚 نیاز به تلاش بیشتر! پیشنهاد می‌کنیم مطالب را دوباره مطالعه کنید."
        }
    }

    /**
     * پیشنهاد بهبود
     */
    private fun getImprovementSuggestion(score: Float): String {
        return when {
            score >= 90 -> "بر همین منوال ادامه دهید و برای حفظ دانش خود به صورت دوره‌ای مرور کنید."
            score >= 75 -> "روی نقاط ضعف خود تمرکز کنید و آزمون‌های تمرینی بیشتری بدهید."
            score >= 50 -> "مطالب را فصل به فصل مرور کنید و پس از هر فصل آزمون تمرینی بدهید."
            else -> "از ابتدا شروع کنید، با دقت بیشتری مطالعه کنید و نکات مهم را یادداشت کنید."
        }
    }

    /**
     * تحلیل کلی عملکرد
     */
    private fun getOverallPerformanceAnalysis(results: List<Result>): String {
        if (results.isEmpty()) return "هیچ داده‌ای برای تحلیل وجود ندارد."

        val averageScore = results.map { it.score }.average()
        val trend = if (results.size >= 2) {
            val firstScore = results.first().score
            val lastScore = results.last().score
            if (lastScore > firstScore) "روند صعودی" else if (lastScore < firstScore) "روند نزولی" else "ثابت"
        } else "تعیین نشده"

        return """
            • میانگین نمره شما: ${String.format("%.1f", averageScore)}%
            • روند کلی نمرات: $trend
            • تعداد آزمون‌های با نمره بالای ۸۰: ${results.count { it.score >= 80 }}
            • نیاز به تمرین بیشتر در: ${if (averageScore < 70) "همه مباحث" else "مباحث خاص"}
            • توصیه: ${if (averageScore >= 80) "ادامه روند فعلی" else "افزایش زمان مطالعه و تمرین"}
        """.trimIndent()
    }

    /**
     * پاک کردن حالت
     */
    fun clearState() {
        _pdfFilePath.value = null
        _errorMessage.value = null
        _pdfContent.value = ""
        _pdfGenerationProgress.value = 0
    }
}