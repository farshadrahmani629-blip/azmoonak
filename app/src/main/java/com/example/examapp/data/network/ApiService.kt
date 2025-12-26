package com.examapp.data.network

import com.examapp.data.models.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ========== احراز هویت ==========
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<ApiResponse<User>>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<ApiResponse<User>>

    @POST("auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<ApiResponse<AuthResponse>>

    @POST("auth/logout")
    suspend fun logout(@Body request: LogoutRequest): Response<ApiResponse<Unit>>

    // ========== کاربران ==========
    @GET("users/profile")
    suspend fun getUserProfile(): Response<ApiResponse<User>>

    @PUT("users/profile")
    suspend fun updateUserProfile(@Body request: UserUpdateRequest): Response<ApiResponse<User>>

    @GET("users/{id}")
    suspend fun getUserById(@Path("id") userId: Int): Response<ApiResponse<User>>

    @POST("users/change-password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): Response<ApiResponse<Unit>>

    // ========== کتاب‌ها و فصل‌ها ==========
    @GET("books")
    suspend fun getBooks(
        @Query("grade") grade: Int? = null,
        @Query("subject") subject: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<PaginatedResponse<Book>>

    @GET("books/{id}")
    suspend fun getBookById(@Path("id") bookId: Int): Response<ApiResponse<Book>>

    @GET("books/{bookId}/chapters")
    suspend fun getBookChapters(
        @Path("bookId") bookId: Int,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50
    ): Response<PaginatedResponse<Chapter>>

    @GET("chapters/{id}")
    suspend fun getChapterById(@Path("id") chapterId: Int): Response<ApiResponse<Chapter>>

    // ========== سوالات ==========
    @GET("questions")
    suspend fun getQuestions(
        @Query("book_id") bookId: Int? = null,
        @Query("chapter_id") chapterId: Int? = null,
        @Query("page") page: Int? = null,
        @Query("difficulty") difficulty: DifficultyLevel? = null,
        @Query("question_type") questionType: QuestionType? = null,
        @Query("bloom_level") bloomLevel: BloomLevel? = null,
        @Query("is_active") isActive: Boolean? = true,
        @Query("page") pageNum: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<PaginatedResponse<Question>>

    @GET("questions/{id}")
    suspend fun getQuestionById(@Path("id") questionId: Int): Response<ApiResponse<Question>>

    @GET("questions/{id}/options")
    suspend fun getQuestionOptions(@Path("id") questionId: Int): Response<ApiResponse<List<QuestionOption>>>

    @GET("questions/count")
    suspend fun getQuestionsCount(
        @Query("book_id") bookId: Int? = null,
        @Query("chapter_id") chapterId: Int? = null,
        @Query("difficulty") difficulty: DifficultyLevel? = null
    ): Response<ApiResponse<QuestionCountResponse>>

    @POST("questions/batch")
    suspend fun getQuestionsByIds(@Body request: QuestionBatchRequest): Response<ApiResponse<List<Question>>>

    // ========== آزمون‌ها ==========
    @POST("exams/generate")
    suspend fun generateExam(@Body request: ExamRequest): Response<ApiResponse<Exam>>

    @GET("exams")
    suspend fun getExams(
        @Query("status") status: ExamStatus? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<PaginatedResponse<Exam>>

    @GET("exams/{id}")
    suspend fun getExamById(@Path("id") examId: Int): Response<ApiResponse<Exam>>

    @GET("exams/code/{code}")
    suspend fun getExamByCode(@Path("code") examCode: String): Response<ApiResponse<Exam>>

    @GET("exams/{id}/questions")
    suspend fun getExamQuestions(@Path("id") examId: Int): Response<ApiResponse<List<Question>>>

    @POST("exams/{id}/start")
    suspend fun startExam(@Path("id") examId: Int): Response<ApiResponse<ExamSession>>

    @POST("exams/{id}/submit")
    suspend fun submitExam(
        @Path("id") examId: Int,
        @Body request: SubmitExamRequest
    ): Response<ApiResponse<ExamResult>>

    @POST("exams/{id}/cancel")
    suspend fun cancelExam(@Path("id") examId: Int): Response<ApiResponse<Unit>>

    @DELETE("exams/{id}")
    suspend fun deleteExam(@Path("id") examId: Int): Response<ApiResponse<Unit>>

    // ========== جلسات آزمون ==========
    @GET("sessions/active")
    suspend fun getActiveSessions(): Response<ApiResponse<List<ExamSession>>>

    @GET("sessions/{id}")
    suspend fun getSessionById(@Path("id") sessionId: String): Response<ApiResponse<ExamSession>>

    @PUT("sessions/{id}")
    suspend fun updateSession(
        @Path("id") sessionId: String,
        @Body request: UpdateSessionRequest
    ): Response<ApiResponse<ExamSession>>

    @POST("sessions/{id}/heartbeat")
    suspend fun sendHeartbeat(@Path("id") sessionId: String): Response<ApiResponse<Unit>>

    // ========== نتایج آزمون ==========
    @GET("results")
    suspend fun getResults(
        @Query("exam_id") examId: Int? = null,
        @Query("user_id") userId: Int? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<PaginatedResponse<ExamResult>>

    @GET("results/{id}")
    suspend fun getResultById(@Path("id") resultId: Int): Response<ApiResponse<ExamResult>>

    @GET("results/{id}/detail")
    suspend fun getResultDetail(@Path("id") resultId: Int): Response<ApiResponse<ExamResultDetail>>

    @GET("results/{id}/answers")
    suspend fun getResultAnswers(@Path("id") resultId: Int): Response<ApiResponse<List<StudentAnswer>>>

    @POST("results/{id}/share")
    suspend fun shareResult(@Path("id") resultId: Int): Response<ApiResponse<String>>

    @GET("results/statistics")
    suspend fun getStatistics(
        @Query("period") period: StatisticsPeriod = StatisticsPeriod.MONTHLY
    ): Response<ApiResponse<UserStatistics>>

    // ========== اشتراک‌ها و پرداخت ==========
    @GET("subscriptions/plans")
    suspend fun getSubscriptionPlans(): Response<ApiResponse<List<SubscriptionPlan>>>

    @POST("subscriptions/subscribe")
    suspend fun subscribe(@Body request: SubscribeRequest): Response<ApiResponse<Subscription>>

    @GET("subscriptions/current")
    suspend fun getCurrentSubscription(): Response<ApiResponse<Subscription>>

    @POST("subscriptions/cancel")
    suspend fun cancelSubscription(): Response<ApiResponse<Unit>>

    // ========== نوتیفیکیشن‌ها ==========
    @GET("notifications")
    suspend fun getNotifications(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<PaginatedResponse<Notification>>

    @PUT("notifications/{id}/read")
    suspend fun markNotificationAsRead(@Path("id") notificationId: Int): Response<ApiResponse<Unit>>

    @PUT("notifications/read-all")
    suspend fun markAllNotificationsAsRead(): Response<ApiResponse<Unit>>

    @POST("devices/register")
    suspend fun registerDevice(@Body request: DeviceRegistrationRequest): Response<ApiResponse<Unit>>

    // ========== گزارش خطا و بازخورد ==========
    @POST("feedback")
    suspend fun submitFeedback(@Body request: FeedbackRequest): Response<ApiResponse<Unit>>

    @POST("bug-report")
    suspend fun reportBug(@Body request: BugReportRequest): Response<ApiResponse<Unit>>
}

// مدل‌های درخواست و پاسخ (این‌ها رو نگه داشتم و تکمیل کردم)
data class LoginRequest(
    val username: String,
    val password: String,
    val deviceToken: String? = null
)

data class RegisterRequest(
    val username: String,
    val password: String,
    val email: String? = null,
    val phone: String? = null,
    val firstName: String,
    val lastName: String,
    val grade: Int? = null,
    val school: String? = null,
    val role: UserRole = UserRole.STUDENT
)

data class RefreshTokenRequest(val refreshToken: String)
data class LogoutRequest(val deviceToken: String? = null)
data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Int
)

data class UserUpdateRequest(
    val firstName: String? = null,
    val lastName: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val grade: Int? = null,
    val school: String? = null,
    val avatarUrl: String? = null
)

data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String
)

data class QuestionCountResponse(
    val total: Int,
    val byDifficulty: Map<DifficultyLevel, Int>,
    val byType: Map<QuestionType, Int>
)

data class QuestionBatchRequest(val questionIds: List<Int>)
data class SubmitExamRequest(val answers: List<StudentAnswer>)
data class UpdateSessionRequest(
    val currentQuestion: Int? = null,
    val answers: List<Map<String, Any>>? = null
)

enum class StatisticsPeriod { DAILY, WEEKLY, MONTHLY, YEARLY }

data class UserStatistics(
    val totalExams: Int,
    val averageScore: Float,
    val bestScore: Float,
    val totalTimeSpent: Int,
    val accuracyRate: Float,
    val rank: Int? = null
)

data class SubscriptionPlan(
    val id: Int,
    val name: String,
    val description: String,
    val price: Float,
    val durationDays: Int,
    val features: List<String>
)

data class SubscribeRequest(
    val planId: Int,
    val paymentMethod: String
)

data class Subscription(
    val id: Int,
    val planId: Int,
    val startDate: Long,
    val endDate: Long,
    val isActive: Boolean,
    val autoRenew: Boolean
)

data class Notification(
    val id: Int,
    val title: String,
    val message: String,
    val type: NotificationType,
    val data: Map<String, Any>? = null,
    val isRead: Boolean,
    val createdAt: Long
)

enum class NotificationType {
    EXAM_RESULT, NEW_EXAM, SYSTEM_UPDATE, PROMOTION, GENERAL
}

data class DeviceRegistrationRequest(
    val deviceId: String,
    val deviceToken: String,
    val platform: String = "android",
    val appVersion: String
)

data class FeedbackRequest(
    val rating: Int,
    val comment: String? = null,
    val category: FeedbackCategory
)

data class BugReportRequest(
    val title: String,
    val description: String,
    val stepsToReproduce: String? = null,
    val deviceInfo: String,
    val appVersion: String
)

enum class FeedbackCategory {
    GENERAL, BUG, FEATURE_REQUEST, UI_UX, PERFORMANCE
}