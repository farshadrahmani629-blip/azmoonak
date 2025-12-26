package com.examapp.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import com.google.gson.annotations.SerializedName

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey
    @ColumnInfo(name = "id")
    @SerializedName("id")
    val id: Int,

    @ColumnInfo(name = "name")
    @SerializedName("name")
    val name: String,

    @ColumnInfo(name = "description")
    @SerializedName("description")
    val description: String? = null,

    @ColumnInfo(name = "icon")
    @SerializedName("icon")
    val icon: String? = null,

    @ColumnInfo(name = "parent_id")
    @SerializedName("parent_id")
    val parentId: Int? = null,

    @ColumnInfo(name = "order_index")
    @SerializedName("order")
    val orderIndex: Int = 0,

    @ColumnInfo(name = "is_active")
    @SerializedName("is_active")
    val isActive: Boolean = true,

    @ColumnInfo(name = "exam_count")
    @SerializedName("exam_count")
    val examCount: Int = 0,

    @ColumnInfo(name = "created_at")
    @SerializedName("created_at")
    val createdAt: String? = null
) {
    val displayName: String
        get() = name

    val hasIcon: Boolean
        get() = !icon.isNullOrBlank()

    val isRootCategory: Boolean
        get() = parentId == null || parentId == 0
}