package com.examapp.utils

import android.os.CountDownTimer
import java.util.concurrent.TimeUnit

/**
 * تایمر پیشرفته برای مدیریت زمان آزمون
 * قابلیت‌ها: شروع، توقف، مکث، ازسرگیری، نمایش فرمت‌شده
 */
class ExamTimer(
    private val totalTimeMillis: Long,
    private val intervalMillis: Long = 1000,
    private var onTickListener: ((Long) -> Unit)? = null,
    private var onFinishListener: (() -> Unit)? = null
) {

    private var countDownTimer: CountDownTimer? = null
    private var isRunning = false
    private var isPaused = false
    private var remainingTime = totalTimeMillis
    private var startTime = 0L

    companion object {
        /**
         * تبدیل میلی‌ثانیه به فرمت دقیقه:ثانیه
         */
        fun formatTime(milliseconds: Long): String {
            val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds)
            val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds) -
                    TimeUnit.MINUTES.toSeconds(minutes)
            return String.format("%02d:%02d", minutes, seconds)
        }

        /**
         * تبدیل میلی‌ثانیه به متن فارسی
         */
        fun formatTimePersian(milliseconds: Long): String {
            val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds)
            val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds) -
                    TimeUnit.MINUTES.toSeconds(minutes)
            return "${minutes} دقیقه و ${seconds} ثانیه"
        }
    }

    /**
     * شروع تایمر
     */
    fun start() {
        if (isRunning) return

        startTime = System.currentTimeMillis()

        countDownTimer = object : CountDownTimer(remainingTime, intervalMillis) {
            override fun onTick(millisUntilFinished: Long) {
                remainingTime = millisUntilFinished
                onTickListener?.invoke(millisUntilFinished)
            }

            override fun onFinish() {
                remainingTime = 0
                isRunning = false
                isPaused = false
                onFinishListener?.invoke()
            }
        }.start()

        isRunning = true
        isPaused = false
    }

    /**
     * توقف کامل تایمر
     */
    fun stop() {
        countDownTimer?.cancel()
        isRunning = false
        isPaused = false
        remainingTime = totalTimeMillis
    }

    /**
     * مکث تایمر
     */
    fun pause() {
        if (!isRunning || isPaused) return

        countDownTimer?.cancel()
        isPaused = true
        isRunning = false
    }

    /**
     * ازسرگیری تایمر
     */
    fun resume() {
        if (!isPaused || remainingTime <= 0) return

        start()
    }

    /**
     * تنظیم listener برای تیک تایمر
     */
    fun setOnTimerTickListener(listener: (Long) -> Unit) {
        this.onTickListener = listener
    }

    /**
     * تنظیم listener برای پایان تایمر
     */
    fun setOnTimerFinishListener(listener: () -> Unit) {
        this.onFinishListener = listener
    }

    /**
     * گرفتن زمان باقی‌مانده
     */
    fun getRemainingTime(): Long = remainingTime

    /**
     * گرفتن زمان سپری شده
     */
    fun getElapsedTime(): Long {
        return if (isRunning || isPaused) {
            totalTimeMillis - remainingTime
        } else {
            0
        }
    }

    /**
     * بررسی آیا تایمر در حال اجراست
     */
    fun isRunning(): Boolean = isRunning

    /**
     * بررسی آیا تایمر مکث شده
     */
    fun isPaused(): Boolean = isPaused

    /**
     * تنظیم زمان باقی‌مانده (برای مواقع خاص)
     */
    fun setRemainingTime(milliseconds: Long) {
        if (!isRunning) {
            remainingTime = milliseconds.coerceAtMost(totalTimeMillis)
        }
    }

    /**
     * اضافه کردن زمان اضافی
     */
    fun addExtraTime(milliseconds: Long) {
        remainingTime += milliseconds
    }

    /**
     * گرفتن درصد پیشرفت
     */
    fun getProgressPercentage(): Float {
        return if (totalTimeMillis > 0) {
            ((totalTimeMillis - remainingTime).toFloat() / totalTimeMillis) * 100
        } else {
            0f
        }
    }

    /**
     * گرفتن وضعیت تایمر به صورت متن
     */
    fun getStatusString(): String {
        return when {
            isRunning -> "در حال اجرا"
            isPaused -> "مکث شده"
            remainingTime <= 0 -> "پایان یافته"
            else -> "آماده"
        }
    }

    /**
     * پاک کردن listenerها
     */
    fun clearListeners() {
        onTickListener = null
        onFinishListener = null
    }
}