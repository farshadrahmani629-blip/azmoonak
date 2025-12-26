package com.examapp.ui.exam.pdf

import android.content.Context
import android.os.Build
import android.os.Environment
import java.io.File

/**
 * کلاس تنظیمات و مسیرهای ذخیره‌سازی فایل‌های PDF
 */
object StorageConfig {

    // ==================== پوشه‌های اصلی ====================

    /**
     * پوشه ریشه برنامه در حافظه
     * ساختار: /Documents/ExamApp/
     */
    fun getAppRootDirectory(context: Context): File {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ : استفاده از Scoped Storage
            File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "ExamApp")
        } else {
            // Android 9 و پایین‌تر: دسترسی مستقیم
            File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                "ExamApp"
            )
        }
    }

    /**
     * پوشه آزمون‌های تولید شده
     * ساختار: /Documents/ExamApp/Exams/
     */
    fun getExamsDirectory(context: Context): File {
        return File(getAppRootDirectory(context), "آزمون‌ها").apply {
            if (!exists()) mkdirs()
        }
    }

    /**
     * پوشه نتایج و کارنامه‌ها
     * ساختار: /Documents/ExamApp/Results/
     */
    fun getResultsDirectory(context: Context): File {
        return File(getAppRootDirectory(context), "نتایج").apply {
            if (!exists()) mkdirs()
        }
    }

    /**
     * پوشه پاسخنامه‌ها
     * ساختار: /Documents/ExamApp/AnswerSheets/
     */
    fun getAnswerSheetsDirectory(context: Context): File {
        return File(getAppRootDirectory(context), "پاسخنامه‌ها").apply {
            if (!exists()) mkdirs()
        }
    }

    /**
     * پوشه فایل‌های موقت (برای اشتراک‌گذاری)
     * ساختار: /Documents/ExamApp/Temp/
     */
    fun getTempDirectory(context: Context): File {
        return File(getAppRootDirectory(context), "موقت").apply {
            if (!exists()) mkdirs()
        }
    }

    /**
     * پوشه آرشیو آزمون‌های قدیمی
     * ساختار: /Documents/ExamApp/Archive/[سال]/[ماه]/
     */
    fun getArchiveDirectory(context: Context, year: Int, month: Int): File {
        return File(getAppRootDirectory(context), "آرشیو/$year/$month").apply {
            if (!exists()) mkdirs()
        }
    }

    /**
     * پوشه پشتیبان‌گیری
     * ساختار: /Documents/ExamApp/Backup/
     */
    fun getBackupDirectory(context: Context): File {
        return File(getAppRootDirectory(context), "پشتیبان").apply {
            if (!exists()) mkdirs()
        }
    }

    // ==================== مسیرهای کامل ذخیره‌سازی ====================

    /**
     * مسیر کامل ذخیره‌سازی PDF آزمون
     * @param examTitle عنوان آزمون
     * @param studentName نام دانش‌آموز
     * @param timestamp timestamp برای منحصر به فرد بودن
     */
    fun getExamPdfPath(
        context: Context,
        examTitle: String,
        studentName: String,
        timestamp: Long = System.currentTimeMillis()
    ): String {
        val safeExamTitle = examTitle.replace("/", "-").replace(":", "-")
        val safeStudentName = studentName.replace("/", "-").replace(":", "-")

        val fileName = "آزمون_${safeExamTitle}_${safeStudentName}_${timestamp}.pdf"
        val file = File(getExamsDirectory(context), fileName)

        return file.absolutePath
    }

    /**
     * مسیر کامل ذخیره‌سازی PDF نتیجه
     */
    fun getResultPdfPath(
        context: Context,
        examTitle: String,
        studentName: String,
        score: Float,
        timestamp: Long = System.currentTimeMillis()
    ): String {
        val safeExamTitle = examTitle.replace("/", "-").replace(":", "-")
        val safeStudentName = studentName.replace("/", "-").replace(":", "-")
        val formattedScore = String.format("%.1f", score)

        val fileName = "کارنامه_${safeExamTitle}_${safeStudentName}_${formattedScore}_${timestamp}.pdf"
        val file = File(getResultsDirectory(context), fileName)

        return file.absolutePath
    }

    /**
     * مسیر کامل ذخیره‌سازی PDF پاسخنامه
     */
    fun getAnswerSheetPdfPath(
        context: Context,
        examTitle: String,
        studentName: String,
        timestamp: Long = System.currentTimeMillis()
    ): String {
        val safeExamTitle = examTitle.replace("/", "-").replace(":", "-")
        val safeStudentName = studentName.replace("/", "-").replace(":", "-")

        val fileName = "پاسخنامه_${safeExamTitle}_${safeStudentName}_${timestamp}.pdf"
        val file = File(getAnswerSheetsDirectory(context), fileName)

        return file.absolutePath
    }

    /**
     * مسیر فایل موقت برای اشتراک‌گذاری
     */
    fun getTempSharePath(context: Context, originalFileName: String): String {
        val tempName = "share_${System.currentTimeMillis()}_$originalFileName"
        val file = File(getTempDirectory(context), tempName)

        return file.absolutePath
    }

    /**
     * مسیر فایل پشتیبان
     */
    fun getBackupPath(context: Context, backupType: String): String {
        val timestamp = System.currentTimeMillis()
        val fileName = "پشتیبان_${backupType}_${timestamp}.zip"
        val file = File(getBackupDirectory(context), fileName)

        return file.absolutePath
    }

    // ==================== بررسی دسترسی و فضای ذخیره‌سازی ====================

    /**
     * بررسی وجود فضای کافی برای ذخیره‌سازی
     * @param requiredSize اندازه مورد نیاز به بایت
     */
    fun hasEnoughStorage(context: Context, requiredSize: Long): Boolean {
        val storageDir = getAppRootDirectory(context)
        return storageDir.freeSpace >= requiredSize
    }

    /**
     * بررسی دسترسی نوشتن در حافظه
     */
    fun canWriteToStorage(context: Context): Boolean {
        return try {
            val testFile = File(getTempDirectory(context), "test_write.tmp")
            testFile.createNewFile()
            testFile.delete()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * بررسی دسترسی خواندن از حافظه
     */
    fun canReadFromStorage(context: Context): Boolean {
        return try {
            val testFile = File(getTempDirectory(context), "test_read.tmp")
            testFile.exists()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * دریافت فضای آزاد موجود
     */
    fun getAvailableStorageSpace(context: Context): Long {
        return getAppRootDirectory(context).freeSpace
    }

    /**
     * دریافت فضای استفاده شده توسط برنامه
     */
    fun getUsedStorageSpace(context: Context): Long {
        return calculateDirectorySize(getAppRootDirectory(context))
    }

    /**
     * محاسبه اندازه پوشه
     */
    private fun calculateDirectorySize(directory: File): Long {
        var size = 0L
        if (directory.exists() && directory.isDirectory) {
            directory.listFiles()?.forEach { file ->
                size += if (file.isFile) {
                    file.length()
                } else {
                    calculateDirectorySize(file)
                }
            }
        }
        return size
    }

    // ==================== مدیریت فایل‌های قدیمی ====================

    /**
     * حذف فایل‌های موقت قدیمی‌تر از X روز
     */
    fun cleanupOldTempFiles(context: Context, daysOld: Int = 7) {
        val tempDir = getTempDirectory(context)
        val cutoffTime = System.currentTimeMillis() - (daysOld * 24 * 60 * 60 * 1000L)

        tempDir.listFiles()?.forEach { file ->
            if (file.lastModified() < cutoffTime) {
                file.delete()
            }
        }
    }

    /**
     * آرشیو کردن فایل‌های قدیمی‌تر از X روز
     */
    fun archiveOldFiles(context: Context, daysOld: Int = 30) {
        val cutoffTime = System.currentTimeMillis() - (daysOld * 24 * 60 * 60 * 1000L)
        val now = java.util.Calendar.getInstance()
        val year = now.get(java.util.Calendar.YEAR)
        val month = now.get(java.util.Calendar.MONTH) + 1

        // آرشیو آزمون‌ها
        archiveDirectoryFiles(
            sourceDir = getExamsDirectory(context),
            destDir = getArchiveDirectory(context, year, month),
            cutoffTime = cutoffTime,
            prefix = "آزمون"
        )

        // آرشیو نتایج
        archiveDirectoryFiles(
            sourceDir = getResultsDirectory(context),
            destDir = getArchiveDirectory(context, year, month),
            cutoffTime = cutoffTime,
            prefix = "کارنامه"
        )
    }

    private fun archiveDirectoryFiles(
        sourceDir: File,
        destDir: File,
        cutoffTime: Long,
        prefix: String
    ) {
        if (sourceDir.exists() && sourceDir.isDirectory) {
            sourceDir.listFiles()?.forEach { file ->
                if (file.isFile && file.lastModified() < cutoffTime) {
                    val newFile = File(destDir, "${prefix}_${file.name}")
                    file.renameTo(newFile)
                }
            }
        }
    }

    // ==================== اطلاعات ذخیره‌سازی برای نمایش به کاربر ====================

    /**
     * دریافت اطلاعات ذخیره‌سازی برای نمایش
     */
    fun getStorageInfo(context: Context): StorageInfo {
        val appDir = getAppRootDirectory(context)
        val examsDir = getExamsDirectory(context)
        val resultsDir = getResultsDirectory(context)

        return StorageInfo(
            appRootPath = appDir.absolutePath,
            examsPath = examsDir.absolutePath,
            resultsPath = resultsDir.absolutePath,
            totalFiles = countFiles(appDir),
            totalSize = calculateDirectorySize(appDir),
            availableSpace = appDir.freeSpace,
            examsCount = countFiles(examsDir),
            resultsCount = countFiles(resultsDir)
        )
    }

    /**
     * شمارش فایل‌ها در یک پوشه
     */
    private fun countFiles(directory: File): Int {
        var count = 0
        if (directory.exists() && directory.isDirectory) {
            directory.listFiles()?.forEach { file ->
                count += if (file.isFile) 1 else countFiles(file)
            }
        }
        return count
    }

    /**
     * کلاس اطلاعات ذخیره‌سازی
     */
    data class StorageInfo(
        val appRootPath: String,
        val examsPath: String,
        val resultsPath: String,
        val totalFiles: Int,
        val totalSize: Long,
        val availableSpace: Long,
        val examsCount: Int,
        val resultsCount: Int
    ) {
        /**
         * فرمت‌بندی اندازه فایل برای نمایش
         */
        fun getFormattedTotalSize(): String {
            return formatFileSize(totalSize)
        }

        fun getFormattedAvailableSpace(): String {
            return formatFileSize(availableSpace)
        }

        private fun formatFileSize(size: Long): String {
            return when {
                size < 1024 -> "$size بایت"
                size < 1024 * 1024 -> "${size / 1024} کیلوبایت"
                size < 1024 * 1024 * 1024 -> "${size / (1024 * 1024)} مگابایت"
                else -> "${size / (1024 * 1024 * 1024)} گیگابایت"
            }
        }
    }

    // ==================== نمونه‌های آماده مسیر ====================

    /**
     * لیست مسیرهای مهم برای استفاده سریع
     */
    object Paths {

        /**
         * ساختار کامل پوشه‌ها برای نمایش به کاربر
         */
        fun getFullDirectoryStructure(context: Context): Map<String, String> {
            return mapOf(
                "ریشه برنامه" to getAppRootDirectory(context).absolutePath,
                "آزمون‌ها" to getExamsDirectory(context).absolutePath,
                "نتایج" to getResultsDirectory(context).absolutePath,
                "پاسخنامه‌ها" to getAnswerSheetsDirectory(context).absolutePath,
                "موقت" to getTempDirectory(context).absolutePath,
                "آرشیو" to getArchiveDirectory(context, 2024, 1).absolutePath,
                "پشتیبان" to getBackupDirectory(context).absolutePath
            )
        }

        /**
         * مثال مسیرهای تولید شده
         */
        fun getExamplePaths(context: Context): Map<String, String> {
            return mapOf(
                "مثال مسیر آزمون" to getExamPdfPath(
                    context = context,
                    examTitle = "ریاضی پایه ششم",
                    studentName = "علی محمدی"
                ),
                "مثال مسیر کارنامه" to getResultPdfPath(
                    context = context,
                    examTitle = "ریاضی پایه ششم",
                    studentName = "علی محمدی",
                    score = 85.5f
                ),
                "مثال مسیر پاسخنامه" to getAnswerSheetPdfPath(
                    context = context,
                    examTitle = "ریاضی پایه ششم",
                    studentName = "علی محمدی"
                )
            )
        }
    }

    // ==================== توابع کمکی برای دیباگ ====================

    /**
     * چاپ تمام مسیرها در Log (برای دیباگ)
     */
    fun printAllPaths(context: Context) {
        println("📁 ========== ساختار پوشه‌های ExamApp ==========")

        Paths.getFullDirectoryStructure(context).forEach { (name, path) ->
            println("📂 $name: $path")
        }

        println("\n📄 ========== مثال مسیرهای فایل ==========")
        Paths.getExamplePaths(context).forEach { (name, path) ->
            println("📝 $name: $path")
        }

        val storageInfo = getStorageInfo(context)
        println("\n💾 ========== اطلاعات ذخیره‌سازی ==========")
        println("📊 تعداد کل فایل‌ها: ${storageInfo.totalFiles}")
        println("💿 فضای استفاده شده: ${storageInfo.getFormattedTotalSize()}")
        println("🆓 فضای آزاد: ${storageInfo.getFormattedAvailableSpace()}")
        println("📑 تعداد آزمون‌ها: ${storageInfo.examsCount}")
        println("📈 تعداد کارنامه‌ها: ${storageInfo.resultsCount}")
        println("📍 مسیر ریشه: ${storageInfo.appRootPath}")
    }
}

// ==================== استفاده در PdfGenerator ====================

/**
 * نسخه به‌روز شده PdfGenerator با مسیرهای استاندارد
 */
class PdfGeneratorWithStorage(
    private val context: Context
) {
    private val storageConfig = StorageConfig

    /**
     * تولید PDF با مسیر استاندارد
     */
    fun generateExamWithPath(
        examTitle: String,
        studentName: String,
        // ... سایر پارامترها
    ): String {
        // تولید PDF
        val pdfContent = "..." // محتوای PDF

        // دریافت مسیر استاندارد
        val filePath = storageConfig.getExamPdfPath(
            context = context,
            examTitle = examTitle,
            studentName = studentName
        )

        // ذخیره فایل
        val file = File(filePath)
        file.writeText(pdfContent)

        return filePath
    }
}