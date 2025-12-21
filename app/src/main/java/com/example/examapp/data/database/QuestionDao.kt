// app/src/main/java/com/examapp/data/database/dao/QuestionLocalDao.kt
package com.examapp.data.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.examapp.data.models.DifficultyLevel
import com.examapp.data.models.Question
import com.examapp.data.models.QuestionType
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestionDao {

    // ------------ عملیات CRUD ------------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(question: Question): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(questions: List<Question>): List<Long>

    @Update
    suspend fun update(question: Question): Int

    @Delete
    suspend fun delete(question: Question): Int

    @Query("DELETE FROM questions")
    suspend fun deleteAll(): Int

    @Query("DELETE FROM questions WHERE id = :id")
    suspend fun deleteById(id: Int): Int

    // ------------ کوئری‌های دریافت ------------

    @Query("SELECT * FROM questions ORDER BY chapter_id, page_number")
    suspend fun getAllQuestions(): List<Question>

    @Query("SELECT * FROM questions ORDER BY chapter_id, page_number")
    fun getAllQuestionsFlow(): Flow<List<Question>>

    @Query("SELECT * FROM questions ORDER BY chapter_id, page_number")
    fun getAllQuestionsLiveData(): LiveData<List<Question>>

    @Query("SELECT * FROM questions WHERE id = :id LIMIT 1")
    suspend fun getQuestionById(id: Int): Question?

    @Query("SELECT * FROM questions WHERE id = :id LIMIT 1")
    fun getQuestionByIdFlow(id: Int): Flow<Question?>

    @Query("SELECT * FROM questions WHERE fingerprint = :fingerprint LIMIT 1")
    suspend fun getQuestionByFingerprint(fingerprint: String): Question?

    // ------------ فیلتر کردن ------------

    @Query("SELECT * FROM questions WHERE book_id = :bookId ORDER BY page_number")
    fun getQuestionsByBook(bookId: Int): Flow<List<Question>>

    @Query("SELECT * FROM questions WHERE chapter_id = :chapterId ORDER BY page_number")
    fun getQuestionsByChapter(chapterId: Int): Flow<List<Question>>

    @Query("SELECT * FROM questions WHERE difficulty = :difficulty ORDER BY marks DESC")
    fun getQuestionsByDifficulty(difficulty: DifficultyLevel): Flow<List<Question>>

    @Query("SELECT * FROM questions WHERE question_type = :questionType ORDER BY marks DESC")
    fun getQuestionsByType(questionType: QuestionType): Flow<List<Question>>

    @Query("SELECT * FROM questions WHERE is_active = 1 ORDER BY chapter_id, page_number")
    fun getActiveQuestions(): Flow<List<Question>>

    // ------------ کوئری‌های ترکیبی ------------

    @Query("""
        SELECT * FROM questions 
        WHERE book_id = :bookId 
        AND chapter_id = :chapterId
        AND difficulty = :difficulty
        AND question_type = :questionType
        ORDER BY RANDOM() 
        LIMIT :limit
    """)
    suspend fun getFilteredQuestions(
        bookId: Int,
        chapterId: Int,
        difficulty: DifficultyLevel,
        questionType: QuestionType,
        limit: Int
    ): List<Question>

    @Query("""
        SELECT * FROM questions 
        WHERE book_id = :bookId 
        AND page_number BETWEEN :startPage AND :endPage
        AND is_active = 1
        ORDER BY RANDOM() 
        LIMIT :limit
    """)
    suspend fun getRandomQuestionsFromPages(
        bookId: Int,
        startPage: Int,
        endPage: Int,
        limit: Int
    ): List<Question>

    @Query("""
        SELECT * FROM questions 
        WHERE book_id = :bookId 
        AND difficulty = :difficulty
        AND is_active = 1
        ORDER BY RANDOM() 
        LIMIT :limit
    """)
    suspend fun getRandomQuestionsByDifficulty(
        bookId: Int,
        difficulty: DifficultyLevel,
        limit: Int
    ): List<Question>

    // ------------ آمار ------------

    @Query("SELECT COUNT(*) FROM questions")
    suspend fun getTotalCount(): Int

    @Query("SELECT COUNT(*) FROM questions WHERE book_id = :bookId")
    suspend fun getCountByBook(bookId: Int): Int

    @Query("SELECT COUNT(*) FROM questions WHERE chapter_id = :chapterId")
    suspend fun getCountByChapter(chapterId: Int): Int

    @Query("SELECT COUNT(*) FROM questions WHERE difficulty = :difficulty")
    suspend fun getCountByDifficulty(difficulty: DifficultyLevel): Int

    @Query("SELECT COUNT(*) FROM questions WHERE question_type = :questionType")
    suspend fun getCountByType(questionType: QuestionType): Int

    @Query("SELECT COUNT(*) FROM questions WHERE is_active = 1")
    suspend fun getActiveCount(): Int

    // ------------ جستجو ------------

    @Query("""
        SELECT * FROM questions 
        WHERE question_text LIKE '%' || :query || '%' 
        OR explanation LIKE '%' || :query || '%'
        ORDER BY book_id, chapter_id, page_number
    """)
    suspend fun searchQuestions(query: String): List<Question>

    @Query("""
        SELECT * FROM questions 
        WHERE question_text LIKE '%' || :query || '%' 
        OR explanation LIKE '%' || :query || '%'
        ORDER BY book_id, chapter_id, page_number
    """)
    fun searchQuestionsFlow(query: String): Flow<List<Question>>

    // ------------ مدیریت وضعیت سوالات ------------

    @Query("UPDATE questions SET is_active = :isActive WHERE id = :questionId")
    suspend fun updateQuestionStatus(questionId: Int, isActive: Boolean): Int

    @Query("UPDATE questions SET user_answer = :answer WHERE id = :questionId")
    suspend fun updateUserAnswer(questionId: Int, answer: String?): Int

    @Query("UPDATE questions SET is_answered = :isAnswered WHERE id = :questionId")
    suspend fun updateAnsweredStatus(questionId: Int, isAnswered: Boolean): Int

    @Query("UPDATE questions SET is_bookmarked = :isBookmarked WHERE id = :questionId")
    suspend fun updateBookmarkStatus(questionId: Int, isBookmarked: Boolean): Int

    // ------------ سوالات پاسخ داده شده/نشده ------------

    @Query("SELECT * FROM questions WHERE is_answered = 1 ORDER BY last_accessed DESC")
    fun getAnsweredQuestions(): Flow<List<Question>>

    @Query("SELECT * FROM questions WHERE is_answered = 0 ORDER BY chapter_id, page_number")
    fun getUnansweredQuestions(): Flow<List<Question>>

    @Query("SELECT * FROM questions WHERE is_bookmarked = 1 ORDER BY last_accessed DESC")
    fun getBookmarkedQuestions(): Flow<List<Question>>

    // ------------ پاک کردن داده‌های قدیمی ------------

    @Query("DELETE FROM questions WHERE last_accessed < :timestamp AND is_bookmarked = 0")
    suspend fun deleteOldQuestions(timestamp: Long): Int

    @Query("DELETE FROM questions WHERE book_id = :bookId AND is_active = 0")
    suspend fun deleteInactiveQuestionsByBook(bookId: Int): Int
}