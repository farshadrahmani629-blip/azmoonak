// app/src/main/java/com/examapp/data/models/local/ResultEntity.kt
package com.examapp.data.models.local

import androidx.room.*
import java.util.*

@Entity(
    tableName = "results",
    indices = [
        Index(value = ["user_id"]),
        Index(value = ["exam_id"]),
        Index(value = ["user_id", "exam_id"])
    ]
)
data class ResultEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "user_id")
    val userId: Int,

    @ColumnInfo(name = "exam_id")
    val examId: Int,

    @ColumnInfo(name = "exam_title")
    val examTitle: String,

    @ColumnInfo(name = "score")
    val score: Float,

    @ColumnInfo(name = "total_questions")
    val totalQuestions: Int,

    @ColumnInfo(name = "correct_answers")
    val correctAnswers: Int,

    @ColumnInfo(name = "wrong_answers")
    val wrongAnswers: Int,

    @ColumnInfo(name = "time_taken")
    val timeTaken: Long, // milliseconds

    @ColumnInfo(name = "date")
    val date: Date = Date(),

    @ColumnInfo(name = "user_answers")
    val userAnswers: Map<Int, String> = emptyMap(),

    @ColumnInfo(name = "details")
    val details: String? = null,

    @ColumnInfo(name = "feedback")
    val feedback: String? = null,

    @ColumnInfo(name = "is_synced")
    val isSynced: Boolean = false
)