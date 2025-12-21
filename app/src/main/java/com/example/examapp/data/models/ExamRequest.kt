// app/src/main/java/com/examapp/data/models/ExamRequest.kt
package com.examapp.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.Ignore
import com.google.gson.annotations.SerializedName

/**
 * درخواست ساخت آزمون جدید
 * این مدل هم برای API و هم برای ذخیره در دیتابیس استفاده می‌شود
 */
@Entity(tableName = "exam_requests")
data class ExamRequest(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "local_id")
    val localId: Long = 0,

    @ColumnInfo(name = "user_id", index = true)
    @SerializedName("user_id")
    val userId: Int,

    @ColumnInfo(name = "grade")
    @SerializedName("grade")
    val grade: Int? = null,

    @ColumnInfo(name = "subject")
    @SerializedName("subject")
    val subject: String? = null,

    @ColumnInfo(name = "book_id")
    @SerializedName("book_id")
    val bookId: Int? = null,

    @ColumnInfo(name = "chapter_ids")
    @SerializedName("chapter_ids")
    val chapterIds: List<Int>? = null,

    @ColumnInfo(name = "page_range_start")
    @SerializedName("page_range_start")
    val pageRangeStart: Int? = null,

    @ColumnInfo(name = "page_range_end")
    @SerializedName("page_range_end")
    val pageRangeEnd: Int? = null,

    @ColumnInfo(name = "question_types")
    @SerializedName("question_types")
    val questionTypes: List<QuestionType>? = null,

    @ColumnInfo(name = "difficulty_levels")
    @SerializedName("difficulty_levels")
    val difficultyLevels: List<DifficultyLevel>? = null,

    @ColumnInfo(name = "bloom_levels")
    @SerializedName("bloom_levels")
    val bloomLevels: List<BloomLevel>? = null,

    @ColumnInfo(name = "total_questions")
    @SerializedName("total_questions")
    val totalQuestions: Int,

    @ColumnInfo(name = "exam_duration")
    @SerializedName("exam_duration")
    val examDuration: Int,

    @ColumnInfo(name = "title")
    @SerializedName("title")
    val title: String? = null,

    @ColumnInfo(name = "description")
    @SerializedName("description")
    val description: String? = null,

    @ColumnInfo(name = "is_random")
    @SerializedName("is_random")
    val isRandom: Boolean = true,

    @ColumnInfo(name = "show_answers_immediately")
    @SerializedName("show_answers_immediately")
    val showAnswersImmediately: Boolean = false,

    @ColumnInfo(name = "allow_retake")
    @SerializedName("allow_retake")
    val allowRetake: Boolean = true,

    // فیلدهای اضافی
    @ColumnInfo(name = "status")
    var status: ExamRequestStatus = ExamRequestStatus.PENDING,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "processed_at")
    var processedAt: Long? = null,

    @ColumnInfo(name = "exam_id")
    var examId: Int? = null
) {
    @Ignore
    fun getFilterDescription(): String {
        val parts = mutableListOf<String>()

        if (grade != null) parts.add("پایه $grade")
        if (subject != null) parts.add(subject)
        if (bookId != null) parts.add("کتاب: $bookId")
        if (chapterIds != null && chapterIds.isNotEmpty()) {
            parts.add("${chapterIds.size} فصل")
        }
        if (totalQuestions > 0) parts.add("$totalQuestions سوال")
        if (examDuration > 0) parts.add("${examDuration} دقیقه")

        return parts.joinToString(" • ")
    }

    @Ignore
    fun isValid(): Boolean {
        return totalQuestions > 0 && examDuration > 0
    }
}

enum class ExamRequestStatus {
    PENDING, PROCESSING, COMPLETED, FAILED
}