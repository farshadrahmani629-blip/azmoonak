package com.examapp.data.remote

import com.examapp.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import timber.log.Timber

/**
 * کلاس Singleton برای ایجاد و مدیریت ارتباط با سرور
 */
@Singleton
class ApiClient {

    // ==================== تنظیمات پایه ====================

    companion object {
        // آدرس اصلی سرور - نسخه Production
        const val BASE_URL = "https://azmoonak-edu.ir/api/v1/"

        // آدرس تستی برای توسعه - در صورت نیاز می‌توانید تغییر دهید
        const val BASE_URL_DEV = "http://10.0.2.2:8000/api/v1/" // برای شبیه‌ساز اندروید

        // Timeout تنظیمات
        private const val CONNECT_TIMEOUT = 30L
        private const val READ_TIMEOUT = 30L
        private const val WRITE_TIMEOUT = 30L
    }

    // ==================== Interceptor ها ====================

    /**
     * Interceptor برای اضافه کردن هدرهای عمومی به همه درخواست‌ها
     */
    private fun getHeadersInterceptor(): Interceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()

        // هدرهای عمومی
        requestBuilder
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .addHeader("Accept-Language", "fa") // زبان فارسی
            .addHeader("App-Version", BuildConfig.VERSION_NAME)
            .addHeader("Platform", "Android")
            .addHeader("OS-Version", android.os.Build.VERSION.RELEASE ?: "Unknown")

        // اگر توکن احراز هویت وجود داشت، اضافه می‌شود
        // در آینده این بخش را از SharedPreferences یا Datastore می‌خوانیم
        val authToken = getAuthToken()
        authToken?.let {
            requestBuilder.addHeader("Authorization", "Bearer $it")
        }

        val request = requestBuilder.build()
        chain.proceed(request)
    }

    /**
     * Interceptor برای لاگ کردن درخواست‌ها و پاسخ‌ها (فقط در حالت Debug)
     */
    private fun getLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor { message ->
            Timber.tag("API").d(message)
        }.apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }

    /**
     * Interceptor برای مدیریت خطاهای سرور
     */
    private fun getErrorInterceptor(): Interceptor = Interceptor { chain ->
        val request = chain.request()
        val response = chain.proceed(request)

        // بررسی کدهای خطای HTTP
        if (!response.isSuccessful) {
            Timber.tag("API").e("HTTP Error: ${response.code} - ${response.message}")

            // می‌توانید خطاهای خاص را اینجا مدیریت کنید
            when (response.code) {
                401 -> {
                    // توکن منقضی شده - باید کاربر را به صفحه لاگین هدایت کرد
                    // این رویداد را به ViewModel اطلاع می‌دهیم
                }
                403 -> {
                    // دسترسی ممنوع
                }
                404 -> {
                    // منبع پیدا نشد
                }
                500 -> {
                    // خطای داخلی سرور
                }
                503 -> {
                    // سرور در دسترس نیست
                }
            }
        }

        response
    }

    // ==================== Client و Retrofit ====================

    /**
     * ایجاد OkHttpClient با تنظیمات مورد نیاز
     */
    private fun getOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
            .addInterceptor(getHeadersInterceptor())
            .addInterceptor(getLoggingInterceptor())
            .addInterceptor(getErrorInterceptor())
            .retryOnConnectionFailure(true) // تلاش مجدد در صورت قطع ارتباط
            .build()
    }

    /**
     * ایجاد نمونه Retrofit
     */
    private fun getRetrofit(baseUrl: String = BASE_URL): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(getOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // ==================== سرویس‌ها ====================

    /**
     * سرویس اصلی Exam
     */
    val examService: ExamApiService by lazy {
        getRetrofit().create(ExamApiService::class.java)
    }

    /**
     * سرویس برای حالت توسعه (با آدرس متفاوت)
     */
    val examServiceDev: ExamApiService by lazy {
        getRetrofit(BASE_URL_DEV).create(ExamApiService::class.java)
    }

    // ==================== متدهای کمکی ====================

    /**
     * دریافت توکن احراز هویت از ذخیره‌سازی محلی
     * فعلاً خالی است - در آینده پیاده‌سازی می‌شود
     */
    private fun getAuthToken(): String? {
        // TODO: در آینده از SharedPreferences یا DataStore خوانده می‌شود
        return null
    }

    /**
     * ذخیره توکن احراز هویت
     */
    fun setAuthToken(token: String) {
        // TODO: در آینده در SharedPreferences یا DataStore ذخیره می‌شود
        Timber.tag("API").d("Auth token set: ${token.take(10)}...")
    }

    /**
     * پاک کردن توکن احراز هویت (برای خروج از حساب)
     */
    fun clearAuthToken() {
        // TODO: در آینده از SharedPreferences یا DataStore پاک می‌شود
        Timber.tag("API").d("Auth token cleared")
    }

    /**
     * بررسی وضعیت اتصال به سرور
     */
    suspend fun checkServerHealth(): Boolean {
        return try {
            val response = examService.checkServerStatus()
            response.isSuccessful() && response.data?.isServerUp() == true
        } catch (e: Exception) {
            Timber.tag("API").e(e, "Server health check failed")
            false
        }
    }

    /**
     * دریافت سرویس فعال (بر اساس حالت توسعه/تولید)
     */
    fun getActiveService(): ExamApiService {
        return if (BuildConfig.DEBUG && BuildConfig.FLAVOR == "dev") {
            examServiceDev
        } else {
            examService
        }
    }
}

/**
 * Extension function برای استفاده راحت‌تر از ApiClient
 */
fun getApiClient(): ApiClient = ApiClient()