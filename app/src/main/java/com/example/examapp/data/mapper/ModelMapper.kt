package com.examapp.data.mapper

import com.examapp.data.models.*
import com.examapp.data.remote.*

/**
 * تبدیل مدل‌های Remote (از سرور) به Local (دیتابیس)
 */

// ExamRemote → Exam
fun ExamRemote.toLocalModel(): Exam {
    return Exam(
        id = this.id,
        examCode = "EXAM-${this.id}",
        title = this.title,
        description = this.description,
        userId = 0, // TODO: از SharedPreferences بگیر
        grade = 0, // TODO: از category بگیر
        subject = this.category?.name ?: "عمومی",
        totalQuestions = this.totalQuestions,
        totalMarks = this.totalQuestions.toFloat(),
        examDuration = this.durationMinutes,
        startTime = null,
        endTime = null,
        status = if (this.isPublished) ExamStatus.ACTIVE else ExamStatus.DRAFT,
        isPublic = this.isFree,
        showAnswersImmediately = true,
        allowRetake = true,
        retakeCount = 0,
        createdAt = this.createdAt,
        updatedAt = null,
        // فیلدهای UI
        isBookmarked = false,
        lastAccessed = System.currentTimeMillis(),
        progress = 0f,
        currentQuestionIndex = 0,
        timeSpent = 0,
        isDownloaded = false,
        downloadPath = null
    ).apply {
        // ذخیره categoryId برای استفاده بعدی
        categoryId = this@toLocalModel.categoryId
    }
}

// QuestionRemote → Question
fun QuestionRemote.toLocalModel(): Question {
    return Question(
        id = this.id,
        fingerprint = "Q-${this.examId}-${this.id}",
        examId = this.examId,
        bookId = 0, // TODO: اگر کتاب‌ها از سرور می‌آیند
        chapterId = 0, // TODO: اگر فصل‌ها از سرور می‌آیند
        pageNumber = null,
        questionType = when (this.questionType.lowercase()) {
            "multiple_choice", "true_false" -> QuestionType.MULTIPLE_CHOICE
            "short_answer" -> QuestionType.SHORT_ANSWER
            "descriptive" -> QuestionType.DESCRIPTIVE
            else -> QuestionType.MULTIPLE_CHOICE
        },
        questionText = this.questionText,
        questionImageUrl = null,
        explanation = this.explanation,
        explanationImageUrl = null,
        difficulty = DifficultyLevel.MEDIUM, // TODO: از سرور بگیر
        bloomLevel = BloomLevel.REMEMBER, // TODO: از سرور بگیر
        correctAnswer = this.correctAnswer,
        marks = this.points.toFloat(),
        timeLimit = null,
        isActive = true,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
        // فیلدهای پاسخ کاربر
        userAnswer = null,
        isAnswered = false,
        isCorrect = null,
        timeSpent = 0,
        isBookmarked = false,
        isFlagged = false,
        sequenceNumber = this.orderIndex,
        lastAccessed = System.currentTimeMillis(),
        answerHistory = null
    ).apply {
        // تبدیل options
        this.options = this@toLocalModel.options.map { it.toLocalModel(this.id) }
    }
}

// OptionRemote → QuestionOption
fun OptionRemote.toLocalModel(questionId: Int): QuestionOption {
    return QuestionOption(
        questionId = questionId,
        optionId = this.letter,
        optionText = this.optionText,
        optionImageUrl = null,
        isCorrect = this.isCorrect,
        optionOrder = when (this.letter.uppercase()) {
            "A" -> 1
            "B" -> 2
            "C" -> 3
            "D" -> 4
            "E" -> 5
            else -> 0
        },
        explanation = null
    )
}

// UserAnswerRemote → StudentAnswer
fun UserAnswerRemote.toLocalModel(examId: Int): StudentAnswer {
    return StudentAnswer(
        id = 0, // AutoGenerate
        examId = examId,
        questionId = this.questionId,
        answer = this.selectedOption ?: this.descriptiveAnswer ?: "",
        isCorrect = false, // بعداً محاسبه می‌شود
        timeSpent = 0
    )
}

