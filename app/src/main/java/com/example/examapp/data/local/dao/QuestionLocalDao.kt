// app/src/main/java/com/examapp/data/local/dao/QuestionLocalDao.kt
package com.examapp.data.local.dao

import androidx.room.*
import com.examapp.data.models.Question
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestionLocalDao {

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

    // ------------ دریافت سوالات ------------

    @Query("SELECT * FROM questions WHERE is_active = 1 ORDER BY book_id, chapter_id, page_number")
    fun getAllQuestions(): Flow<List<Question>>

    @Query("SELECT * FROM questions WHERE id = :id LIMIT 1")
    suspend fun getQuestionById(id: Int): Question?

    @Query("SELECT * FROM questions WHERE fingerprint = :fingerprint LIMIT 1")
    suspend fun getQuestionByFingerprint(fingerprint: String): Question?

    // ------------ فیلتر برای ساخت آزمون ------------

    @Query("SELECT * FROM questions WHERE book_id = :bookId AND is_active = 1 ORDER BY chapter_id, page_number")
    fun getQuestionsByBook(bookId: Int): Flow<List<Question>>

    @Query("""
        SELECT * FROM questions 
        WHERE grade = :grade 
        AND subject = :subject 
        AND (:fromPage IS NULL OR page_number >= :fromPage)
        AND (:toPage IS NULL OR page_number <= :toPage)
        AND (:difficulty IS NULL OR difficulty = :difficulty)
        AND (:bloomLevel IS NULL OR bloom_level = :bloomLevel)
        AND is_active = 1
        ORDER BY RANDOM()
        LIMIT :limit
    """)
    suspend fun getFilteredQuestions(
        grade: Int,
        subject: String,
        fromPage: Int?,
        toPage: Int?,
        difficulty: String?,
        bloomLevel: String?,
        limit: Int
    ): List<Question>

    @Query("""
        SELECT * FROM questions 
        WHERE book_id = :bookId
        AND chapter_id = :chapterId
        AND (:fromPage IS NULL OR page_number >= :fromPage)
        AND (:toPage IS NULL OR page_number <= :toPage)
        AND (:difficulty IS NULL OR difficulty = :difficulty)
        AND is_active = 1
        ORDER BY RANDOM()
        LIMIT :limit
    """)
    suspend fun getFilteredQuestionsByChapter(
        bookId: Int,
        chapterId: Int,
        fromPage: Int?,
        toPage: Int?,
        difficulty: String?,
        limit: Int
    ): List<Question>

    // ------------ آمار و اطلاعات ------------

    @Query("SELECT COUNT(*) FROM questions WHERE is_active = 1")
    suspend fun getTotalCount(): Int

    @Query("SELECT COUNT(*) FROM questions WHERE grade = :grade AND subject = :subject AND is_active = 1")
    suspend fun getQuestionCount(grade: Int, subject: String): Int

    @Query("SELECT COUNT(*) FROM questions WHERE book_id = :bookId AND is_active = 1")
    suspend fun getQuestionCountByBook(bookId: Int): Int

    @Query("SELECT DISTINCT subject FROM questions WHERE grade = :grade AND is_active = 1")
    suspend fun getSubjectsByGrade(grade: Int): List<String>

    @Query("SELECT DISTINCT grade FROM questions WHERE is_active = 1 ORDER BY grade")
    suspend fun getAllGrades(): List<Int>

    @Query("SELECT DISTINCT book_id FROM questions WHERE grade = :grade AND subject = :subject AND is_active = 1")
    suspend fun getBookIdsByGradeAndSubject(grade: Int, subject: String): List<Int>

    @Query("SELECT MIN(page_number) as minPage, MAX(page_number) as maxPage FROM questions WHERE grade = :grade AND subject = :subject AND is_active = 1")
    suspend fun getPageRange(grade: Int, subject: String): PageRange

    @Query("SELECT MIN(page_number) as minPage, MAX(page_number) as maxPage FROM questions WHERE book_id = :bookId AND is_active = 1")
    suspend fun getPageRangeByBook(bookId: Int): PageRange

    // ------------ مدیریت داده آفلاین ------------

    @Query("UPDATE questions SET is_active = :isActive WHERE id = :questionId")
    suspend fun updateQuestionStatus(questionId: Int, isActive: Boolean): Int

    @Query("UPDATE questions SET last_synced = :timestamp WHERE id = :questionId")
    suspend fun updateSyncTimestamp(questionId: Int, timestamp: Long): Int

    @Query("DELETE FROM questions WHERE last_synced < :timestamp AND is_bookmarked = 0")
    suspend fun deleteOldUnsyncedQuestions(timestamp: Long): Int

    @Query("SELECT COUNT(*) FROM questions WHERE last_synced IS NULL OR last_synced < :timestamp")
    suspend fun getUnsyncedCount(timestamp: Long): Int

    // برای دانلود تدریجی
    @Query("SELECT * FROM questions WHERE book_id = :bookId AND chapter_id = :chapterId AND page_number >= :startPage AND page_number <= :endPage AND is_active = 1")
    suspend fun getQuestionsByPageRange(
        bookId: Int,
        chapterId: Int,
        startPage: Int,
        endPage: Int
    ): List<Question>

    data class PageRange(val minPage: Int, val maxPage: Int)
}