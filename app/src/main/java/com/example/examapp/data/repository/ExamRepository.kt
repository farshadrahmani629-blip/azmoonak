// app/src/main/java/com/examapp/data/repository/ExamRepository.kt
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
class ExamRepository @Inject constructor(
    private val apiService: ApiClient.ApiService,
    private val database: ExamDatabase
) {

    // ==================== آزمون‌ها ====================

    suspend fun generateExam(request: ExamRequest): Result<Exam> = withContext(Dispatchers.IO) {
        try {
            val response: Response<ApiResponse<Exam>> = apiService.generateExam(request)

            if (response.isSuccessful) {
                val apiResponse = response.body()
                if (apiResponse?.success == true) {
                    val exam = apiResponse.data
                    if (exam != null) {
                        // ذخیره در دیتابیس
                        database.examDao().insertExam(exam)
                        Result.success(exam)
                    } else {
                        Result.failure(Exception("آزمون ایجاد نشد"))
                    }
                } else {
                    Result.failure(Exception(apiResponse?.message ?: "خطا در ایجاد آزمون"))
                }
            } else {
                Result.failure(Exception("خطای سرور: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("خطا در ارتباط با سرور: ${e.message}"))
        }
    }

    suspend fun getExamById(examId: Int): Result<Exam> = withContext(Dispatchers.IO) {
        try {
            // اول از دیتابیس بگیر
            val localExam = database.examDao().getExamById(examId)
            if (localExam != null) {
                return@withContext Result.success(localExam)
            }

            // اگر نبود از API بگیر
            val response: Response<ApiResponse<Exam>> = apiService.getExamById(examId)
            if (response.isSuccessful && response.body()?.success == true) {
                val exam = response.body()?.data
                if (exam != null) {
                    database.examDao().insertExam(exam)
                    Result.success(exam)
                } else {
                    Result.failure(Exception("آزمون یافت نشد"))
                }
            } else {
                Result.failure(Exception("خطا در دریافت آزمون"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("خطا در ارتباط: ${e.message}"))
        }
    }

    fun getUserExamsFlow(userId: Int): Flow<List<Exam>> = flow {
        // اول دیتابیس
        val localExams = database.examDao().getExamsByUserId(userId)
        emit(localExams)

        try {
            // سپس API
            val response = apiService.getExams()
            if (response.isSuccessful) {
                val exams = response.body()?.data ?: emptyList()
                database.examDao().insertAll(exams)
                emit(exams)
            }
        } catch (e: Exception) {
            // فقط خطا لاگ کن، داده‌های دیتابیس رو قبلاً فرستادیم
            e.printStackTrace()
        }
    }.flowOn(Dispatchers.IO)

    suspend fun startExam(examId: Int): Result<ExamSession> = withContext(Dispatchers.IO) {
        try {
            val response: Response<ApiResponse<ExamSession>> = apiService.startExam(examId)
            if (response.isSuccessful && response.body()?.success == true) {
                val session = response.body()?.data
                if (session != null) {
                    database.examSessionDao().insertSession(session)
                    Result.success(session)
                } else {
                    Result.failure(Exception("جلسه آزمون ایجاد نشد"))
                }
            } else {
                Result.failure(Exception("خطا در شروع آزمون"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("خطا: ${e.message}"))
        }
    }

    suspend fun submitExam(
        examId: Int,
        answers: List<StudentAnswer>
    ): Result<ExamResult> = withContext(Dispatchers.IO) {
        try {
            val request = SubmitExamRequest(answers)
            val response: Response<ApiResponse<ExamResult>> = apiService.submitExam(examId, request)

            if (response.isSuccessful && response.body()?.success == true) {
                val result = response.body()?.data
                if (result != null) {
                    // ذخیره نتیجه
                    database.examResultDao().insertResult(result)

                    // به‌روزرسانی وضعیت آزمون
                    database.examDao().updateExamStatus(examId, ExamStatus.COMPLETED)

                    // حذف جلسه فعال
                    database.examSessionDao().deleteByExamId(examId)

                    Result.success(result)
                } else {
                    Result.failure(Exception("نتیجه دریافت نشد"))
                }
            } else {
                Result.failure(Exception("خطا در ثبت آزمون"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("خطا: ${e.message}"))
        }
    }

    // ==================== نتایج ====================

    suspend fun getResultById(resultId: Int): Result<ExamResult> = withContext(Dispatchers.IO) {
        try {
            // اول دیتابیس
            val localResult = database.examResultDao().getResultById(resultId)
            if (localResult != null) {
                return@withContext Result.success(localResult)
            }

            // سپس API
            val response: Response<ApiResponse<ExamResult>> = apiService.getResultById(resultId)
            if (response.isSuccessful && response.body()?.success == true) {
                val result = response.body()?.data
                if (result != null) {
                    database.examResultDao().insertResult(result)
                    Result.success(result)
                } else {
                    Result.failure(Exception("نتیجه یافت نشد"))
                }
            } else {
                Result.failure(Exception("خطا در دریافت نتیجه"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("خطا: ${e.message}"))
        }
    }

    fun getUserResultsFlow(userId: Int): Flow<List<ExamResult>> = flow {
        // دیتابیس
        val localResults = database.examResultDao().getResultsByUserId(userId)
        emit(localResults)

        try {
            // API
            val response = apiService.getResults(userId = userId)
            if (response.isSuccessful) {
                val results = response.body()?.data ?: emptyList()
                database.examResultDao().insertAll(results)
                emit(results)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }.flowOn(Dispatchers.IO)

    suspend fun getResultDetails(resultId: Int): Result<ExamResultDetail> = withContext(Dispatchers.IO) {
        try {
            val response: Response<ApiResponse<ExamResultDetail>> = apiService.getResultDetail(resultId)
            if (response.isSuccessful && response.body()?.success == true) {
                val detail = response.body()?.data
                if (detail != null) {
                    Result.success(detail)
                } else {
                    Result.failure(Exception("جزئیات یافت نشد"))
                }
            } else {
                Result.failure(Exception("خطا در دریافت جزئیات"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("خطا: ${e.message}"))
        }
    }

    // ==================== جلسات آزمون ====================

    suspend fun getActiveSession(examId: Int): ExamSession? = withContext(Dispatchers.IO) {
        database.examSessionDao().getSessionByExamId(examId)
    }

    suspend fun saveSessionProgress(session: ExamSession) = withContext(Dispatchers.IO) {
        database.examSessionDao().insertSession(session)
    }

    suspend fun deleteSession(sessionId: String) = withContext(Dispatchers.IO) {
        database.examSessionDao().deleteSession(sessionId)
    }

    // ==================== مدیریت کش ====================

    suspend fun clearCache() = withContext(Dispatchers.IO) {
        database.examDao().clearAll()
        database.examResultDao().clearAll()
        database.examSessionDao().clearAll()
    }

    companion object {
        private const val TAG = "ExamRepository"
    }
}

// مدل درخواست ارسال آزمون
data class SubmitExamRequest(
    @SerializedName("answers") val answers: List<StudentAnswer>
)