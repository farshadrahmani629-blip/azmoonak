// app/src/main/java/com/examapp/data/local/dao/ResultDao.kt
package com.examapp.data.local.dao

import androidx.room.*
import com.examapp.data.models.local.ResultEntity
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface ResultDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResult(result: ResultEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResults(results: List<ResultEntity>)

    @Update
    suspend fun updateResult(result: ResultEntity)

    @Delete
    suspend fun deleteResult(result: ResultEntity)

    @Query("DELETE FROM results WHERE id = :resultId")
    suspend fun deleteResultById(resultId: Int)

    @Query("DELETE FROM results")
    suspend fun deleteAllResults()

    @Query("SELECT * FROM results ORDER BY date DESC")
    fun getAllResults(): Flow<List<ResultEntity>>

    @Query("SELECT * FROM results WHERE id = :resultId")
    suspend fun getResultById(resultId: Int): ResultEntity?

    @Query("SELECT * FROM results WHERE exam_id = :examId")
    fun getResultsByExam(examId: Int): Flow<List<ResultEntity>>

    @Query("SELECT * FROM results WHERE user_id = :userId ORDER BY date DESC")
    fun getResultsByUser(userId: Int): Flow<List<ResultEntity>>

    @Query("""
        SELECT * FROM results 
        WHERE date BETWEEN :startDate AND :endDate
        ORDER BY date DESC
    """)
    suspend fun getResultsBetweenDates(startDate: Date, endDate: Date): List<ResultEntity>

    @Query("""
        SELECT COUNT(*) FROM results 
        WHERE user_id = :userId
    """)
    suspend fun getResultCountByUser(userId: Int): Int

    @Query("""
        SELECT AVG(score) FROM results 
        WHERE user_id = :userId
    """)
    suspend fun getAverageScoreByUser(userId: Int): Float?

    @Query("""
        SELECT MAX(score) FROM results 
        WHERE user_id = :userId
    """)
    suspend fun getBestScoreByUser(userId: Int): Float?

    @Query("""
        SELECT * FROM results 
        WHERE user_id = :userId 
        AND exam_id = :examId
        ORDER BY date DESC 
        LIMIT 1
    """)
    suspend fun getLatestResultByUserAndExam(userId: Int, examId: Int): ResultEntity?
}