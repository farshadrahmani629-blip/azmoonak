// app/src/main/java/com/examapp/data/models/Question.kt
package com.examapp.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.Ignore
import androidx.room.Embedded
import com.google.gson.annotations.SerializedName
import java.util.Date

/**
 * مدل سوال برای ذخیره در دیتابیس و استفاده در آزمون
 */
@Entity(tableName = "questions")
data class Question(
    @PrimaryKey
    @ColumnInfo(name = "id")
    @SerializedName("id")
    val id: Int,

    @ColumnInfo(name = "fingerprint", index = true)
    @SerializedName("fingerprint")
    val fingerprint: String,

    @ColumnInfo(name = "exam_id", index = true)
    @SerializedName("exam_id")
    val examId: Int? = null,

    @ColumnInfo(name = "book_id", index = true)
    @SerializedName("book_id")
    val bookId: Int,

    @ColumnInfo(name = "chapter_id", index = true)
    @SerializedName("chapter_id")
    val chapterId: Int,

    @ColumnInfo(name = "page_number")
    @SerializedName("page_number")
    val pageNumber: Int? = null,

    @ColumnInfo(name = "question_type")
    @SerializedName("question_type")
    val questionType: QuestionType,

    @ColumnInfo(name = "question_text")
    @SerializedName("question_text")
    val questionText: String,

    @ColumnInfo(name = "question_image_url")
    @SerializedName("question_image_url")
    val questionImageUrl: String? = null,

    @ColumnInfo(name = "explanation")
    @SerializedName("explanation")
    val explanation: String? = null,

    @ColumnInfo(name = "explanation_image_url")
    @SerializedName("explanation_image_url")
    val explanationImageUrl: String? = null,

    @ColumnInfo(name = "difficulty")
    @SerializedName("difficulty")
    val difficulty: DifficultyLevel,

    @ColumnInfo(name = "bloom_level")
    @SerializedName("bloom_level")
    val bloomLevel: BloomLevel,

    @ColumnInfo(name = "correct_answer")
    @SerializedName("correct_answer")
    val correctAnswer: String? = null,

    @ColumnInfo(name = "marks")
    @SerializedName("marks")
    val marks: Float,

    @ColumnInfo(name = "time_limit")
    @SerializedName("time_limit")
    val timeLimit: Int? = null,

    @ColumnInfo(name = "is_active")
    @SerializedName("is_active")
    val isActive: Boolean = true,

    @ColumnInfo(name = "created_at")
    @SerializedName("created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    @SerializedName("updated_at")
    val updatedAt: Long = System.currentTimeMillis(),

    // فیلدهای مربوط به پاسخ کاربر (در حین آزمون)
    @ColumnInfo(name = "user_answer")
    var userAnswer: String? = null,

    @ColumnInfo(name = "is_answered")
    var isAnswered: Boolean = false,

    @ColumnInfo(name = "is_correct")
    var isCorrect: Boolean? = null,

    @ColumnInfo(name = "time_spent")
    var timeSpent: Int = 0, // زمان صرف شده بر حسب ثانیه

    @ColumnInfo(name = "is_bookmarked")
    var isBookmarked: Boolean = false,

    @ColumnInfo(name = "is_flagged")
    var isFlagged: Boolean = false,

    @ColumnInfo(name = "sequence_number")
    var sequenceNumber: Int = 0,

    @ColumnInfo(name = "last_accessed")
    var lastAccessed: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "answer_history")
    var answerHistory: String? = null // برای ذخیره تاریخچه پاسخ‌ها (JSON)
) {
    // فیلدهای Ignore (از JSON می‌آیند اما در دیتابیس ذخیره نمی‌شوند)
    @Ignore
    @SerializedName("options")
    var options: List<QuestionOption> = emptyList()

    @Ignore
    @SerializedName("book")
    var book: Book? = null

    @Ignore
    @SerializedName("chapter")
    var chapter: Chapter? = null

    @Ignore
    @SerializedName("created_by")
    val createdBy: Int? = null

    @Ignore
    @SerializedName("verified_by")
    val verifiedBy: Int? = null

    // متدهای کمکی برای UI و منطق برنامه
    val difficultyText: String
        @Ignore
        get() = when (difficulty) {
            DifficultyLevel.EASY -> "آسان"
            DifficultyLevel.MEDIUM -> "متوسط"
            DifficultyLevel.HARD -> "سخت"
            else -> "نامشخص"
        }

    val difficultyColor: Int
        @Ignore
        get() = when (difficulty) {
            DifficultyLevel.EASY -> 0xFF4CAF50.toInt() // سبز
            DifficultyLevel.MEDIUM -> 0xFFFF9800.toInt() // نارنجی
            DifficultyLevel.HARD -> 0xFFF44336.toInt() // قرمز
            else -> 0xFF9E9E9E.toInt() // خاکستری
        }

    val bloomText: String
        @Ignore
        get() = when (bloomLevel) {
            BloomLevel.REMEMBER -> "یادآوری"
            BloomLevel.UNDERSTAND -> "درک"
            BloomLevel.APPLY -> "کاربرد"
            BloomLevel.ANALYZE -> "تحلیل"
            BloomLevel.EVALUATE -> "ارزیابی"
            BloomLevel.CREATE -> "خلاقیت"
            else -> "نامشخص"
        }

    val questionTypeText: String
        @Ignore
        get() = when (questionType) {
            QuestionType.MULTIPLE_CHOICE -> "چندگزینه‌ای"
            QuestionType.SHORT_ANSWER -> "کوتاه‌پاسخ"
            QuestionType.DESCRIPTIVE -> "تشریحی"
            else -> "نامشخص"
        }

    val statusText: String
        @Ignore
        get() = when {
            isAnswered && isCorrect == true -> "صحیح"
            isAnswered && isCorrect == false -> "غلط"
            isAnswered -> "پاسخ داده شده"
            isFlagged -> "علامت‌گذاری شده"
            else -> "بی‌پاسخ"
        }

    val statusColor: Int
        @Ignore
        get() = when {
            isAnswered && isCorrect == true -> 0xFF4CAF50.toInt() // سبز
            isAnswered && isCorrect == false -> 0xFFF44336.toInt() // قرمز
            isAnswered -> 0xFFFF9800.toInt() // نارنجی
            isFlagged -> 0xFF9C27B0.toInt() // بنفش
            else -> 0xFF9E9E9E.toInt() // خاکستری
        }

    // بررسی نوع سوال
    @Ignore
    fun isMultipleChoice(): Boolean = questionType == QuestionType.MULTIPLE_CHOICE

    @Ignore
    fun isShortAnswer(): Boolean = questionType == QuestionType.SHORT_ANSWER

    @Ignore
    fun isDescriptive(): Boolean = questionType == QuestionType.DESCRIPTIVE

    // محاسبه نمره بر اساس پاسخ کاربر
    @Ignore
    fun calculateScore(): Float {
        return if (isAnswered && isCorrect == true) marks else 0f
    }

    // بررسی آیا سوال گزینه دارد
    @Ignore
    fun hasOptions(): Boolean = options.isNotEmpty()

    // دریافت گزینه‌ها به ترتیب
    @Ignore
    fun getSortedOptions(): List<QuestionOption> {
        return options.sortedBy { it.optionOrder }
    }

    // بررسی پاسخ کاربر
    @Ignore
    fun checkAnswer(userInput: String? = this.userAnswer): Boolean {
        if (userInput.isNullOrBlank() || correctAnswer.isNullOrBlank()) {
            return false
        }

        return when (questionType) {
            QuestionType.MULTIPLE_CHOICE -> userInput.trim() == correctAnswer.trim()
            QuestionType.SHORT_ANSWER -> userInput.trim().equals(correctAnswer.trim(), ignoreCase = true)
            QuestionType.DESCRIPTIVE -> {
                // برای سوالات تشریحی، منطق پیچیده‌تری نیاز است
                val similarity = calculateAnswerSimilarity(userInput, correctAnswer!!)
                similarity >= 0.7f // حداقل 70% شباهت
            }
            else -> false
        }
    }

    // محاسبه شباهت پاسخ (برای سوالات تشریحی)
    @Ignore
    private fun calculateAnswerSimilarity(userAnswer: String, correctAnswer: String): Float {
        // پیاده‌سازی ساده - در واقعیت نیاز به الگوریتم پیچیده‌تری دارد
        val userWords = userAnswer.toLowerCase().split("\\s+".toRegex()).toSet()
        val correctWords = correctAnswer.toLowerCase().split("\\s+".toRegex()).toSet()

        val intersection = userWords.intersect(correctWords).size
        val union = userWords.union(correctWords).size

        return if (union > 0) intersection.toFloat() / union else 0f
    }

    // ریست وضعیت سوال
    @Ignore
    fun reset() {
        userAnswer = null
        isAnswered = false
        isCorrect = null
        timeSpent = 0
        isFlagged = false
    }

    // ایجاد کپی از سوال (برای جلوگیری از تغییرات ناخواسته)
    @Ignore
    fun copyForExam(): Question {
        return this.copy(
            userAnswer = null,
            isAnswered = false,
            isCorrect = null,
            timeSpent = 0,
            isFlagged = false,
            sequenceNumber = 0
        ).apply {
            this.options = this@Question.options.map { it.copy() }
        }
    }
}

