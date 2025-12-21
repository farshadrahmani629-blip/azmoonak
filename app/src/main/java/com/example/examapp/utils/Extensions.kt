package com.examapp.utils

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import java.text.SimpleDateFormat
import java.util.*

/**
 * Extension Functions برای پروژه ExamApp
 */

// ==================== Context Extensions ====================

/**
 * نمایش Toast با متن فارسی
 */
fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

/**
 * گرفتن رنگ از resources
 */
fun Context.getColorRes(colorResId: Int): Int {
    return ContextCompat.getColor(this, colorResId)
}

/**
 * گرفتن Drawable از resources
 */
fun Context.getDrawableRes(drawableResId: Int): Drawable? {
    return ContextCompat.getDrawable(this, drawableResId)
}

/**
 * نمایش کیبورد
 */
fun Context.showKeyboard(view: View) {
    val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
}

/**
 * مخفی کردن کیبورد
 */
fun Context.hideKeyboard(view: View) {
    val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
}

/**
 * تبدیل dp به px
 */
fun Context.dpToPx(dp: Float): Float {
    return dp * resources.displayMetrics.density
}

/**
 * تبدیل px به dp
 */
fun Context.pxToDp(px: Float): Float {
    return px / resources.displayMetrics.density
}

// ==================== View Extensions ====================

/**
 * نمایش View
 */
fun View.show() {
    visibility = View.VISIBLE
}

/**
 * مخفی کردن View
 */
fun View.hide() {
    visibility = View.GONE
}

/**
 * نیمه شفاف کردن View
 */
fun View.invisible() {
    visibility = View.INVISIBLE
}

/**
 * تغییر visibility بر اساس boolean
 */
fun View.setVisible(isVisible: Boolean) {
    visibility = if (isVisible) View.VISIBLE else View.GONE
}

/**
 * تغییر enabled state با animation
 */
fun View.setEnabledWithAnimation(enabled: Boolean) {
    isEnabled = enabled
    alpha = if (enabled) 1.0f else 0.5f
}

/**
 * تنظیم کلیک listener با debounce
 */
fun View.setSafeOnClickListener(debounceTime: Long = 600L, action: (View) -> Unit) {
    setOnClickListener(object : View.OnClickListener {
        private var lastClickTime: Long = 0

        override fun onClick(v: View) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime > debounceTime) {
                lastClickTime = currentTime
                action(v)
            }
        }
    })
}

// ==================== ImageView Extensions ====================

/**
 * بارگذاری تصویر با Glide
 */
fun ImageView.loadImage(url: String?, placeholderResId: Int = 0) {
    if (url.isNullOrEmpty()) {
        if (placeholderResId != 0) {
            setImageResource(placeholderResId)
        }
        return
    }

    try {
        Glide.with(context)
            .load(url)
            .apply(RequestOptions().centerCrop())
            .apply {
                if (placeholderResId != 0) {
                    placeholder(placeholderResId)
                    error(placeholderResId)
                }
            }
            .into(this)
    } catch (e: Exception) {
        e.printStackTrace()
        if (placeholderResId != 0) {
            setImageResource(placeholderResId)
        }
    }
}

/**
 * بارگذاری تصویر از drawable
 */
