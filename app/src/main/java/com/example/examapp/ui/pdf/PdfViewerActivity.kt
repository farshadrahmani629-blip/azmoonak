// app/src/main/java/com/examapp/ui/pdf/PdfViewerActivity.kt
package com.examapp.ui.pdf

import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
import com.examapp.data.models.Result
import java.io.File

/**
 * Activity برای نمایش و مدیریت فایل‌های PDF
 * قابلیت‌ها: نمایش PDF، اشتراک‌گذاری، ذخیره، پرینت
 */
class PdfViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPdfViewerBinding
    private val viewModel: PdfViewModel by viewModels {
        PdfViewModelFactory(
            (application as com.examapp.App).resultRepository
        )
    }

    private var resultId: Int? = null
    private var showAllResults: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // دریافت پارامترهای Intent
        resultId = intent.getIntExtra("result_id", -1).takeIf { it != -1 }
        showAllResults = intent.getBooleanExtra("all_results", false)

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
            }

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    binding.progressBar.visibility = android.view.View.GONE
                }
            }
        }
    }

    private fun setupObservers() {
        // مشاهده وضعیت تولید PDF
        viewModel.isGeneratingPdf.observe(this) { isGenerating ->
            binding.progressBar.visibility = if (isGenerating) android.view.View.VISIBLE else android.view.View.GONE
            binding.progressHorizontal.visibility = if (isGenerating) android.view.View.VISIBLE else android.view.View.GONE
            binding.btnShare.isEnabled = !isGenerating
            binding.btnSave.isEnabled = !isGenerating
            binding.btnPrint.isEnabled = !isGenerating
        }

        // مشاهده پیشرفت تولید PDF
        viewModel.pdfGenerationProgress.observe(this) { progress ->
            binding.progressHorizontal.progress = progress
            binding.tvProgress.text = "$progress%"
        }

        // مشاهده محتوای PDF
        viewModel.pdfContent.observe(this) { content ->
            if (content.isNotEmpty()) {
                // نمایش HTML در WebView
                binding.webView.loadDataWithBaseURL(
                    null,
                    content,
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
                Toast.makeText(this, error, Toast.LENGTH_LONG).show()
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
            savePdf()
        }

        // دکمه پرینت
        binding.btnPrint.setOnClickListener {
            printPdf()
        }

        // دکمه بازسازی PDF
        binding.btnRegenerate.setOnClickListener {
            startPdfGeneration()
        }
    }

    private fun startPdfGeneration() {
        if (resultId != null) {
            viewModel.generateSingleResultPdf(this, resultId!!)
        } else if (showAllResults) {
            viewModel.generateAllResultsPdf(this)
        } else {
            Toast.makeText(this, "پارامترهای نامعتبر", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

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
            type = "application/pdf"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        // شروع Activity اشتراک‌گذاری
        startActivity(Intent.createChooser(shareIntent, "اشتراک‌گذاری گزارش"))
    }

    private fun savePdf() {
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

        // ذخیره در پوشه Downloads
        val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
            android.os.Environment.DIRECTORY_DOWNLOADS
        )
        val destinationFile = File(downloadsDir, file.name)

        try {
            file.copyTo(destinationFile, overwrite = true)
            Toast.makeText(this, "PDF در پوشه Downloads ذخیره شد", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "خطا در ذخیره PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun printPdf() {
        // نمایش پیام - در نسخه‌های بعدی پیاده‌سازی می‌شود
        Toast.makeText(this, "امکان پرینت در نسخه‌های بعدی اضافه می‌شود", Toast.LENGTH_SHORT).show()
    }

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
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "برنامه‌ای برای باز کردن فایل PDF یافت نشد", Toast.LENGTH_SHORT).show()
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
            putExtra(Intent.EXTRA_SUBJECT, "گزارش آزمون فارسی پایه چهارم")
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
        viewModel.clearState()
    }

    companion object {
        const val EXTRA_RESULT_ID = "result_id"
        const val EXTRA_ALL_RESULTS = "all_results"
    }
}