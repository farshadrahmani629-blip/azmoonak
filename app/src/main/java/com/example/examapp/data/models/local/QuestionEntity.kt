// app/src/main/java/com/examapp/data/models/local/QuestionEntity.kt
package com.examapp.data.models.local

import androidx.room.*
import com.examapp.data.models.QuestionType
import java.util.*

@Entity(
    tableName = "questions",
    indices = [
        Index(value = ["uuid"], unique = true),
        Index(value = ["book_id"]),
        Index(value = ["chapter_id"]),
        Index(value = ["book_id", "chapter_id"]),
        Index(value = ["book_id", "page_number"])
    ]
)
data class QuestionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "uuid")
    val uuid: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "book_id")
    val bookId: Int,

    @ColumnInfo(name = "chapter_id")
    val chapterId: Int? = null,

    @ColumnInfo(name = "subject")
    val subject: String,

    @ColumnInfo(name = "question_text")
    val text: String,

    @ColumnInfo(name = "question_type")
    val type: QuestionType,

    @ColumnInfo(name = "page_number")
    val pageNumber: Int? = null,

    @ColumnInfo(name = "sub_topic")
    val subTopic: String? = null,

    @ColumnInfo(name = "difficulty")
    val difficulty: String = "MEDIUM",

    @ColumnInfo(name = "bloom_level")
    val bloomLevel: String = "UNDERSTANDING",

    @ColumnInfo(name = "correct_answer")
    val correctAnswer: String? = null,

    @ColumnInfo(name = "explanation")
    val explanation: String? = null,

    @ColumnInfo(name = "keywords")
    val keywords: List<String> = emptyList(),

    @ColumnInfo(name = "has_diagram")
    val hasDiagram: Boolean = false,

    @ColumnInfo(name = "is_multi_select")
    val isMultiSelect: Boolean = false,

    @ColumnInfo(name = "model_answer")
    val modelAnswer: String? = null,

    @ColumnInfo(name = "rubric")
    val rubric: String? = null,

    @ColumnInfo(name = "is_verified")
    val isVerified: Boolean = false,

    @ColumnInfo(name = "verified_by")
    val verifiedBy: Int? = null,

    @ColumnInfo(name = "verification_date")
    val verificationDate: Date? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Date = Date(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Date = Date()
)