// app/src/main/java/com/examapp/data/models/ExamResultDetail.kt
package com.examapp.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.Ignore
import androidx.room.TypeConverters
import com.examapp.data.database.Converters
import com.google.gson.annotations.SerializedName

/**
 * نتیجه کامل آزمون با جزئیات سوالات
 * این مدل برای نمایش نتایج در UI استفاده می‌شود
 */
@Entity(tableName = "exam_result_details")
@TypeConverters(Converters::class)
data class ExamResultDetail(
    @PrimaryKey
    @ColumnInfo(name = "result_id")
    @SerializedName("result_id")
    val resultId: String,

    @ColumnInfo(name = "exam_id", index = true)
    @SerializedName("exam_id")
    val examId: String,

    @ColumnInfo(name = "exam_title")
    @SerializedName("exam_title")
    val examTitle: String,

    @ColumnInfo(name = "user_id", index = true)
    @SerializedName("user_id")
    val userId: String,

    @ColumnInfo(name = "course_name")
    @SerializedName("course_name")
    val courseName: String,

    @ColumnInfo(name = "exam_date")
    @SerializedName("exam_date")
    val examDate: String,

    @ColumnInfo(name = "final_score")
    @SerializedName("final_score")
    val finalScore: Float,

    @ColumnInfo(name = "total_score")
    @SerializedName("total_score")
    val totalScore: Float,

    @ColumnInfo(name = "percentage")
    @SerializedName("percentage")
    val percentage: Float,

    @ColumnInfo(name = "correct_answers")
    @SerializedName("correct_answers")
    val correctAnswers: Int,

    @ColumnInfo(name = "wrong_answers")
    @SerializedName("wrong_answers")
    val wrongAnswers: Int,

    @ColumnInfo(name = "unanswered")
    @SerializedName("unanswered")
    val unanswered: Int,

    @ColumnInfo(name = "total_questions")
    @SerializedName("total_questions")
    val totalQuestions: Int,

    @ColumnInfo(name = "time_spent") // ثانیه
    @SerializedName("time_spent")
    val timeSpent: Int,

    @ColumnInfo(name = "time_limit") // دقیقه
    @SerializedName("time_limit")
    val timeLimit: Int,

    @ColumnInfo(name = "is_passed")
    @SerializedName("is_passed")
    val isPassed: Boolean,

    @ColumnInfo(name = "class_rank")
    @SerializedName("class_rank")
    val classRank: Int? = null,

    @ColumnInfo(name = "total_students")
    @SerializedName("total_students")
    val totalStudents: Int? = null,

    @ColumnInfo(name = "class_average")
    @SerializedName("class_average")
    val classAverage: Float? = null,

    @ColumnInfo(name = "recommendations")
    @SerializedName("recommendations")
    val recommendations: String? = null,

    // فیلدهای اضافی برای UI
    @ColumnInfo(name = "is_bookmarked")
    var isBookmarked: Boolean = false,

    @ColumnInfo(name = "last_viewed")
    var lastViewed: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "is_shared")
    var isShared: Boolean = false,

    @ColumnInfo(name = "pdf_path")
    var pdfPath: String? = null
) {
    // فیلدهای Ignore (از JSON می‌آیند)
    @Ignore
    @SerializedName("question_results")
    val questionResults: List<QuestionResult> = emptyList()

    // Extension properties برای UI
    val percentageText: String
        @Ignore
        get() = String.format("%.1f%%", percentage)

    val scoreText: String
        @Ignore
        get() = String.format("%.1f / %.1f", finalScore, totalScore)

    val timeSpentText: String
        @Ignore
        get() {
            val hours = timeSpent / 3600
            val minutes = (timeSpent % 3600) / 60
            val seconds = timeSpent % 60

            return if (hours > 0) {
                String.format("%d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        }

    val timeLimitText: String
        @Ignore
        get() = "${timeLimit} دقیقه"

    val rankText: String
        @Ignore
        get() = classRank?.let { rank ->
            totalStudents?.let { total ->
                "رتبه $rank از $total"
            } ?: "رتبه $rank"
        } ?: "---"

    val classAverageText: String
        @Ignore
        get() = classAverage?.let {
            String.format("%.1f%%", it)
        } ?: "---"

    val statusText: String
        @Ignore
        get() = if (isPassed) "قبول" else "نیاز به تلاش بیشتر"

    val statusColor: Int
        @Ignore
        get() = if (isPassed) 0xFF4CAF50.toInt() else 0xFFF44336.toInt()

    // متدهای کمکی
    @Ignore
    fun getPerformanceLevel(): String {
        return when {
            percentage >= 90 -> "عالی"
            percentage >= 75 -> "خوب"
            percentage >= 50 -> "متوسط"
            else -> "نیاز به بهبود"
        }
    }

    @Ignore
    fun getTimeEfficiency(): Float {
        return if (timeLimit > 0) {
            val totalSeconds = timeLimit * 60
            (totalSeconds - timeSpent).toFloat() / totalSeconds
        } else {
            1f
        }
    }

    @Ignore
    fun getAccuracy(): Float {
        return if (totalQuestions > 0) {
            correctAnswers.toFloat() / totalQuestions
        } else {
            0f
        }
    }

    @Ignore
    fun getQuestionResult(questionId: String): QuestionResult? {
        return questionResults.find { it.questionId == questionId }
    }

    @Ignore
    fun getQuestionsByDifficulty(): Map<String, Int> {
        return questionResults.groupingBy { it.difficulty }
            .eachCount()
    }

    @Ignore
    fun getQuestionsByTopic(): Map<String?, Int> {
        return questionResults.groupingBy { it.topic }
            .eachCount()
    }
}

/**
 * نتیجه هر سوال در آزمون
 */
@Entity(tableName = "question_results")
data class QuestionResult(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "local_id")
    val localId: Long = 0,

    @ColumnInfo(name = "result_id", index = true)
    @SerializedName("result_id")
    val resultId: String,

    @ColumnInfo(name = "question_id", index = true)
    @SerializedName("question_id")
    val questionId: String,

    @ColumnInfo(name = "question_number")
    @SerializedName("question_number")
    val questionNumber: Int,

    @ColumnInfo(name = "question_text")
    @SerializedName("question_text")
    val questionText: String,

    @ColumnInfo(name = "question_type")
    @SerializedName("question_type")
    val questionType: String,

    @ColumnInfo(name = "correct_answer")
    @SerializedName("correct_answer")
    val correctAnswer: String? = null,

    @ColumnInfo(name = "user_answer")
    @SerializedName("user_answer")
    val userAnswer: String? = null,

    @ColumnInfo(name = "is_correct")
    @SerializedName("is_correct")
    val isCorrect: Boolean,

    @ColumnInfo(name = "marks")
    @SerializedName("marks")
    val marks: Int,

    @ColumnInfo(name = "explanation")
    @SerializedName("explanation")
    val explanation: String? = null,

    @ColumnInfo(name = "topic")
    @SerializedName("topic")
    val topic: String? = null,

    @ColumnInfo(name = "difficulty")
    @SerializedName("difficulty")
    val difficulty: String,

    // فیلدهای اضافی
    @ColumnInfo(name = "time_spent")
    var timeSpent: Int = 0, // ثانیه

    @ColumnInfo(name = "is_reviewed")
    var isReviewed: Boolean = false,

    @ColumnInfo(name = "review_note")
    var reviewNote: String? = null
) {
    // Extension properties برای UI
    val difficultyText: String
        @Ignore
        get() = when (difficulty.lowercase()) {
            "easy" -> "آسان"
            "medium" -> "متوسط"
            "hard" -> "سخت"
            else -> difficulty
        }

    val difficultyColor: Int
        @Ignore
        get() = when (difficulty.lowercase()) {
            "easy" -> 0xFF4CAF50.toInt() // Green
            "medium" -> 0xFFFF9800.toInt() // Orange
            "hard" -> 0xFFF44336.toInt() // Red
            else -> 0xFF9E9E9E.toInt() // Gray
        }

    val statusText: String
        @Ignore
        get() = when {
            isCorrect -> "صحیح"
            userAnswer.isNullOrEmpty() -> "بی‌پاسخ"
            else -> "غلط"
        }

    val statusColor: Int
        @Ignore
        get() = when {
            isCorrect -> 0xFF4CAF50.toInt() // Green
            userAnswer.isNullOrEmpty() -> 0xFF9E9E9E.toInt() // Gray
            else -> 0xFFF44336.toInt() // Red
        }

    val questionTypeText: String
        @Ignore
        get() = when (questionType.lowercase()) {
            "multiple_choice" -> "چندگزینه‌ای"
            "short_answer" -> "کوتاه‌پاسخ"
            "descriptive" -> "تشریحی"
            else -> questionType
        }

    // متدهای کمکی
    @Ignore
    fun hasExplanation(): Boolean = !explanation.isNullOrEmpty()

    @Ignore
    fun isMultipleChoice(): Boolean = questionType.equals("multiple_choice", ignoreCase = true)

    @Ignore
    fun isShortAnswer(): Boolean = questionType.equals("short_answer", ignoreCase = true)

    @Ignore
    fun isDescriptive(): Boolean = questionType.equals("descriptive", ignoreCase = true)
}