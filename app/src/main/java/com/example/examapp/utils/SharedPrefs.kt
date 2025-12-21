package com.examapp.utils

import android.content.Context
import android.content.SharedPreferences
import com.examapp.data.models.network.UserResponse
import com.google.gson.Gson

/**
 * مدیریت SharedPreferences
 */
object SharedPrefs {

    private const val PREFS_NAME = "ExamAppPrefs"
    private const val KEY_TOKEN = "auth_token"
    private const val KEY_USER = "user_data"
    private const val KEY_FIRST_RUN = "first_run"
    private const val KEY_DARK_MODE = "dark_mode"
    private const val KEY_LAST_SYNC = "last_sync"

    private lateinit var prefs: SharedPreferences
    private val gson = Gson()

    /**
     * مقداردهی اولیه (در کلاس Application فراخوانی شود)
     */
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // ============ احراز هویت ============

    /**
     * ذخیره توکن
     */
    fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    /**
     * دریافت توکن
     */
    fun getToken(): String {
        return prefs.getString(KEY_TOKEN, "") ?: ""
    }

    /**
     * ذخیره اطلاعات کاربر
     */
    fun saveUser(user: UserResponse) {
        val userJson = gson.toJson(user)
        prefs.edit().putString(KEY_USER, userJson).apply()
    }

    /**
     * دریافت اطلاعات کاربر
     */
    fun getUser(): UserResponse? {
        val userJson = prefs.getString(KEY_USER, null)
        return if (userJson != null) {
            gson.fromJson(userJson, UserResponse::class.java)
        } else {
            null
        }
    }

    /**
     * دریافت ID کاربر
     */
    fun getUserId(): String {
        return getUser()?.id ?: ""
    }

    /**
     * دریافت نام کاربر
     */
    fun getUserName(): String {
        return getUser()?.name ?: "کاربر مهمان"
    }

    /**
     * خروج کاربر
     */
    fun logout() {
        prefs.edit().remove(KEY_TOKEN).remove(KEY_USER).apply()
    }

    /**
     * بررسی لاگین بودن کاربر
     */
    fun isLoggedIn(): Boolean {
        return getToken().isNotEmpty() && getUser() != null
    }

    // ============ تنظیمات اپلیکیشن ============

    /**
     * بررسی اولین اجرا
     */
    fun isFirstRun(): Boolean {
        return prefs.getBoolean(KEY_FIRST_RUN, true)
    }

    /**
     * علامت‌گذاری که اولین اجرا انجام شده
     */
    fun setFirstRunCompleted() {
        prefs.edit().putBoolean(KEY_FIRST_RUN, false).apply()
    }

    /**
     * ذخیره حالت تاریک/روشن
     */
    fun setDarkMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply()
    }

    /**
     * دریافت حالت تاریک/روشن
     */
    fun isDarkMode(): Boolean {
        return prefs.getBoolean(KEY_DARK_MODE, false)
    }

    /**
     * ذخیره زمان آخرین همگام‌سازی
     */
    fun setLastSyncTime(timestamp: Long) {
        prefs.edit().putLong(KEY_LAST_SYNC, timestamp).apply()
    }

    /**
     * دریافت زمان آخرین همگام‌سازی
     */
    fun getLastSyncTime(): Long {
        return prefs.getLong(KEY_LAST_SYNC, 0)
    }

    /**
     * بررسی نیاز به همگام‌سازی (هر ۱ ساعت)
     */
    fun shouldSync(): Boolean {
        val lastSync = getLastSyncTime()
        val currentTime = System.currentTimeMillis()
        return (currentTime - lastSync) > (60 * 60 * 1000) // 1 ساعت
    }

    /**
     * پاک کردن تمام تنظیمات
     */
    fun clearAll() {
        prefs.edit().clear().apply()
    }
}