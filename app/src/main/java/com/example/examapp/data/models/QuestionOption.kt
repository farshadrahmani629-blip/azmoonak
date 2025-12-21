// app/src/main/java/com/examapp/data/models/QuestionOption.kt
package com.examapp.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.Ignore
import com.google.gson.annotations.SerializedName

@Entity(tableName = "question_options")
data class QuestionOption(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "local_id")
    val localId: Long = 0,

    @ColumnInfo(name = "id")
    @SerializedName("id")
    val id: Int,

    @ColumnInfo(name = "question_id", index = true)
    @SerializedName("question_id")
    val questionId: Int,

    @ColumnInfo(name = "option_number")
    @SerializedName("option_number")
    val optionNumber: Int,

    @ColumnInfo(name = "option_text")
    @SerializedName("option_text")
    val optionText: String,

    @ColumnInfo(name = "option_image_url")
    val optionImageUrl: String? = null,

    @ColumnInfo(name = "is_correct")
    @SerializedName("is_correct")
    val isCorrect: Boolean,

    @ColumnInfo(name = "explanation")
    @SerializedName("explanation")
    val explanation: String? = null,

    @ColumnInfo(name = "created_at")
    @SerializedName("created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    @SerializedName("updated_at")
    val updatedAt: Long = System.currentTimeMillis(),

    // فیلدهای UI
    @ColumnInfo(name = "is_selected")
    var isSelected: Boolean = false,

    @ColumnInfo(name = "is_revealed")
    var isRevealed: Boolean = false
) {
    val optionLetter: String
        @Ignore
        get() = when (optionNumber) {
            1 -> "الف"
            2 -> "ب"
            3 -> "ج"
            4 -> "د"
            5 -> "ه"
            else -> optionNumber.toString()
        }

    val displayText: String
        @Ignore
        get() = "$optionLetter) $optionText"

    val optionColor: Int
        @Ignore
        get() = when {
            isSelected && isCorrect -> 0xFF4CAF50.toInt() // سبز
            isSelected && !isCorrect -> 0xFFF44336.toInt() // قرمز
            isCorrect && isRevealed -> 0xFF4CAF50.toInt() // سبز
            isSelected -> 0x2196F3.toInt() // آبی
            else -> 0xFFE0E0E0.toInt() // خاکستری
        }

    val textColor: Int
        @Ignore
        get() = when {
            isSelected || (isCorrect && isRevealed) -> 0xFFFFFFFF.toInt() // سفید
            else -> 0xFF000000.toInt() // مشکی
        }
}