fun ImageView.loadDrawable(drawableResId: Int) {
    try {
        setImageResource(drawableResId)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

// ==================== String Extensions ====================

/**
 * بررسی اینکه آیا رشته خالی یا null است
 */
fun String?.isNotNullOrEmpty(): Boolean {
    return !this.isNullOrEmpty()
}

/**
 * مخفی کردن بخشی از متن (برای اطلاعات حساس)
 */
fun String.mask(start: Int = 0, end: Int = length, maskChar: Char = '*'): String {
    if (start > end || start < 0 || end > length) {
        return this
    }

    val maskedPart = maskChar.toString().repeat(end - start)
    return this.replaceRange(start, end, maskedPart)
}

/**
 * فرمت کردن شماره تلفن فارسی
 */
fun String.formatPersianPhone(): String {
    return this.replace(" ", "")
        .replace("-", "")
        .replace("+98", "0")
        .chunked(4)
        .joinToString(" ")
}

/**
 * تبدیل اعداد انگلیسی به فارسی
 */
fun String.toPersianDigits(): String {
    val englishDigits = "0123456789"
    val persianDigits = "۰۱۲۳۴۵۶۷۸۹"

    return this.map { char ->
        val index = englishDigits.indexOf(char)
        if (index != -1) persianDigits[index] else char
    }.joinToString("")
}

/**
 * تبدیل اعداد فارسی به انگلیسی
 */
fun String.toEnglishDigits(): String {
    val persianDigits = "۰۱۲۳۴۵۶۷۸۹"
    val englishDigits = "0123456789"

    return this.map { char ->
        val index = persianDigits.indexOf(char)
        if (index != -1) englishDigits[index] else char
    }.joinToString("")
}

/**
 * خلاصه کردن متن با اضافه کردن ...
 */
fun String.truncate(maxLength: Int): String {
    return if (length > maxLength) {
        substring(0, maxLength) + "..."
    } else {
        this
    }
}

// ==================== Number Extensions ====================

/**
 * فرمت کردن عدد با جداکننده هزارگان فارسی
 */
fun Int.formatWithSeparator(): String {
    return String.format(Locale("fa"), "%,d", this)
}

/**
 * فرمت کردن عدد اعشاری با جداکننده هزارگان
 */
fun Double.formatDecimal(digits: Int = 2): String {
    return String.format(Locale("fa"), "%,.${digits}f", this)
}

/**
 * تبدیل درصد به متن فارسی
 */
fun Float.toPersianPercent(): String {
    return "${String.format(Locale("fa"), "%.1f", this)}%"
}

/**
 * تبدیل دقیقه به متن زمان فارسی
 */
fun Int.toPersianTime(): String {
    return when {
        this < 60 -> "$this دقیقه"
        this % 60 == 0 -> "${this / 60} ساعت"
        else -> "${this / 60} ساعت و ${this % 60} دقیقه"
    }
}

// ==================== Date Extensions ====================

/**
 * فرمت کردن تاریخ به فارسی
 */
fun Date.toPersianDate(): String {
    val calendar = Calendar.getInstance()
    calendar.time = this

    val persianCalendar = java.util.GregorianCalendar(
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    val year = persianCalendar.get(Calendar.YEAR)
    val month = persianCalendar.get(Calendar.MONTH) + 1
    val day = persianCalendar.get(Calendar.DAY_OF_MONTH)

    val monthNames = listOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    )

    return "$day ${monthNames.getOrNull(month - 1) ?: month} $year"
}

/**
 * فرمت کردن زمان به فارسی
 */
fun Date.toPersianTime(): String {
    val dateFormat = SimpleDateFormat("HH:mm", Locale("fa"))
    return dateFormat.format(this)
}

/**
 * فرمت کردن تاریخ و زمان به فارسی
 */
fun Date.toPersianDateTime(): String {
    return "${toPersianDate()} - ${toPersianTime()}"
}

/**
 * محاسبه اختلاف زمان تا الآن به فارسی
 */
fun Date.timeAgo(): String {
    val diff = Date().time - this.time
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        days > 30 -> "${days / 30} ماه پیش"
        days > 7 -> "${days / 7} هفته پیش"
        days > 0 -> "$days روز پیش"
        hours > 0 -> "$hours ساعت پیش"
        minutes > 0 -> "$minutes دقیقه پیش"
        else -> "همین الان"
    }
}

// ==================== Fragment Extensions ====================

/**
 * نمایش Toast در Fragment
 */
fun Fragment.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    requireContext().showToast(message, duration)
}

/**
 * مخفی کردن کیبورد در Fragment
 */
fun Fragment.hideKeyboard() {
    val view = requireView()
    requireContext().hideKeyboard(view)
}

/**
 * نمایش Dialog ساده
 */
