// app/src/main/java/com/examapp/ui/main/MainActivity.kt
package com.examapp.ui.main

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.examapp.R
import com.examapp.core.managers.FreeTrialManager
import com.examapp.core.managers.VersionManager
import com.examapp.data.models.ExamConfig
import com.examapp.ui.exam.ExamActivity
import com.examapp.ui.exam.ExamCreationActivity
import com.examapp.ui.profile.ProfileActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var freeTrialManager: FreeTrialManager

    @Inject
    lateinit var versionManager: VersionManager

    // تعریف viewها
    private lateinit var spinnerGrade: Spinner
    private lateinit var spinnerSubject: Spinner
    private lateinit var spinnerDifficulty: Spinner
    private lateinit var etPageFrom: EditText
    private lateinit var etPageTo: EditText
    private lateinit var btnStartExam: Button
    private lateinit var btnCreatePDF: Button
    private lateinit var btnAdvancedSettings: Button
    private lateinit var btnProfile: Button
    private lateinit var tvVersionStatus: TextView
    private lateinit var tvWelcome: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // وصل کردن viewها
        setupViews()

        // پر کردن spinnerها
        setupSpinners()

        // تنظیم دکمه‌ها
        setupButtons()

        // نمایش وضعیت
        updateVersionStatus()

        // نمایش پیام خوشامد
        showWelcomeMessage()
    }

    /**
     * پیدا کردن viewها از layout
     */
    private fun setupViews() {
        spinnerGrade = findViewById(R.id.spinnerGrade)
        spinnerSubject = findViewById(R.id.spinnerSubject)
        spinnerDifficulty = findViewById(R.id.spinnerDifficulty)
        etPageFrom = findViewById(R.id.etPageFrom)
        etPageTo = findViewById(R.id.etPageTo)
        btnStartExam = findViewById(R.id.btnStartExam)
        btnCreatePDF = findViewById(R.id.btnCreatePDF)
        btnAdvancedSettings = findViewById(R.id.btnAdvancedSettings)
        btnProfile = findViewById(R.id.btnProfile)
        tvVersionStatus = findViewById(R.id.tvVersionStatus)
        tvWelcome = findViewById(R.id.tvWelcome)
    }

    /**
     * تنظیم داده‌های spinnerها
     */
    private fun setupSpinners() {
        // پایه‌های تحصیلی (۱ تا ۱۲)
        val grades = (1..12).map { "پایه $it" }
        val gradeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, grades)
        gradeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerGrade.adapter = gradeAdapter
        spinnerGrade.setSelection(5) // انتخاب پایه ۶ به صورت پیش‌فرض

        // دروس
        val subjects = listOf(
            "ریاضی",
            "علوم تجربی",
            "فارسی",
            "مطالعات اجتماعی",
            "هدیه‌های آسمان",
            "قرآن",
            "انگلیسی",
            "هنر",
            "ورزش",
            "کار و فناوری"
        )
        val subjectAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, subjects)
        subjectAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSubject.adapter = subjectAdapter

        // سطح دشواری
        val difficulties = listOf("آسان", "متوسط", "سخت", "ترکیبی")
        val difficultyAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, difficulties)
        difficultyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDifficulty.adapter = difficultyAdapter
    }

    /**
     * تنظیم رویداد کلیک دکمه‌ها
     */
    private fun setupButtons() {
        btnStartExam.setOnClickListener {
            startExam()
        }

        btnCreatePDF.setOnClickListener {
            createPDF()
        }

        btnAdvancedSettings.setOnClickListener {
            openAdvancedSettings()
        }

        btnProfile.setOnClickListener {
            openProfile()
        }
    }

    /**
     * شروع آزمون
     */
    private fun startExam() {
        // بررسی اعتبارسنجی
        if (!validateInputs()) {
            return
        }

        // بررسی نسخه و آزمون‌های رایگان
        val isPro = versionManager.isProVersion()
        val canTakeFree = freeTrialManager.canTakeFreeExam()

        if (!isPro && !canTakeFree) {
            // نمایش تبلیغ یا درخواست ارتقاء
            showUpgradeOrAdDialog()
            return
        }

        // گرفتن تنظیمات
        val config = createExamConfig()

        // نمایش تایید نهایی
        showExamConfirmationDialog(config, isPro)
    }

    /**
     * اعتبارسنجی ورودی‌ها
     */
    private fun validateInputs(): Boolean {
        val pageFrom = etPageFrom.text.toString().toIntOrNull()
        val pageTo = etPageTo.text.toString().toIntOrNull()

        // بررسی صفحات
        if (pageFrom != null && pageTo != null) {
            if (pageFrom > pageTo) {
                etPageFrom.error = "صفحه شروع باید کوچکتر یا مساوی صفحه پایان باشد"
                etPageTo.error = "صفحه پایان باید بزرگتر یا مساوی صفحه شروع باشد"
                return false
            }

            if (pageFrom < 1) {
                etPageFrom.error = "شماره صفحه باید حداقل ۱ باشد"
                return false
            }
        }

        // پاک کردن خطاها در صورت معتبر بودن
        etPageFrom.error = null
        etPageTo.error = null

        return true
    }

    /**
     * نمایش دیالوگ ارتقاء یا تبلیغ
     */
    private fun showUpgradeOrAdDialog() {
        val remainingExams = freeTrialManager.getRemainingFreeExams()

        AlertDialog.Builder(this)
            .setTitle("محدودیت نسخه رایگان")
            .setMessage("""
                شما از نسخه رایگان استفاده می‌کنید.
                آزمون‌های رایگان باقی‌مانده: $remainingExams
                
                گزینه‌ها:
                ۱. مشاهده تبلیغ برای استفاده رایگان
                ۲. ارتقاء به نسخه پرو (دسترسی نامحدود)
                ۳. بازگشت
            """.trimIndent())
            .setPositiveButton("مشاهده تبلیغ") { _, _ ->
                showAdAndStartExam()
            }
            .setNegativeButton("ارتقاء به نسخه پرو") { _, _ ->
                openUpgradeScreen()
            }
            .setNeutralButton("بازگشت", null)
            .show()
    }

    /**
     * نمایش دیالوگ تایید آزمون
     */
    private fun showExamConfirmationDialog(config: ExamConfig, isPro: Boolean) {
        val remainingExams = if (!isPro) {
            "\nآزمون‌های رایگان باقی‌مانده: ${freeTrialManager.getRemainingFreeExams()}"
        } else {
            ""
        }

        AlertDialog.Builder(this)
            .setTitle("تایید شروع آزمون")
            .setMessage("""
                مشخصات آزمون:
                • پایه: ${config.grade}
                • درس: ${config.subject}
                • سطح دشواری: ${config.difficultyName}
                • محدوده صفحات: ${config.pageFrom} تا ${config.pageTo}
                • تعداد سوالات: ${config.questionCount}
                
                آیا مایل به شروع آزمون هستید؟
                $remainingExams
            """.trimIndent())
            .setPositiveButton("شروع آزمون") { _, _ ->
                proceedToExam(config, isPro)
            }
            .setNegativeButton("ویرایش", null)
            .show()
    }

    /**
     * ادامه به صفحه آزمون
     */
    private fun proceedToExam(config: ExamConfig, isPro: Boolean) {
        // ثبت آزمون (برای نسخه رایگان)
        if (!isPro) {
            freeTrialManager.recordExamTaken()
            updateVersionStatus()
        }

        // رفتن به صفحه آزمون
        val intent = Intent(this, ExamActivity::class.java).apply {
            putExtra("EXAM_CONFIG", config.toBundle())
        }
        startActivity(intent)

        // نمایش پیام موفقیت
        Toast.makeText(this, "آزمون در حال بارگذاری...", Toast.LENGTH_SHORT).show()
    }

    /**
     * ساخت PDF
     */
    private fun createPDF() {
        if (!validateInputs()) {
            return
        }

        val config = createExamConfig()

        AlertDialog.Builder(this)
            .setTitle("ساخت PDF")
            .setMessage("""
                PDF با مشخصات زیر ساخته خواهد شد:
                
                درس: ${config.subject}
                پایه: ${config.grade}
                محدوده صفحات: ${config.pageFrom}-${config.pageTo}
                تعداد سوالات: ${config.questionCount}
                سطح دشواری: ${config.difficultyName}
                
                آیا مایل به ادامه هستید؟
            """.trimIndent())
            .setPositiveButton("ساخت PDF") { _, _ ->
                generatePDF(config)
            }
            .setNegativeButton("لغو", null)
            .show()
    }

    /**
     * تولید PDF
     */
    private fun generatePDF(config: ExamConfig) {
        // نمایش progress
        val progressDialog = AlertDialog.Builder(this)
            .setTitle("در حال ساخت PDF")
            .setMessage("لطفاً منتظر بمانید...")
            .setCancelable(false)
            .create()
        progressDialog.show()

        lifecycleScope.launch {
            try {
                // شبیه‌سازی ساخت PDF
                kotlinx.coroutines.delay(2000)

                progressDialog.dismiss()

                // نمایش موفقیت
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("PDF ساخته شد")
                    .setMessage("فایل PDF با موفقیت ایجاد شد.\n\nفایل در پوشه Downloads ذخیره شد.")
                    .setPositiveButton("مشاهده فایل") { _, _ ->
                        // TODO: باز کردن فایل PDF
                        Toast.makeText(this@MainActivity, "در حال باز کردن فایل...", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("باشه", null)
                    .show()

            } catch (e: Exception) {
                progressDialog.dismiss()
                Toast.makeText(this@MainActivity, "خطا در ساخت PDF: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * باز کردن تنظیمات پیشرفته
     */
    private fun openAdvancedSettings() {
        val intent = Intent(this, ExamCreationActivity::class.java)
        startActivity(intent)
    }

    /**
     * باز کردن پروفایل
     */
    private fun openProfile() {
        val intent = Intent(this, ProfileActivity::class.java)
        startActivity(intent)
    }

    /**
     * نمایش تبلیغ و شروع آزمون
     */
    private fun showAdAndStartExam() {
        // نمایش دیالوگ تبلیغ
        val adDialog = AlertDialog.Builder(this)
            .setTitle("در حال نمایش تبلیغ")
            .setMessage("لطفاً ۵ ثانیه منتظر بمانید...")
            .setCancelable(false)
            .create()
        adDialog.show()

        // شبیه‌سازی نمایش تبلیغ
        lifecycleScope.launch {
            kotlinx.coroutines.delay(5000)
            adDialog.dismiss()

            // بعد از اتمام تبلیغ
            freeTrialManager.recordExamTaken()
            updateVersionStatus()

            // شروع آزمون
            val config = createExamConfig()
            proceedToExam(config, isPro = false)
        }
    }

    /**
     * باز کردن صفحه ارتقاء
     */
    private fun openUpgradeScreen() {
        // TODO: رفتن به صفحه ارتقاء نسخه
        Toast.makeText(this, "صفحه ارتقاء به زودی اضافه می‌شود", Toast.LENGTH_SHORT).show()
    }

    /**
     * ساخت شیء ExamConfig از تنظیمات کاربر
     */
    private fun createExamConfig(): ExamConfig {
        val pageFrom = etPageFrom.text.toString().toIntOrNull() ?: 1
        val pageTo = etPageTo.text.toString().toIntOrNull() ?: 100

        return ExamConfig(
            id = System.currentTimeMillis().toString(),
            grade = spinnerGrade.selectedItemPosition + 1,
            subject = spinnerSubject.selectedItem.toString(),
            pageFrom = pageFrom,
            pageTo = pageTo,
            difficulty = spinnerDifficulty.selectedItemPosition,
            questionCount = 20,
            isProVersion = versionManager.isProVersion(),
            difficultyName = spinnerDifficulty.selectedItem.toString(),
            createdAt = System.currentTimeMillis()
        )
    }

    /**
     * به‌روزرسانی وضعیت نسخه در صفحه
     */
    private fun updateVersionStatus() {
        val status = if (versionManager.isProVersion()) {
            "✅ نسخه پرو - دسترسی نامحدود"
        } else {
            val remaining = freeTrialManager.getRemainingFreeExams()
            "📱 نسخه رایگان - $remaining آزمون باقی‌مانده"
        }
        tvVersionStatus.text = status

        // به‌روزرسانی وضعیت دکمه‌ها
        updateButtonsState()
    }

    /**
     * به‌روزرسانی وضعیت دکمه‌ها
     */
    private fun updateButtonsState() {
        val isPro = versionManager.isProVersion()
        val canTakeFree = freeTrialManager.canTakeFreeExam()

        btnStartExam.isEnabled = isPro || canTakeFree
        btnCreatePDF.isEnabled = isPro

        if (!isPro && !canTakeFree) {
            btnStartExam.text = "مشاهده تبلیغ برای آزمون"
        } else {
            btnStartExam.text = "شروع آزمون"
        }
    }

    /**
     * نمایش پیام خوشامد
     */
    private fun showWelcomeMessage() {
        // TODO: دریافت نام کاربر از SharedPreferences یا API
        val userName = "کاربر گرامی"
        tvWelcome.text = "سلام $userName! 👋"

        // نمایش tooltip برای نسخه رایگان
        if (!versionManager.isProVersion()) {
            Toast.makeText(
                this,
                "برای دسترسی نامحدود به نسخه پرو ارتقاء پیدا کنید",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * هنگام بازگشت به صفحه
     */
    override fun onResume() {
        super.onResume()
        updateVersionStatus()
    }
}

// Extension function برای تبدیل ExamConfig به Bundle
fun ExamConfig.toBundle(): Bundle {
    return Bundle().apply {
        putString("id", this@toBundle.id)
        putInt("grade", this@toBundle.grade)
        putString("subject", this@toBundle.subject)
        putInt("pageFrom", this@toBundle.pageFrom)
        putInt("pageTo", this@toBundle.pageTo)
        putInt("difficulty", this@toBundle.difficulty)
        putInt("questionCount", this@toBundle.questionCount)
        putBoolean("isProVersion", this@toBundle.isProVersion)
        putString("difficultyName", this@toBundle.difficultyName)
        putLong("createdAt", this@toBundle.createdAt)
    }
}