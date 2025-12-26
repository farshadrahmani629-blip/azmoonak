package com.examapp.data.remote

import com.examapp.data.models.ApiResponse
import com.examapp.data.models.PaginatedResponse
import com.examapp.data.models.SuccessResponse
import retrofit2.http.*
import okhttp3.RequestBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

/**
 * سرویس API برای ارتباط با سرور آزمون‌ها
 */
interface ExamApiService {

    // ================ آزمون‌ها ================

    /**
     * دریافت لیست آزمون‌ها با صفحه‌بندی
     */
    @GET("api/exams")
    suspend fun getExams(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("search") search: String? = null,
        @Query("category") category: String? = null,
        @Query("is_published") isPublished: Boolean = true
    ): ApiResponse<PaginatedResponse<ExamRemote>>

    /**
     * دریافت آزمون بر اساس ID
     */
    @GET("api/exams/{id}")
    suspend fun getExamById(@Path("id") examId: Int): ApiResponse<ExamRemote>

    /**
     * دریافت سوالات یک آزمون
     */
    @GET("api/exams/{id}/questions")
    suspend fun getExamQuestions(@Path("id") examId: Int): ApiResponse<List<QuestionRemote>>

    // ================ دسته‌بندی‌ها ================

    /**
     * دریافت لیست دسته‌بندی‌ها
     */
    @GET("api/categories")
    suspend fun getCategories(): ApiResponse<List<CategoryRemote>>

    // ================ مدیریت آزمون‌ها ================

    /**
     * ارسال پاسخ‌های آزمون
     */
    @POST("api/exams/{id}/submit")
    suspend fun submitExam(
        @Path("id") examId: Int,
        @Body submitRequest: SubmitExamRequest
    ): ApiResponse<SubmitExamResponse>

    /**
     * دریافت نتایج آزمون‌های کاربر
     */
    @GET("api/user/results")
    suspend fun getUserResults(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): ApiResponse<PaginatedResponse<ExamResultRemote>>

    /**
     * دریافت جزئیات یک نتیجه آزمون
     */
    @GET("api/user/results/{resultId}")
    suspend fun getResultDetails(@Path("resultId") resultId: Int): ApiResponse<ExamResultDetail>

    // ================ احراز هویت و کاربر ================

    /**
     * لاگین کاربر
     */
    @POST("api/auth/login")
    suspend fun login(@Body loginRequest: LoginRequest): ApiResponse<AuthResponse>

    /**
     * ثبت‌نام کاربر
     */
    @POST("api/auth/register")
    suspend fun register(@Body registerRequest: RegisterRequest): ApiResponse<AuthResponse>

    /**
     * دریافت پروفایل کاربر
     */
    @GET("api/user/profile")
    suspend fun getUserProfile(): ApiResponse<UserProfileRemote>

    // ================ فایل‌ها ================

    /**
     * آپلود فایل (مثلاً پاسخ تشریحی)
     */
    @Multipart
    @POST("api/upload")
    suspend fun uploadFile(
        @Part("file") file: RequestBody,
        @Part("type") type: String = "answer"
    ): ApiResponse<UploadResponse>

    // ================ وضعیت سرور ================

    /**
     * بررسی وضعیت سرور
     */
    @GET("api/status")
    suspend fun checkServerStatus(): ApiResponse<ServerStatus>

    /**
     * دریافت اطلاعات ورژن
     */
    @GET("api/version")
    suspend fun getApiVersion(): ApiResponse<VersionInfo>
}

// ================ مدل‌های کمکی ================

/**
 * مدل آزمون از سرور
 */
data class ExamRemote(
    val id: Int,
    val title: String,
    val description: String? = null,
    val categoryId: Int? = null,
    val durationMinutes: Int = 60,
    val totalQuestions: Int = 0,
    val passingScore: Int = 50,
    val price: Double = 0.0,
    val isPublished: Boolean = true,
    val isFree: Boolean = true,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val category: CategoryRemote? = null
)

/**
 * مدل دسته‌بندی
 */
data class CategoryRemote(
    val id: Int,
    val name: String,
    val description: String? = null,
    val icon: String? = null,
    val examCount: Int = 0
)

/**
 * مدل سوال از سرور
 */
