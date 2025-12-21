// app/src/main/java/com/examapp/data/models/Chapter.kt
package com.examapp.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.Ignore
import com.google.gson.annotations.SerializedName

@Entity(tableName = "chapters")
data class Chapter(
    @PrimaryKey
    @ColumnInfo(name = "id")
    @SerializedName("id")
    val id: Int,

    @ColumnInfo(name = "book_id", index = true)
    @SerializedName("book_id")
    val bookId: Int,

    @ColumnInfo(name = "chapter_number")
    @SerializedName("chapter_number")
    val chapterNumber: Int,

    @ColumnInfo(name = "title")
    @SerializedName("title")
    val title: String,

    @ColumnInfo(name = "description")
    @SerializedName("description")
    val description: String? = null,

    @ColumnInfo(name = "start_page")
    @SerializedName("start_page")
    val startPage: Int,

    @ColumnInfo(name = "end_page")
    @SerializedName("end_page")
    val endPage: Int,

    @ColumnInfo(name = "is_active")
    @SerializedName("is_active")
    val isActive: Boolean = true,

    @ColumnInfo(name = "created_at")
    @SerializedName("created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    @SerializedName("updated_at")
    val updatedAt: Long = System.currentTimeMillis(),

    // فیلدهای اضافی
    @ColumnInfo(name = "is_selected")
    var isSelected: Boolean = false,

    @ColumnInfo(name = "question_count")
    var questionCount: Int = 0
) {
    val displayTitle: String
        @Ignore
        get() = "فصل $chapterNumber: $title"

    val pageRangeText: String
        @Ignore
        get() = "صفحات $startPage تا $endPage"

    @Ignore
    fun getPageCount(): Int = endPage - startPage + 1
}