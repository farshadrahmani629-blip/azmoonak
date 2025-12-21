package com.examapp.core.managers

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object FreeTrialManager {

    private const val PREFS_NAME = "free_trial_prefs"
    private const val KEY_FREE_EXAMS_TAKEN = "free_exams_taken"
    private const val MAX_FREE_EXAMS = 10

    private lateinit var prefs: SharedPreferences

    // باید در Application کلاس initialize شود
    fun initialize(context: Context) {
        if (!::prefs.isInitialized) {
            prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    fun canTakeFreeExam(): Boolean {
        if (!::prefs.isInitialized) {
            throw IllegalStateException("FreeTrialManager not initialized. Call initialize() first.")
        }
        return getTakenExams() < MAX_FREE_EXAMS
    }

    fun recordExamTaken() {
        if (!::prefs.isInitialized) {
            throw IllegalStateException("FreeTrialManager not initialized. Call initialize() first.")
        }
        prefs.edit {
            putInt(KEY_FREE_EXAMS_TAKEN, getTakenExams() + 1)
        }
    }

    fun getTakenExams(): Int {
        if (!::prefs.isInitialized) {
            return 0 // یا خطا بدهید
        }
        return prefs.getInt(KEY_FREE_EXAMS_TAKEN, 0)
    }

    fun getRemainingFreeExams(): Int {
        return MAX_FREE_EXAMS - getTakenExams()
    }

    fun resetFreeExams() {
        if (!::prefs.isInitialized) {
            throw IllegalStateException("FreeTrialManager not initialized. Call initialize() first.")
        }
        prefs.edit {
            remove(KEY_FREE_EXAMS_TAKEN)
        }
    }

    // متد کمکی برای تست
    fun setTakenExamsForTesting(count: Int) {
        if (!::prefs.isInitialized) {
            throw IllegalStateException("FreeTrialManager not initialized. Call initialize() first.")
        }
        prefs.edit {
            putInt(KEY_FREE_EXAMS_TAKEN, count)
        }
    }
}