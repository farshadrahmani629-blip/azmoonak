// app/src/main/java/com/examapp/data/models/User.kt
package com.examapp.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.Ignore
import com.google.gson.annotations.SerializedName
import java.util.Date

@Entity(tableName = "users")
data class User(
    @PrimaryKey
    @ColumnInfo(name = "id")
    @SerializedName("id")
    val id: Int,

    @ColumnInfo(name = "username", index = true)
    @SerializedName("username")
    val username: String,

    @ColumnInfo(name = "email", index = true)
    @SerializedName("email")
    val email: String? = null,

    @ColumnInfo(name = "phone")
    @SerializedName("phone")
    val phone: String? = null,

    @ColumnInfo(name = "first_name")
    @SerializedName("first_name")
    val firstName: String,

    @ColumnInfo(name = "last_name")
    @SerializedName("last_name")
    val lastName: String,

    @ColumnInfo(name = "full_name")
    val fullName: String = "$firstName $lastName",

    @ColumnInfo(name = "grade")
    @SerializedName("grade")
    val grade: Int? = null,

    @ColumnInfo(name = "school")
    @SerializedName("school")
    val school: String? = null,

    @ColumnInfo(name = "avatar_url")
    @SerializedName("avatar_url")
    val avatarUrl: String? = null,

    @ColumnInfo(name = "role")
    @SerializedName("role")
    val role: UserRole,

    @ColumnInfo(name = "subscription_type")
    @SerializedName("subscription_type")
    val subscriptionType: SubscriptionType = SubscriptionType.FREE,

    @ColumnInfo(name = "subscription_expires_at")
    @SerializedName("subscription_expires_at")
    val subscriptionExpiresAt: Long? = null,

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
    @ColumnInfo(name = "last_login")
    var lastLogin: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "total_exams")
    var totalExams: Int = 0,

    @ColumnInfo(name = "average_score")
    var averageScore: Float = 0f
) {
    val roleText: String
        @Ignore
        get() = when (role) {
            UserRole.STUDENT -> "دانش‌آموز"
            UserRole.TEACHER -> "معلم"
            UserRole.PARENT -> "والدین"
            UserRole.ADMIN -> "مدیر"
        }

    val subscriptionText: String
        @Ignore
        get() = when (subscriptionType) {
            SubscriptionType.FREE -> "رایگان"
            SubscriptionType.PRO -> "پرو"
        }

    val isSubscriptionActive: Boolean
        @Ignore
        get() {
            return subscriptionExpiresAt?.let { it > System.currentTimeMillis() } ?: true
        }

    @Ignore
    fun isTeacherOrAdmin(): Boolean = role == UserRole.TEACHER || role == UserRole.ADMIN
}

enum class UserRole {
    @SerializedName("student") STUDENT,
    @SerializedName("teacher") TEACHER,
    @SerializedName("parent") PARENT,
    @SerializedName("admin") ADMIN
}

enum class SubscriptionType {
    @SerializedName("free") FREE,
    @SerializedName("pro") PRO,
    @SerializedName("premium") PREMIUM
}