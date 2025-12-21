// app/src/main/java/com/examapp/ui/results/ResultActivity.kt
package com.examapp.ui.results

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.examapp.R
import com.examapp.databinding.ActivityResultBinding
import com.examapp.ui.pdf.PdfViewerActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResultBinding
    private val viewModel: ResultViewModel by viewModels {
        ResultViewModelFactory(
            (application as com.examapp.App).resultRepository
        )
    }

    private lateinit var resultAdapter: ResultAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupObservers()
        setupListeners()

        // بارگذاری نتایج
        viewModel.loadAllResults()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "نتایج آزمون‌ها"
        }
    }

    private fun setupRecyclerView() {
        resultAdapter = ResultAdapter(
            onItemClick = { result ->
                showResultDetails(result)
            },
            onDeleteClick = { result ->
                showDeleteConfirmation(result)
            }
        )

        binding.recyclerViewResults.apply {
            layoutManager = LinearLayoutManager(this@ResultActivity)
            adapter = resultAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupObservers() {
        // مشاهده لیست نتایج
        viewModel.allResults.observe(this) { results ->
            if (results.isEmpty()) {
                binding.recyclerViewResults.visibility = View.GONE
                binding.emptyStateView.visibility = View.VISIBLE
                binding.chartContainer.visibility = View.GONE
            } else {
                binding.recyclerViewResults.visibility = View.VISIBLE
                binding.emptyStateView.visibility = View.GONE
                binding.chartContainer.visibility = View.VISIBLE

                resultAdapter.submitList(results)
                updateStatistics()
            }
        }

        // مشاهده داده‌های نمودار
        viewModel.chartData.observe(this) { chartData ->
            if (chartData.isNotEmpty()) {
                binding.resultChart.setData(chartData)
            }
        }

        // مشاهده وضعیت لودینگ
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // مشاهده خطاها
        viewModel.errorMessage.observe(this) { error ->
            error?.let {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupListeners() {
        // فیلتر بر اساس تاریخ
        binding.btnFilter.setOnClickListener {
            showDateFilterDialog()
        }

        // بازنشانی فیلتر
        binding.btnResetFilter.setOnClickListener {
            viewModel.loadAllResults()
        }

        // اشتراک‌گذاری نتایج
        binding.btnShare.setOnClickListener {
            shareResults()
        }

        // خروجی PDF
        binding.btnExportPdf.setOnClickListener {
            exportToPdf()
        }
    }

    private fun updateStatistics() {
        val stats = viewModel.getStatistics()

        binding.tvTotalExams.text = stats["totalExams"].toString()
        binding.tvAverageScore.text = String.format("%.1f%%", stats["averageScore"] as Float)
        binding.tvBestScore.text = String.format("%.1f%%", stats["bestScore"] as Float)
        binding.tvAccuracy.text = String.format("%.1f%%", stats["accuracy"] as Float)

        // نمایش ارزیابی عملکرد
        val avgScore = stats["averageScore"] as Float
        binding.tvPerformance.text = viewModel.getPerformanceEvaluation(avgScore)
        binding.tvSuggestion.text = viewModel.getImprovementSuggestion(avgScore)
    }

    private fun showResultDetails(result: com.examapp.data.models.Result) {
        AlertDialog.Builder(this)
            .setTitle("جزئیات نتیجه")
            .setMessage(
                """
                آزمون: ${result.examTitle ?: "بدون عنوان"}
                
                نمره: ${String.format("%.1f", result.score)}%
                
                پاسخ‌های صحیح: ${result.correctAnswers} از ${result.totalQuestions}
                
                زمان مصرف شده: ${formatTime(result.timeTaken)}
                
                تاریخ: ${viewModel.getFormattedDate(Date(result.date))}
                
                ارزیابی: ${viewModel.getPerformanceEvaluation(result.score)}
                """.trimIndent()
            )
            .setPositiveButton("متوجه شدم") { dialog, _ ->
                dialog.dismiss()
            }
            .setNegativeButton("خروجی PDF") { dialog, _ ->
                exportSingleResultToPdf(result)
                dialog.dismiss()
            }
            .show()
    }

    private fun showDeleteConfirmation(result: com.examapp.data.models.Result) {
        AlertDialog.Builder(this)
            .setTitle("حذف نتیجه")
            .setMessage("آیا مطمئن هستید که می‌خواهید این نتیجه را حذف کنید؟")
            .setPositiveButton("بله") { dialog, _ ->
                viewModel.deleteResult(result.id)
                dialog.dismiss()
            }
            .setNegativeButton("خیر", null)
            .show()
    }

    private fun showDateFilterDialog() {
        // اینجا می‌تونی از DatePickerDialog استفاده کنی
        // برای سادگی فعلی، نمایش پیام
        Toast.makeText(this, "فیلتر تاریخ به زودی اضافه می‌شود", Toast.LENGTH_SHORT).show()
    }

    private fun shareResults() {
        val stats = viewModel.getStatistics()
        val shareText = """
            📊 نتایج آزمون‌های فارسی پایه چهارم
            
            📈 تعداد آزمون‌ها: ${stats["totalExams"]}
            🎯 میانگین نمره: ${String.format("%.1f", stats["averageScore"] as Float)}%
            ⭐ بهترین نمره: ${String.format("%.1f", stats["bestScore"] as Float)}%
            ✅ دقت کلی: ${String.format("%.1f", stats["accuracy"] as Float)}%
            
            عملکرد: ${viewModel.getPerformanceEvaluation(stats["averageScore"] as Float)}
            
            از اپلیکیشن ExamApp
        """.trimIndent()

        val intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }

        startActivity(Intent.createChooser(intent, "اشتراک‌گذاری نتایج"))
    }

    private fun exportToPdf() {
        val results = viewModel.allResults.value
        if (results.isNullOrEmpty()) {
            Toast.makeText(this, "هیچ نتیجه‌ای برای خروجی وجود ندارد", Toast.LENGTH_SHORT).show()
            return
        }

        // انتقال به صفحه PDF
        val intent = Intent(this, PdfViewerActivity::class.java).apply {
            putExtra("all_results", true)
        }
        startActivity(intent)
    }

    private fun exportSingleResultToPdf(result: com.examapp.data.models.Result) {
        val intent = Intent(this, PdfViewerActivity::class.java).apply {
            putExtra("result_id", result.id)
        }
        startActivity(intent)
    }

    private fun formatTime(milliseconds: Long): String {
        val minutes = (milliseconds / (1000 * 60)) % 60
        val seconds = (milliseconds / 1000) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_results, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.action_delete_all -> {
                showDeleteAllConfirmation()
                true
            }
            R.id.action_export_all -> {
                exportToPdf()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showDeleteAllConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("حذف همه نتایج")
            .setMessage("آیا مطمئن هستید که می‌خواهید همه نتایج را حذف کنید؟")
            .setPositiveButton("بله، حذف کن") { dialog, _ ->
                deleteAllResults()
                dialog.dismiss()
            }
            .setNegativeButton("لغو", null)
            .show()
    }

    private fun deleteAllResults() {
        val results = viewModel.allResults.value ?: return
        results.forEach { result ->
            viewModel.deleteResult(result.id)
        }
        Toast.makeText(this, "همه نتایج حذف شدند", Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val EXTRA_RESULT_ID = "extra_result_id"
    }
}