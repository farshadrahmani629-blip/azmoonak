// app/src/main/java/com/examapp/di/AppModule.kt
package com.examapp.di

import android.content.Context
import com.examapp.data.network.ApiClient
import com.examapp.data.repository.*
import com.examapp.data.database.ExamDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideApiService(): ApiClient.ApiService {
        return ApiClient.apiService
    }

    @Provides
    @Singleton
    fun provideBookRepository(
        apiService: ApiClient.ApiService,
        examDatabase: ExamDatabase
    ): BookRepository {
        return BookRepository(apiService, examDatabase.bookDao())
    }

    @Provides
    @Singleton
    fun provideQuestionRepository(
        apiService: ApiClient.ApiService,
        examDatabase: ExamDatabase
    ): QuestionRepository {
        return QuestionRepository(apiService, examDatabase.questionDao())
    }

    @Provides
    @Singleton
    fun provideExamRepository(
        apiService: ApiClient.ApiService,
        examDatabase: ExamDatabase
    ): ExamRepository {
        return ExamRepository(apiService, examDatabase.examDao())
    }

    @Provides
    @Singleton
    fun provideAuthRepository(
        apiService: ApiClient.ApiService,
        examDatabase: ExamDatabase
    ): AuthRepository {
        return AuthRepository(apiService, examDatabase.userDao())
    }

    @Provides
    @Singleton
    fun provideExamDatabase(@ApplicationContext context: Context): ExamDatabase {
        return ExamDatabase.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): AppPreferences {
        return AppPreferences(context)
    }
}

// SharedPreferences helper class
class AppPreferences(context: Context) {
    private val sharedPreferences = context.getSharedPreferences(
        "exam_app_prefs",
        Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
    }

    // Auth related
    fun saveAuthToken(token: String) {
        sharedPreferences.edit().putString(KEY_AUTH_TOKEN, token).apply()
    }

    fun getAuthToken(): String? {
        return sharedPreferences.getString(KEY_AUTH_TOKEN, null)
    }

    fun saveUserId(userId: String) {
        sharedPreferences.edit().putString(KEY_USER_ID, userId).apply()
    }

    fun getUserId(): String? {
        return sharedPreferences.getString(KEY_USER_ID, null)
    }

    fun saveUserName(userName: String) {
        sharedPreferences.edit().putString(KEY_USER_NAME, userName).apply()
    }

    fun getUserName(): String? {
        return sharedPreferences.getString(KEY_USER_NAME, null)
    }

    fun setLoggedIn(isLoggedIn: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_IS_LOGGED_IN, isLoggedIn).apply()
    }

    fun isLoggedIn(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    // Exam preferences
    fun saveLastExamId(examId: String) {
        sharedPreferences.edit().putString("last_exam_id", examId).apply()
    }

    fun getLastExamId(): String? {
        return sharedPreferences.getString("last_exam_id", null)
    }

    // Settings
    fun saveTheme(theme: String) {
        sharedPreferences.edit().putString("app_theme", theme).apply()
    }

    fun getTheme(): String {
        return sharedPreferences.getString("app_theme", "system") ?: "system"
    }

    fun saveLanguage(language: String) {
        sharedPreferences.edit().putString("app_language", language).apply()
    }

    fun getLanguage(): String {
        return sharedPreferences.getString("app_language", "fa") ?: "fa"
    }

    // Clear all preferences
    fun clear() {
        sharedPreferences.edit().clear().apply()
    }

    // Clear only auth data
    fun clearAuthData() {
        sharedPreferences.edit()
            .remove(KEY_AUTH_TOKEN)
            .remove(KEY_USER_ID)
            .remove(KEY_USER_NAME)
            .remove(KEY_IS_LOGGED_IN)
            .apply()
    }
}