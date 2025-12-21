// app/src/main/java/com/examapp/ui/exam/ExamActivity.kt
package com.examapp.ui.exam

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.examapp.data.models.Question
import com.examapp.databinding.ActivityExamBinding
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class ExamActivity : AppCompatActivity() {
    private lateinit var binding: ActivityExamBinding
    private lateinit var questions: List<Question>
    private val userAnswers = mutableMapOf<Int, String>()
    private var currentQuestionIndex = 0
    private var examTimer: CountDownTimer? = null
    private var remainingTimeMillis = 45 * 60 * 1000L // 45 دقیقه

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExamBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
        loadQuestions()
        setupTimer()
        showQuestion(currentQuestionIndex)
    }

    private fun setupViews() {
        binding.btnPrev.setOnClickListener {
            if (currentQuestionIndex > 0) {
                saveCurrentAnswer()
                currentQuestionIndex--
                showQuestion(currentQuestionIndex)
            }
        }

        binding.btnNext.setOnClickListener {
            if (currentQuestionIndex < questions.size - 1) {
                saveCurrentAnswer()
                currentQuestionIndex++
                showQuestion(currentQuestionIndex)
            }
        }

        binding.btnSubmit.setOnClickListener {
            showSubmitConfirmation()
        }

        binding.btnFinish.setOnClickListener {
            finishExam()
        }
    }

    private fun loadQuestions() {
        binding.progressBar.isVisible = true

        lifecycleScope.launch {
            try {
                // TODO: دریافت سوالات از ViewModel/Repository
                questions = listOf() // Placeholder

                binding.progressBar.isVisible = false
                if (questions.isEmpty()) {
                    Toast.makeText(this@ExamActivity, "سوالی یافت نشد!", Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    updateProgress()
                    showQuestion(currentQuestionIndex)
                }
            } catch (e: Exception) {
                binding.progressBar.isVisible = false
                Toast.makeText(this@ExamActivity, "خطا در بارگذاری سوالات", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun setupTimer() {
        examTimer = object : CountDownTimer(remainingTimeMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                remainingTimeMillis = millisUntilFinished
                updateTimerDisplay()
            }

            override fun onFinish() {
                finishExam()
            }
        }.start()
    }

    @SuppressLint("DefaultLocale")
    private fun updateTimerDisplay() {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(remainingTimeMillis)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(remainingTimeMillis) % 60
        binding.txtTimer.text = String.format("%02d:%02d", minutes, seconds)
    }

    private fun showQuestion(index: Int) {
        if (index >= questions.size) return

        val question = questions[index]
        binding.txtQuestionNumber.text = "سوال ${index + 1} از ${questions.size}"
        binding.txtQuestionText.text = question.text

        // پاک کردن گزینه‌های قبلی
        binding.radioGroup.removeAllViews()
        binding.layoutOptions.isVisible = false
        binding.txtAnswerInput.isVisible = false

        when (question.type) {
            "multiple_choice", "MULTIPLE_CHOICE" -> showMCQOptions(question)
            "short_answer", "descriptive", "SHORT_ANSWER", "DESCRIPTIVE" -> showTextInput()
            "fill_blank", "FILL_BLANK" -> showFillBlank(question)
            "true_false", "TRUE_FALSE" -> showTrueFalseOptions()
        }

        loadSavedAnswer(index)
        updateNavigationButtons()
        updateProgress()
    }

    private fun showMCQOptions(question: Question) {
        binding.layoutOptions.isVisible = true
        question.options?.forEachIndexed { index, option ->
            RadioButton(this).apply {
                text = "${index + 1}) ${option.text}"
                id = index
                textSize = 16f
                setPadding(20, 20, 20, 20)
                binding.radioGroup.addView(this)
            }
        }

        binding.radioGroup.setOnCheckedChangeListener { _, checkedId ->
            userAnswers[currentQuestionIndex] = (checkedId + 1).toString()
        }
    }

    private fun showTrueFalseOptions() {
        binding.layoutOptions.isVisible = true
        listOf("صحیح", "غلط").forEachIndexed { index, text ->
            RadioButton(this).apply {
                this.text = text
                id = index
                textSize = 16f
                setPadding(20, 20, 20, 20)
                binding.radioGroup.addView(this)
            }
        }
    }

    private fun showTextInput() {
        binding.txtAnswerInput.isVisible = true
        binding.txtAnswerInput.setText("")
    }

    private fun showFillBlank(question: Question) {
        binding.txtAnswerInput.isVisible = true
        binding.txtAnswerInput.hint = "پاسخ خود را وارد کنید"
        binding.txtAnswerInput.setText("")
    }

    private fun loadSavedAnswer(index: Int) {
        val savedAnswer = userAnswers[index]
        val question = questions[index]

        when (question.type) {
            "multiple_choice", "MULTIPLE_CHOICE", "true_false", "TRUE_FALSE" -> {
                savedAnswer?.let {
                    val answerIndex = it.toIntOrNull() ?: 0
                    if (answerIndex in 0 until binding.radioGroup.childCount) {
                        binding.radioGroup.check(answerIndex)
                    }
                }
            }
            "short_answer", "descriptive", "SHORT_ANSWER", "DESCRIPTIVE",
            "fill_blank", "FILL_BLANK" -> {
                binding.txtAnswerInput.setText(savedAnswer ?: "")
            }
        }
    }

    private fun saveCurrentAnswer() {
        val question = questions[currentQuestionIndex]
        when (question.type) {
            "multiple_choice", "MULTIPLE_CHOICE", "true_false", "TRUE_FALSE" -> {
                val selectedId = binding.radioGroup.checkedRadioButtonId
                if (selectedId != -1) {
                    userAnswers[currentQuestionIndex] = selectedId.toString()
                }
            }
            "short_answer", "descriptive", "SHORT_ANSWER", "DESCRIPTIVE",
            "fill_blank", "FILL_BLANK" -> {
                val answer = binding.txtAnswerInput.text.toString().trim()
                if (answer.isNotEmpty()) {
                    userAnswers[currentQuestionIndex] = answer
                }
            }
        }
    }

    private fun updateNavigationButtons() {
        binding.btnPrev.isEnabled = currentQuestionIndex > 0
        binding.btnNext.isEnabled = currentQuestionIndex < questions.size - 1
        binding.btnSubmit.isVisible = currentQuestionIndex == questions.size - 1
    }

    private fun updateProgress() {
        val progress = ((currentQuestionIndex + 1).toFloat() / questions.size.toFloat() * 100).toInt()
        binding.progressBar.progress = progress
        binding.txtProgress.text = "$progress%"
    }

    private fun showSubmitConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("پایان آزمون")
            .setMessage("آیا مطمئن هستید که می‌خواهید آزمون را پایان دهید؟\nپاسخ‌های داده شده: ${userAnswers.size}/${questions.size}")
            .setPositiveButton("بله، پایان آزمون") { _, _ -> finishExam() }
            .setNegativeButton("خیر، ادامه می‌دهم", null)
            .show()
    }

    private fun finishExam() {
        examTimer?.cancel()
        val score = calculateScore()
        showResultDialog(score)
    }

    private fun calculateScore(): Int {
        var correctAnswers = 0
        userAnswers.forEach { (index, userAnswer) ->
            val question = questions[index]
            val correctAnswer = when (question.type) {
                "multiple_choice", "MULTIPLE_CHOICE" -> question.correctOption?.toString()
                "true_false", "TRUE_FALSE" -> if (question.isCorrect == true) "1" else "0"
                else -> question.correctAnswer
            }
            if (userAnswer == correctAnswer) correctAnswers++
        }
        return (correctAnswers.toFloat() / questions.size * 100).toInt()
    }

    private fun showResultDialog(score: Int) {
        val message = """
            آزمون با موفقیت پایان یافت!
            
            📊 نتیجه:
            نمره: $score%
            کل سوالات: ${questions.size}
            پاسخ داده شده: ${userAnswers.size}
            زمان: ${TimeUnit.MILLISECONDS.toMinutes(45 * 60 * 1000 - remainingTimeMillis)} دقیقه
            
            ${if (score >= 70) "🎉 عالی!" else "📝 نیاز به تمرین بیشتر."}
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("نتیجه آزمون")
            .setMessage(message)
            .setPositiveButton("مشاهده نتایج") { _, _ -> finish() }
            .setNegativeButton("بستن") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    override fun onBackPressed() {
        AlertDialog.Builder(this)
            .setTitle("خروج از آزمون")
            .setMessage("پیشرفت ذخیره نخواهد شد. خارج شوید؟")
            .setPositiveButton("خروج") { _, _ ->
                examTimer?.cancel()
                finish()
            }
            .setNegativeButton("ماندن", null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        examTimer?.cancel()
    }
}