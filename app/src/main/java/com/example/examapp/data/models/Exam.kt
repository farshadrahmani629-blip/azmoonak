// app/src/main/java/com/examapp/data/models/Exam.kt
package com.examapp.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.Ignore
import androidx.room.TypeConverters
import com.examapp.data.database.Converters
import com.google.gson.annotations.SerializedName
import java.util.*

@Entity(tableName = "exams")
@TypeConverters(Converters::class)
data class Exam(
    @PrimaryKey
    @ColumnInfo(name = "id")
    @SerializedName("id")
    val id: Int,

    @ColumnInfo(name = "exam_code", index = true)
    @SerializedName("exam_code")
    val examCode: String,

    @ColumnInfo(name = "title")
    @SerializedName("title")
    val title: String,

    @ColumnInfo(name = "description")
    @SerializedName("description")
    val description: String? = null,

    @ColumnInfo(name = "user_id", index = true)
    @SerializedName("user_id")
    val userId: Int,

    @ColumnInfo(name = "grade")
    @SerializedName("grade")
    val grade: Int,

    @ColumnInfo(name = "subject")
    @SerializedName("subject")
    val subject: String,

    @ColumnInfo(name = "total_questions")
    @SerializedName("total_questions")
    val totalQuestions: Int,

    @ColumnInfo(name = "total_marks")
    @SerializedName("total_marks")
    val totalMarks: Float,

    @ColumnInfo(name = "exam_duration") // دقیقه
    @SerializedName("exam_duration")
    val examDuration: Int,

    @ColumnInfo(name = "start_time")
    @SerializedName("start_time")
    val startTime: String? = null,

    @ColumnInfo(name = "end_time")
    @SerializedName("end_time")
    val endTime: String? = null,

    @ColumnInfo(name = "status")
    @SerializedName("status")
    val status: ExamStatus,

    @ColumnInfo(name = "is_public")
    @SerializedName("is_public")
    val isPublic: Boolean = false,

    @ColumnInfo(name = "show_answers_immediately")
    @SerializedName("show_answers_immediately")
    val showAnswersImmediately: Boolean = false,

    @ColumnInfo(name = "allow_retake")
    @SerializedName("allow_retake")
    val allowRetake: Boolean = true,

    @ColumnInfo(name = "retake_count")
    @SerializedName("retake_count")
    val retakeCount: Int = 0,

    @ColumnInfo(name = "created_at")
    @SerializedName("created_at")
    val createdAt: String? = null,

    @ColumnInfo(name = "updated_at")
    @SerializedName("updated_at")
    val updatedAt: String? = null,

    // فیلدهای اضافی برای UI و مدیریت
    @ColumnInfo(name = "is_bookmarked")
    var isBookmarked: Boolean = false,

    @ColumnInfo(name = "last_accessed")
    var lastAccessed: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "progress")
    var progress: Float = 0f, // پیشرفت آزمون (0-100)

    @ColumnInfo(name = "current_question_index")
    var currentQuestionIndex: Int = 0,

    @ColumnInfo(name = "time_spent") // ثانیه
    var timeSpent: Int = 0,

    @ColumnInfo(name = "is_downloaded")
    var isDownloaded: Boolean = false,

    @ColumnInfo(name = "download_path")
    var downloadPath: String? = null
) {
    // فیلدهای Ignore (در دیتابیس ذخیره نمی‌شوند)
    @Ignore
    @SerializedName("questions")
    val questions: List<Question>? = null

    @Ignore
    @SerializedName("book")
    val book: Book? = null

    @Ignore
    @SerializedName("chapters")
    val chapters: List<Chapter>? = null

    @Ignore
    @SerializedName("user")
    val user: User? = null

    // Extension properties برای UI
    val statusText: String
        @Ignore
        get() = when (status) {
            ExamStatus.DRAFT -> "پیش‌نویس"
            ExamStatus.SCHEDULED -> "زمان‌بندی شده"
            ExamStatus.ACTIVE -> "فعال"
            ExamStatus.COMPLETED -> "تکمیل شده"
            ExamStatus.CANCELLED -> "لغو شده"
        }

    val statusColor: Int
        @Ignore
        get() = when (status) {
            ExamStatus.DRAFT -> 0xFF9E9E9E.toInt() // Gray
            ExamStatus.SCHEDULED -> 0xFFFF9800.toInt() // Orange
            ExamStatus.ACTIVE -> 0xFF4CAF50.toInt() // Green
            ExamStatus.COMPLETED -> 0xFF2196F3.toInt() // Blue
            ExamStatus.CANCELLED -> 0xFFF44336.toInt() // Red
        }

    val formattedDuration: String
        @Ignore
        get() {
            val hours = examDuration / 60
            val minutes = examDuration % 60
            return if (hours > 0) {
                "${hours} ساعت و ${minutes} دقیقه"
            } else {
                "${minutes} دقیقه"
            }
        }

    val isActive: Boolean
        @Ignore
        get() = status == ExamStatus.ACTIVE

    val isCompleted: Boolean
        @Ignore
        get() = status == ExamStatus.COMPLETED

    val canStart: Boolean
        @Ignore
        get() = status == ExamStatus.SCHEDULED || status == ExamStatus.DRAFT

    // متدهای کمکی
    @Ignore
    fun getRemainingTime(): String {
        timeSpent.let { spent ->
            val remaining = (examDuration * 60) - spent
            val minutes = remaining / 60
            val seconds = remaining % 60
            return String.format("%02d:%02d", minutes, seconds)
        }
    }

    @Ignore
    fun updateProgress(answeredQuestions: Int) {
        progress = if (totalQuestions > 0) {
            (answeredQuestions.toFloat() / totalQuestions) * 100
        } else {
            0f
        }
    }

    @Ignore
    fun markAsDownloaded(path: String) {
        isDownloaded = true
        downloadPath = path
    }

    @Ignore
    fun resetExamState() {
        currentQuestionIndex = 0
        timeSpent = 0
        progress = 0f
    }

    companion object {
        fun createDraftExam(
            userId: Int,
            grade: Int,
            subject: String,
            totalQuestions: Int,
            examDuration: Int
        ): Exam {
            return Exam(
                id = 0, // برای Exam جدید
                examCode = generateExamCode(),
                title = "آزمون جدید",
                userId = userId,
                grade = grade,
                subject = subject,
                totalQuestions = totalQuestions,
                totalMarks = totalQuestions * 1f, // هر سوال 1 نمره
                examDuration = examDuration,
                status = ExamStatus.DRAFT
            )
        }

        private fun generateExamCode(): String {
            val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
            return (1..8)
                .map { chars.random() }
                .joinToString("")
        }
    }
}

// ExamStatus enum در همین فایل باقی می‌ماند
enum class ExamStatus {
    @SerializedName("draft") DRAFT,
    @SerializedName("scheduled") SCHEDULED,
    @SerializedName("active") ACTIVE,
    @SerializedName("completed") COMPLETED,
    @SerializedName("cancelled") CANCELLED;

    fun getPersianName(): String {
        return when (this) {
            DRAFT -> "پیش‌نویس"
            SCHEDULED -> "زمان‌بندی شده"
            ACTIVE -> "فعال"
            COMPLETED -> "تکمیل شده"
            CANCELLED -> "لغو شده"
        }
    }
}