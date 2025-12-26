package com.examapp.ui.exam

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.examapp.R
import com.examapp.data.models.Student
import com.examapp.data.remote.ExamRemote
import com.examapp.ui.result.ResultActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class ExamActivity : AppCompatActivity() {

    private val viewModel: ExamViewModel by viewModels()

    // Views
    private lateinit var txtStudentName: TextView
    private lateinit var txtGrade: TextView
    private lateinit var txtTeacherName: TextView
    private lateinit var txtSubject: TextView
    private lateinit var txtLevel: TextView
    private lateinit var txtExamTitle: TextView
    private lateinit var txtQuestionNumber: TextView
    private lateinit var txtQuestionText: TextView
    private lateinit var txtTimer: TextView
    private lateinit var txtProgress: TextView
    private lateinit var progressBar: ProgressBar

    private lateinit var imgQuestion: ImageView
    private lateinit var layoutMcq: LinearLayout
    private lateinit var radioGroup: RadioGroup
    private lateinit var layoutShortAnswer: LinearLayout
    private lateinit var editTextAnswer: EditText
    private lateinit var layoutDescriptive: LinearLayout
    private lateinit var editTextDescriptive: EditText

    private lateinit var btnPrevious: Button
    private lateinit var btnNext: Button
    private lateinit var btnSubmit: Button
    private lateinit var btnStartExam: Button
    private lateinit var btnFlagQuestion: Button

    private lateinit var loadingLayout: LinearLayout
    private lateinit var errorLayout: LinearLayout
    private lateinit var readyLayout: LinearLayout
    private lateinit var examLayout: LinearLayout
    private lateinit var completedLayout: LinearLayout

    private lateinit var txtExamInfo: TextView
    private lateinit var txtExamResult: TextView
    private lateinit var btnDownload: Button

    private lateinit var headerLayout: LinearLayout
    private lateinit var chronometer: TextView

    private lateinit var txtQuestionType: TextView
    private lateinit var txtDifficulty: TextView
    private lateinit var txtPoints: TextView

    private var countDownTimer: CountDownTimer? = null
    private var totalExamTimeMillis: Long = 45 * 60 * 1000 // 45 دقیقه پیش‌فرض

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exam)

        // دریافت examId از Intent
        val examId = intent.getIntExtra("EXAM_ID", -1)
        if (examId == -1) {
            finish()
            return
        }

        initViews()
        setupObservers()
        setupClickListeners()

        // بارگذاری آزمون
        viewModel.loadExam(examId)
    }

    private fun initViews() {
        // هدر
        txtStudentName = findViewById(R.id.tvStudentName)
        txtGrade = findViewById(R.id.tvGrade)
        txtTeacherName = findViewById(R.id.tvTeacherName)
        txtSubject = findViewById(R.id.tvSubject)
        txtLevel = findViewById(R.id.tvLevel)
        headerLayout = findViewById(R.id.headerLayout)
        chronometer = findViewById(R.id.tvTimer)

        // اطلاعات آزمون
        txtExamTitle = findViewById(R.id.tvExamTitle)
        txtQuestionNumber = findViewById(R.id.tvQuestionCounter)
        txtQuestionText = findViewById(R.id.tvQuestionText)
        txtTimer = findViewById(R.id.tvTimer)
        txtProgress = findViewById(R.id.txtProgress)
        progressBar = findViewById(R.id.progressBar)

        // نوع و مشخصات سوال
        txtQuestionType = findViewById(R.id.tvQuestionType)
        txtDifficulty = findViewById(R.id.tvDifficulty)
        txtPoints = findViewById(R.id.tvPoints)

        // سوال و گزینه‌ها
        imgQuestion = findViewById(R.id.imgQuestion)
        layoutMcq = findViewById(R.id.optionsContainer)
        radioGroup = findViewById(R.id.radioGroup)
        layoutShortAnswer = findViewById(R.id.layoutShortAnswer)
        editTextAnswer = findViewById(R.id.editTextAnswer)
        layoutDescriptive = findViewById(R.id.layoutDescriptive)
        editTextDescriptive = findViewById(R.id.editTextDescriptive)

        // دکمه‌ها
        btnPrevious = findViewById(R.id.btnPrevious)
        btnNext = findViewById(R.id.btnNext)
        btnSubmit = findViewById(R.id.btnSubmit)
        btnStartExam = findViewById(R.id.btnStartExam)
        btnFlagQuestion = findViewById(R.id.btnFlagQuestion)
        btnDownload = findViewById(R.id.btnDownloadExam)

        // لایه‌های مختلف
        loadingLayout = findViewById(R.id.loadingLayout)
        errorLayout = findViewById(R.id.errorLayout)
        readyLayout = findViewById(R.id.readyLayout)
        examLayout = findViewById(R.id.examLayout)
        completedLayout = findViewById(R.id.completedLayout)

        txtExamInfo = findViewById(R.id.txtExamInfo)
        txtExamResult = findViewById(R.id.txtExamResult)
    }

    private fun setupObservers() {
        // مشاهده وضعیت UI
        viewModel.uiState.observe(this) { state ->
            when (state) {
                is ExamViewModel.ExamUiState.Loading -> showLoading()
                is ExamViewModel.ExamUiState.Ready -> showReady()
                is ExamViewModel.ExamUiState.Active -> showActive()
                is ExamViewModel.ExamUiState.Completed -> showCompleted(state.message)
                is ExamViewModel.ExamUiState.Error -> showError(state.message)
            }
        }

        // مشاهده داده‌های آزمون
        viewModel.examData.observe(this) { exam ->
            exam?.let {
                updateExamInfo(it)
                totalExamTimeMillis = it.durationMinutes * 60 * 1000L
            }
        }

        // مشاهده سوالات
        viewModel.questions.observe(this) { questions ->
            if (questions.isNotEmpty()) {
                updateQuestionNavigation()
            }
        }

        // مشاهده سوال فعلی
        viewModel.currentQuestion.observe(this) { question ->
            question?.let { showQuestion(it) }
        }

        // مشاهده زمان باقی‌مانده
        viewModel.remainingTime.observe(this) { time ->
            updateTimerDisplay(time)
        }

        // مشاهده پیشرفت
        viewModel.progress.observe(this) { progress ->
            updateProgress(progress)
        }

        // مشاهده تعداد سوالات پاسخ داده شده
        viewModel.answeredCount.observe(this) { count ->
            updateAnsweredCount(count)
        }

        // مشاهده خطاها
        viewModel.errorMessage.observe(this) { error ->
            error?.let { showErrorMessage(it) }
        }
    }

    private fun setupClickListeners() {
        // ناوبری بین سوالات
        btnPrevious.setOnClickListener { viewModel.goToPreviousQuestion() }
        btnNext.setOnClickListener { viewModel.goToNextQuestion() }

        // شروع آزمون
        btnStartExam.setOnClickListener {
            startExamTimer()
            viewModel.startExam()
        }

        // ارسال آزمون
        btnSubmit.setOnClickListener {
            saveCurrentAnswer()
            viewModel.submitExam()
        }

        // علامت‌گذاری سوال
        btnFlagQuestion.setOnClickListener {
            viewModel.toggleFlagCurrentQuestion()
            updateFlagButton()
        }

        // دانلود آزمون (برای نسخه Pro)
        btnDownload.setOnClickListener {
            viewModel.downloadExam()
        }

        // انتخاب گزینه در سوالات چندگزینه‌ای
        radioGroup.setOnCheckedChangeListener { group, checkedId ->
            if (checkedId != -1) {
                val selectedIndex = group.indexOfChild(findViewById(checkedId))
                val answer = ('A' + selectedIndex).toString()
                viewModel.saveCurrentAnswer(selectedOption = answer)
            }
        }

        // ذخیره پاسخ متنی هنگام از دست دادن فوکوس
        editTextAnswer.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                saveTextAnswer()
            }
        }

        editTextDescriptive.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                saveTextAnswer()
            }
        }
    }

    private fun startExamTimer() {
        countDownTimer?.cancel()

        countDownTimer = object : CountDownTimer(totalExamTimeMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000
                viewModel.updateRemainingTime(seconds)
                updateTimerDisplay(seconds)
            }

            override fun onFinish() {
                viewModel.updateRemainingTime(0)
                saveCurrentAnswer()
                viewModel.submitExam()
            }
        }.start()

        examLayout.isVisible = true
        readyLayout.isVisible = false
    }

    private fun updateExamInfo(exam: ExamRemote) {
        txtExamTitle.text = exam.title
        txtSubject.text = "درس: ${exam.category?.name ?: "عمومی"}"
        txtLevel.text = "زمان: ${exam.durationMinutes} دقیقه"

        exam.description?.let {
            findViewById<TextView>(R.id.tvExamDescription)?.text = it
        }

        // نمایش دکمه دانلود فقط برای نسخه Pro
        btnDownload.isVisible = viewModel.repository.isProVersion()
    }

    private fun showQuestion(question: com.examapp.data.remote.QuestionRemote) {
        txtQuestionNumber.text = "سوال ${viewModel.getCurrentQuestionNumber()} از ${viewModel.getTotalQuestions()}"
        txtQuestionText.text = question.questionText

        // نمایش نوع سوال
        txtQuestionType.text = when (question.questionType) {
            "multiple_choice" -> "چندگزینه‌ای"
            "true_false" -> "صحیح/غلط"
            "short_answer" -> "کوتاه‌پاسخ"
            "descriptive" -> "تشریحی"
            else -> "نامشخص"
        }

        // نمایش سختی و نمره
        txtDifficulty.text = "سختی: متوسط"
        txtPoints.text = "نمره: ${question.points}"

        // پنهان کردن همه لایه‌های پاسخ
        layoutMcq.isVisible = false
        layoutShortAnswer.isVisible = false
        layoutDescriptive.isVisible = false

        // نمایش گزینه‌ها بر اساس نوع سوال
        when (question.questionType) {
            "multiple_choice", "true_false" -> showMultipleChoice(question)
            "short_answer" -> showShortAnswer(question)
            "descriptive" -> showDescriptive(question)
        }

        // نمایش وضعیت علامت‌گذاری
        updateFlagButton()

        // نمایش تصویر سوال (اگر وجود دارد)
        // TODO: لود تصویر با Glide/Picasso
    }

    private fun showMultipleChoice(question: com.examapp.data.remote.QuestionRemote) {
        layoutMcq.isVisible = true

        // پاک کردن گزینه‌های قبلی
        radioGroup.removeAllViews()

        // ایجاد گزینه‌ها
        question.options.forEachIndexed { index, option ->
            val radioButton = RadioButton(this).apply {
                id = View.generateViewId()
                text = "${option.letter}. ${option.optionText}"
                textSize = 16f
                setPadding(32, 16, 32, 16)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 8
                }
            }
            radioGroup.addView(radioButton)
        }

        // انتخاب گزینه قبلی کاربر (اگر وجود دارد)
        val savedAnswer = viewModel.getSavedAnswer(question.id)
        savedAnswer?.selectedOption?.let { answer ->
            val index = answer.first().minus('A').toInt()
            if (index in 0 until radioGroup.childCount) {
                (radioGroup.getChildAt(index) as RadioButton).isChecked = true
            }
        }
    }

    private fun showShortAnswer(question: com.examapp.data.remote.QuestionRemote) {
        layoutShortAnswer.isVisible = true

        val savedAnswer = viewModel.getSavedAnswer(question.id)
        editTextAnswer.setText(savedAnswer?.descriptiveAnswer ?: "")
    }

    private fun showDescriptive(question: com.examapp.data.remote.QuestionRemote) {
        layoutDescriptive.isVisible = true

        val savedAnswer = viewModel.getSavedAnswer(question.id)
        editTextDescriptive.setText(savedAnswer?.descriptiveAnswer ?: "")
    }

    private fun saveCurrentAnswer() {
        when {
            layoutMcq.isVisible -> {
                val checkedId = radioGroup.checkedRadioButtonId
                if (checkedId != -1) {
                    val selectedIndex = radioGroup.indexOfChild(findViewById(checkedId))
                    val answer = ('A' + selectedIndex).toString()
                    viewModel.saveCurrentAnswer(selectedOption = answer)
                }
            }
            layoutShortAnswer.isVisible -> {
                val answer = editTextAnswer.text.toString()
                viewModel.saveCurrentAnswer(descriptiveAnswer = answer)
            }
            layoutDescriptive.isVisible -> {
                val answer = editTextDescriptive.text.toString()
                viewModel.saveCurrentAnswer(descriptiveAnswer = answer)
            }
        }
    }

    private fun saveTextAnswer() {
        if (layoutShortAnswer.isVisible) {
            val answer = editTextAnswer.text.toString()
            viewModel.saveCurrentAnswer(descriptiveAnswer = answer)
        } else if (layoutDescriptive.isVisible) {
            val answer = editTextDescriptive.text.toString()
            viewModel.saveCurrentAnswer(descriptiveAnswer = answer)
        }
    }

    private fun updateTimerDisplay(seconds: Long) {
        val minutes = TimeUnit.SECONDS.toMinutes(seconds)
        val remainingSeconds = seconds - TimeUnit.MINUTES.toSeconds(minutes)
        chronometer.text = String.format("%02d:%02d", minutes, remainingSeconds)

        // تغییر رنگ هنگام اتمام زمان
        if (seconds <= 300) { // 5 دقیقه پایانی
            chronometer.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
        }
    }

    private fun updateProgress(progress: Float) {
        progressBar.progress = progress.toInt()
        txtProgress.text = "${progress.toInt()}%"
    }

    private fun updateAnsweredCount(count: Int) {
        findViewById<TextView>(R.id.tvAnsweredCount)?.text = "پاسخ داده شده: $count"
    }

    private fun updateFlagButton() {
        val question = viewModel.currentQuestion.value ?: return
        val savedAnswer = viewModel.getSavedAnswer(question.id)

        if (savedAnswer?.isFlagged == true) {
            btnFlagQuestion.text = "حذف علامت"
            btnFlagQuestion.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_flag_filled, 0, 0, 0
            )
        } else {
            btnFlagQuestion.text = "علامت‌گذاری"
            btnFlagQuestion.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_flag_outline, 0, 0, 0
            )
        }
    }

    private fun updateQuestionNavigation() {
        btnPrevious.isEnabled = !viewModel.isFirstQuestion()
        btnNext.isEnabled = !viewModel.isLastQuestion()
        btnNext.text = if (viewModel.isLastQuestion()) "پایان" else "بعدی"
    }

    // ==================== UI State Handlers ====================

    private fun showLoading() {
        loadingLayout.isVisible = true
        errorLayout.isVisible = false
        readyLayout.isVisible = false
        examLayout.isVisible = false
        completedLayout.isVisible = false
    }

    private fun showReady() {
        loadingLayout.isVisible = false
        errorLayout.isVisible = false
        readyLayout.isVisible = true
        examLayout.isVisible = false
        completedLayout.isVisible = false

        // نمایش اطلاعات آماده‌سازی
        val exam = viewModel.examData.value
        exam?.let {
            findViewById<TextView>(R.id.tvReadyExamTitle).text = it.title
            findViewById<TextView>(R.id.tvReadyQuestionCount).text = "تعداد سوالات: ${it.totalQuestions}"
            findViewById<TextView>(R.id.tvReadyDuration).text = "زمان آزمون: ${it.durationMinutes} دقیقه"
        }
    }

    private fun showActive() {
        loadingLayout.isVisible = false
        errorLayout.isVisible = false
        readyLayout.isVisible = false
        examLayout.isVisible = true
        completedLayout.isVisible = false

        updateQuestionNavigation()
    }

    private fun showCompleted(message: String) {
        loadingLayout.isVisible = false
        errorLayout.isVisible = false
        readyLayout.isVisible = false
        examLayout.isVisible = false
        completedLayout.isVisible = true

        txtExamResult.text = message

        // نمایش دکمه رفتن به نتایج
        findViewById<Button>(R.id.btnViewResults).setOnClickListener {
            navigateToResults()
        }
    }

    private fun showError(message: String) {
        loadingLayout.isVisible = false
        errorLayout.isVisible = true
        readyLayout.isVisible = false
        examLayout.isVisible = false
        completedLayout.isVisible = false

        txtExamInfo.text = message

        // دکمه تلاش مجدد
        findViewById<Button>(R.id.btnRetry).setOnClickListener {
            val examId = viewModel.currentExamId ?: return@setOnClickListener
            viewModel.loadExam(examId)
        }
    }

    private fun showErrorMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun navigateToResults() {
        // انتقال به صفحه نتایج
        val intent = Intent(this, ResultActivity::class.java).apply {
            putExtra("EXAM_ID", viewModel.currentExamId)
        }
        startActivity(intent)
        finish()
    }

    override fun onBackPressed() {
        if (examLayout.isVisible) {
            // نمایش دیالوگ تایید خروج از آزمون
            AlertDialog.Builder(this)
                .setTitle("خروج از آزمون")
                .setMessage("آیا مطمئن هستید که می‌خواهید از آزمون خارج شوید؟")
                .setPositiveButton("بله") { _, _ ->
                    super.onBackPressed()
                }
                .setNegativeButton("خیر", null)
                .show()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
}

// برای دیالوگ تایید
import androidx.appcompat.app.AlertDialog