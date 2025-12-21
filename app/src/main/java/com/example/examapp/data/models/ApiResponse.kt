// app/src/main/java/com/examapp/data/models/ApiResponse.kt
package com.examapp.data.models

import com.google.gson.annotations.SerializedName

/**
 * پاسخ عمومی API
 * @param success وضعیت موفقیت
 * @param message پیام توضیحی
 * @param data داده اصلی
 * @param error پیام خطا
 * @param errorCode کد خطا
 * @param timestamp زمان ایجاد پاسخ
 */
data class ApiResponse<T>(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("data")
    val data: T? = null,

    @SerializedName("error")
    val error: String? = null,

    @SerializedName("error_code")
    val errorCode: Int? = null,

    @SerializedName("timestamp")
    val timestamp: String? = null
) {
    /**
     * بررسی می‌کند آیا درخواست موفق بوده است
     */
    fun isSuccessful(): Boolean = success && error == null

    /**
     * دریافت پیام خطا (اولویت: error > message)
     */
    fun getErrorMessage(): String {
        return error ?: message ?: "خطای نامشخص"
    }

    /**
     * دریافت داده یا پرتاب exception در صورت خطا
     */
    fun getDataOrThrow(): T {
        return if (isSuccessful() && data != null) {
            data!!
        } else {
            throw RuntimeException(getErrorMessage())
        }
    }

    /**
     * دریافت داده یا مقدار پیش‌فرض
     */
    fun getDataOrDefault(defaultValue: T): T {
        return if (isSuccessful() && data != null) {
            data!!
        } else {
            defaultValue
        }
    }
}

/**
 * پاسخ صفحه‌بندی شده
 */
data class PaginatedResponse<T>(
    @SerializedName("data")
    val data: List<T>,

    @SerializedName("current_page")
    val currentPage: Int,

    @SerializedName("total_pages")
    val totalPages: Int,

    @SerializedName("total_items")
    val totalItems: Int,

    @SerializedName("has_next")
    val hasNext: Boolean,

    @SerializedName("has_previous")
    val hasPrevious: Boolean
) {
    /**
     * بررسی می‌کند آیا صفحه بعدی وجود دارد
     */
    fun hasNextPage(): Boolean = hasNext && currentPage < totalPages

    /**
     * بررسی می‌کند آیا صفحه قبلی وجود دارد
     */
    fun hasPreviousPage(): Boolean = hasPrevious && currentPage > 1

    /**
     * دریافت شماره صفحه بعدی
     */
    fun getNextPage(): Int? = if (hasNextPage()) currentPage + 1 else null

    /**
     * دریافت شماره صفحه قبلی
     */
    fun getPreviousPage(): Int? = if (hasPreviousPage()) currentPage - 1 else null
}

/**
 * پاسخ موفقیت آمیز ساده (برای عملیات بدون داده)
 */
data class SuccessResponse(
    @SerializedName("success")
    val success: Boolean = true,

    @SerializedName("message")
    val message: String? = null
)

/**
 * پاسخ خطای دقیق
 */
data class ErrorResponse(
    @SerializedName("error")
    val error: String,

    @SerializedName("error_description")
    val errorDescription: String? = null,

    @SerializedName("error_code")
    val errorCode: Int? = null,

    @SerializedName("field_errors")
    val fieldErrors: Map<String, List<String>>? = null
) {
    /**
     * دریافت همه خطاها به صورت متن
     */
    fun getAllErrors(): String {
        val errors = mutableListOf<String>()

        errorDescription?.let { errors.add(it) }

        fieldErrors?.forEach { (field, fieldErrors) ->
            fieldErrors.forEach { error ->
                errors.add("$field: $error")
            }
        }

        return if (errors.isNotEmpty()) {
            errors.joinToString("\n")
        } else {
            error
        }
    }
}

/**
 * پاسخ آپلود فایل
 */
data class UploadResponse(
    @SerializedName("url")
    val url: String,

    @SerializedName("file_name")
    val fileName: String,

    @SerializedName("file_size")
    val fileSize: Long,

    @SerializedName("mime_type")
    val mimeType: String
)

/**
 * وضعیت سرور
 */
data class ServerStatus(
    @SerializedName("status")
    val status: String,

    @SerializedName("version")
    val version: String,

    @SerializedName("uptime")
    val uptime: Long,

    @SerializedName("timestamp")
    val timestamp: String
) {
    /**
     * بررسی می‌کند آیا سرور فعال است
     */
    fun isServerUp(): Boolean = status.equals("up", ignoreCase = true)
}