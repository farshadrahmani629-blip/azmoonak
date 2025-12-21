// app/src/main/java/com/examapp/data/network/ApiClient.kt
package com.examapp.data.network

import android.content.Context
import com.examapp.data.preferences.AuthPreferences
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiClient @Inject constructor(
    private val context: Context,
    private val authPreferences: AuthPreferences
) {

    // آدرس‌های سرور
    companion object {
        const val BASE_URL_DEV = "http://10.0.2.2:8000/api/"
        const val BASE_URL_PROD = "https://api.examapp.ir/api/"
        const val BASE_URL_STAGING = "https://staging.examapp.ir/api/"

        const val CONNECT_TIMEOUT = 30L
        const val READ_TIMEOUT = 30L
        const val WRITE_TIMEOUT = 30L
    }

    private val gson: Gson by lazy {
        GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .registerTypeAdapterFactory(EnumTypeAdapterFactory())
            .create()
    }

    private val authInterceptor by lazy {
        { chain ->
            val original = chain.request()
            val requestBuilder = original.newBuilder()
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("User-Agent", "ExamApp-Android/${getAppVersion()}")
                .header("Device-Id", getDeviceId())
                .header("Device-Type", "Android")

            // افزودن توکن احراز هویت
            authPreferences.getAuthToken()?.let { token ->
                if (token.isNotEmpty()) {
                    requestBuilder.header("Authorization", "Bearer $token")
                }
            }

            val request = requestBuilder.build()
            chain.proceed(request)
        }
    }

    private val loggingInterceptor by lazy {
        HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.BASIC
            }
        }
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .addInterceptor(authInterceptor)
            .addInterceptor(ConnectivityInterceptor(context))
            .addInterceptor(ErrorInterceptor())
            .build()
    }

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(getBaseUrl())
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .addCallAdapterFactory(CoroutineCallAdapterFactory())
            .build()
    }

    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }

    // متدهای کمکی
    private fun getBaseUrl(): String {
        return when (BuildConfig.BUILD_TYPE) {
            "debug" -> BASE_URL_DEV
            "staging" -> BASE_URL_STAGING
            else -> BASE_URL_PROD
        }
    }

    private fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    private fun getDeviceId(): String {
        return android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        ) ?: "unknown-device"
    }

    // تغییر آدرس سرور در زمان اجرا
    fun updateBaseUrl(newBaseUrl: String): ApiService {
        val newRetrofit = retrofit.newBuilder()
            .baseUrl(newBaseUrl)
            .build()
        return newRetrofit.create(ApiService::class.java)
    }

    // رفرش توکن
    fun updateAuthToken(token: String) {
        authPreferences.saveAuthToken(token)
    }
}

// اینترسپتور برای بررسی اتصال اینترنت
class ConnectivityInterceptor(private val context: Context) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        if (!isNetworkAvailable()) {
            throw NoNetworkException()
        }
        return chain.proceed(chain.request())
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
                as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }
}

// اینترسپتور برای مدیریت خطاها
class ErrorInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        when (response.code) {
            401 -> throw UnauthorizedException()
            403 -> throw ForbiddenException()
            404 -> throw NotFoundException()
            500 -> throw ServerException()
            502, 503, 504 -> throw ServiceUnavailableException()
            in 400..499 -> throw ClientException(response.message)
            in 500..599 -> throw ServerException(response.message)
        }

        return response
    }
}

// استثناهای سفارشی
class NoNetworkException : IOException("دستگاه شما به اینترنت متصل نیست")
class UnauthorizedException : IOException("دسترسی غیرمجاز. لطفاً دوباره وارد شوید")
class ForbiddenException : IOException("دسترسی به این منبع ممنوع است")
class NotFoundException : IOException("منبع مورد نظر یافت نشد")
class ServerException(message: String = "خطای سرور") : IOException(message)
class ServiceUnavailableException : IOException("سرور در دسترس نیست")
class ClientException(message: String) : IOException(message)

// Factory برای مدیریت enum ها
class EnumTypeAdapterFactory : TypeAdapterFactory {
    override fun <T> create(gson: Gson, type: Token<T>): TypeAdapter<T>? {
        val rawType = type.rawType as Class<T>
        if (!rawType.isEnum) return null

        return object : TypeAdapter<T>() {
            override fun write(out: JsonWriter, value: T) {
                out.value(value.toString().toLowerCase())
            }

            override fun read(`in`: JsonReader): T {
                val value = `in`.nextString()
                return rawType.enumConstants.firstOrNull {
                    it.toString().equals(value, ignoreCase = true)
                } ?: rawType.enumConstants.first()
            }
        }
    }
}