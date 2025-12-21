// app/src/main/java/com/examapp/data/repository/BookRepository.kt
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
class BookRepository @Inject constructor(
    private val apiService: ApiClient.ApiService,
    private val database: ExamDatabase
) {

    suspend fun getBooks(
        grade: Int? = null,
        subject: String? = null
    ): Result<List<Book>> = withContext(Dispatchers.IO) {
        try {
            val response: Response<ApiResponse<List<Book>>> =
                apiService.getBooks(grade, subject)

            if (response.isSuccessful) {
                val books = response.body()?.data ?: emptyList()
                // ذخیره در دیتابیس
                database.bookDao().insertAll(books)
                Result.success(books)
            } else {
                Result.failure(Exception("خطا در دریافت کتاب‌ها"))
            }
        } catch (e: Exception) {
            // اگر خطا خورد، از دیتابیس بگیر
            val cachedBooks = database.bookDao().getBooks(grade, subject)
            if (cachedBooks.isNotEmpty()) {
                Result.success(cachedBooks)
            } else {
                Result.failure(Exception("خطا در ارتباط: ${e.message}"))
            }
        }
    }

    fun getBooksFlow(grade: Int? = null, subject: String? = null): Flow<List<Book>> = flow {
        // اول دیتابیس
        val localBooks = database.bookDao().getBooks(grade, subject)
        emit(localBooks)

        try {
            // سپس API
            val response = apiService.getBooks(grade, subject)
            if (response.isSuccessful) {
                val books = response.body()?.data ?: emptyList()
                database.bookDao().insertAll(books)
                emit(books)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }.flowOn(Dispatchers.IO)

    suspend fun getChapters(bookId: Int): Result<List<Chapter>> = withContext(Dispatchers.IO) {
        try {
            val response: Response<ApiResponse<List<Chapter>>> =
                apiService.getBookChapters(bookId)

            if (response.isSuccessful) {
                val chapters = response.body()?.data ?: emptyList()
                database.chapterDao().insertAll(chapters)
                Result.success(chapters)
            } else {
                Result.failure(Exception("خطا در دریافت فصل‌ها"))
            }
        } catch (e: Exception) {
            val cachedChapters = database.chapterDao().getChaptersByBook(bookId)
            if (cachedChapters.isNotEmpty()) {
                Result.success(cachedChapters)
            } else {
                Result.failure(Exception("خطا در ارتباط: ${e.message}"))
            }
        }
    }

    suspend fun getBookById(bookId: Int): Result<Book> = withContext(Dispatchers.IO) {
        try {
            // اول دیتابیس
            val localBook = database.bookDao().getBookById(bookId)
            if (localBook != null) {
                return@withContext Result.success(localBook)
            }

            // سپس API
            val response: Response<ApiResponse<Book>> = apiService.getBookById(bookId)
            if (response.isSuccessful && response.body()?.success == true) {
                val book = response.body()?.data
                if (book != null) {
                    database.bookDao().insertBook(book)
                    Result.success(book)
                } else {
                    Result.failure(Exception("کتاب یافت نشد"))
                }
            } else {
                Result.failure(Exception("خطا در دریافت کتاب"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("خطا: ${e.message}"))
        }
    }

    suspend fun searchBooks(query: String): Result<List<Book>> = withContext(Dispatchers.IO) {
        try {
            val allBooks = database.bookDao().getAllBooks()
            val filtered = allBooks.filter { book ->
                book.title.contains(query, ignoreCase = true) ||
                        book.subject.contains(query, ignoreCase = true) ||
                        book.publisher?.contains(query, ignoreCase = true) == true
            }
            Result.success(filtered)
        } catch (e: Exception) {
            Result.failure(Exception("خطا در جستجو: ${e.message}"))
        }
    }
}