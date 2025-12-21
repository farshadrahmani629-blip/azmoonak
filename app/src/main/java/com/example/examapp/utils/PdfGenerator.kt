package com.examapp.utils

import android.content.Context
import android.os.Build
import android.print.PrintAttributes
import android.print.PrintManager
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.RequiresApi
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * کلاس کمکی برای تولید و مدیریت فایل‌های PDF
 * از WebView برای تبدیل HTML به PDF استفاده می‌کند
 */
class PdfGenerator(private val context: Context) {

    companion object {
        private const val TAG = "PdfGenerator"

        // پوشه ذخیره‌سازی پیش‌فرض
        const val PDF_DIRECTORY = "ExamApp/Reports"
    }

    /**
     * ایجاد PDF از محتوای HTML
     */
    fun generatePdfFromHtml(
        htmlContent: String,
        fileName: String,
        onSuccess: (File) -> Unit,
        onError: (Exception) -> Unit
    ) {
        try {
            // ایجاد WebView موقت
            val webView = WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
            }

            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)

                    // پس از لود کامل صفحه، PDF ایجاد می‌شود
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        createPdfFromWebView(webView, fileName, onSuccess, onError)
                    } else {
                        onError(Exception("API زیر ۲۱ پشتیبانی نمی‌شود"))
                    }
                }
            }

            // بارگذاری HTML در WebView
            webView.loadDataWithBaseURL(
                null,
                htmlContent,
                "text/html",
                "UTF-8",
                null
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error generating PDF", e)
            onError(e)
        }
    }

    /**
     * ایجاد PDF از WebView (برای API 21 و بالاتر)
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun createPdfFromWebView(
        webView: WebView,
        fileName: String,
        onSuccess: (File) -> Unit,
        onError: (Exception) -> Unit
    ) {
        try {
            // ایجاد PrintManager
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager

            // ایجاد آداپتر پرینت
            val printAdapter = webView.createPrintDocumentAdapter("ExamApp_Report")

            // ایجاد فایل خروجی
            val pdfFile = createPdfFile(fileName)

            // شروع فرآیند پرینت به فایل
            printManager.print(
                "ExamApp_Report_$fileName",
                printAdapter,
                PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                    .setResolution(PrintAttributes.Resolution("pdf", "pdf", 300, 300))
                    .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                    .build()
            ).also { printJob ->
                // در اینجا می‌توانید وضعیت پرینت را مانیتور کنید
                printJob?.addCompletionCallback({
                    if (printJob.isCompleted) {
                        onSuccess(pdfFile)
                    } else if (printJob.isFailed) {
                        onError(Exception("پرینت ناموفق بود"))
                    }
                }, null)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error creating PDF from WebView", e)
            onError(e)
        }
    }

    /**
     * ایجاد فایل PDF در حافظه
     */
    private fun createPdfFile(baseFileName: String): File {
        // ایجاد پوشه اگر وجود ندارد
        val storageDir = File(context.getExternalFilesDir(null), PDF_DIRECTORY)
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }

        // ایجاد نام فایل با timestamp
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "${baseFileName}_$timestamp.pdf"

        return File(storageDir, fileName)
    }

    /**
     * ذخیره PDF به صورت مستقیم (بدون WebView)
     */
    fun saveHtmlAsPdfDirectly(
        htmlContent: String,
        fileName: String
    ): File? {
        return try {
            // ایجاد فایل
            val pdfFile = createPdfFile(fileName)

            // نوشتن HTML در فایل (موقت - در نسخه‌های بعدی به PDF واقعی تبدیل می‌شود)
            FileOutputStream(pdfFile).use { outputStream ->
                outputStream.write(htmlContent.toByteArray(Charsets.UTF_8))
            }

            pdfFile
        } catch (e: Exception) {
            Log.e(TAG, "Error saving PDF directly", e)
            null
        }
    }

    /**
     * ایجاد HTML برای یک نتیجه
     */
    fun createResultHtml(result: Result): String {
        val dateFormat = SimpleDateFormat("yyyy/MM/dd - HH:mm", Locale("fa", "IR"))
        val formattedDate = dateFormat.format(result.date)

        val timeTakenMinutes = result.timeTaken / (1000 * 60)
        val timeTakenSeconds = (result.timeTaken / 1000) % 60

        return """
            <!DOCTYPE html>
            <html dir="rtl" lang="fa">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>گزارش آزمون</title>
                <style>
                    * {
                        font-family: 'Tahoma', 'Arial', sans-serif;
                        line-height: 1.6;
                    }
                    
                    body {
                        margin: 0;
                        padding: 20px;
                        background: #f5f5f5;
                    }
                    
                    .container {
                        max-width: 800px;
                        margin: 0 auto;
                        background: white;
                        padding: 30px;
                        border-radius: 15px;
                        box-shadow: 0 0 20px rgba(0,0,0,0.1);
                    }
                    
                    .header {
                        text-align: center;
                        margin-bottom: 30px;
                        padding-bottom: 20px;
                        border-bottom: 2px solid #6200EE;
                    }
                    
                    .title {
                        color: #6200EE;
                        font-size: 28px;
                        margin-bottom: 10px;
                        font-weight: bold;
                    }
                    
                    .subtitle {
                        color: #666;
                        font-size: 18px;
                    }
                    
                    .score-box {
                        text-align: center;
                        background: linear-gradient(135deg, #6200EE, #9C27B0);
                        color: white;
                        padding: 25px;
                        border-radius: 10px;
                        margin: 25px 0;
                    }
                    
                    .score-value {
                        font-size: 48px;
                        font-weight: bold;
                        margin-bottom: 10px;
                    }
                    
                    .score-label {
                        font-size: 18px;
                        opacity: 0.9;
                    }
                    
                    .section {
                        margin-bottom: 25px;
                        padding: 20px;
                        border: 1px solid #e0e0e0;
                        border-radius: 10px;
                        background: #fafafa;
                    }
                    
                    .section-title {
                        color: #6200EE;
                        font-size: 20px;
                        margin-bottom: 15px;
                        font-weight: bold;
                        display: flex;
                        align-items: center;
                        gap: 10px;
                    }
                    
                    .info-grid {
                        display: grid;
                        grid-template-columns: repeat(2, 1fr);
                        gap: 15px;
                    }
                    
                    .info-item {
                        padding: 12px;
                        background: white;
                        border-radius: 8px;
                        border: 1px solid #eee;
                    }
                    
                    .info-label {
                        color: #666;
                        font-size: 14px;
                        margin-bottom: 5px;
                    }
                    
                    .info-value {
                        color: #333;
                        font-size: 16px;
                        font-weight: bold;
                    }
                    
                    .performance {
                        text-align: center;
                        padding: 20px;
                        background: #E8F5E9;
                        border-radius: 10px;
                        margin: 20px 0;
                        border-right: 5px solid #4CAF50;
                    }
                    
                    .performance-title {
                        color: #2E7D32;
                        font-size: 20px;
                        margin-bottom: 10px;
                        font-weight: bold;
                    }
                    
                    .performance-text {
                        color: #424242;
                        font-size: 16px;
                    }
                    
                    .footer {
                        text-align: center;
                        margin-top: 40px;
                        padding-top: 20px;
                        border-top: 1px solid #e0e0e0;
                        color: #9E9E9E;
                        font-size: 14px;
                    }
                    
                    table {
                        width: 100%;
                        border-collapse: collapse;
                        margin-top: 15px;
                    }
                    
                    th {
                        background: #6200EE;
                        color: white;
                        padding: 12px;
                        text-align: right;
                        font-weight: bold;
                    }
                    
                    td {
                        padding: 12px;
                        border-bottom: 1px solid #e0e0e0;
                        text-align: right;
                    }
                    
                    tr:nth-child(even) {
                        background: #f9f9f9;
                    }
                    
                    .correct {
                        color: #4CAF50;
                        font-weight: bold;
                    }
                    
                    .incorrect {
                        color: #F44336;
                        font-weight: bold;
                    }
                    
                    @media print {
                        body {
                            background: white;
                            padding: 0;
                        }
                        
                        .container {
                            box-shadow: none;
                            padding: 15px;
                        }
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <div class="title">📊 گزارش نتیجه آزمون</div>
                        <div class="subtitle">اپلیکیشن آزمون فارسی پایه چهارم</div>
                    </div>
                    
                    <div class="score-box">
                        <div class="score-value">${String.format("%.1f", result.score)}%</div>
                        <div class="score-label">نمره نهایی شما</div>
                    </div>
                    
                    <div class="section">
                        <div class="section-title">📋 اطلاعات آزمون</div>
                        <div class="info-grid">
                            <div class="info-item">
                                <div class="info-label">عنوان آزمون</div>
                                <div class="info-value">${result.examTitle}</div>
                            </div>
                            <div class="info-item">
                                <div class="info-label">تاریخ و زمان</div>
                                <div class="info-value">$formattedDate</div>
                            </div>
                            <div class="info-item">
                                <div class="info-label">زمان مصرف شده</div>
                                <div class="info-value">${String.format("%02d:%02d", timeTakenMinutes, timeTakenSeconds)}</div>
                            </div>
                            <div class="info-item">
                                <div class="info-label">نوع آزمون</div>
                                <div class="info-value">آزمون تشریحی</div>
                            </div>
                        </div>
                    </div>
                    
                    <div class="section">
                        <div class="section-title">🎯 آمار پاسخ‌ها</div>
                        <div class="info-grid">
                            <div class="info-item">
                                <div class="info-label">تعداد کل سوالات</div>
                                <div class="info-value">${result.totalQuestions}</div>
                            </div>
                            <div class="info-item">
                                <div class="info-label">پاسخ‌های صحیح</div>
                                <div class="info-value">${result.correctAnswers}</div>
                            </div>
                            <div class="info-item">
                                <div class="info-label">پاسخ‌های نادرست</div>
                                <div class="info-value">${result.totalQuestions - result.correctAnswers}</div>
                            </div>
                            <div class="info-item">
                                <div class="info-label">درصد دقت</div>
                                <div class="info-value">${String.format("%.1f", (result.correctAnswers.toFloat() / result.totalQuestions) * 100)}%</div>
                            </div>
                        </div>
                    </div>
                    
                    <div class="performance">
                        <div class="performance-title">📈 ارزیابی عملکرد</div>
                        <div class="performance-text">
                            ${getPerformanceEvaluation(result.score)}
                        </div>
                        <div style="margin-top: 15px; font-size: 14px;">
                            💡 <strong>پیشنهاد بهبود:</strong> ${getImprovementSuggestion(result.score)}
                        </div>
                    </div>
                    
                    ${if (result.userAnswers.isNotEmpty()) createAnswersTableHtml(result) else ""}
                    
                    <div class="footer">
                        <p>✅ این گزارش به صورت خودکار توسط اپلیکیشن ExamApp ایجاد شده است.</p>
                        <p>🕒 تاریخ ایجاد: ${SimpleDateFormat("yyyy/MM/dd - HH:mm:ss", Locale("fa", "IR")).format(Date())}</p>
                        <p>📱 نسخه اپلیکیشن: ۱.۰.۰</p>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    /**
     * ایجاد HTML برای همه نتایج
     */
    fun createAllResultsHtml(results: List<Result>): String {
        if (results.isEmpty()) {
            return createEmptyResultsHtml()
        }

        val dateFormat = SimpleDateFormat("yyyy/MM/dd - HH:mm", Locale("fa", "IR"))

        // محاسبه آمار
        val stats = calculateResultsStats(results)

        return """
            <!DOCTYPE html>
            <html dir="rtl" lang="fa">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>گزارش کلی نتایج</title>
                <style>
                    ${getCommonStyles()}
                    
                    .stats-container {
                        display: grid;
                        grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
                        gap: 15px;
                        margin: 25px 0;
                    }
                    
                    .stat-card {
                        background: linear-gradient(135deg, #6200EE, #9C27B0);
                        color: white;
                        padding: 20px;
                        border-radius: 10px;
                        text-align: center;
                    }
                    
                    .stat-value {
                        font-size: 32px;
                        font-weight: bold;
                        margin-bottom: 5px;
                    }
                    
                    .stat-label {
                        font-size: 14px;
                        opacity: 0.9;
                    }
                    
                    .analysis-box {
                        background: #E3F2FD;
                        padding: 20px;
                        border-radius: 10px;
                        margin: 25px 0;
                        border-right: 5px solid #2196F3;
                    }
                    
                    .analysis-title {
                        color: #1976D2;
                        font-size: 20px;
                        margin-bottom: 15px;
                        font-weight: bold;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <div class="title">📊 گزارش کلی نتایج آزمون‌ها</div>
                        <div class="subtitle">خلاصه عملکرد در ${results.size} آزمون</div>
                    </div>
                    
                    <div class="stats-container">
                        <div class="stat-card">
                            <div class="stat-value">${stats["totalExams"]}</div>
                            <div class="stat-label">تعداد آزمون‌ها</div>
                        </div>
                        <div class="stat-card">
                            <div class="stat-value">${String.format("%.1f", stats["averageScore"])}%</div>
                            <div class="stat-label">میانگین نمره</div>
                        </div>
                        <div class="stat-card">
                            <div class="stat-value">${String.format("%.1f", stats["bestScore"])}%</div>
                            <div class="stat-label">بهترین نمره</div>
                        </div>
                        <div class="stat-card">
                            <div class="stat-value">${String.format("%.1f", stats["accuracy"])}%</div>
                            <div class="stat-label">دقت کلی</div>
                        </div>
                    </div>
                    
                    <div class="section">
                        <div class="section-title">📋 لیست نتایج</div>
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
            val scoreClass = when {
                result.score >= 90 -> "correct"
                result.score >= 75 -> "score-good"
                result.score >= 50 -> "score-average"
                else -> "incorrect"
            }
            val timeTaken = "${result.timeTaken / (1000 * 60)}:${String.format("%02d", (result.timeTaken / 1000) % 60)}"

            """
                                    <tr>
                                        <td>${index + 1}</td>
                                        <td>${result.examTitle}</td>
                                        <td>${dateFormat.format(result.date)}</td>
                                        <td class="$scoreClass">${String.format("%.1f", result.score)}%</td>
                                        <td>${result.correctAnswers}/${result.totalQuestions}</td>
                                        <td>$timeTaken</td>
                                    </tr>
                                    """
        }.joinToString("")}
                            </tbody>
                        </table>
                    </div>
                    
                    <div class="analysis-box">
                        <div class="analysis-title">📈 تحلیل عملکرد کلی</div>
                        <div class="performance-text">
                            ${getOverallAnalysis(stats, results)}
                        </div>
                    </div>
                    
                    <div class="footer">
                        <p>📊 این گزارش شامل ${results.size} نتیجه آزمون می‌باشد</p>
                        <p>🕒 تاریخ ایجاد: ${SimpleDateFormat("yyyy/MM/dd - HH:mm:ss", Locale("fa", "IR")).format(Date())}</p>
                        <p>📱 اپلیکیشن ExamApp - فارسی پایه چهارم</p>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    /**
     * محاسبه آمار نتایج
     */
    private fun calculateResultsStats(results: List<Result>): Map<String, Float> {
        val totalExams = results.size
        val averageScore = results.map { it.score }.average().toFloat()
        val bestScore = results.maxByOrNull { it.score }?.score ?: 0f
        val totalCorrect = results.sumOf { it.correctAnswers }
        val totalQuestions = results.sumOf { it.totalQuestions }
        val accuracy = if (totalQuestions > 0) (totalCorrect.toFloat() / totalQuestions) * 100 else 0f

        return mapOf(
            "totalExams" to totalExams.toFloat(),
            "averageScore" to averageScore,
            "bestScore" to bestScore,
            "accuracy" to accuracy
        )
    }

    /**
     * ایجاد جدول پاسخ‌ها در HTML
     */
    private fun createAnswersTableHtml(result: Result): String {
        return """
            <div class="section">
                <div class="section-title">📝 جزئیات پاسخ‌ها</div>
                <table>
                    <thead>
                        <tr>
                            <th>شماره سوال</th>
                            <th>پاسخ کاربر</th>
                            <th>وضعیت</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${result.userAnswers.entries.mapIndexed { index, entry ->
            val userAnswer = entry.value + 1
            val isCorrect = true // اینجا نیاز به مقایسه با پاسخ صحیح داریم
            val status = if (isCorrect) "✅ صحیح" else "❌ نادرست"
            val statusClass = if (isCorrect) "correct" else "incorrect"

            """
                            <tr>
                                <td>${index + 1}</td>
                                <td>گزینه $userAnswer</td>
                                <td class="$statusClass">$status</td>
                            </tr>
                            """
        }.joinToString("")}
                    </tbody>
                </table>
            </div>
        """
    }

    /**
     * HTML برای حالت خالی
     */
    private fun createEmptyResultsHtml(): String {
        return """
            <!DOCTYPE html>
            <html dir="rtl" lang="fa">
            <head>
                <meta charset="UTF-8">
                <style>
                    ${getCommonStyles()}
                    .empty-state {
                        text-align: center;
                        padding: 50px 20px;
                    }
                    .empty-icon {
                        font-size: 60px;
                        margin-bottom: 20px;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="empty-state">
                        <div class="empty-icon">📭</div>
                        <h2 style="color: #666;">هیچ نتیجه‌ای یافت نشد</h2>
                        <p style="color: #999;">هنوز هیچ آزمونی انجام نشده است.</p>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    /**
     * استایل‌های مشترک
     */
    private fun getCommonStyles(): String {
        return """
            * {
                font-family: 'Tahoma', 'Arial', sans-serif;
                line-height: 1.6;
            }
            
            body {
                margin: 0;
                padding: 20px;
                background: #f5f5f5;
            }
            
            .container {
                max-width: 800px;
                margin: 0 auto;
                background: white;
                padding: 30px;
                border-radius: 15px;
                box-shadow: 0 0 20px rgba(0,0,0,0.1);
            }
            
            .header {
                text-align: center;
                margin-bottom: 30px;
                padding-bottom: 20px;
                border-bottom: 2px solid #6200EE;
            }
            
            .title {
                color: #6200EE;
                font-size: 28px;
                margin-bottom: 10px;
                font-weight: bold;
            }
            
            .subtitle {
                color: #666;
                font-size: 18px;
            }
            
            .section {
                margin-bottom: 25px;
                padding: 20px;
                border: 1px solid #e0e0e0;
                border-radius: 10px;
                background: #fafafa;
            }
            
            .section-title {
                color: #6200EE;
                font-size: 20px;
                margin-bottom: 15px;
                font-weight: bold;
            }
            
            table {
                width: 100%;
                border-collapse: collapse;
                margin-top: 15px;
            }
            
            th {
                background: #6200EE;
                color: white;
                padding: 12px;
                text-align: right;
                font-weight: bold;
            }
            
            td {
                padding: 12px;
                border-bottom: 1px solid #e0e0e0;
                text-align: right;
            }
            
            tr:nth-child(even) {
                background: #f9f9f9;
            }
            
            .correct {
                color: #4CAF50;
                font-weight: bold;
            }
            
            .incorrect {
                color: #F44336;
                font-weight: bold;
            }
            
            .score-good { color: #2196F3; }
            .score-average { color: #FF9800; }
            
            .footer {
                text-align: center;
                margin-top: 40px;
                padding-top: 20px;
                border-top: 1px solid #e0e0e0;
                color: #9E9E9E;
                font-size: 14px;
            }
            
            @media print {
                body {
                    background: white;
                    padding: 0;
                }
                
                .container {
                    box-shadow: none;
                    padding: 15px;
                }
            }
        """
    }

    /**
     * ارزیابی عملکرد
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
            score >= 90 -> "برای حفظ دانش خود، هفته‌ای یک بار مطالب را مرور کنید."
            score >= 75 -> "روی سوالاتی که پاسخ نداده‌اید یا اشتباه پاسخ داده‌اید تمرکز کنید."
            score >= 50 -> "هر درس را جداگانه مطالعه کرده و سپس آزمون دهید."
            else -> "از ابتدای کتاب شروع کنید و هر بخش را با دقت کامل بخوانید."
        }
    }

    /**
     * تحلیل کلی
     */
    private fun getOverallAnalysis(stats: Map<String, Float>, results: List<Result>): String {
        val averageScore = stats["averageScore"] ?: 0f
        val trend = if (results.size >= 2) {
            val firstScore = results.first().score
            val lastScore = results.last().score
            when {
                lastScore > firstScore + 5 -> "صعودی بسیار خوب 📈"
                lastScore > firstScore -> "صعودی مناسب ↗️"
                lastScore < firstScore -> "نزولی نیاز به توجه 🔻"
                else -> "ثابت ⏸️"
            }
        } else "تعیین نشده"

        val examsAbove80 = results.count { it.score >= 80 }

        return """
            • میانگین نمره شما: <strong>${String.format("%.1f", averageScore)}%</strong>
            • روند کلی: $trend
            • تعداد آزمون‌های با نمره عالی (بالای ۸۰): $examsAbove80 از ${results.size}
            • ${if (averageScore >= 80) "🎯 عالی هستید! ادامه دهید." else "💪 نیاز به تمرین بیشتر دارید."}
            • پیشنهاد: ${if (averageScore >= 85) "روی مباحث پیشرفته تمرکز کنید." else "مباحث پایه را مرور کنید."}
        """.trimIndent()
    }

    /**
     * پاک کردن فایل‌های قدیمی PDF
     */
    fun cleanupOldPdfFiles(maxAgeDays: Int = 30) {
        try {
            val storageDir = File(context.getExternalFilesDir(null), PDF_DIRECTORY)
            if (!storageDir.exists()) return

            val cutoffTime = System.currentTimeMillis() - (maxAgeDays * 24 * 60 * 60 * 1000L)

            storageDir.listFiles()?.forEach { file ->
                if (file.lastModified() < cutoffTime) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up PDF files", e)
        }
    }
}