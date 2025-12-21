// app/src/main/java/com/examapp/data/database/dao/ResultDao.kt
package com.examapp.data.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.examapp.data.models.ExamResult
import kotlinx.coroutines.flow.Flow

@Dao
interface ResultDao {

    // ------------ عملیات CRUD ------------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(result: ExamResult): Long

    @Update
    suspend fun update(result: ExamResult): Int

    @Delete
    suspend fun delete(result: ExamResult): Int

    @Query("DELETE FROM exam_results")
    suspend fun deleteAll(): Int

    @Query("DELETE FROM exam_results WHERE id = :id")
    suspend fun deleteById(id: Int): Int

    @Query("DELETE FROM exam_results WHERE exam_id = :examId")
    suspend fun deleteByExamId(examId: Int): Int

    // ------------ دریافت نتایج ------------

    @Query("SELECT * FROM exam_results ORDER BY completed_at DESC")
    suspend fun getAllResults(): List<ExamResult>

    @Query("SELECT * FROM exam_results ORDER BY completed_at DESC")
    fun getAllResultsFlow(): Flow<List<ExamResult>>

    @Query("SELECT * FROM exam_results ORDER BY completed_at DESC")
    fun getAllResultsLiveData(): LiveData<List<ExamResult>>

    @Query("SELECT * FROM exam_results WHERE id = :id LIMIT 1")
    suspend fun getResultById(id: Int): ExamResult?

    @Query("SELECT * FROM exam_results WHERE id = :id LIMIT 1")
    fun getResultByIdFlow(id: Int): Flow<ExamResult?>

    @Query("SELECT * FROM exam_results WHERE exam_id = :examId")
    suspend fun getResultsByExamId(examId: Int): List<ExamResult>

    @Query("SELECT * FROM exam_results WHERE exam_id = :examId ORDER BY completed_at DESC LIMIT 1")
    suspend fun getLatestResultByExamId(examId: Int): ExamResult?

    // ------------ نتایج کاربر ------------

    @Query("SELECT * FROM exam_results WHERE user_id = :userId ORDER BY completed_at DESC")
    fun getResultsByUser(userId: Int): Flow<List<ExamResult>>

    @Query("SELECT * FROM exam_results WHERE user_id = :userId AND is_passed = 1 ORDER BY completed_at DESC")
    fun getPassedResultsByUser(userId: Int): Flow<List<ExamResult>>

    // ------------ فیلتر بر اساس وضعیت ------------

    @Query("SELECT * FROM exam_results WHERE is_passed = :isPassed ORDER BY completed_at DESC")
    fun getResultsByPassStatus(isPassed: Boolean): Flow<List<ExamResult>>

    @Query("SELECT * FROM exam_results WHERE score >= :minScore ORDER BY completed_at DESC")
    fun getResultsByMinScore(minScore: Float): Flow<List<ExamResult>>

    // ------------ آمار و تحلیل ------------

    @Query("SELECT COUNT(*) FROM exam_results")
    suspend fun getTotalCount(): Int

    @Query("SELECT COUNT(*) FROM exam_results WHERE is_passed = 1")
    suspend fun getPassedCount(): Int

    @Query("SELECT COUNT(*) FROM exam_results WHERE user_id = :userId")
    suspend fun getUserResultCount(userId: Int): Int

    @Query("SELECT AVG(score) FROM exam_results")
    suspend fun getAverageScore(): Float?

    @Query("SELECT AVG(score) FROM exam_results WHERE user_id = :userId")
    suspend fun getUserAverageScore(userId: Int): Float?

    @Query("SELECT AVG(time_spent) FROM exam_results")
    suspend fun getAverageTimeSpent(): Float?

    @Query("SELECT AVG(percentage) FROM exam_results WHERE user_id = :userId")
    suspend fun getUserAveragePercentage(userId: Int): Float?

    // ------------ بهترین نتایج ------------

    @Query("SELECT * FROM exam_results ORDER BY score DESC LIMIT :limit")
    suspend fun getTopResults(limit: Int = 10): List<ExamResult>

    @Query("SELECT * FROM exam_results WHERE user_id = :userId ORDER BY score DESC LIMIT :limit")
    suspend fun getUserTopResults(userId: Int, limit: Int = 10): List<ExamResult>

    @Query("SELECT * FROM exam_results WHERE exam_id = :examId ORDER BY score DESC LIMIT :limit")
    suspend fun getExamTopResults(examId: Int, limit: Int = 10): List<ExamResult>

    // ------------ جستجو ------------

    @Query("""
        SELECT r.* FROM exam_results r
        JOIN exams e ON r.exam_id = e.id
        WHERE e.title LIKE '%' || :query || '%' 
           OR e.subject LIKE '%' || :query || '%'
        ORDER BY r.completed_at DESC
    """)
    suspend fun searchResults(query: String): List<ExamResult>

    // ------------ تحلیل پیشرفت ------------

    @Query("""
        SELECT 
            strftime('%Y-%m', datetime(completed_at/1000, 'unixepoch')) as month,
            AVG(score) as average_score,
            COUNT(*) as exam_count
        FROM exam_results 
        WHERE user_id = :userId 
        GROUP BY month 
        ORDER BY month
    """)
    suspend fun getUserProgressByMonth(userId: Int): List<UserProgress>

    @Query("SELECT * FROM exam_results WHERE user_id = :userId AND completed_at >= :startDate ORDER BY completed_at ASC")
    suspend fun getUserResultsSinceDate(userId: Int, startDate: String): List<ExamResult>

    // ------------ آمار کلی کاربر ------------

    @Query("""
        SELECT 
            COUNT(*) as total_exams,
            SUM(CASE WHEN is_passed = 1 THEN 1 ELSE 0 END) as passed_exams,
            AVG(score) as average_score,
            AVG(percentage) as average_percentage,
            SUM(time_spent) as total_time_spent
        FROM exam_results 
        WHERE user_id = :userId
    """)
    suspend fun getUserStats(userId: Int): UserStats?
}

// کلاس‌های کمکی برای نتایج کوئری‌های پیچیده
data class UserProgress(
    val month: String,
    val averageScore: Float,
    val examCount: Int
)

data class UserStats(
    val totalExams: Int,
    val passedExams: Int,
    val averageScore: Float?,
    val averagePercentage: Float?,
    val totalTimeSpent: Int
)