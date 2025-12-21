// app/src/main/java/com/examapp/data/local/dao/QuestionDao.kt
package com.examapp.data.local.dao

import androidx.room.*
import com.examapp.data.models.local.QuestionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(question: QuestionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<QuestionEntity>)

    @Update
    suspend fun updateQuestion(question: QuestionEntity)

    @Delete
    suspend fun deleteQuestion(question: QuestionEntity)

    @Query("DELETE FROM questions")
    suspend fun deleteAllQuestions()

    @Query("SELECT * FROM questions ORDER BY id DESC")
    fun getAllQuestions(): Flow<List<QuestionEntity>>

    @Query("SELECT * FROM questions WHERE id = :questionId")
    suspend fun getQuestionById(questionId: Int): QuestionEntity?

    @Query("SELECT * FROM questions WHERE uuid = :uuid")
    suspend fun getQuestionByUuid(uuid: String): QuestionEntity?

    @Query("""
        SELECT * FROM questions 
        WHERE book_id = :bookId 
        ORDER BY page_number, chapter_id
    """)
    fun getQuestionsByBook(bookId: Int): Flow<List<QuestionEntity>>

    @Query("""
        SELECT * FROM questions 
        WHERE chapter_id = :chapterId 
        ORDER BY page_number
    """)
    fun getQuestionsByChapter(chapterId: Int): Flow<List<QuestionEntity>>

    @Query("""
        SELECT * FROM questions 
        WHERE book_id = :bookId 
        AND page_number BETWEEN :startPage AND :endPage
        ORDER BY page_number
    """)
    fun getQuestionsByPageRange(bookId: Int, startPage: Int, endPage: Int): Flow<List<QuestionEntity>>

    @Query("""
        SELECT * FROM questions 
        WHERE book_id = :bookId 
        AND difficulty = :difficulty
        ORDER BY RANDOM() 
        LIMIT :limit
    """)
    suspend fun getRandomQuestionsByDifficulty(
        bookId: Int,
        difficulty: String,
        limit: Int
    ): List<QuestionEntity>

    @Query("""
        SELECT COUNT(*) FROM questions 
        WHERE book_id = :bookId
    """)
    suspend fun getQuestionCountByBook(bookId: Int): Int

    @Query("""
        SELECT * FROM questions 
        WHERE book_id = :bookId 
        AND question_type = :questionType
        ORDER BY RANDOM() 
        LIMIT :count
    """)
    suspend fun getRandomQuestionsByType(
        bookId: Int,
        questionType: String,
        count: Int
    ): List<QuestionEntity>

    @Query("""
        SELECT DISTINCT subject FROM questions 
        WHERE book_id = :bookId
    """)
    suspend fun getSubjectsByBook(bookId: Int): List<String>

    @Query("""
        SELECT COUNT(*) FROM questions 
        WHERE book_id = :bookId 
        AND difficulty = :difficulty
    """)
    suspend fun getDifficultyCount(bookId: Int, difficulty: String): Int
}