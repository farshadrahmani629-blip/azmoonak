// app/src/main/java/com/examapp/data/models/ExamSession.kt
package com.examapp.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.Ignore
import com.google.gson.annotations.SerializedName

@Entity(tableName = "exam_sessions")
data class ExamSession(
    @PrimaryKey
    @ColumnInfo(name = "session_id")
    @SerializedName("session_id")
    val sessionId: String,

    @ColumnInfo(name = "exam_id", index = true)
    @SerializedName("exam_id")
    val examId: String,

    @ColumnInfo(name = "user_id", index = true)
    @SerializedName("user_id")
    val userId: String,

    @ColumnInfo(name = "started_at")
    @SerializedName("started_at")
    val startedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "submitted_at")
    @SerializedName("submitted_at")
    val submittedAt: Long? = null,

    @ColumnInfo(name = "time_spent")
    @SerializedName("time_spent")
    val timeSpent: Int = 0,

    @ColumnInfo(name = "time_limit")
    @SerializedName("time_limit")
    val timeLimit: Int? = null,

    @ColumnInfo(name = "status")
    @SerializedName("status")
    val status: SessionStatus = SessionStatus.ACTIVE,

    // فیلدهای اضافی
    @ColumnInfo(name = "current_question_index")
    var currentQuestionIndex: Int = 0,

    @ColumnInfo(name = "total_questions")
    var totalQuestions: Int = 0,

    @ColumnInfo(name = "answered_questions")
    var answeredQuestions: Int = 0,

    @ColumnInfo(name = "remaining_time")
    var remainingTime: Int = 0,

    @ColumnInfo(name = "is_synced")
    var isSynced: Boolean = false
) {
    val isActive: Boolean
        @Ignore
        get() = status == SessionStatus.ACTIVE

    val isSubmitted: Boolean
        @Ignore
        get() = status == SessionStatus.SUBMITTED

    val isExpired: Boolean
        @Ignore
        get() = status == SessionStatus.EXPIRED

    val statusText: String
        @Ignore
        get() = when (status) {
            SessionStatus.ACTIVE -> "فعال"
            SessionStatus.SUBMITTED -> "ارسال شده"
            SessionStatus.EXPIRED -> "منقضی شده"
        }

    val timeSpentText: String
        @Ignore
        get() {
            val hours = timeSpent / 3600
            val minutes = (timeSpent % 3600) / 60
            val seconds = timeSpent % 60

            return if (hours > 0) "$hours:$minutes:$seconds" else "$minutes:$seconds"
        }

    @Ignore
    fun getProgressPercentage(): Float {
        return if (totalQuestions > 0) answeredQuestions.toFloat() / totalQuestions else 0f
    }

    @Ignore
    fun updateRemainingTime(): Boolean {
        if (timeLimit == null || !isActive) return false

        val elapsedSeconds = ((System.currentTimeMillis() - startedAt) / 1000).toInt()
        remainingTime = (timeLimit * 60) - elapsedSeconds

        if (remainingTime <= 0) {
            return false // زمان تمام شده
        }
        return true
    }
}

enum class SessionStatus {
    @SerializedName("active") ACTIVE,
    @SerializedName("submitted") SUBMITTED,
    @SerializedName("expired") EXPIRED
}