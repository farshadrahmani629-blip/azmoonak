// app/src/main/java/com/examapp/data/models/AuthModels.kt
package com.examapp.data.models

import com.google.gson.annotations.SerializedName

// ========== enum نقش کاربر ==========

enum class UserRole {
    @SerializedName("student")
    STUDENT,

    @SerializedName("teacher")
    TEACHER,

    @SerializedName("parent")
    PARENT,

    @SerializedName("admin")
    ADMIN;

    companion object {
        fun fromString(value: String): UserRole {
            return when (value.lowercase()) {
                "student" -> STUDENT
                "teacher" -> TEACHER
                "parent" -> PARENT
                "admin" -> ADMIN
                else -> STUDENT
            }
        }
    }

    fun getPersianName(): String {
        return when (this) {
            STUDENT -> "دانش‌آموز"
            TEACHER -> "معلم"
            PARENT -> "والدین"
            ADMIN -> "مدیر"
        }
    }
}

// ========== مدل‌های احراز هویت ==========

data class LoginRequest(
    @SerializedName("username")
    val username: String,

    @SerializedName("password")
    val password: String,

    @SerializedName("device_token")
    val deviceToken: String? = null, // برای نوتیفیکیشن

    @SerializedName("device_type")
    val deviceType: String = "android",

    @SerializedName("app_version")
    val appVersion: String? = null
) {
    fun isValid(): Boolean {
        return username.isNotBlank() && password.isNotBlank() && password.length >= 6
    }
}

data class RegisterRequest(
    @SerializedName("username")
    val username: String,

    @SerializedName("password")
    val password: String,

    @SerializedName("email")
    val email: String? = null,

    @SerializedName("phone")
    val phone: String? = null,

    @SerializedName("first_name")
    val firstName: String,

    @SerializedName("last_name")
    val lastName: String,

    @SerializedName("grade")
    val grade: Int? = null, // برای دانش‌آموزان اجباری

    @SerializedName("school")
    val school: String? = null,

    @SerializedName("role")
    val role: UserRole = UserRole.STUDENT,

    @SerializedName("device_token")
    val deviceToken: String? = null
) {
    fun isValid(): Boolean {
        return username.isNotBlank() &&
                password.isNotBlank() &&
                password.length >= 8 &&
                firstName.isNotBlank() &&
                lastName.isNotBlank() &&
                (role != UserRole.STUDENT || grade != null)
    }

    fun getValidationErrors(): List<String> {
        val errors = mutableListOf<String>()

        if (username.isBlank()) errors.add("نام کاربری الزامی است")
        if (password.length < 8) errors.add("رمز عبور باید حداقل ۸ کاراکتر باشد")
        if (firstName.isBlank()) errors.add("نام الزامی است")
        if (lastName.isBlank()) errors.add("نام خانوادگی الزامی است")
        if (role == UserRole.STUDENT && grade == null) errors.add("پایه تحصیلی برای دانش‌آموز الزامی است")

        return errors
    }
}

data class UserUpdateRequest(
    @SerializedName("first_name")
    val firstName: String? = null,

    @SerializedName("last_name")
    val lastName: String? = null,

    @SerializedName("email")
    val email: String? = null,

    @SerializedName("phone")
    val phone: String? = null,

    @SerializedName("grade")
    val grade: Int? = null,

    @SerializedName("school")
    val school: String? = null,

    @SerializedName("avatar_url")
    val avatarUrl: String? = null,

    @SerializedName("current_password")
    val currentPassword: String? = null,

    @SerializedName("new_password")
    val newPassword: String? = null
) {
    fun hasPasswordChange(): Boolean {
        return !currentPassword.isNullOrBlank() && !newPassword.isNullOrBlank()
    }

    fun isValidPasswordChange(): Boolean {
        return hasPasswordChange() && newPassword!!.length >= 8
    }
}

// ========== مدل‌های اعتبارسنجی ==========

data class VerificationRequest(
    @SerializedName("teacher_id")
    val teacherId: Int,

    @SerializedName("is_approved")
    val isApproved: Boolean,

    @SerializedName("feedback")
    val feedback: String? = null,

    @SerializedName("verified_at")
    val verifiedAt: String? = null
) {
    fun getStatusText(): String {
        return if (isApproved) "تایید شده" else "رد شده"
    }
}

data class VerificationStatus(
    @SerializedName("is_verified")
    val isVerified: Boolean,

    @SerializedName("verified_by")
    val verifiedBy: Int? = null,

    @SerializedName("verified_at")
    val verifiedAt: String? = null,

    @SerializedName("feedback")
    val feedback: String? = null
)

// ========== مدل پرداخت ==========

data class PurchaseRequest(
    @SerializedName("user_id")
    val userId: Int,

    @SerializedName("product_id")
    val productId: String, // شناسه محصول در کافه‌بازار

    @SerializedName("purchase_token")
    val purchaseToken: String,

    @SerializedName("order_id")
    val orderId: String? = null,

    @SerializedName("purchase_time")
    val purchaseTime: Long? = null
)

data class SubscriptionStatus(
    @SerializedName("is_pro")
    val isPro: Boolean,

    @SerializedName("expires_at")
    val expiresAt: String? = null,

    @SerializedName("purchase_date")
    val purchaseDate: String? = null,

    @SerializedName("product_id")
    val productId: String? = null,

    @SerializedName("subscription_type")
    val subscriptionType: String = "monthly", // monthly, yearly, lifetime

    @SerializedName("auto_renew")
    val autoRenew: Boolean = true
) {
    fun isExpired(): Boolean {
        return expiresAt?.let {
            // بررسی تاریخ انقضا
            // اینجا نیاز به Date parsing دارید
            false // placeholder
        } ?: false
    }

    fun daysUntilExpiration(): Int? {
        // محاسبه روزهای باقی‌مانده
        return null // placeholder
    }
}

// ========== مدل‌های بازیابی رمز عبور ==========

data class ForgotPasswordRequest(
    @SerializedName("username")
    val username: String? = null,

    @SerializedName("email")
    val email: String? = null,

    @SerializedName("phone")
    val phone: String? = null
) {
    fun isValid(): Boolean {
        return !username.isNullOrBlank() || !email.isNullOrBlank() || !phone.isNullOrBlank()
    }
}

data class ResetPasswordRequest(
    @SerializedName("token")
    val token: String,

    @SerializedName("new_password")
    val newPassword: String,

    @SerializedName("confirm_password")
    val confirmPassword: String
) {
    fun isValid(): Boolean {
        return token.isNotBlank() &&
                newPassword.isNotBlank() &&
                newPassword.length >= 8 &&
                newPassword == confirmPassword
    }
}

data class ChangePasswordRequest(
    @SerializedName("current_password")
    val currentPassword: String,

    @SerializedName("new_password")
    val newPassword: String,

    @SerializedName("confirm_password")
    val confirmPassword: String
) {
    fun isValid(): Boolean {
        return currentPassword.isNotBlank() &&
                newPassword.isNotBlank() &&
                newPassword.length >= 8 &&
                newPassword == confirmPassword &&
                newPassword != currentPassword
    }
}