// app/src/main/java/com/examapp/ui/exam/ExamActivity.kt
package com.examapp.ui.exam

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.examapp.R
import com.examapp.data.models.Question
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ExamActivity : AppCompatActivity() {

    // ------------ ViewModel ------------
    private val viewModel: ExamActivityViewModel by viewModels()

    // ------------ Viewها ------------
    private lateinit var txtExamTitle: TextView
    private lateinit var txtQuestionNumber: TextView
    private lateinit var txtQuestionText: TextView
    private lateinit var txtTimer: TextView
    private lateinit var txtProgress: TextView
    private lateinit var progressBar: ProgressBar

    private lateinit var layoutMcq: LinearLayout
    private lateinit var radioGroup: RadioGroup
    private lateinit var layoutShortAnswer: LinearLayout
    private lateinit var editTextAnswer: EditText
    private lateinit var layoutFillBlank: LinearLayout
    private lateinit var editTextFillBlank: EditText

    private lateinit var btnPrevious: Button
    private lateinit var btnNext: Button
    private lateinit var btnSubmit: Button
    private lateinit var btnStartExam: Button

    private lateinit var loadingLayout: LinearLayout
    private lateinit var errorLayout: LinearLayout
    private lateinit var readyLayout: LinearLayout
    private lateinit var examLayout: LinearLayout
    private lateinit var completedLayout: LinearLayout

    private lateinit var txtExamInfo: TextView
    private lateinit var txtExamResult: TextView

    // ------------ متغیرها ------------
    private var currentQuestion: Question? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exam)

        // دریافت ExamId از Intent
        val examId = intent.getStringExtra("EXAM_ID")
        if (examId.isNullOrEmpty()) {
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
        // TextViewها
        txtExamTitle = findViewById(R.id.txtExamTitle)
        txtQuestionNumber = findViewById(R.id.txtQuestionNumber)
        txtQuestionText = findViewById(R.id.txtQuestionText)
        txtTimer = findViewById(R.id.txtTimer)
        txtProgress = findViewById(R.id.txtProgress)
        progressBar = findViewById(R.id.progressBar)

        // Layoutهای مختلف سوالات
        layoutMcq = findViewById(R.id.layoutMcq)
        radioGroup = findViewById(R.id.radioGroup)
        layoutShortAnswer = findViewById(R.id.layoutShortAnswer)
        editTextAnswer = findViewById(R.id.editTextAnswer)
        layoutFillBlank = findViewById(R.id.layoutFillBlank)
        editTextFillBlank = findViewById(R.id.editTextFillBlank)

        // دکمه‌ها
        btnPrevious = findViewById(R.id.btnPrevious)
        btnNext = findViewById(R.id.btnNext)
        btnSubmit = findViewById(R.id.btnSubmit)
        btnStartExam = findViewById(R.id.btnStartExam)

        // Layoutهای وضعیت
        loadingLayout = findViewById(R.id.loadingLayout)
        errorLayout = findViewById(R.id.errorLayout)
        readyLayout = findViewById(R.id.readyLayout)
        examLayout = findViewById(R.id.examLayout)
        completedLayout = findViewById(R.id.completedLayout)

        // سایر TextViewها
        txtExamInfo = findViewById(R.id.txtExamInfo)
        txtExamResult = findViewById(R.id.txtExamResult)
    }

    private fun setupObservers() {
        // مشاهده وضعیت آزمون
        viewModel.uiState.observe(this) { state ->
            updateUIForState(state)
        }

        // مشاهده سوال جاری
        viewModel.currentQuestion.observe(this) { question ->
            currentQuestion = question
            question?.let { showQuestion(it) }
        }

        // مشاهده زمان باقیمانده
        viewModel.remainingTime.observe(this) { time ->
            txtTimer.text = time?.let { formatTime(it) } ?: "--:--"
        }

        // مشاهده پیشرفت
        viewModel.progress.observe(this) { progress ->
            updateProgress(progress)
        }

        // مشاهده خطاها
        viewModel.errorMessage.observe(this) { error ->
            error?.let {
                showErrorToast(it)
            }
        }
    }

    private fun setupClickListeners() {
        btnPrevious.setOnClickListener {
            viewModel.goToPreviousQuestion()
        }

        btnNext.setOnClickListener {
            viewModel.goToNextQuestion()
        }

        btnSubmit.setOnClickListener {
            viewModel.submitExam()
        }

        btnStartExam.setOnClickListener {
            viewModel.startExam()
        }

        // رویدادهای RadioGroup برای سوالات MCQ
        radioGroup.setOnCheckedChangeListener { group, checkedId ->
            if (checkedId != -1) {
                val selectedIndex = group.indexOfChild(findViewById(checkedId))
                val answer = (selectedIndex + 1).toString()
                viewModel.saveCurrentAnswer(answer)
            }
        }

        // رویدادهای EditText برای سوالات تشریحی
        editTextAnswer.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                saveCurrentTextAnswer()
            }
        }

        editTextFillBlank.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                saveCurrentTextAnswer()
            }
        }
    }

    private fun updateUIForState(state: ExamActivityUiState) {
        // پنهان کردن همه Layoutها
        loadingLayout.isVisible = false
        errorLayout.isVisible = false
        readyLayout.isVisible = false
        examLayout.isVisible = false
        completedLayout.isVisible = false

        when (state) {
            is ExamActivityUiState.Loading -> {
                loadingLayout.isVisible = true
                txtExamTitle.text = "در حال بارگذاری..."
            }

            is ExamActivityUiState.Ready -> {
                readyLayout.isVisible = true
                val exam = state.exam
                val infoText = """
                    آزمون: ${exam.title}
                    تعداد سوالات: ${exam.totalQuestions}
                    زمان: ${formatDuration(exam.examDuration)}
                    درس: ${exam.subject} - پایه ${exam.grade}
                    
                    برای شروع آزمون روی دکمه زیر کلیک کنید.
                """.trimIndent()

                txtExamInfo.text = infoText
                txtExamTitle.text = exam.title
            }

            is ExamActivityUiState.Active -> {
                examLayout.isVisible = true
                txtExamTitle.text = state.exam.title
                updateNavigationButtons()
            }

            is ExamActivityUiState.Completed -> {
                completedLayout.isVisible = true
                val resultText = """
                    آزمون تکمیل شد!
                    
                    نمره: ${state.score}/${state.totalScore}
                    سوالات صحیح: ${state.correctAnswers}
                    سوالات غلط: ${state.wrongAnswers}
                    سوالات بی‌پاسخ: ${state.unanswered}
                    
                    ${if (state.isPassed) "🎉 قبول شدید!" else "📚 نیاز به مطالعه بیشتر دارید."}
                """.trimIndent()

                txtExamResult.text = resultText
                txtExamTitle.text = state.exam.title
            }

            is ExamActivityUiState.Error -> {
                errorLayout.isVisible = true
                findViewById<TextView>(R.id.txtError).text = state.message
            }
        }
    }

    private fun showQuestion(question: Question) {
        // به‌روزرسانی شماره سوال
        val totalQuestions = viewModel.totalQuestions.value ?: 0
        val currentIndex = viewModel.currentQuestionIndex.value ?: 0
        txtQuestionNumber.text = "سوال ${currentIndex + 1} از $totalQuestions"

        // نمایش متن سوال
        txtQuestionText.text = question.questionText

        // پنهان کردن همه Layoutهای پاسخ
        layoutMcq.isVisible = false
        layoutShortAnswer.isVisible = false
        layoutFillBlank.isVisible = false

        // نمایش Layout مناسب بر اساس نوع سوال
        when (question.questionType) {
            "MCQ" -> showMCQQuestion(question)
            "SHORT_ANSWER", "DESCRIPTIVE" -> showTextAnswerQuestion(question)
            "FILL_BLANK" -> showFillBlankQuestion(question)
            else -> {
                // نوع سوال نامشخص
                txtQuestionText.text = "نوع سوال پشتیبانی نمی‌شود: ${question.questionType}"
            }
        }

        // به‌روزرسانی دکمه‌ها
        updateNavigationButtons()
    }

    private fun showMCQQuestion(question: Question) {
        layoutMcq.isVisible = true
        radioGroup.removeAllViews()

        // بارگذاری گزینه‌ها
        question.options?.let { options ->
            options.forEachIndexed { i, option ->
                val radioButton = RadioButton(this).apply {
                    text = "${i + 1}) ${option.optionText}"
                    id = View.generateViewId()
                }
                radioGroup.addView(radioButton)
            }

            // انتخاب گزینه ذخیره شده
            val savedAnswer = viewModel.getCurrentAnswer()
            savedAnswer?.let {
                val answerIndex = it.toIntOrNull() ?: 1
                if (answerIndex - 1 in 0 until radioGroup.childCount) {
                    val radioButton = radioGroup.getChildAt(answerIndex - 1) as RadioButton
                    radioButton.isChecked = true
                }
            }
        } ?: run {
            // گزینه‌ها موجود نیستند
            val textView = TextView(this).apply {
                text = "گزینه‌ای برای این سوال تعریف نشده است."
                setTextColor(resources.getColor(android.R.color.darker_gray, theme))
            }
            layoutMcq.addView(textView)
        }
    }

    private fun showTextAnswerQuestion(question: Question) {
        layoutShortAnswer.isVisible = true
        editTextAnswer.setText("")

        // بارگذاری پاسخ ذخیره شده
        val savedAnswer = viewModel.getCurrentAnswer()
        savedAnswer?.let {
            editTextAnswer.setText(it)
        }
    }

    private fun showFillBlankQuestion(question: Question) {
        layoutFillBlank.isVisible = true
        editTextFillBlank.setText("")

        // بارگذاری پاسخ ذخیره شده
        val savedAnswer = viewModel.getCurrentAnswer()
        savedAnswer?.let {
            editTextFillBlank.setText(it)
        }
    }

    private fun saveCurrentTextAnswer() {
        val answer = when {
            layoutShortAnswer.isVisible -> editTextAnswer.text.toString().trim()
            layoutFillBlank.isVisible -> editTextFillBlank.text.toString().trim()
            else -> null
        }

        answer?.let {
            if (it.isNotEmpty()) {
                viewModel.saveCurrentAnswer(it)
            }
        }
    }

    private fun updateNavigationButtons() {
        val currentIndex = viewModel.currentQuestionIndex.value ?: 0
        val totalQuestions = viewModel.totalQuestions.value ?: 0

        btnPrevious.isEnabled = currentIndex > 0
        btnNext.isEnabled = currentIndex < totalQuestions - 1
        btnSubmit.isVisible = currentIndex == totalQuestions - 1
    }

    private fun updateProgress(progress: Int) {
        txtProgress.text = "$progress%"
        progressBar.progress = progress
    }

    private fun showErrorToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun formatTime(seconds: Long): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%02d:%02d", minutes, remainingSeconds)
    }

    private fun formatDuration(minutes: Int?): String {
        return minutes?.let { "$it دقیقه" } ?: "زمان نامشخص"
    }

    override fun onBackPressed() {
        when (viewModel.uiState.value) {
            is ExamActivityUiState.Active -> {
                // در حین آزمون، نمایش Dialog تایید
                android.app.AlertDialog.Builder(this)
                    .setTitle("خروج از آزمون")
                    .setMessage("اگر خارج شوید، پیشرفت شما ذخیره خواهد شد اما آزمون متوقف می‌شود.")
                    .setPositiveButton("خروج") { _, _ ->
                        super.onBackPressed()
                    }
                    .setNegativeButton("ماندن", null)
                    .show()
            }
            else -> {
                super.onBackPressed()
            }
        }
    }
}