// Enumها
enum class QuestionType {
    @SerializedName("multiple_choice") MULTIPLE_CHOICE,
    @SerializedName("short_answer") SHORT_ANSWER,
    @SerializedName("descriptive") DESCRIPTIVE,
    @SerializedName("true_false") TRUE_FALSE // اضافه شده برای کامل‌تر شدن
}

enum class DifficultyLevel {
    @SerializedName("easy") EASY,
    @SerializedName("medium") MEDIUM,
    @SerializedName("hard") HARD,
    @SerializedName("very_hard") VERY_HARD // اضافه شده
}

enum class BloomLevel {
    @SerializedName("remember") REMEMBER,
    @SerializedName("understand") UNDERSTAND,
    @SerializedName("apply") APPLY,
    @SerializedName("analyze") ANALYZE,
    @SerializedName("evaluate") EVALUATE,
    @SerializedName("create") CREATE
}

/**
 * مدل گزینه‌های سوال (اگر فایل جداگانه‌ای دارید، می‌توانید آن را حذف کنید)
 */
@Entity(tableName = "question_options")
data class QuestionOption(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "local_id")
    val localId: Long = 0,

    @ColumnInfo(name = "question_id", index = true)
    @SerializedName("question_id")
    val questionId: Int,

    @ColumnInfo(name = "option_id")
    @SerializedName("option_id")
    val optionId: String,

    @ColumnInfo(name = "option_text")
    @SerializedName("option_text")
    val optionText: String,

    @ColumnInfo(name = "option_image_url")
    @SerializedName("option_image_url")
    val optionImageUrl: String? = null,

    @ColumnInfo(name = "is_correct")
    @SerializedName("is_correct")
    val isCorrect: Boolean,

    @ColumnInfo(name = "option_order")
    @SerializedName("order")
    val optionOrder: Int,

    @ColumnInfo(name = "explanation")
    @SerializedName("explanation")
    val explanation: String? = null
) {
    val displayText: String
        @Ignore
        get() = "${optionId.toUpperCase()}. $optionText"
}

