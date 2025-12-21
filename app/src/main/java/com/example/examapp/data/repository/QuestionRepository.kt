// app/src/main/java/com/examapp/data/repository/QuestionRepository.kt
package com.examapp.data.repository

import com.examapp.data.network.ApiClient
import com.examapp.data.models.*
import com.examapp.data.database.ExamDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuestionRepository @Inject constructor(
    private val apiService: ApiClient.ApiService,
    private val database: ExamDatabase
) {

    suspend fun getQuestions(
        bookId: Int? = null,
        chapterId: Int? = null,
        difficulty: DifficultyLevel? = null,
        questionType: QuestionType? = null
    ): Result<List<Question>> = withContext(Dispatchers.IO) {
        try {
            val response: Response<PaginatedResponse<Question>> =
                apiService.getQuestions(bookId, chapterId, difficulty = difficulty, questionType = questionType)

            if (response.isSuccessful) {
                val questions = response.body()?.data ?: emptyList()
                // ذخیره سوالات
                database.questionDao().insertAll(questions)

                // ذخیره گزینه‌ها
                questions.forEach { question ->
                    question.options?.let { options ->
                        database.questionOptionDao().insertAll(options)
                    }
                }

                Result.success(questions)
            } else {
                Result.failure(Exception("خطا در دریافت سوالات"))
            }
        } catch (e: Exception) {
            // از دیتابیس بگیر
            val cachedQuestions = database.questionDao().getQuestions(
                bookId, chapterId, difficulty, questionType
            )
            Result.success(cachedQuestions)
        }
    }

    suspend fun getQuestionWithOptions(questionId: Int): Result<Question> = withContext(Dispatchers.IO) {
        try {
            // اول از دیتابیس کامل بگیر
            val localQuestion = database.questionDao().getQuestionById(questionId)
            val options = database.questionOptionDao().getOptionsByQuestion(questionId)

            if (localQuestion != null && options.isNotEmpty()) {
                val questionWithOptions = localQuestion.copy().apply {
                    this.options = options
                }
                return@withContext Result.success(questionWithOptions)
            }

            // اگر کامل نبود از API بگیر
            val questionResponse: Response<ApiResponse<Question>> =
                apiService.getQuestionById(questionId)

            if (questionResponse.isSuccessful && questionResponse.body()?.success == true) {
                val question = questionResponse.body()?.data
                if (question != null) {
                    // ذخیره در دیتابیس
                    database.questionDao().insertQuestion(question)

                    // گزینه‌ها رو هم بگیر و ذخیره کن
                    val optionsResponse: Response<ApiResponse<List<QuestionOption>>> =
                        apiService.getQuestionOptions(questionId)

                    if (optionsResponse.isSuccessful && optionsResponse.body()?.success == true) {
                        val fetchedOptions = optionsResponse.body()?.data ?: emptyList()
                        database.questionOptionDao().insertAll(fetchedOptions)
                        question.options = fetchedOptions
                    }

                    Result.success(question)
                } else {
                    Result.failure(Exception("سوال یافت نشد"))
                }
            } else {
                Result.failure(Exception("خطا در دریافت سوال"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("خطا: ${e.message}"))
        }
    }

    suspend fun getQuestionsForExam(request: ExamRequest): Result<List<Question>> = withContext(Dispatchers.IO) {
        try {
            // از دیتابیس سوالات مربوطه رو بگیر
            val questions = database.questionDao().getQuestionsForExam(
                bookId = request.bookId,
                chapterIds = request.chapterIds,
                difficultyLevels = request.difficultyLevels,
                questionTypes = request.questionTypes,
                limit = request.totalQuestions
            )

            if (questions.size >= request.totalQuestions) {
                // اگر سوالات کافی در دیتابیس هست
                Result.success(questions.take(request.totalQuestions))
            } else {
                // از API بگیر
                val response = getQuestions(
                    bookId = request.bookId,
                    chapterId = request.chapterIds?.firstOrNull(),
                    difficulty = request.difficultyLevels?.firstOrNull(),
                    questionType = request.questionTypes?.firstOrNull()
                )

                if (response.isSuccess) {
                    val fetchedQuestions = response.getOrNull() ?: emptyList()
                    // به تعداد مورد نیاز محدود کن
                    val selectedQuestions = if (request.isRandom) {
                        fetchedQuestions.shuffled().take(request.totalQuestions)
                    } else {
                        fetchedQuestions.take(request.totalQuestions)
                    }
                    Result.success(selectedQuestions)
                } else {
                    Result.failure(Exception("سوالات کافی یافت نشد"))
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception("خطا در آماده‌سازی سوالات: ${e.message}"))
        }
    }

    suspend fun saveUserAnswer(
        questionId: Int,
        userAnswer: String?,
        isCorrect: Boolean? = null,
        timeSpent: Int = 0
    ) = withContext(Dispatchers.IO) {
        database.questionDao().updateUserAnswer(questionId, userAnswer, isCorrect, timeSpent)
    }

    suspend fun getQuestionCount(
        bookId: Int? = null,
        chapterId: Int? = null,
        difficulty: DifficultyLevel? = null
    ): Map<String, Int> = withContext(Dispatchers.IO) {
        database.questionDao().getQuestionCount(bookId, chapterId, difficulty)
    }
}