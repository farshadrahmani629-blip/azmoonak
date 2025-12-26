package com.examapp.ui.exam.pdf

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import com.examapp.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * کلاس کمکی برای اشتراک‌گذاری، ذخیره و مدیریت فایل‌های PDF
 */
class PdfShareUtil(private val context: Context) {

    companion object {
        private const val PROVIDER_AUTHORITY = "com.examapp.fileprovider"
        private const val MIME_TYPE_PDF = "application/pdf"

        // پوشه‌های مختلف برای ذخیره فایل‌ها
        private const val SUBDIR_EXAMS = "آزمون‌ها"
        private const val SUBDIR_RESULTS = "نتایج"
        private const val SUBDIR_TEMP = "temp"
    }

    /**
     * اشتراک‌گذاری فایل PDF از طریق Intent
     */
    suspend fun sharePdfFile(
        pdfFile: File,
        title: String = "اشتراک فایل PDF",
        subject: String = "آزمون آموزشی"
    ): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            // کپی فایل به مکان قابل اشتراک (برای APIهای جدید)
            val shareableFile = prepareFileForSharing(pdfFile)

            val shareUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(context, PROVIDER_AUTHORITY, shareableFile)
            } else {
                Uri.fromFile(shareableFile)
            }

            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = MIME_TYPE_PDF
                putExtra(Intent.EXTRA_STREAM, shareUri)
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, "فایل $subject پیوست شده است.")

                // برای واتس‌اپ، تلگرام و دیگر برنامه‌ها
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                // برای ایمیل
                putExtra(Intent.EXTRA_EMAIL, arrayOf<String>())
            }

            // انتخاب برنامه برای اشتراک‌گذاری
            val chooserIntent = Intent.createChooser(shareIntent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(chooserIntent)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "خطا در اشتراک فایل: ${e.message}", Toast.LENGTH_SHORT).show()
            }
            false
        }
    }

    /**
     * ذخیره فایل PDF در گالری/فایل‌های دانلود شده
     */
    suspend fun savePdfToDownloads(
        pdfFile: File,
        displayName: String,
        folderType: String = SUBDIR_EXAMS
    ): Uri? = withContext(Dispatchers.IO) {
        return@withContext try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // روش جدید برای Android 10 به بالا
                saveWithMediaStore(pdfFile, displayName, folderType)
            } else {
                // روش قدیمی
                saveToExternalStorage(pdfFile, displayName, folderType)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "خطا در ذخیره فایل: ${e.message}", Toast.LENGTH_SHORT).show()
            }
            null
        }
    }

    /**
     * باز کردن فایل PDF با برنامه پیش‌فرض
     */
    suspend fun openPdfFile(pdfFile: File): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(context, PROVIDER_AUTHORITY, pdfFile)
            } else {
                Uri.fromFile(pdfFile)
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, MIME_TYPE_PDF)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(intent)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            // اگر برنامه‌ای برای باز کردن PDF نبود، از کاربر بخواهید یکی نصب کند
            if (e is android.content.ActivityNotFoundException) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "برنامه‌ای برای باز کردن PDF یافت نشد. لطفاً یک برنامه PDF خوان نصب کنید.",
                        Toast.LENGTH_LONG
                    ).show()

                    // هدایت به فروشگاه برای نصب PDF خوان
                    openPdfReaderInStore()
                }
            }
            false
        }
    }

    /**
     * چاپ فایل PDF
     */
    suspend fun printPdfFile(pdfFile: File, jobName: String = "آزمون"): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    FileProvider.getUriForFile(context, PROVIDER_AUTHORITY, pdfFile)
                } else {
                    Uri.fromFile(pdfFile)
                }

                val printIntent = Intent(Intent.ACTION_SEND).apply {
                    type = MIME_TYPE_PDF
                    putExtra(Intent.EXTRA_STREAM, uri)

                    // برای پرینتر
                    val printManager = context.getSystemService(Context.PRINT_SERVICE)
                            as android.print.PrintManager

                    if (printManager != null) {
                        val printAdapter = android.print.pdf.PrintedPdfDocument(context, pdfFile)
                        printManager.print(jobName, printAdapter, null)
                    }
                }

                context.startActivity(printIntent)
                true
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "چاپ از Android 4.4 به بالا پشتیبانی می‌شود.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * ارسال PDF از طریق بلوتوث
     */
    suspend fun shareViaBluetooth(pdfFile: File): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val shareableFile = prepareFileForSharing(pdfFile)
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(context, PROVIDER_AUTHORITY, shareableFile)
            } else {
                Uri.fromFile(shareableFile)
            }

            val intent = Intent().apply {
                action = Intent.ACTION_SEND
                type = MIME_TYPE_PDF
                putExtra(Intent.EXTRA_STREAM, uri)

                // مشخص کردن بلوتوث
                setPackage("com.android.bluetooth")

                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(intent)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * ارسال PDF به پرینترهای شبکه
     */
    suspend fun shareToCloudPrint(pdfFile: File): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            // Google Cloud Print (اگر نصب باشد)
            val intent = Intent().apply {
                action = Intent.ACTION_SEND
                type = MIME_TYPE_PDF

                val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    FileProvider.getUriForFile(context, PROVIDER_AUTHORITY, pdfFile)
                } else {
                    Uri.fromFile(pdfFile)
                }

                putExtra(Intent.EXTRA_STREAM, uri)
                setPackage("com.google.android.apps.cloudprint")
            }

            context.startActivity(intent)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * ایجاد لینک اشتراک برای برنامه‌های ایرانی (مثل بله، ایتا، روبیکا)
     */
    suspend fun shareToIranianApps(pdfFile: File, message: String = "فایل آزمون"): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val shareableFile = prepareFileForSharing(pdfFile)
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(context, PROVIDER_AUTHORITY, shareableFile)
            } else {
                Uri.fromFile(shareableFile)
            }

            // لیست برنامه‌های ایرانی معروف
            val iranianApps = listOf(
                "com.elta.elta" to "ایتا",
                "com.rubika.ir" to "روبیکا",
                "org.telegram.messenger" to "تلگرام",
                "com.whatsapp" to "واتس‌اپ",
                "com.instagram.android" to "اینستاگرام",
                "com.nogoon.financial" to "بله"
            )

            var shared = false

            iranianApps.forEach { (packageName, appName) ->
                try {
                    val intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        type = MIME_TYPE_PDF
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(Intent.EXTRA_TEXT, message)
                        setPackage(packageName)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }

                    if (intent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(intent)
                        shared = true
                        return@forEach
                    }
                } catch (e: Exception) {
                    // برنامه نصب نیست یا خطا دارد، ادامه بده
                }
            }

            if (!shared) {
                // اگر هیچکدام از برنامه‌های ایرانی نبودند، از انتخابگر معمولی استفاده کن
                sharePdfFile(pdfFile, "اشتراک فایل", message)
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * مدیریت تاریخچه فایل‌های PDF ذخیره شده
     */
    suspend fun getSavedPdfs(folderType: String = SUBDIR_EXAMS): List<File> = withContext(Dispatchers.IO) {
        return@withContext try {
            val folder = getStorageFolder(folderType)
            if (folder.exists() && folder.isDirectory) {
                folder.listFiles { file ->
                    file.isFile && file.name.endsWith(".pdf", ignoreCase = true)
                }?.toList() ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * حذف فایل PDF قدیمی
     */
    suspend fun deletePdfFile(file: File): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            if (file.exists()) {
                file.delete()
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * پاکسازی فایل‌های موقت
     */
    suspend fun clearTempFiles(): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val tempFolder = getStorageFolder(SUBDIR_TEMP)
            if (tempFolder.exists() && tempFolder.isDirectory) {
                tempFolder.listFiles()?.forEach { it.delete() }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * کپی فایل برای اشتراک‌گذاری ایمن
     */
    private fun prepareFileForSharing(originalFile: File): File {
        val tempDir = getStorageFolder(SUBDIR_TEMP)
        if (!tempDir.exists()) {
            tempDir.mkdirs()
        }

        val tempFile = File(tempDir, "share_${System.currentTimeMillis()}.pdf")

        FileInputStream(originalFile).use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }

        return tempFile
    }

    /**
     * ذخیره فایل با MediaStore (برای API >= 29)
     */
    private fun saveWithMediaStore(
        pdfFile: File,
        displayName: String,
        folderType: String
    ): Uri? {
        val resolver = context.contentResolver

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "$displayName.pdf")
            put(MediaStore.MediaColumns.MIME_TYPE, MIME_TYPE_PDF)
            put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOCUMENTS}/ExamApp/$folderType")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Files.getContentUri("external")
        }

        val uri = resolver.insert(collection, values)

        uri?.let {
            resolver.openOutputStream(it)?.use { outputStream ->
                FileInputStream(pdfFile).use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }

            // اطلاع‌رسانی به سیستم درباره فایل جدید
            val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            mediaScanIntent.data = uri
            context.sendBroadcast(mediaScanIntent)

            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    "فایل در پوشه Documents/ExamApp/$folderType ذخیره شد",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        return uri
    }

    /**
     * ذخیره فایل در حافظه خارجی (برای API < 29)
     */
    private fun saveToExternalStorage(
        pdfFile: File,
        displayName: String,
        folderType: String
    ): Uri? {
        val folder = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            "ExamApp/$folderType"
        )

        if (!folder.exists()) {
            folder.mkdirs()
        }

        // ایجاد نام منحصر به فرد برای فایل
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "${displayName}_$timestamp.pdf"
        val destination = File(folder, fileName)

        // کپی فایل
        FileInputStream(pdfFile).use { input ->
            FileOutputStream(destination).use { output ->
                input.copyTo(output)
            }
        }

        // اطلاع‌رسانی به سیستم
        val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
        mediaScanIntent.data = Uri.fromFile(destination)
        context.sendBroadcast(mediaScanIntent)

        withContext(Dispatchers.Main) {
            Toast.makeText(
                context,
                "فایل در پوشه Documents/ExamApp/$folderType ذخیره شد",
                Toast.LENGTH_LONG
            ).show()
        }

        return Uri.fromFile(destination)
    }

    /**
     * گرفتن مسیر پوشه ذخیره‌سازی
     */
    private fun getStorageFolder(folderType: String): File {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
                "ExamApp/$folderType"
            )
        } else {
            File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                "ExamApp/$folderType"
            )
        }
    }

    /**
     * باز کردن فروشگاه برای نصب برنامه PDF خوان
     */
    private fun openPdfReaderInStore() {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("market://details?id=com.adobe.reader")
                setPackage("com.android.vending")
            }

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                // اگر Play Store نبود، لینک وب را باز کن
                val webIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://play.google.com/store/apps/details?id=com.adobe.reader")
                }
                context.startActivity(webIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * گرفتن اطلاعات فایل PDF
     */
    fun getPdfInfo(pdfFile: File): PdfFileInfo {
        return PdfFileInfo(
            name = pdfFile.name,
            path = pdfFile.absolutePath,
            size = pdfFile.length(),
            lastModified = pdfFile.lastModified(),
            readableSize = formatFileSize(pdfFile.length())
        )
    }

    /**
     * فرمت‌بندی اندازه فایل
     */
    private fun formatFileSize(size: Long): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            size < 1024 * 1024 * 1024 -> "${size / (1024 * 1024)} MB"
            else -> "${size / (1024 * 1024 * 1024)} GB"
        }
    }

    /**
     * کلاس اطلاعات فایل PDF
     */
    data class PdfFileInfo(
        val name: String,
        val path: String,
        val size: Long,
        val lastModified: Long,
        val readableSize: String
    )
}

/**
 * دیالوگ برای انتخاب نوع اشتراک
 */
class PdfShareDialogHelper(private val context: Context) {

    fun showShareOptions(pdfFile: File, onOptionSelected: (ShareOption) -> Unit) {
        // در اینجا می‌توانید یک BottomSheetDialog یا AlertDialog ایجاد کنید
        // که گزینه‌های مختلف اشتراک را نشان دهد
        val options = listOf(
            ShareOption.SHARE_GENERAL,
            ShareOption.SHARE_IRANIAN_APPS,
            ShareOption.SAVE_TO_DEVICE,
            ShareOption.PRINT,
            ShareOption.OPEN
        )

        // ایجاد UI برای نمایش گزینه‌ها
        // (برای سادگی، فعلاً مستقیماً اولین گزینه را انتخاب می‌کنیم)
        onOptionSelected(options.first())
    }

    sealed class ShareOption {
        object SHARE_GENERAL : ShareOption()
        object SHARE_IRANIAN_APPS : ShareOption()
        object SAVE_TO_DEVICE : ShareOption()
        object PRINT : ShareOption()
        object OPEN : ShareOption()
    }
}