fun Fragment.showSimpleDialog(
    title: String,
    message: String,
    positiveText: String = "باشه",
    negativeText: String? = null,
    onPositiveClick: (() -> Unit)? = null,
    onNegativeClick: (() -> Unit)? = null
) {
    android.app.AlertDialog.Builder(requireContext())
        .setTitle(title)
        .setMessage(message)
        .setPositiveButton(positiveText) { dialog, _ ->
            dialog.dismiss()
            onPositiveClick?.invoke()
        }
        .apply {
            if (negativeText != null) {
                setNegativeButton(negativeText) { dialog, _ ->
                    dialog.dismiss()
                    onNegativeClick?.invoke()
                }
            }
        }
        .show()
}

// ==================== List Extensions ====================

/**
 * تبدیل لیست به متن با جداکننده
 */
fun <T> List<T>.joinToStringPersian(separator: String = "، "): String {
    return this.joinToString(separator) { it.toString() }
}

/**
 * تقسیم لیست به بخش‌های کوچکتر
 */
fun <T> List<T>.chunkedList(size: Int): List<List<T>> {
    return this.chunked(size)
}

/**
 * گرفتن آیتم تصادفی از لیست
 */
fun <T> List<T>.getRandomItem(): T? {
    return if (isNotEmpty()) this[Random().nextInt(size)] else null
}

/**
 * بررسی اینکه آیا اندکس در محدوده معتبر است
 */
fun <T> List<T>.isValidIndex(index: Int): Boolean {
    return index in 0 until size
}

// ==================== Boolean Extensions ====================

/**
 * تبدیل boolean به متن فارسی (بله/خیر)
 */
fun Boolean.toPersianText(): String {
    return if (this) "بله" else "خیر"
}

/**
 * تبدیل boolean به اعداد (1/0)
 */
fun Boolean.toInt(): Int {
    return if (this) 1 else 0
}

// ==================== SharedPreferences Extensions ====================

/**
 * ذخیره boolean در SharedPreferences (با استفاده از کلاس SharedPrefs)
 */
fun SharedPrefs.saveBooleanSafe(key: String, value: Boolean) {
    try {
        saveBoolean(key, value)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 * ذخیره string در SharedPreferences (با استفاده از کلاس SharedPrefs)
 */
fun SharedPrefs.saveStringSafe(key: String, value: String) {
    try {
        saveString(key, value)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

// ==================== ExamApp Specific Extensions ====================

/**
 * فرمت کردن نمره آزمون
 */
fun Float.formatScore(): String {
    return String.format(Locale("fa"), "%.1f", this)
}

/**
 * گرفتن رنگ بر اساس نمره
 */
fun Float.getScoreColor(context: Context): Int {
    return when {
        this >= 90 -> context.getColorRes(R.color.green)
        this >= 75 -> context.getColorRes(R.color.blue)
        this >= 50 -> context.getColorRes(R.color.orange)
        else -> context.getColorRes(R.color.red)
    }
}

/**
 * گرفتن متن عملکرد بر اساس نمره
 */
fun Float.getPerformanceText(): String {
    return when {
        this >= 90 -> "عالی 🎉"
        this >= 75 -> "خوب 👍"
        this >= 50 -> "متوسط 😊"
        else -> "نیاز به تلاش 📚"
    }
}

/**
 * فرمت کردن زمان آزمون (میلی‌ثانیه به دقیقه:ثانیه)
 */
fun Long.formatExamTime(): String {
    val minutes = this / (1000 * 60)
    val seconds = (this / 1000) % 60
    return String.format("%02d:%02d", minutes, seconds)
}

/**
 * محاسبه درصد پیشرفت آزمون
 */
fun calculateProgress(current: Int, total: Int): Float {
    return if (total > 0) {
        (current.toFloat() / total) * 100
    } else {
        0f
    }
}

/**
 * بررسی اعتبار ایمیل
 */
fun String.isValidEmail(): Boolean {
    return android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()
}

/**
 * بررسی اعتبار شماره تلفن ایرانی
 */
fun String.isValidIranianPhone(): Boolean {
    val pattern = Regex("^(\\+98|0)?9\\d{9}$")
    return pattern.matches(this)
}