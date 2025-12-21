// app/src/main/java/com/examapp/data/network/ApiService.kt
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

// مدل‌های درخواست اضافی
data class LoginRequest(
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String,
    @SerializedName("device_token") val deviceToken: String? = null
)

data class RegisterRequest(
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String,
    @SerializedName("email") val email: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("first_name") val firstName: String,
    @SerializedName("last_name") val lastName: String,
    @SerializedName("grade") val grade: Int? = null,
    @SerializedName("school") val school: String? = null,
    @SerializedName("role") val role: UserRole = UserRole.STUDENT
)

data class RefreshTokenRequest(@SerializedName("refresh_token") val refreshToken: String)
data class LogoutRequest(@SerializedName("device_token") val deviceToken: String? = null)
data class AuthResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String,
    @SerializedName("expires_in") val expiresIn: Int
)

data class UserUpdateRequest(
    @SerializedName("first_name") val firstName: String? = null,
    @SerializedName("last_name") val lastName: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("grade") val grade: Int? = null,
    @SerializedName("school") val school: String? = null,
    @SerializedName("avatar_url") val avatarUrl: String? = null
)

data class ChangePasswordRequest(
    @SerializedName("current_password") val currentPassword: String,
    @SerializedName("new_password") val newPassword: String
)

data class QuestionCountResponse(
    @SerializedName("total") val total: Int,
    @SerializedName("by_difficulty") val byDifficulty: Map<DifficultyLevel, Int>,
    @SerializedName("by_type") val byType: Map<QuestionType, Int>
)

data class QuestionBatchRequest(@SerializedName("question_ids") val questionIds: List<Int>)
data class SubmitExamRequest(@SerializedName("answers") val answers: List<StudentAnswer>)
data class UpdateSessionRequest(
    @SerializedName("current_question") val currentQuestion: Int? = null,
    @SerializedName("answers") val answers: List<Map<String, Any>>? = null
)

enum class StatisticsPeriod { DAILY, WEEKLY, MONTHLY, YEARLY }

data class UserStatistics(
    @SerializedName("total_exams") val totalExams: Int,
    @SerializedName("average_score") val averageScore: Float,
    @SerializedName("best_score") val bestScore: Float,
    @SerializedName("total_time_spent") val totalTimeSpent: Int,
    @SerializedName("accuracy_rate") val accuracyRate: Float,
    @SerializedName("rank") val rank: Int? = null
)

data class SubscriptionPlan(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String,
    @SerializedName("price") val price: Float,
    @SerializedName("duration_days") val durationDays: Int,
    @SerializedName("features") val features: List<String>
)

data class SubscribeRequest(
    @SerializedName("plan_id") val planId: Int,
    @SerializedName("payment_method") val paymentMethod: String
)

data class Subscription(
    @SerializedName("id") val id: Int,
    @SerializedName("plan_id") val planId: Int,
    @SerializedName("start_date") val startDate: Long,
    @SerializedName("end_date") val endDate: Long,
    @SerializedName("is_active") val isActive: Boolean,
    @SerializedName("auto_renew") val autoRenew: Boolean
)

data class Notification(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String,
    @SerializedName("message") val message: String,
    @SerializedName("type") val type: NotificationType,
    @SerializedName("data") val data: Map<String, Any>? = null,
    @SerializedName("is_read") val isRead: Boolean,
    @SerializedName("created_at") val createdAt: Long
)

enum class NotificationType {
    EXAM_RESULT, NEW_EXAM, SYSTEM_UPDATE, PROMOTION, GENERAL
}

data class DeviceRegistrationRequest(
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("device_token") val deviceToken: String,
    @SerializedName("platform") val platform: String = "android",
    @SerializedName("app_version") val appVersion: String
)

data class FeedbackRequest(
    @SerializedName("rating") val rating: Int,
    @SerializedName("comment") val comment: String? = null,
    @SerializedName("category") val category: FeedbackCategory
)

data class BugReportRequest(
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String,
    @SerializedName("steps_to_reproduce") val stepsToReproduce: String? = null,
    @SerializedName("device_info") val deviceInfo: String,
    @SerializedName("app_version") val appVersion: String
)

enum class FeedbackCategory {
    GENERAL, BUG, FEATURE_REQUEST, UI_UX, PERFORMANCE
}