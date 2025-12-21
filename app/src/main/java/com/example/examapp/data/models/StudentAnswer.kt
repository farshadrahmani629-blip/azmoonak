// app/src/main/java/com/examapp/data/models/StudentAnswer.kt
package com.examapp.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.Ignore
import com.google.gson.annotations.SerializedName

@Entity(tableName = "student_answers")
data class StudentAnswer(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "local_id")
    val localId: Long = 0,

    @ColumnInfo(name = "id")
    @SerializedName("id")
    val id: Int? = null,

    @ColumnInfo(name = "exam_result_id", index = true)
    @SerializedName("exam_result_id")
    val examResultId: Int,

    @ColumnInfo(name = "question_id", index = true)
    @SerializedName("question_id")
    val questionId: Int,

    @ColumnInfo(name = "selected_option_id")
    @SerializedName("selected_option_id")
    val selectedOptionId: Int? = null,

    @ColumnInfo(name = "answer_text")
    @SerializedName("answer_text")
    val answerText: String? = null,

    @ColumnInfo(name = "is_correct")
    @SerializedName("is_correct")
    val isCorrect: Boolean? = null,

    @ColumnInfo(name = "marks_obtained")
    @SerializedName("marks_obtained")
    val marksObtained: Float,

    @ColumnInfo(name = "teacher_feedback")
    @SerializedName("teacher_feedback")
    val teacherFeedback: String? = null,

    @ColumnInfo(name = "time_spent")
    @SerializedName("time_spent")
    val timeSpent: Int,

    @ColumnInfo(name = "created_at")
    @SerializedName("created_at")
    val createdAt: Long = System.currentTimeMillis(),

    // فیلدهای اضافی
    @ColumnInfo(name = "is_reviewed")
    var isReviewed: Boolean = false,

    @ColumnInfo(name = "review_notes")
    var reviewNotes: String? = null
) {
    // فیلدهای Ignore (از JSON می‌آیند)
    @Ignore
    @SerializedName("question")
    val question: Question? = null

    @Ignore
    @SerializedName("selected_option")
    val selectedOption: QuestionOption? = null

    val statusText: String
        @Ignore
        get() = when {
            isCorrect == true -> "صحیح"
            isCorrect == false -> "غلط"
            else -> "بررسی نشده"
        }

    val statusColor: Int
        @Ignore
        get() = when {
            isCorrect == true -> 0xFF4CAF50.toInt()
            isCorrect == false -> 0xFFF44336.toInt()
            else -> 0xFFFF9800.toInt()
        }

    val answerDisplay: String
        @Ignore
        get() {
            return when {
                !answerText.isNullOrEmpty() -> answerText
                selectedOption != null -> selectedOption.optionText
                else -> "پاسخی داده نشده"
            }
        }

    @Ignore
    fun hasTeacherFeedback(): Boolean = !teacherFeedback.isNullOrEmpty()
}