data class QuestionRemote(
    val id: Int,
    val examId: Int,
    val questionText: String,
    val questionType: String, // "multiple_choice", "true_false", "descriptive"
    val points: Int = 1,
    val options: List<OptionRemote> = emptyList(),
    val correctAnswer: String? = null,
    val explanation: String? = null,
    val orderIndex: Int = 0
)

/**
 * مدل گزینه
 */
data class OptionRemote(
    val id: Int,
    val questionId: Int,
    val optionText: String,
    val letter: String, // A, B, C, D
    val isCorrect: Boolean = false
)

/**
 * درخواست ارسال آزمون
 */
data class SubmitExamRequest(
    val examId: Int,
    val answers: List<UserAnswerRemote>,
    val timeSpentSeconds: Int = 0,
    val deviceInfo: String? = null
)

/**
 * پاسخ کاربر
 */
data class UserAnswerRemote(
    val questionId: Int,
    val selectedOption: String? = null, // برای سوالات تستی
    val descriptiveAnswer: String? = null, // برای سوالات تشریحی
    val isFlagged: Boolean = false // اگر کاربر سوال رو علامت‌گذاری کرده
)

/**
 * پاسخ سرور به ارسال آزمون
 */
data class SubmitExamResponse(
    val score: Int,
    val totalScore: Int,
    val correctAnswers: Int,
    val wrongAnswers: Int,
    val unanswered: Int = 0,
    val percentage: Double,
    val isPassed: Boolean,
    val answersDetails: List<AnswerDetail>,
    val resultId: Int,
    val generatedAt: String
)

/**
 * جزئیات پاسخ
 */
data class AnswerDetail(
    val questionId: Int,
    val isCorrect: Boolean,
    val userAnswer: String?,
    val correctAnswer: String?,
    val pointsEarned: Int,
    val explanation: String?
)

/**
 * نتیجه آزمون کاربر
 */
data class ExamResultRemote(
    val id: Int,
    val examId: Int,
    val examTitle: String,
    val score: Int,
    val totalScore: Int,
    val percentage: Double,
    val isPassed: Boolean,
    val completedAt: String,
    val durationSeconds: Int
)

/**
 * جزئیات نتیجه آزمون
 */
data class ExamResultDetail(
    val result: ExamResultRemote,
    val exam: ExamRemote,
    val answers: List<UserAnswerWithDetail>
)

/**
 * پاسخ کاربر با جزئیات
 */
data class UserAnswerWithDetail(
    val userAnswer: UserAnswerRemote,
    val question: QuestionRemote,
    val isCorrect: Boolean,
    val pointsEarned: Int
)

/**
 * درخواست لاگین
 */
data class LoginRequest(
    val email: String,
    val password: String,
    val deviceName: String? = "Android Device"
)

/**
 * درخواست ثبت‌نام
 */
data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val passwordConfirmation: String
)

/**
 * پاسخ احراز هویت
 */
data class AuthResponse(
    val token: String,
    val tokenType: String = "Bearer",
    val expiresIn: Int? = null,
    val user: UserProfileRemote
)

/**
 * پروفایل کاربر
 */
data class UserProfileRemote(
    val id: Int,
    val name: String,
    val email: String,
    val avatar: String? = null,
    val role: String = "student",
    val createdAt: String,
    val stats: UserStats? = null
)

/**
 * آمار کاربر
 */
data class UserStats(
    val totalExamsTaken: Int = 0,
    val averageScore: Double = 0.0,
    val totalStudyTime: Int = 0, // بر حسب دقیقه
    val rank: Int? = null
)

/**
 * اطلاعات ورژن
 */
data class VersionInfo(
    val apiVersion: String,
    val minAppVersion: String? = null,
    val latestAppVersion: String? = null,
    val requiresUpdate: Boolean = false
)

/**
 * مدل آپلود فایل
 */
data class UploadResponse(
    val url: String,
    val fileName: String,
    val fileSize: Long,
    val mimeType: String
)

/**
 * وضعیت سرور
 */
data class ServerStatus(
    val status: String,
    val version: String,
    val uptime: Long,
    val timestamp: String
) {
    fun isServerUp(): Boolean = status.equals("up", ignoreCase = true)
}