// SubmitExamResponse → ExamResult
fun SubmitExamResponse.toLocalModel(examId: Int, userId: Int, examTitle: String): ExamResult {
    return ExamResult(
        id = this.resultId,
        examId = examId,
        examTitle = examTitle,
        studentId = userId,
        score = this.percentage.toFloat(),
        correctAnswers = this.correctAnswers,
        totalQuestions = this.correctAnswers + this.wrongAnswers + this.unanswered,
        submittedAt = System.currentTimeMillis(),
        timeSpent = 0, // TODO: از request بگیر
        feedback = if (this.isPassed) "قبول شدید" else "نیاز به تلاش بیشتر",
        detailedResults = this.answersDetails.map { detail ->
            QuestionResult(
                questionId = detail.questionId,
                questionText = "", // TODO: از context بگیر
                correctAnswer = detail.correctAnswer,
                studentAnswer = detail.userAnswer,
                isCorrect = detail.isCorrect
            )
        },
        isOnline = true
    )
}

// CategoryRemote → Category (اگر model دارید)
fun CategoryRemote.toLocalModel(): Category {
    return Category(
        id = this.id,
        name = this.name,
        description = this.description,
        icon = this.icon,
        examCount = this.examCount
    )
}

/**
 * تبدیل مدل‌های Local (دیتابیس) به Remote (برای ارسال به سرور)
 */

// Exam → ExamRemote
fun Exam.toRemoteModel(): ExamRemote {
    return ExamRemote(
        id = this.id,
        title = this.title,
        description = this.description,
        categoryId = this.categoryId,
        durationMinutes = this.examDuration,
        totalQuestions = this.totalQuestions,
        passingScore = 50, // پیش‌فرض
        price = 0.0, // TODO: از model بگیر
        isPublished = this.status == ExamStatus.ACTIVE,
        isFree = this.isPublic,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        category = null // TODO: اگر نیاز است
    )
}

// Question → QuestionRemote
fun Question.toRemoteModel(): QuestionRemote {
    return QuestionRemote(
        id = this.id,
        examId = this.examId ?: 0,
        questionText = this.questionText,
        questionType = when (this.questionType) {
            QuestionType.MULTIPLE_CHOICE -> "multiple_choice"
            QuestionType.SHORT_ANSWER -> "short_answer"
            QuestionType.DESCRIPTIVE -> "descriptive"
            QuestionType.TRUE_FALSE -> "true_false"
            else -> "multiple_choice"
        },
        points = this.marks.toInt(),
        options = this.options.map { it.toRemoteModel() },
        correctAnswer = this.correctAnswer,
        explanation = this.explanation,
        orderIndex = this.sequenceNumber
    )
}

// QuestionOption → OptionRemote
fun QuestionOption.toRemoteModel(): OptionRemote {
    return OptionRemote(
        id = 0, // TODO: اگر id داریم
        questionId = this.questionId,
        optionText = this.optionText,
        letter = this.optionId,
        isCorrect = this.isCorrect
    )
}

// StudentAnswer → UserAnswerRemote
fun StudentAnswer.toRemoteModel(): UserAnswerRemote {
    return UserAnswerRemote(
        questionId = this.questionId,
        selectedOption = if (this.answer.length == 1) this.answer else null,
        descriptiveAnswer = if (this.answer.length > 1) this.answer else null,
        isFlagged = false // TODO: از model بگیر
    )
}

// ExamResult → ExamResultRemote
fun ExamResult.toRemoteModel(): ExamResultRemote {
    return ExamResultRemote(
        id = this.id,
        examId = this.examId,
        examTitle = this.examTitle,
        score = this.score.toInt(),
        totalScore = this.totalQuestions * 1, // پیش‌فرض هر سوال 1 نمره
        percentage = this.score.toDouble(),
        isPassed = this.score >= 50,
        completedAt = java.time.LocalDateTime.now().toString(),
        durationSeconds = this.timeSpent
    )
}

/**
 * تبدیل لیست‌ها
 */
fun List<ExamRemote>.toLocalModels(): List<Exam> = this.map { it.toLocalModel() }
fun List<QuestionRemote>.toLocalModels(): List<Question> = this.map { it.toLocalModel() }
fun List<CategoryRemote>.toLocalModels(): List<Category> = this.map { it.toLocalModel() }

fun List<Exam>.toRemoteModels(): List<ExamRemote> = this.map { it.toRemoteModel() }
fun List<Question>.toRemoteModels(): List<QuestionRemote> = this.map { it.toRemoteModel() }