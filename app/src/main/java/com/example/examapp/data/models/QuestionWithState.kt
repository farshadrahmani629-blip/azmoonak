// app/src/main/java/com/examapp/data/models/QuestionWithState.kt
package com.examapp.data.models

import androidx.room.Ignore

/**
 * wrapper برای ترکیب سوال با وضعیت UI
 * در دیتابیس ذخیره نمی‌شود، فقط برای نمایش در UI استفاده می‌شود
 */
data class QuestionWithState(
    val question: Question,
    var userAnswer: String? = null,
    var questionNumber: Int = 0,
    var hasNext: Boolean = false,
    var hasPrev: Boolean = false,
    var isCurrent: Boolean = false,
    var isFlagged: Boolean = false,
    var timeSpent: Int = 0,
    var isAnswered: Boolean = false,
    var isCorrect: Boolean? = null
) {
    val statusText: String
        @Ignore
        get() = when {
            isAnswered && isCorrect == true -> "✓"
            isAnswered && isCorrect == false -> "✗"
            isAnswered -> "●"
            isFlagged -> "⚑"
            else → "○"
        }

    val statusColor: Int
        @Ignore
        get() = when {
            isCurrent -> 0x2196F3.toInt() // آبی
            isAnswered && isCorrect == true -> 0xFF4CAF50.toInt() // سبز
            isAnswered && isCorrect == false -> 0xFFF44336.toInt() // قرمز
            isAnswered -> 0xFFFF9800.toInt() // نارنجی
            isFlagged -> 0xFF9C27B0.toInt() // بنفش
            else -> 0xFF9E9E9E.toInt() // خاکستری
        }

    @Ignore
    fun updateFromQuestion() {
        this.userAnswer = question.userAnswer
        this.isAnswered = question.isAnswered
        this.isCorrect = question.isCorrect
        this.isFlagged = question.isFlagged
        this.timeSpent = question.timeSpent
    }

    @Ignore
    fun applyToQuestion() {
        question.userAnswer = this.userAnswer
        question.isAnswered = this.isAnswered
        question.isCorrect = this.isCorrect
        question.isFlagged = this.isFlagged
        question.timeSpent = this.timeSpent
    }
}