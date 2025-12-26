// app/src/main/java/com/examapp/ExamApplication.kt
package com.examapp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class ExamApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        initializeAppSettings()
        Timber.d("Exam Application Started")
    }

    private fun initializeAppSettings() {
        // Settings moved to SharedPrefs utility
    }

    companion object {
        const val APP_TAG = "ExamApp"
        const val DATABASE_NAME = "exam_database"
        const val PREFERENCES_NAME = "exam_app_prefs"
        const val RC_SIGN_IN = 1001
        const val RC_PURCHASE = 1002
        const val RC_PERMISSION_STORAGE = 1003
        const val RC_PERMISSION_CAMERA = 1004
        const val PREFS_USER_ID = "user_id"
        const val PREFS_USERNAME = "username"
        const val PREFS_IS_PRO = "is_pro_user"
        const val PREFS_LAST_SYNC = "last_sync_time"
        const val PREFS_APP_LANGUAGE = "app_language"
    }
}