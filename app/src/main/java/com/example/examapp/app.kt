// app/src/main/java/com/examapp/App.kt
package com.examapp

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.examapp.data.local.ExamDatabase
import com.examapp.utils.SharedPrefs
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class App : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    private var _sharedPrefs: SharedPrefs? = null

    // Room Database instance
    val database: ExamDatabase by lazy {
        ExamDatabase.getDatabase(this)
    }

    val sharedPrefs: SharedPrefs
        get() = _sharedPrefs ?: SharedPrefs(this).also { _sharedPrefs = it }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannels()
        checkFirstRun()
    }

    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importantChannel = NotificationChannel(
                CHANNEL_ID_IMPORTANT,
                "اعلان‌های مهم",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "اعلان‌های مربوط به آزمون‌ها و نتایج"
            }

            val reminderChannel = NotificationChannel(
                CHANNEL_ID_REMINDER,
                "یادآوری‌ها",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "یادآوری برای شرکت در آزمون‌ها"
            }

            val updateChannel = NotificationChannel(
                CHANNEL_ID_UPDATES,
                "بروزرسانی‌ها",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "بروزرسانی‌های برنامه و محتوا"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannels(
                listOf(importantChannel, reminderChannel, updateChannel)
            )
        }
    }

    private fun checkFirstRun() {
        if (sharedPrefs.isFirstRun()) {
            sharedPrefs.saveUserInfo("کاربر مهمان", 4)
            sharedPrefs.saveSoundSettings(true, true)
            sharedPrefs.saveReminderSettings(true, 18, 0)
            sharedPrefs.saveAppearanceSettings("auto", 1.0f, "fa")
            sharedPrefs.markFirstRunCompleted()
        }
    }

    companion object {
        const val CHANNEL_ID_IMPORTANT = "important_notifications"
        const val CHANNEL_ID_REMINDER = "reminder_notifications"
        const val CHANNEL_ID_UPDATES = "update_notifications"
        const val PREFS_NAME = "exam_app_prefs"
        const val ACTION_EXAM_STARTED = "com.examapp.ACTION_EXAM_STARTED"
        const val ACTION_EXAM_FINISHED = "com.examapp.ACTION_EXAM_FINISHED"
        const val ACTION_RESULT_SAVED = "com.examapp.ACTION_RESULT_SAVED"
        const val REQUEST_CODE_EXAM = 1001
        const val REQUEST_CODE_RESULT = 1002
        const val REQUEST_CODE_SETTINGS = 1003
        const val RESULT_EXAM_COMPLETED = 2001
        const val RESULT_EXAM_CANCELLED = 2002

        @Volatile
        private var instance: App? = null

        fun getInstance(): App {
            return instance ?: throw IllegalStateException("Application not initialized!")
        }
    }
}