// app/src/main/java/com/examapp/ui/exam/QuestionAdapter.kt
package com.examapp.ui.exam

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.examapp.R
import com.examapp.data.models.Question

class QuestionAdapter(
    private val questions: List<Question>,
    private val userAnswers: Map<String, String>, // questionId to selectedOption
    private val onQuestionClick: (Int) -> Unit
) : RecyclerView.Adapter<QuestionAdapter.QuestionViewHolder>() {

    // ViewHolder برای هر آیتم
    class QuestionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvQuestionNumber: TextView = itemView.findViewById(R.id.tvQuestionNumber)
        val tvQuestionStatus: TextView = itemView.findViewById(R.id.tvQuestionStatus)
        val rootView: View = itemView.findViewById(R.id.rootView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuestionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_question, parent, false)
        return QuestionViewHolder(view)
    }

    override fun onBindViewHolder(holder: QuestionViewHolder, position: Int) {
        val question = questions[position]
        val questionNumber = position + 1

        // شماره سوال
        holder.tvQuestionNumber.text = questionNumber.toString()

        // وضعیت سوال
        val (statusText, statusColor) = getQuestionStatus(question)

        holder.tvQuestionStatus.text = statusText
        holder.tvQuestionStatus.setTextColor(
            ContextCompat.getColor(holder.itemView.context, statusColor)
        )

        // رنگ‌بندی بر اساس وضعیت
        val (backgroundColor, textColor) = getQuestionColors(question)

        holder.rootView.setBackgroundResource(backgroundColor)
        holder.tvQuestionNumber.setTextColor(
            ContextCompat.getColor(holder.itemView.context, textColor)
        )

        // کلیک روی سوال
        holder.itemView.setOnClickListener {
            onQuestionClick(position)
        }

        // Accessibility
        holder.itemView.contentDescription = "سوال $questionNumber - وضعیت: $statusText"
    }

    override fun getItemCount(): Int = questions.size

    // آپدیت پاسخ‌های کاربر
    fun updateUserAnswers(newAnswers: Map<String, String>) {
        notifyDataSetChanged()
    }

    // گرفتن وضعیت سوال برای نمایش
    fun getQuestionStatus(position: Int): Pair<String, Int> {
        val question = questions[position]
        return getQuestionStatus(question)
    }

    private fun getQuestionStatus(question: Question): Pair<String, Int> {
        return when {
            userAnswers.containsKey(question.id) -> {
                val userAnswer = userAnswers[question.id]
                if (userAnswer != null) {
                    // بررسی اگر پاسخ درست است (در صورت وجود اطلاعات پاسخ صحیح)
                    if (question.correctAnswer != null && userAnswer == question.correctAnswer) {
                        Pair("✓", R.color.correct_answer)  // پاسخ صحیح
                    } else {
                        Pair("✗", R.color.wrong_answer)    // پاسخ غلط
                    }
                } else {
                    Pair("?", R.color.answered)            // پاسخ داده شده اما نامشخص
                }
            }
            question.isFlagged -> Pair("📍", R.color.flagged)  // سوال علامت‌گذاری شده
            else -> Pair("", R.color.unanswered)           // سوال بدون وضعیت خاص
        }
    }

    private fun getQuestionColors(question: Question): Pair<Int, Int> {
        return when {
            userAnswers.containsKey(question.id) -> {
                val userAnswer = userAnswers[question.id]
                if (userAnswer != null && question.correctAnswer != null && userAnswer == question.correctAnswer) {
                    // پاسخ صحیح - سبز
                    Pair(R.drawable.item_question_correct, R.color.white)
                } else {
                    // پاسخ غلط - قرمز
                    Pair(R.drawable.item_question_wrong, R.color.white)
                }
            }
            question.isFlagged -> {
                // علامت‌گذاری شده - زرد
                Pair(R.drawable.item_question_flagged, R.color.black)
            }
            else -> {
                // عادی
                Pair(R.drawable.item_question_normal, R.color.black)
            }
        }
    }

    // Helper functions
    fun getAnsweredCount(): Int = userAnswers.size

    fun getCorrectCount(): Int = questions.count { question ->
        userAnswers[question.id] != null &&
                question.correctAnswer != null &&
                userAnswers[question.id] == question.correctAnswer
    }

    fun getWrongCount(): Int = questions.count { question ->
        userAnswers[question.id] != null &&
                question.correctAnswer != null &&
                userAnswers[question.id] != question.correctAnswer
    }

    fun getUnansweredCount(): Int = questions.size - userAnswers.size

    fun getFlaggedCount(): Int = questions.count { it.isFlagged }

    // Extension function to get question by position
    fun getQuestion(position: Int): Question? {
        return if (position in 0 until questions.size) {
            questions[position]
        } else {
            null
        }
    }
}

// Extension properties for Question
val Question.isAnswered: Boolean
    get() = false // This should be determined by userAnswers in adapter

val Question.isCorrectlyAnswered: Boolean
    get() = false // This should be determined by comparing userAnswer with correctAnswer

val Question.displayStatus: String
    get() = when {
        this.isFlagged -> "علامت‌گذاری شده"
        else -> "بدون وضعیت"
    }