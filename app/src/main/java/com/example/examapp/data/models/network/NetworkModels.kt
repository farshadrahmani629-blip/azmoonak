// app/src/main/java/com/examapp/data/models/network/NetworkModels.kt
package com.examapp.data.models.network

import com.google.gson.annotations.SerializedName

/**
 * درخواست لاگین
 */
data class LoginRequest(
    @SerializedName("username")
    val username: String,

    @SerializedName("password")
    val password: String,

    @SerializedName("device_token")
    val deviceToken: String? = null,

    @SerializedName("device_type")
    val deviceType: String = "android"
)

/**
 * پاسخ لاگین
 */
data class LoginResponse(
    @SerializedName("token")
    val token: String,

    @SerializedName("refresh_token")
    val refreshToken: String? = null,

    @SerializedName("expires_in")
    val expiresIn: Long? = null,

    @SerializedName("user")
    val user: UserResponse
)

// بقیه مدل‌های شبکه که قبلاً داشتیم...