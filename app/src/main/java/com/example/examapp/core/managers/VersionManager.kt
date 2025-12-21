package com.examapp.core.managers

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object VersionManager {

    private const val PREFS_NAME = "version_prefs"
    private const val KEY_IS_PRO = "is_pro_version"

    private lateinit var prefs: SharedPreferences

    // باید در Application کلاس initialize شود
    fun initialize(context: Context) {
        if (!::prefs.isInitialized) {
            prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    fun isProVersion(): Boolean {
        if (!::prefs.isInitialized) {
            throw IllegalStateException("VersionManager not initialized. Call initialize() first.")
        }
        return prefs.getBoolean(KEY_IS_PRO, false)
    }

    fun unlockProVersion() {
        if (!::prefs.isInitialized) {
            throw IllegalStateException("VersionManager not initialized. Call initialize() first.")
        }
        prefs.edit {
            putBoolean(KEY_IS_PRO, true)
        }
        FreeTrialManager.resetFreeExams()
    }

    fun lockToLite() {
        if (!::prefs.isInitialized) {
            throw IllegalStateException("VersionManager not initialized. Call initialize() first.")
        }
        prefs.edit {
            putBoolean(KEY_IS_PRO, false)
        }
    }

    // متد کمکی برای تست
    fun setProVersionForTesting(isPro: Boolean) {
        if (!::prefs.isInitialized) {
            throw IllegalStateException("VersionManager not initialized. Call initialize() first.")
        }
        prefs.edit {
            putBoolean(KEY_IS_PRO, isPro)
        }
    }
}