// app/src/main/java/com/examapp/data/models/ExamConfig.kt
package com.examapp.data.models

data class ExamConfig(
    val grade: Int = 1,
    val subject: String = "",
    val pageFrom: Int = 1,
    val pageTo: Int = 100,
    val difficulty: DifficultyLevel = DifficultyLevel.MEDIUM, // استفاده از enum
    val questionCount: Int = 20,
    val questionTypes: List<QuestionType> = listOf(QuestionType.MULTIPLE_CHOICE), // انواع سوالات
    val bloomLevels: List<BloomLevel> = emptyList(), // فقط برای پرو
    val isProVersion: Boolean = false,
    val bookId: Int? = null, // کتاب خاص
    val chapterIds: List<Int> = emptyList(), // فصل‌های خاص
    val timeLimit: Int = 60, // زمان آزمون به دقیقه
    val shuffleQuestions: Boolean = true, // تصادفی کردن سوالات
    val showResultsImmediately: Boolean = false, // نمایش فوری نتایج
    val allowRetake: Boolean = true // اجازه آزمون مجدد
) {
    // متدهای کمکی
    fun isValid(): Boolean {
        return grade in 1..6 &&
                subject.isNotBlank() &&
                pageFrom <= pageTo &&
                questionCount in 5..50 &&
                timeLimit in 10..180
    }

    fun getDifficultyValue(): Int {
        return when (difficulty) {
            DifficultyLevel.EASY -> 1
            DifficultyLevel.MEDIUM -> 2
            DifficultyLevel.HARD -> 3
        }
    }

    fun getPageRange(): IntRange = pageFrom..pageTo

    // برای نمایش در UI
    fun getSummary(): String {
        return "پایه $grade - $subject - $questionCount سوال"
    }
}

// Extension برای تبدیل به ExamRequest
fun ExamConfig.toExamRequest(userId: Int): ExamRequest {
    return ExamRequest(
        userId = userId,
        grade = grade,
        subject = subject,
        bookId = bookId,
        pageRangeStart = pageFrom,
        pageRangeEnd = pageTo,
        questionTypes = questionTypes,
        difficultyLevels = listOf(difficulty),
        bloomLevels = bloomLevels,
        totalQuestions = questionCount,
        examDuration = timeLimit,
        isRandom = shuffleQuestions,
        showAnswersImmediately = showResultsImmediately,
        allowRetake = allowRetake
    )
}