// app/src/main/java/com/examapp/service/ExamService.kt
package com.examapp.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.examapp.R
import com.examapp.ui.exam.ExamActivity
import com.examapp.ui.MainActivity
import com.examapp.utils.SharedPrefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.*

class ExamService : Service() {

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val NOTIFICATION_CHANNEL_ID = "exam_service_channel"
        const val ACTION_START_EXAM_TIMER = "ACTION_START_EXAM_TIMER"
        const val ACTION_STOP_EXAM_TIMER = "ACTION_STOP_EXAM_TIMER"
        const val ACTION_SEND_REMINDER = "ACTION_SEND_REMINDER"
        const val ACTION_SYNC_DATA = "ACTION_SYNC_DATA"
        const val ACTION_BACKUP_DATA = "ACTION_BACKUP_DATA"
        const val EXTRA_EXAM_ID = "extra_exam_id"
        const val EXTRA_EXAM_TITLE = "extra_exam_title"
        const val EXTRA_REMAINING_TIME = "extra_remaining_time"

        fun startService(context: Context, action: String, examId: Int = -1, examTitle: String = "") {
            Intent(context, ExamService::class.java).apply {
                this.action = action
                putExtra(EXTRA_EXAM_ID, examId)
                putExtra(EXTRA_EXAM_TITLE, examTitle)
            }.let { intent ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }
        }

        fun stopService(context: Context) {
            context.stopService(Intent(context, ExamService::class.java))
        }
    }

    private lateinit var notificationManager: NotificationManager
    private lateinit var sharedPrefs: SharedPrefs
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var isExamTimerRunning = false
    private var currentExamId = -1
    private var currentExamTitle = ""
    private var remainingTime = 0L

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        sharedPrefs = SharedPrefs(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_EXAM_TIMER -> {
                val examId = intent.getIntExtra(EXTRA_EXAM_ID, -1)
                val examTitle = intent.getStringExtra(EXTRA_EXAM_TITLE) ?: ""
                startExamTimer(examId, examTitle)
            }
            ACTION_STOP_EXAM_TIMER -> stopExamTimer()
            ACTION_SEND_REMINDER -> sendDailyReminder()
            ACTION_SYNC_DATA -> syncData()
            ACTION_BACKUP_DATA -> backupData()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "سرویس آزمون",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "سرویس پس‌زمینه برای مدیریت آزمون‌ها"
                setShowBadge(false)
            }.let { channel ->
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    private fun startForegroundNotification() {
        Intent(this, ExamActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }.let { notificationIntent ->
            PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            ).let { pendingIntent ->
                NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                    .setContentTitle("⏱️ آزمون در حال اجراست")
                    .setContentText("$currentExamTitle - زمان باقی‌مانده: ${formatTime(remainingTime)}")
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentIntent(pendingIntent)
                    .setOngoing(true)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setCategory(NotificationCompat.CATEGORY_SERVICE)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .build()
                    .let { notification ->
                        startForeground(NOTIFICATION_ID, notification)
                    }
            }
        }
    }

    private fun startExamTimer(examId: Int, examTitle: String) {
        if (isExamTimerRunning) return
        currentExamId = examId
        currentExamTitle = examTitle
        isExamTimerRunning = true
        remainingTime = 45 * 60 * 1000L
        startForegroundNotification()
        startTimer()
    }

    private fun startTimer() {
        Thread {
            while (isExamTimerRunning && remainingTime > 0) {
                Thread.sleep(1000)
                remainingTime -= 1000
                updateNotification()
                if (remainingTime <= 0) {
                    onExamTimeFinished()
                    break
                }
                if (remainingTime % 60000 == 0L) saveExamProgress()
            }
        }.start()
    }

    private fun stopExamTimer() {
        isExamTimerRunning = false
        saveExamProgress()
        stopForeground(true)
        stopSelf()
    }

    private fun onExamTimeFinished() {
        isExamTimerRunning = false
        Intent(ACTION_EXAM_FINISHED).apply {
            putExtra(EXTRA_EXAM_ID, currentExamId)
        }.let { sendBroadcast(it) }
        showTimeUpNotification()
        stopSelf()
    }

    private fun saveExamProgress() {
        sharedPrefs.saveLong("exam_${currentExamId}_remaining_time", remainingTime)
        sharedPrefs.saveLong("exam_last_update", System.currentTimeMillis())
    }

    private fun updateNotification() {
        NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("⏱️ آزمون در حال اجراست")
            .setContentText("$currentExamTitle - زمان باقی‌مانده: ${formatTime(remainingTime)}")
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
            .let { notificationManager.notify(NOTIFICATION_ID, it) }
    }

    private fun showTimeUpNotification() {
        Intent(this, ExamActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(EXTRA_EXAM_ID, currentExamId)
        }.let { intent ->
            PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            ).let { pendingIntent ->
                NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                    .setContentTitle("⏰ زمان آزمون به پایان رسید!")
                    .setContentText("آزمون $currentExamTitle تمام شده است.")
                    .setSmallIcon(R.drawable.ic_time_up)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .build()
                    .let { notificationManager.notify(NOTIFICATION_ID + 1, it) }
            }
        }
    }

    private fun sendDailyReminder() {
        if (!sharedPrefs.isReminderEnabled()) return
        val (hour, minute) = sharedPrefs.getReminderTime()
        val calendar = Calendar.getInstance()
        if (calendar.get(Calendar.HOUR_OF_DAY) == hour && calendar.get(Calendar.MINUTE) == minute) {
            val lastReminder = sharedPrefs.getLong("last_reminder_time", 0)
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastReminder > 24 * 60 * 60 * 1000) {
                showReminderNotification()
                sharedPrefs.saveLong("last_reminder_time", currentTime)
            }
        }
    }

    private fun showReminderNotification() {
        Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }.let { intent ->
            PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            ).let { pendingIntent ->
                NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                    .setContentTitle("📚 زمان مطالعه!")
                    .setContentText("یادتان نرود امروز هم در آزمون شرکت کنید.")
                    .setSmallIcon(R.drawable.ic_reminder)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .build()
                    .let { notificationManager.notify(NOTIFICATION_ID + 2, it) }
            }
        }
    }

    private fun syncData() {
        serviceScope.launch {
            sharedPrefs.saveLong("last_sync_time", System.currentTimeMillis())
        }
    }

    private fun backupData() {
        serviceScope.launch {
            sharedPrefs.saveLong("last_backup_time", System.currentTimeMillis())
        }
    }

    private fun formatTime(milliseconds: Long): String {
        val minutes = milliseconds / (1000 * 60)
        val seconds = (milliseconds / 1000) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isExamTimerRunning) saveExamProgress()
    }
}