// ViewModel for ExamActivity
@dagger.hilt.android.lifecycle.HiltViewModel
class ExamActivityViewModel @javax.inject.Inject constructor(
    private val examRepository: com.examapp.data.repository.ExamRepository
) : androidx.lifecycle.ViewModel() {

    private val _uiState = androidx.lifecycle.MutableLiveData<ExamActivityUiState>(ExamActivityUiState.Loading)
    val uiState: androidx.lifecycle.LiveData<ExamActivityUiState> = _uiState

    private val _currentQuestion = androidx.lifecycle.MutableLiveData<Question?>()
    val currentQuestion: androidx.lifecycle.LiveData<Question?> = _currentQuestion

    private val _currentQuestionIndex = androidx.lifecycle.MutableLiveData<Int>(0)
    val currentQuestionIndex: androidx.lifecycle.LiveData<Int> = _currentQuestionIndex

    private val _totalQuestions = androidx.lifecycle.MutableLiveData<Int>(0)
    val totalQuestions: androidx.lifecycle.LiveData<Int> = _totalQuestions

    private val _remainingTime = androidx.lifecycle.MutableLiveData<Long?>()
    val remainingTime: androidx.lifecycle.LiveData<Long?> = _remainingTime

    private val _progress = androidx.lifecycle.MutableLiveData<Int>(0)
    val progress: androidx.lifecycle.LiveData<Int> = _progress

    private val _errorMessage = androidx.lifecycle.MutableLiveData<String?>()
    val errorMessage: androidx.lifecycle.LiveData<String?> = _errorMessage

    private var exam: com.examapp.data.models.Exam? = null
    private var questions: List<Question> = emptyList()
    private var userAnswers = mutableMapOf<String, String>()

    fun loadExam(examId: String) {
        viewModelScope.launch {
            _uiState.value = ExamActivityUiState.Loading

            try {
                // Load exam details
                val examResult = examRepository.getExamById(examId)
                if (examResult.isFailure) {
                    _uiState.value = ExamActivityUiState.Error(
                        examResult.exceptionOrNull()?.message ?: "خطا در بارگذاری آزمون"
                    )
                    return@launch
                }

                exam = examResult.getOrNull()
                if (exam == null) {
                    _uiState.value = ExamActivityUiState.Error("آزمون یافت نشد")
                    return@launch
                }

                // Load questions
                val questionsResult = examRepository.getExamQuestions(examId)
                if (questionsResult.isFailure) {
                    _uiState.value = ExamActivityUiState.Error(
                        questionsResult.exceptionOrNull()?.message ?: "خطا در بارگذاری سوالات"
                    )
                    return@launch
                }

                questions = questionsResult.getOrNull() ?: emptyList()
                _totalQuestions.value = questions.size

                _uiState.value = ExamActivityUiState.Ready(exam!!)

            } catch (e: Exception) {
                _uiState.value = ExamActivityUiState.Error("خطا در اتصال: ${e.message}")
            }
        }
    }

    fun startExam() {
        if (questions.isEmpty()) {
            _errorMessage.value = "سوالی برای شروع آزمون وجود ندارد"
            return
        }

        _uiState.value = ExamActivityUiState.Active(exam!!)
        _currentQuestion.value = questions[0]
        _currentQuestionIndex.value = 0
        _progress.value = 0

        // Start timer if exam has duration
        exam?.examDuration?.let { duration ->
            _remainingTime.value = duration * 60L // Convert minutes to seconds
        }
    }

    fun goToPreviousQuestion() {
        val currentIndex = _currentQuestionIndex.value ?: 0
        if (currentIndex > 0) {
            _currentQuestionIndex.value = currentIndex - 1
            _currentQuestion.value = questions[currentIndex - 1]
            updateProgress()
        }
    }

    fun goToNextQuestion() {
        val currentIndex = _currentQuestionIndex.value ?: 0
        if (currentIndex < questions.size - 1) {
            _currentQuestionIndex.value = currentIndex + 1
            _currentQuestion.value = questions[currentIndex + 1]
            updateProgress()
        }
    }

    fun saveCurrentAnswer(answer: String) {
        val currentIndex = _currentQuestionIndex.value ?: 0
        if (currentIndex < questions.size) {
            val questionId = questions[currentIndex].id
            userAnswers[questionId] = answer
            updateProgress()
        }
    }

    fun getCurrentAnswer(): String? {
        val currentIndex = _currentQuestionIndex.value ?: 0
        if (currentIndex < questions.size) {
            val questionId = questions[currentIndex].id
            return userAnswers[questionId]
        }
        return null
    }

    fun submitExam() {
        viewModelScope.launch {
            try {
                val examId = exam?.id ?: run {
                    _errorMessage.value = "آزمون یافت نشد"
                    return@launch
                }

                // Prepare answers
                val answers = questions.mapNotNull { question ->
                    userAnswers[question.id]?.let { userAnswer ->
                        mapOf(
                            "questionId" to question.id,
                            "answer" to userAnswer,
                            "questionType" to question.questionType
                        )
                    }
                }

                // Submit exam
                val submitResult = examRepository.submitExam(examId, answers)
                if (submitResult.isSuccess) {
                    val result = submitResult.getOrNull()
                    _uiState.value = ExamActivityUiState.Completed(
                        exam = exam!!,
                        score = result?.score ?: 0,
                        totalScore = result?.totalScore ?: 100,
                        correctAnswers = result?.correctAnswers ?: 0,
                        wrongAnswers = result?.wrongAnswers ?: 0,
                        unanswered = questions.size - (result?.correctAnswers ?: 0) - (result?.wrongAnswers ?: 0),
                        isPassed = result?.isPassed ?: false
                    )
                } else {
                    _errorMessage.value = submitResult.exceptionOrNull()?.message ?: "خطا در ارسال آزمون"
                }

            } catch (e: Exception) {
                _errorMessage.value = "خطا در ارسال آزمون: ${e.message}"
            }
        }
    }

    private fun updateProgress() {
        val answeredCount = userAnswers.size
        val total = questions.size
        val progress = if (total > 0) {
            (answeredCount.toFloat() / total * 100).toInt()
        } else {
            0
        }
        _progress.value = progress
    }
}

// UI State classes
sealed class ExamActivityUiState {
    data object Loading : ExamActivityUiState()
    data class Ready(val exam: com.examapp.data.models.Exam) : ExamActivityUiState()
    data class Active(val exam: com.examapp.data.models.Exam) : ExamActivityUiState()
    data class Completed(
        val exam: com.examapp.data.models.Exam,
        val score: Int,
        val totalScore: Int,
        val correctAnswers: Int,
        val wrongAnswers: Int,
        val unanswered: Int,
        val isPassed: Boolean
    ) : ExamActivityUiState()
    data class Error(val message: String) : ExamActivityUiState()
}