/**
 * مدل کتاب (اگر فایل جداگانه‌ای دارید، می‌توانید آن را حذف کنید)
 */
@Entity(tableName = "books")
data class Book(
    @PrimaryKey
    @ColumnInfo(name = "id")
    @SerializedName("id")
    val id: Int,

    @ColumnInfo(name = "title")
    @SerializedName("title")
    val title: String,

    @ColumnInfo(name = "author")
    @SerializedName("author")
    val author: String? = null,

    @ColumnInfo(name = "publisher")
    @SerializedName("publisher")
    val publisher: String? = null,

    @ColumnInfo(name = "publication_year")
    @SerializedName("publication_year")
    val publicationYear: Int? = null,

    @ColumnInfo(name = "isbn")
    @SerializedName("isbn")
    val isbn: String? = null,

    @ColumnInfo(name = "cover_image_url")
    @SerializedName("cover_image_url")
    val coverImageUrl: String? = null,

    @ColumnInfo(name = "description")
    @SerializedName("description")
    val description: String? = null
)

/**
 * مدل فصل (اگر فایل جداگانه‌ای دارید، می‌توانید آن را حذف کنید)
 */
@Entity(tableName = "chapters")
data class Chapter(
    @PrimaryKey
    @ColumnInfo(name = "id")
    @SerializedName("id")
    val id: Int,

    @ColumnInfo(name = "book_id", index = true)
    @SerializedName("book_id")
    val bookId: Int,

    @ColumnInfo(name = "title")
    @SerializedName("title")
    val title: String,

    @ColumnInfo(name = "chapter_number")
    @SerializedName("chapter_number")
    val chapterNumber: Int,

    @ColumnInfo(name = "page_start")
    @SerializedName("page_start")
    val pageStart: Int? = null,

    @ColumnInfo(name = "page_end")
    @SerializedName("page_end")
    val pageEnd: Int? = null,

    @ColumnInfo(name = "description")
    @SerializedName("description")
    val description: String? = null
)