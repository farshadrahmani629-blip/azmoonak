// app/src/main/java/com/examapp/data/repository/AuthRepository.kt
package com.examapp.data.repository

import com.examapp.data.network.ApiClient
import com.examapp.data.models.*
import com.examapp.data.preferences.AuthPreferences
import com.examapp.data.database.ExamDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val apiService: ApiClient.ApiService,
    private val authPreferences: AuthPreferences,
    private val database: ExamDatabase
) {

    suspend fun login(username: String, password: String): Result<User> = withContext(Dispatchers.IO) {
        try {
            val request = LoginRequest(username, password)
            val response: Response<ApiResponse<User>> = apiService.login(request)

            if (response.isSuccessful) {
                val apiResponse = response.body()
                if (apiResponse?.success == true) {
                    val user = apiResponse.data
                    if (user != null) {
                        // ذخیره کاربر
                        database.userDao().insertUser(user)

                        // ذخیره توکن اگر وجود داشت
                        val authHeader = response.headers()["Authorization"]
                        authHeader?.let { header ->
                            if (header.startsWith("Bearer ")) {
                                val token = header.substring(7)
                                authPreferences.saveAuthToken(token)
                            }
                        }

                        // ذخیره اطلاعات کاربر در preferences
                        authPreferences.saveUserId(user.id)
                        authPreferences.saveUserRole(user.role)

                        Result.success(user)
                    } else {
                        Result.failure(Exception("اطلاعات کاربر دریافت نشد"))
                    }
                } else {
                    Result.failure(Exception(apiResponse?.message ?: "خطا در ورود"))
                }
            } else {
                Result.failure(Exception("خطای سرور: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("خطا در ارتباط: ${e.message}"))
        }
    }

    suspend fun register(request: RegisterRequest): Result<User> = withContext(Dispatchers.IO) {
        try {
            val response: Response<ApiResponse<User>> = apiService.register(request)

            if (response.isSuccessful) {
                val apiResponse = response.body()
                if (apiResponse?.success == true) {
                    val user = apiResponse.data
                    if (user != null) {
                        database.userDao().insertUser(user)
                        Result.success(user)
                    } else {
                        Result.failure(Exception("کاربر ایجاد نشد"))
                    }
                } else {
                    Result.failure(Exception(apiResponse?.message ?: "خطا در ثبت‌نام"))
                }
            } else {
                Result.failure(Exception("خطای سرور: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("خطا در ارتباط: ${e.message}"))
        }
    }

    suspend fun getCurrentUser(): User? = withContext(Dispatchers.IO) {
        val userId = authPreferences.getUserId()
        userId?.let { database.userDao().getUserById(it) }
    }

    suspend fun updateUserProfile(request: UserUpdateRequest): Result<User> = withContext(Dispatchers.IO) {
        try {
            val userId = authPreferences.getUserId()
            if (userId == null) {
                return@withContext Result.failure(Exception("کاربر لاگین نیست"))
            }

            val response: Response<ApiResponse<User>> = apiService.updateUserProfile(userId, request)

            if (response.isSuccessful) {
                val apiResponse = response.body()
                if (apiResponse?.success == true) {
                    val updatedUser = apiResponse.data
                    if (updatedUser != null) {
                        database.userDao().insertUser(updatedUser)
                        Result.success(updatedUser)
                    } else {
                        Result.failure(Exception("پروفایل به‌روزرسانی نشد"))
                    }
                } else {
                    Result.failure(Exception(apiResponse?.message ?: "خطا در به‌روزرسانی"))
                }
            } else {
                Result.failure(Exception("خطای سرور: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("خطا در ارتباط: ${e.message}"))
        }
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        try {
            // ارسال درخواست logout به سرور
            val deviceToken = authPreferences.getDeviceToken()
            if (deviceToken != null) {
                val request = LogoutRequest(deviceToken)
                apiService.logout(request)
            }
        } catch (e: Exception) {
            // حتی اگر خطا هم خورد، ادامه بده
            e.printStackTrace()
        } finally {
            // پاک‌سازی اطلاعات محلی
            authPreferences.clear()
            database.clearAllTables()
        }
    }

    fun isLoggedIn(): Boolean {
        return authPreferences.getAuthToken() != null && authPreferences.getUserId() != null
    }

    fun getAuthToken(): String? {
        return authPreferences.getAuthToken()
    }

    suspend fun refreshToken(): Boolean = withContext(Dispatchers.IO) {
        val refreshToken = authPreferences.getRefreshToken()
        if (refreshToken == null) {
            return@withContext false
        }

        try {
            val request = RefreshTokenRequest(refreshToken)
            val response: Response<ApiResponse<AuthResponse>> = apiService.refreshToken(request)

            if (response.isSuccessful && response.body()?.success == true) {
                val authResponse = response.body()?.data
                if (authResponse != null) {
                    authPreferences.saveAuthToken(authResponse.accessToken)
                    authPreferences.saveRefreshToken(authResponse.refreshToken)
                    true
                } else {
                    false
                }
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
}

// فایل AuthPreferences.kt
package com.examapp.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.examapp.data.models.UserRole
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthPreferences @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val PREFS_NAME = "exam_app_auth"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_DEVICE_TOKEN = "device_token"
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveAuthToken(token: String) {
        prefs.edit().putString(KEY_AUTH_TOKEN, token).apply()
    }

    fun getAuthToken(): String? = prefs.getString(KEY_AUTH_TOKEN, null)

    fun saveRefreshToken(token: String) {
        prefs.edit().putString(KEY_REFRESH_TOKEN, token).apply()
    }

    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)

    fun saveUserId(userId: Int) {
        prefs.edit().putInt(KEY_USER_ID, userId).apply()
    }

    fun getUserId(): Int? {
        val id = prefs.getInt(KEY_USER_ID, -1)
        return if (id != -1) id else null
    }

    fun saveUserRole(role: UserRole) {
        prefs.edit().putString(KEY_USER_ROLE, role.name).apply()
    }

    fun getUserRole(): UserRole? {
        val roleName = prefs.getString(KEY_USER_ROLE, null)
        return roleName?.let { UserRole.valueOf(it) }
    }

    fun saveDeviceToken(token: String) {
        prefs.edit().putString(KEY_DEVICE_TOKEN, token).apply()
    }

    fun getDeviceToken(): String? = prefs.getString(KEY_DEVICE_TOKEN, null)

    fun clear() {
        prefs.edit().clear().apply()
    }
}