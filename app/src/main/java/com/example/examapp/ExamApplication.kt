// app/src/main/java/com/examapp/ExamApplication.kt

package com.examapp

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.examapp.utils.SharedPrefs
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class ExamApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    private var _sharedPrefs: SharedPrefs? = null

    val sharedPrefs: SharedPrefs
        get() = _sharedPrefs ?: SharedPrefs(this).also { _sharedPrefs = it }

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        createNotificationChannels()
        checkFirstRun()

        Timber.d("ExamApplication Started")
    }

    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

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
    }
}