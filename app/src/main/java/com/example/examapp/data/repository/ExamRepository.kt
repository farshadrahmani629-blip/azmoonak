package com.examapp.data.repository

import com.examapp.data.remote.ApiClient
import com.examapp.data.remote.ExamApiService
import com.examapp.data.models.*
import com.examapp.data.database.ExamDatabase
import com.examapp.data.local.AppPreferences
import com.examapp.data.pdf.PdfGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExamRepository @Inject constructor(
    private val apiClient: ApiClient,
    private val database: ExamDatabase,
    private val preferences: AppPreferences,
    private val pdfGenerator: PdfGenerator
) {

    // ==================== سرویس API ====================
    private val examApiService: ExamApiService get() = apiClient.getActiveService()

    // ==================== تشخیص نسخه ====================
    fun isLiteVersion(): Boolean = preferences.isLiteVersion()
    fun isProVersion(): Boolean = !preferences.isLiteVersion() && preferences.isProPurchased()

    // ==================== آزمون‌ها ====================

    suspend fun getExams(
        page: Int = 1,
        limit: Int = 20,
        search: String? = null,
        category: String? = null
    ): Result<List<ExamRemote>> = withContext(Dispatchers.IO) {
        try {
            // اگر Pro هست و آفلاین کار می‌کنه
            if (isProVersion() && !isOnline()) {
                val localExams = database.examDao().getAllExams()
                return@withContext Result.success(localExams.map { it.toRemoteModel() })
            }

            // درخواست از سرور
            val response = examApiService.getExams(page, limit, search, category)

            if (response.isSuccessful()) {
                val paginatedData = response.data?.data ?: emptyList()

                // ذخیره در دیتابیس
                paginatedData.forEach { remoteExam ->
                    database.examDao().insertExam(remoteExam.toLocalModel())
                }

                Result.success(paginatedData)
            } else {
                Result.failure(Exception(response.getErrorMessage()))
            }
        } catch (e: Exception) {
            Timber.e(e, "خطا در دریافت آزمون‌ها")

            // Fallback: از دیتابیس بخون
            if (isProVersion()) {
                val localExams = database.examDao().getAllExams()
                Result.success(localExams.map { it.toRemoteModel() })
            } else {
                Result.failure(Exception("اتصال اینترنت برقرار نیست. نسخه Lite نیاز به اینترنت دارد."))
            }
        }
    }

    suspend fun getExamById(examId: Int): Result<ExamRemote> = withContext(Dispatchers.IO) {
        try {
            // اول از دیتابیس بگیر
            val localExam = database.examDao().getExamById(examId)
            if (localExam != null) {
                return@withContext Result.success(localExam.toRemoteModel())
            }

            // اگر نبود و نسخه Lite هست یا می‌خوایم آپدیت کنیم
            if (!isProVersion() || isOnline()) {
                val response = examApiService.getExamById(examId)

                if (response.isSuccessful() && response.data != null) {
                    val remoteExam = response.data!!
                    database.examDao().insertExam(remoteExam.toLocalModel())
                    Result.success(remoteExam)
                } else {
                    Result.failure(Exception(response.getErrorMessage()))
                }
            } else {
                Result.failure(Exception("آزمون در دستگاه یافت نشد. لطفاً دوباره دانلود کنید."))
            }
        } catch (e: Exception) {
            Timber.e(e, "خطا در دریافت آزمون")
            Result.failure(Exception("خطا در ارتباط: ${e.message}"))
        }
    }

    suspend fun getExamQuestions(examId: Int): Result<List<QuestionRemote>> = withContext(Dispatchers.IO) {
        try {
            // نسخه Pro: از دیتابیس
            if (isProVersion()) {
                val localQuestions = database.questionDao().getQuestionsByExamId(examId)
                if (localQuestions.isNotEmpty()) {
                    return@withContext Result.success(localQuestions.map { it.toRemoteModel() })
                }
            }

            // درخواست از سرور
            val response = examApiService.getExamQuestions(examId)

            if (response.isSuccessful() && response.data != null) {
                val questions = response.data!!

                // ذخیره در دیتابیس برای نسخه Pro
                if (isProVersion()) {
                    questions.forEach { question ->
                        database.questionDao().insertQuestion(question.toLocalModel())
                    }
                }

                Result.success(questions)
            } else {
                Result.failure(Exception(response.getErrorMessage()))
            }
        } catch (e: Exception) {
            Timber.e(e, "خطا در دریافت سوالات")
            Result.failure(Exception("خطا در دریافت سوالات: ${e.message}"))
        }
    }

    // ==================== ارسال آزمون ====================

    suspend fun submitExam(
        examId: Int,
        answers: List<UserAnswerRemote>,
        timeSpentSeconds: Int = 0,
        deviceInfo: String? = null
    ): Result<SubmitExamResponse> = withContext(Dispatchers.IO) {
        try {
            val request = SubmitExamRequest(examId, answers, timeSpentSeconds, deviceInfo)
            val response = examApiService.submitExam(examId, request)

            if (response.isSuccessful() && response.data != null) {
                val result = response.data!!

                // ذخیره نتیجه در دیتابیس
                saveExamResult(result, examId, answers)

                Result.success(result)
            } else {
                Result.failure(Exception(response.getErrorMessage()))
            }
        } catch (e: Exception) {
            Timber.e(e, "خطا در ارسال آزمون")

            // اگر آفلاین هست و نسخه Pro، محلی حساب کن
            if (isProVersion() && !isOnline()) {
                submitOfflineExam(examId, answers)
            } else {
                Result.failure(Exception("خطا در ارسال آزمون: ${e.message}"))
            }
        }
    }

    private suspend fun submitOfflineExam(
        examId: Int,
        answers: List<UserAnswerRemote>
    ): Result<SubmitExamResponse> {
        val exam = database.examDao().getExamById(examId)
        if (exam == null) {
            return Result.failure(Exception("آزمون یافت نشد"))
        }

        // محاسبه نتیجه محلی
        val questions = database.questionDao().getQuestionsByExamId(examId)
        var correctCount = 0
        var totalScore = 0
        var earnedScore = 0

        val answerDetails = mutableListOf<AnswerDetail>()

        questions.forEach { question ->
            val studentAnswer = answers.find { it.questionId == question.id }
            val isCorrect = studentAnswer?.selectedOption == question.correctAnswer

            if (isCorrect) {
                correctCount++
                earnedScore += question.points
            }

            totalScore += question.points

            answerDetails.add(
                AnswerDetail(
                    questionId = question.id,
                    isCorrect = isCorrect,
                    userAnswer = studentAnswer?.selectedOption,
                    correctAnswer = question.correctAnswer,
                    pointsEarned = if (isCorrect) question.points else 0,
                    explanation = null
                )
            )
        }

        val percentage = if (totalScore > 0) {
            (earnedScore.toDouble() / totalScore) * 100
        } else {
            0.0
        }

        val result = SubmitExamResponse(
            score = earnedScore,
            totalScore = totalScore,
            correctAnswers = correctCount,
            wrongAnswers = questions.size - correctCount,
            unanswered = 0,
            percentage = percentage,
            isPassed = percentage >= (exam.passingScore ?: 50),
            answersDetails = answerDetails,
            resultId = System.currentTimeMillis().toInt(),
            generatedAt = java.time.LocalDateTime.now().toString()
        )

        // ذخیره نتیجه محلی
        saveExamResult(result, examId, answers)

        return Result.success(result)
    }

    private suspend fun saveExamResult(
        result: SubmitExamResponse,
        examId: Int,
        answers: List<UserAnswerRemote>
    ) {
        // تبدیل به مدل محلی و ذخیره در دیتابیس
        // پارامتر سوم examTitle اضافه شده
        val exam = database.examDao().getExamById(examId)
        val examTitle = exam?.title ?: "آزمون"
        val localResult = result.toLocalModel(examId, preferences.getUserId(), examTitle)
        database.examResultDao().insertResult(localResult)

        // ذخیره پاسخ‌های کاربر
        answers.forEach { answer ->
            database.userAnswerDao().insertAnswer(answer.toLocalModel(examId))
        }
    }

    // ==================== نتایج کاربر ====================

    suspend fun getUserResults(page: Int = 1, limit: Int = 20): Result<List<ExamResultRemote>> {
        return withContext(Dispatchers.IO) {
            try {
                // نسخه Pro: اول از دیتابیس
                if (isProVersion()) {
                    val localResults = database.examResultDao().getResultsByUserId(preferences.getUserId())
                    if (localResults.isNotEmpty() && !isOnline()) {
                        return@withContext Result.success(localResults.map { it.toRemoteModel() })
                    }
                }

                // درخواست از سرور
                val response = examApiService.getUserResults(page, limit)

                if (response.isSuccessful() && response.data != null) {
                    val results = response.data!!.data
                    Result.success(results)
                } else {
                    Result.failure(Exception(response.getErrorMessage()))
                }
            } catch (e: Exception) {
                Timber.e(e, "خطا در دریافت نتایج کاربر")
                Result.failure(Exception("خطا در دریافت نتایج: ${e.message}"))
            }
        }
    }

    // ==================== دسته‌بندی‌ها ====================

    suspend fun getCategories(): Result<List<CategoryRemote>> = withContext(Dispatchers.IO) {
        try {
            val response = examApiService.getCategories()

            if (response.isSuccessful() && response.data != null) {
                Result.success(response.data!!)
            } else {
                Result.failure(Exception(response.getErrorMessage()))
            }
        } catch (e: Exception) {
            Timber.e(e, "خطا در دریافت دسته‌بندی‌ها")
            Result.failure(Exception("خطا در دریافت دسته‌بندی‌ها: ${e.message}"))
        }
    }

    // ==================== PDF ====================

    suspend fun generateExamPdf(exam: ExamRemote): Result<File> = withContext(Dispatchers.IO) {
        try {
            val file = pdfGenerator.generateExamPdf(
                exam = exam.toLocalModel(),
                studentName = preferences.getStudentName(),
                grade = 0, // TODO: از exam بگیر
                teacherName = preferences.getTeacherName()
            )
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(Exception("خطا در تولید PDF: ${e.message}"))
        }
    }

    suspend fun generateAnswerSheetPdf(
        exam: ExamRemote,
        result: SubmitExamResponse
    ): Result<File> {
        return withContext(Dispatchers.IO) {
            try {
                // ساخت مدل ExamResult از SubmitExamResponse
                val examResult = ExamResult(
                    id = result.resultId,
                    examId = exam.id,
                    examTitle = exam.title,
                    studentId = preferences.getUserId(),
                    score = result.percentage.toFloat(),
                    correctAnswers = result.correctAnswers,
                    totalQuestions = result.correctAnswers + result.wrongAnswers,
                    submittedAt = System.currentTimeMillis(),
                    timeSpent = 0,
                    feedback = if (result.isPassed) "قبول" else "مردود",
                    detailedResults = emptyList(),
                    isOnline = true
                )

                val file = pdfGenerator.generateAnswerSheetPdf(
                    exam = exam.toLocalModel(),
                    result = examResult,
                    studentName = preferences.getStudentName(),
                    teacherName = preferences.getTeacherName()
                )
                Result.success(file)
            } catch (e: Exception) {
                Result.failure(Exception("خطا در تولید پاسخنامه: ${e.message}"))
            }
        }
    }

    // ==================== مدیریت دانلود (نسخه Pro) ====================

    suspend fun downloadExam(examId: Int): Result<Boolean> = withContext(Dispatchers.IO) {
        if (!isProVersion()) {
            return@withContext Result.failure(Exception("فقط نسخه Pro امکان دانلود دارد"))
        }

        try {
            // دریافت آزمون
            val examResponse = examApiService.getExamById(examId)
            if (!examResponse.isSuccessful() || examResponse.data == null) {
                return@withContext Result.failure(Exception("آزمون یافت نشد"))
            }

            // دریافت سوالات
            val questionsResponse = examApiService.getExamQuestions(examId)
            if (!questionsResponse.isSuccessful() || questionsResponse.data == null) {
                return@withContext Result.failure(Exception("سوالات یافت نشدند"))
            }

            val exam = examResponse.data!!
            val questions = questionsResponse.data!!

            // ذخیره در دیتابیس
            database.examDao().insertExam(exam.toLocalModel())
            questions.forEach { question ->
                database.questionDao().insertQuestion(question.toLocalModel())
            }

            // علامت‌گذاری به عنوان دانلود شده
            preferences.setExamDownloaded(examId, true)

            Result.success(true)
        } catch (e: Exception) {
            Timber.e(e, "خطا در دانلود آزمون")
            Result.failure(Exception("خطا در دانلود: ${e.message}"))
        }
    }

    suspend fun isExamDownloaded(examId: Int): Boolean {
        return preferences.isExamDownloaded(examId)
    }

    // ==================== متدهای کمکی ====================

    private suspend fun isOnline(): Boolean {
        return try {
            apiClient.checkServerHealth()
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        private const val TAG = "ExamRepository"
    }
}

// ==================== Extension Functions برای تبدیل مدل‌ها ====================
// این Extension functions اکنون در فایل ModelMapper.kt قرار دارند و باید حذف شوند
// فقط برای سازگاری موقت باقی می‌مانند - بعداً حذف کنید
/*
// این بخش باید حذف شود چون ModelMapper.kt داریم
fun ExamRemote.toLocalModel(): Exam {
    return Exam(
        id = this.id,
        title = this.title,
        description = this.description,
        grade = 0, // TODO: از remote بگیر
        subject = "", // TODO: از remote بگیر
        difficulty = "medium",
        questions = emptyList(), // سوالات جداگانه لود می‌شن
        isOnline = true,
        duration = this.durationMinutes,
        passingScore = this.passingScore,
        price = this.price,
        isPublished = this.isPublished,
        createdAt = this.createdAt ?: "",
        categoryId = this.categoryId
    )
}

fun QuestionRemote.toLocalModel(): Question {
    return Question(
        id = this.id,
        examId = this.examId,
        text = this.questionText,
        questionType = this.questionType,
        options = this.options.map { it.toLocalModel() },
        correctAnswer = this.correctAnswer,
        points = this.points,
        explanation = this.explanation,
        orderIndex = this.orderIndex
    )
}

fun OptionRemote.toLocalModel(): Option {
    return Option(
        id = this.id,
        questionId = this.questionId,
        text = this.optionText,
        letter = this.letter,
        isCorrect = this.isCorrect
    )
}

fun UserAnswerRemote.toLocalModel(examId: Int): StudentAnswer {
    return StudentAnswer(
        id = 0, // generate
        examId = examId,
        questionId = this.questionId,
        answer = this.selectedOption ?: this.descriptiveAnswer ?: "",
        isCorrect = false, // بعداً محاسبه می‌شه
        timeSpent = 0
    )
}

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
        timeSpent = 0, // TODO: محاسبه کن
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

// تبدیل معکوس (Local به Remote)
fun Exam.toRemoteModel(): ExamRemote {
    return ExamRemote(
        id = this.id,
        title = this.title,
        description = this.description,
        categoryId = this.categoryId,
        durationMinutes = this.duration,
        totalQuestions = this.questions.size,
        passingScore = this.passingScore ?: 50,
        price = this.price ?: 0.0,
        isPublished = this.isPublished ?: true,
        isFree = this.price == 0.0,
        createdAt = this.createdAt,
        updatedAt = null,
        category = null
    )
}
*/