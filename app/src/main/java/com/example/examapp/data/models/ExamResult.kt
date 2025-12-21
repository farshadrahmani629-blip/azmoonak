// app/src/main/java/com/examapp/data/models/ExamResult.kt
package com.examapp.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.Ignore
import com.google.gson.annotations.SerializedName

@Entity(tableName = "exam_results")
data class ExamResult(
    @PrimaryKey
    @ColumnInfo(name = "id")
    @SerializedName("id")
    val id: Int,

    @ColumnInfo(name = "exam_id", index = true)
    @SerializedName("exam_id")
    val examId: Int,

    @ColumnInfo(name = "user_id", index = true)
    @SerializedName("user_id")
    val userId: Int,

    @ColumnInfo(name = "score")
    @SerializedName("score")
    val score: Float,

    @ColumnInfo(name = "total_marks")
    @SerializedName("total_marks")
    val totalMarks: Float,

    @ColumnInfo(name = "percentage")
    @SerializedName("percentage")
    val percentage: Float,

    @ColumnInfo(name = "correct_answers")
    @SerializedName("correct_answers")
    val correctAnswers: Int,

    @ColumnInfo(name = "wrong_answers")
    @SerializedName("wrong_answers")
    val wrongAnswers: Int,

    @ColumnInfo(name = "skipped_questions")
    @SerializedName("skipped_questions")
    val skippedQuestions: Int,

    @ColumnInfo(name = "time_spent")
    @SerializedName("time_spent")
    val timeSpent: Int,

    @ColumnInfo(name = "started_at")
    @SerializedName("started_at")
    val startedAt: Long,

    @ColumnInfo(name = "completed_at")
    @SerializedName("completed_at")
    val completedAt: Long,

    @ColumnInfo(name = "attempt_number")
    @SerializedName("attempt_number")
    val attemptNumber: Int = 1,

    @ColumnInfo(name = "is_passed")
    @SerializedName("is_passed")
    val isPassed: Boolean,

    @ColumnInfo(name = "created_at")
    @SerializedName("created_at")
    val createdAt: Long = System.currentTimeMillis(),

    // فیلدهای اضافی برای UI
    @ColumnInfo(name = "is_bookmarked")
    var isBookmarked: Boolean = false,

    @ColumnInfo(name = "is_shared")
    var isShared: Boolean = false,

    @ColumnInfo(name = "last_viewed")
    var lastViewed: Long = System.currentTimeMillis()
) {
    // فیلدهای Ignore (از JSON می‌آیند)
    @Ignore
    @SerializedName("exam")
    val exam: Exam? = null

    @Ignore
    @SerializedName("user")
    val user: User? = null

    // Extension properties
    val scoreText: String
        @Ignore
        get() = String.format("%.1f / %.1f", score, totalMarks)

    val percentageText: String
        @Ignore
        get() = String.format("%.1f%%", percentage)

    val timeSpentText: String
        @Ignore
        get() {
            val hours = timeSpent / 3600
            val minutes = (timeSpent % 3600) / 60
            return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
        }

    val statusText: String
        @Ignore
        get() = if (isPassed) "قبول" else "نیاز به تلاش بیشتر"

    val statusColor: Int
        @Ignore
        get() = if (isPassed) 0xFF4CAF50.toInt() else 0xFFF44336.toInt()

    val accuracy: Float
        @Ignore
        get() {
            val totalAnswered = correctAnswers + wrongAnswers
            return if (totalAnswered > 0) correctAnswers.toFloat() / totalAnswered else 0f
        }

    val accuracyText: String
        @Ignore
        get() = String.format("%.1f%%", accuracy * 100)

    @Ignore
    fun getPerformanceLevel(): String {
        return when {
            percentage >= 90 -> "عالی"
            percentage >= 75 -> "خوب"
            percentage >= 50 -> "متوسط"
            else -> "نیاز به بهبود"
        }
    }
}