// app/src/main/java/com/examapp/data/models/ExamFilter.kt
package com.examapp.data.models

import com.google.gson.annotations.SerializedName

data class ExamFilter(
    val grade: Int = 3,
    val subject: String = "ریاضی",
    val fromPage: Int? = null,
    val toPage: Int? = null,
    val difficulty: DifficultyLevel? = null,
    val bloomLevel: BloomLevel? = null,
    val questionCount: Int = 20,
    val examType: ExamType = ExamType.ONLINE,
    val includeDiagrams: Boolean = false,
    val questionTypes: List<QuestionType> = listOf(
        QuestionType.MULTIPLE_CHOICE,
        QuestionType.SHORT_ANSWER
    ),
    val bookId: Int? = null,
    val chapterIds: List<Int> = emptyList()
) {
    enum class ExamType {
        @SerializedName("online") ONLINE,
        @SerializedName("pdf") PDF,
        @SerializedName("mixed") MIXED
    }

    fun isValid(): Boolean {
        return grade in 1..6 &&
                questionCount in 1..50 &&
                (fromPage == null || toPage == null || fromPage <= toPage) &&
                subject.isNotBlank()
    }

    fun toDisplayText(): String {
        val difficultyText = difficulty?.let { " - ${it.getPersianName()}" } ?: ""
        val bloomText = bloomLevel?.let { " - ${it.getPersianName()}" } ?: ""
        return "پایه $grade - $subject - $questionCount سوال$difficultyText$bloomText"
    }

    fun toExamConfig(isProVersion: Boolean = false): ExamConfig {
        return ExamConfig(
            grade = grade,
            subject = subject,
            pageFrom = fromPage ?: 1,
            pageTo = toPage ?: 100,
            difficulty = difficulty ?: DifficultyLevel.MEDIUM,
            questionCount = questionCount,
            questionTypes = questionTypes,
            bloomLevels = if (isProVersion) listOfNotNull(bloomLevel) else emptyList(),
            isProVersion = isProVersion,
            bookId = bookId,
            chapterIds = chapterIds,
            shuffleQuestions = true,
            showResultsImmediately = examType == ExamType.ONLINE
        )
    }

    companion object {
        fun getDefaultFilters(): List<ExamFilter> {
            return listOf(
                ExamFilter(grade = 3, subject = "ریاضی"),
                ExamFilter(grade = 3, subject = "فارسی"),
                ExamFilter(grade = 3, subject = "علوم"),
                ExamFilter(grade = 4, subject = "ریاضی"),
                ExamFilter(grade = 4, subject = "فارسی"),
                ExamFilter(grade = 4, subject = "علوم"),
                ExamFilter(grade = 5, subject = "ریاضی"),
                ExamFilter(grade = 5, subject = "فارسی"),
                ExamFilter(grade = 5, subject = "علوم")
            )
        }
    }
}

// Extension functions برای نمایش فارسی
fun DifficultyLevel.getPersianName(): String {
    return when (this) {
        DifficultyLevel.EASY -> "آسان"
        DifficultyLevel.MEDIUM -> "متوسط"
        DifficultyLevel.HARD -> "سخت"
    }
}

fun BloomLevel.getPersianName(): String {
    return when (this) {
        BloomLevel.REMEMBER -> "یادآوری"
        BloomLevel.UNDERSTAND -> "درک"
        BloomLevel.APPLY -> "کاربرد"
        BloomLevel.ANALYZE -> "تحلیل"
        BloomLevel.EVALUATE -> "ارزیابی"
        BloomLevel.CREATE -> "خلاقیت"
    }
}

fun QuestionType.getPersianName(): String {
    return when (this) {
        QuestionType.MULTIPLE_CHOICE -> "چندگزینه‌ای"
        QuestionType.SHORT_ANSWER -> "کوتاه‌پاسخ"
        QuestionType.DESCRIPTIVE -> "تشریحی"
    }
}