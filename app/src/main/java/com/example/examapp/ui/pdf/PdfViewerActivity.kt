// app/src/main/java/com/examapp/ui/pdf/PdfViewerActivity.kt
package com.examapp.ui.pdf

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintManager
import android.view.Menu
import android.view.MenuItem
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.examapp.R
import com.examapp.databinding.ActivityPdfViewerBinding
import com.examapp.ui.exam.pdf.StorageConfig
import kotlinx.coroutines.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.CoroutineContext

/**
 * Activity برای نمایش و مدیریت فایل‌های PDF
 * قابلیت‌ها: نمایش PDF، اشتراک‌گذاری، ذخیره، پرینت
 */
class PdfViewerActivity : AppCompatActivity(), CoroutineScope {

    private lateinit var binding: ActivityPdfViewerBinding
    private val viewModel: PdfViewModel by viewModels {
        PdfViewModelFactory(
            (application as com.examapp.App).resultRepository
        )
    }

    // Coroutine Scope
    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    // State
    private var resultId: Int? = null
    private var showAllResults: Boolean = false
    private var currentProgress = 0

    companion object {
        const val EXTRA_RESULT_ID = "result_id"
        const val EXTRA_ALL_RESULTS = "all_results"
        const val EXTRA_PDF_PATH = "pdf_path"
        const val EXTRA_PDF_TITLE = "pdf_title"
        const val EXTRA_PDF_CONTENT = "pdf_content"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // دریافت پارامترهای Intent
        resultId = intent.getIntExtra(EXTRA_RESULT_ID, -1).takeIf { it != -1 }
        showAllResults = intent.getBooleanExtra(EXTRA_ALL_RESULTS, false)

        setupToolbar()
        setupWebView()
        setupObservers()
        setupListeners()

        // شروع ایجاد PDF
        startPdfGeneration()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = when {
                resultId != null -> "گزارش نتیجه"
                showAllResults -> "گزارش کلی نتایج"
                else -> "نمایش PDF"
            }
        }
    }

    private fun setupWebView() {
        binding.webView.apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                loadWithOverviewMode = true
                useWideViewPort = true
                builtInZoomControls = true
                displayZoomControls = false
                setSupportZoom(true)
                allowFileAccess = true
                allowContentAccess = true
            }

            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    showLoading()
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    hideLoading()
                    enableActionButtons()
                }
            }
        }
    }

    private fun setupObservers() {
        // مشاهده وضعیت تولید PDF
        viewModel.isGeneratingPdf.observe(this) { isGenerating ->
            if (isGenerating) {
                showLoading()
            } else {
                // hideLoading() در onPageFinished فراخوانی می‌شود
            }
            binding.btnShare.isEnabled = !isGenerating
            binding.btnSave.isEnabled = !isGenerating
            binding.btnPrint.isEnabled = !isGenerating
        }

        // مشاهده پیشرفت تولید PDF
        viewModel.pdfGenerationProgress.observe(this) { progress ->
            binding.progressHorizontal.progress = progress
            binding.tvProgress.text = "$progress%"
            currentProgress = progress
        }

        // مشاهده محتوای PDF
        viewModel.pdfContent.observe(this) { content ->
            if (content.isNotEmpty()) {
                // نمایش HTML در WebView
                val htmlContent = createHtmlReport(content)
                binding.webView.loadDataWithBaseURL(
                    null,
                    htmlContent,
                    "text/html",
                    "UTF-8",
                    null
                )
            }
        }

        // مشاهده مسیر فایل PDF
        viewModel.pdfFilePath.observe(this) { filePath ->
            filePath?.let {
                Toast.makeText(this, "PDF با موفقیت ایجاد شد", Toast.LENGTH_SHORT).show()
            }
        }

        // مشاهده خطاها
        viewModel.errorMessage.observe(this) { error ->
            error?.let {
                showError(it)
            }
        }
    }

    private fun setupListeners() {
        // دکمه اشتراک‌گذاری
        binding.btnShare.setOnClickListener {
            sharePdf()
        }

        // دکمه ذخیره
        binding.btnSave.setOnClickListener {
            savePdfToDevice()
        }

        // دکمه پرینت
        binding.btnPrint.setOnClickListener {
            printPdf()
        }

        // دکمه بازسازی PDF
        binding.btnRegenerate.setOnClickListener {
            startPdfGeneration()
        }

        // دکمه تلاش مجدد در صفحه خطا
        binding.errorContainer.findViewById<android.widget.Button>(R.id.btnRetry)?.setOnClickListener {
            startPdfGeneration()
        }
    }

    private fun startPdfGeneration() {
        hideError()

        // شبیه‌سازی پیشرفت
        simulatePdfGeneration()

        if (resultId != null) {
            viewModel.generateSingleResultPdf(this, resultId!!)
        } else if (showAllResults) {
            viewModel.generateAllResultsPdf(this)
        } else {
            // اگر هیچ پارامتری نبود، PDF ساده ایجاد کن
            generateSimplePdf()
        }
    }

    private fun generateSimplePdf() {
        launch {
            try {
                val content = """
                    گزارش آزمون نمونه
                    تاریخ: ${getCurrentDate()}
                    نام دانش‌آموز: نمونه
                    پایه: چهارم
                    درس: فارسی
                    نمره: ۸۵
                """.trimIndent()

                val htmlContent = createHtmlReport(content)
                binding.webView.loadDataWithBaseURL(
                    null,
                    htmlContent,
                    "text/html",
                    "UTF-8",
                    null
                )

                // ذخیره فایل
                saveGeneratedPdf(htmlContent)

            } catch (e: Exception) {
                showError("خطا در ایجاد گزارش: ${e.message}")
            }
        }
    }

    private fun simulatePdfGeneration() {
        launch {
            repeat(100) { progress ->
                delay(30)
                currentProgress = progress + 1
                updateProgress(currentProgress)
            }
        }
    }

    // ==================== HTML Report Generator ====================

    private fun createHtmlReport(content: String): String {
        val title = when {
            resultId != null -> "گزارش نتیجه آزمون"
            showAllResults -> "گزارش کلی نتایج"
            else -> "گزارش آزمون"
        }

        return """
            <!DOCTYPE html>
            <html dir="rtl" lang="fa">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>${title}</title>
                <style>
                    body {
                        font-family: 'Tahoma', 'Arial', sans-serif;
                        line-height: 1.8;
                        color: #333;
                        padding: 20px;
                        background-color: #f9f9f9;
                    }
                    .header {
                        text-align: center;
                        margin-bottom: 30px;
                        padding: 20px;
                        background: linear-gradient(135deg, #6a11cb 0%, #2575fc 100%);
                        color: white;
                        border-radius: 10px;
                        box-shadow: 0 4px 6px rgba(0,0,0,0.1);
                    }
                    .header h1 {
                        margin: 0;
                        font-size: 24px;
                    }
                    .content {
                        background: white;
                        padding: 25px;
                        border-radius: 10px;
                        box-shadow: 0 2px 10px rgba(0,0,0,0.05);
                        margin-bottom: 20px;
                    }
                    .section {
                        margin-bottom: 25px;
                        padding-bottom: 20px;
                        border-bottom: 2px solid #f0f0f0;
                    }
                    .section-title {
                        color: #2575fc;
                        font-size: 20px;
                        margin-bottom: 15px;
                        padding-right: 10px;
                        border-right: 4px solid #2575fc;
                    }
                    .footer {
                        text-align: center;
                        margin-top: 30px;
                        padding-top: 20px;
                        border-top: 1px solid #eee;
                        color: #666;
                        font-size: 14px;
                    }
                </style>
            </head>
            <body>
                <div class="header">
                    <h1>📊 ${title}</h1>
                    <div>تاریخ ایجاد: ${getCurrentDate()}</div>
                </div>
                
                <div class="content">
                    <div class="section">
                        <h2 class="section-title">📋 اطلاعات آزمون</h2>
                        <div style="white-space: pre-line; line-height: 1.8;">
                            ${content}
                        </div>
                    </div>
                </div>
                
                <div class="footer">
                    <p>با تشکر از استفاده شما از سیستم آزمون آنلاین</p>
                    <p>© ${Calendar.getInstance().get(Calendar.YEAR)} - ExamApp</p>
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    private fun saveGeneratedPdf(htmlContent: String): File {
        val storageConfig = StorageConfig
        val filePath = storageConfig.getResultPdfPath(
            context = this,
            examTitle = "گزارش آزمون",
            studentName = "دانش‌آموز",
            score = 85.5f
        )

        val file = File(filePath)
        file.parentFile?.mkdirs()
        file.writeText(htmlContent, Charsets.UTF_8)

        // اطلاع به ViewModel
        viewModel.setPdfFilePath(filePath)

        return file
    }

    // ==================== عملیات فایل ====================

    private fun sharePdf() {
        val filePath = viewModel.pdfFilePath.value
        if (filePath.isNullOrEmpty()) {
            Toast.makeText(this, "ابتدا PDF را ایجاد کنید", Toast.LENGTH_SHORT).show()
            return
        }

        val file = File(filePath)
        if (!file.exists()) {
            Toast.makeText(this, "فایل PDF یافت نشد", Toast.LENGTH_SHORT).show()
            return
        }

        // ایجاد URI با FileProvider
        val uri = FileProvider.getUriForFile(
            this,
            "${packageName}.provider",
            file
        )

        // ایجاد Intent اشتراک‌گذاری
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, uri)
            type = "text/html" // برای HTML
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        // شروع Activity اشتراک‌گذاری
        startActivity(Intent.createChooser(shareIntent, "اشتراک‌گذاری گزارش"))
    }

    private fun savePdfToDevice() {
        val filePath = viewModel.pdfFilePath.value
        if (filePath.isNullOrEmpty()) {
            Toast.makeText(this, "ابتدا PDF را ایجاد کنید", Toast.LENGTH_SHORT).show()
            return
        }

        launch {
            try {
                showLoading()
                updateProgressText("در حال ذخیره گزارش...")

                val savedFile = withContext(Dispatchers.IO) {
                    val file = File(filePath)
                    val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
                        android.os.Environment.DIRECTORY_DOWNLOADS
                    )
                    val examDir = File(downloadsDir, "ExamApp")
                    if (!examDir.exists()) examDir.mkdirs()

                    val destination = File(examDir, file.name)
                    file.copyTo(destination, overwrite = true)

                    // اطلاع‌رسانی به سیستم
                    val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                    mediaScanIntent.data = Uri.fromFile(destination)
                    sendBroadcast(mediaScanIntent)

                    destination
                }

                hideLoading()
                android.app.AlertDialog.Builder(this@PdfViewerActivity)
                    .setTitle("ذخیره موفق")
                    .setMessage("گزارش با موفقیت ذخیره شد:\n${savedFile.absolutePath}")
                    .setPositiveButton("متوجه شدم", null)
                    .show()

            } catch (e: Exception) {
                hideLoading()
                showError("خطا در ذخیره فایل: ${e.message}")
            }
        }
    }

    private fun printPdf() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            val printManager = getSystemService(PRINT_SERVICE) as PrintManager
            val printAdapter = binding.webView.createPrintDocumentAdapter("Report")

            val jobName = "گزارش_${System.currentTimeMillis()}"
            printManager.print(
                jobName,
                printAdapter,
                PrintAttributes.Builder().build()
            )
        } else {
            Toast.makeText(this, "چاپ از Android 4.4 به بالا پشتیبانی می‌شود", Toast.LENGTH_SHORT).show()
        }
    }

    // ==================== UI Management ====================

    private fun showLoading() {
        binding.progressContainer.visibility = android.view.View.VISIBLE
        binding.webView.visibility = android.view.View.GONE
        binding.actionButtonsContainer.visibility = android.view.View.GONE
        binding.errorContainer.visibility = android.view.View.GONE
        binding.btnRegenerate.visibility = android.view.View.GONE
    }

    private fun hideLoading() {
        binding.progressContainer.visibility = android.view.View.GONE
        binding.webView.visibility = android.view.View.VISIBLE
        binding.actionButtonsContainer.visibility = android.view.View.VISIBLE
    }

    private fun updateProgress(progress: Int) {
        binding.progressHorizontal.progress = progress
        binding.tvProgress.text = "$progress%"
    }

    private fun updateProgressText(text: String) {
        // اگر TextView جداگانه برای متن پیشرفت دارید
        binding.progressContainer.findViewById<android.widget.TextView>(R.id.textView)?.text = text
    }

    private fun enableActionButtons() {
        binding.btnShare.isEnabled = true
        binding.btnSave.isEnabled = true
        binding.btnPrint.isEnabled = true
        binding.btnRegenerate.visibility = android.view.View.VISIBLE
    }

    private fun showError(message: String) {
        binding.errorContainer.visibility = android.view.View.VISIBLE
        binding.progressContainer.visibility = android.view.View.GONE
        binding.webView.visibility = android.view.View.GONE
        binding.actionButtonsContainer.visibility = android.view.View.GONE

        binding.tvErrorMessage.text = message
    }

    private fun hideError() {
        binding.errorContainer.visibility = android.view.View.GONE
    }

    private fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("yyyy/MM/dd - HH:mm", Locale("fa", "IR"))
        return dateFormat.format(Date())
    }

    // ==================== Menu ====================

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_pdf_viewer, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.action_refresh -> {
                startPdfGeneration()
                true
            }
            R.id.action_open_external -> {
                openInExternalApp()
                true
            }
            R.id.action_send_email -> {
                sendViaEmail()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun openInExternalApp() {
        val filePath = viewModel.pdfFilePath.value
        if (filePath.isNullOrEmpty()) {
            Toast.makeText(this, "ابتدا PDF را ایجاد کنید", Toast.LENGTH_SHORT).show()
            return
        }

        val file = File(filePath)
        if (!file.exists()) {
            Toast.makeText(this, "فایل PDF یافت نشد", Toast.LENGTH_SHORT).show()
            return
        }

        val uri = FileProvider.getUriForFile(
            this,
            "${packageName}.provider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "text/html")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "برنامه‌ای برای باز کردن فایل یافت نشد", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendViaEmail() {
        val filePath = viewModel.pdfFilePath.value
        if (filePath.isNullOrEmpty()) {
            Toast.makeText(this, "ابتدا PDF را ایجاد کنید", Toast.LENGTH_SHORT).show()
            return
        }

        val file = File(filePath)
        if (!file.exists()) {
            Toast.makeText(this, "فایل PDF یافت نشد", Toast.LENGTH_SHORT).show()
            return
        }

        val uri = FileProvider.getUriForFile(
            this,
            "${packageName}.provider",
            file
        )

        val emailIntent = Intent(Intent.ACTION_SEND).apply {
            type = "message/rfc822"
            putExtra(Intent.EXTRA_SUBJECT, "گزارش آزمون")
            putExtra(Intent.EXTRA_TEXT, "گزارش آزمون ضمیمه شده است.")
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            startActivity(Intent.createChooser(emailIntent, "ارسال ایمیل"))
        } catch (e: Exception) {
            Toast.makeText(this, "برنامه ایمیل یافت نشد", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onBackPressed() {
        if (binding.webView.canGoBack()) {
            binding.webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
        viewModel.clearState()
    }
}