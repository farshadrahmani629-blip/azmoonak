// app/src/main/java/com/examapp/data/models/Book.kt
package com.examapp.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.Ignore
import com.google.gson.annotations.SerializedName

@Entity(tableName = "books")
data class Book(
    @PrimaryKey
    @ColumnInfo(name = "id")
    @SerializedName("id")
    val id: Int,

    @ColumnInfo(name = "grade", index = true)
    @SerializedName("grade")
    val grade: Int,

    @ColumnInfo(name = "subject", index = true)
    @SerializedName("subject")
    val subject: String,

    @ColumnInfo(name = "title")
    @SerializedName("title")
    val title: String,

    @ColumnInfo(name = "publisher")
    @SerializedName("publisher")
    val publisher: String? = null,

    @ColumnInfo(name = "cover_image_url")
    @SerializedName("cover_image_url")
    val coverImageUrl: String? = null,

    @ColumnInfo(name = "is_active")
    @SerializedName("is_active")
    val isActive: Boolean = true,

    @ColumnInfo(name = "created_at")
    @SerializedName("created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    @SerializedName("updated_at")
    val updatedAt: Long = System.currentTimeMillis(),

    // فیلدهای اضافی برای UI
    @ColumnInfo(name = "is_selected")
    var isSelected: Boolean = false,

    @ColumnInfo(name = "question_count")
    var questionCount: Int = 0
) {
    val displayTitle: String
        @Ignore
        get() = "$title - پایه $grade ($subject)"

    val gradeText: String
        @Ignore
        get() = "پایه $grade"
}

// Enum برای دروس (در صورت نیاز)
enum class Subject {
    MATHEMATICS, SCIENCE, LITERATURE, HISTORY, ENGLISH, ARABIC
}