// app/src/main/java/com/examapp/data/database/dao/ExamDao.kt
package com.examapp.data.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.examapp.data.models.Exam
import kotlinx.coroutines.flow.Flow

@Dao
interface ExamDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(exam: Exam): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(exams: List<Exam>)

    @Update
    suspend fun update(exam: Exam)

    @Delete
    suspend fun delete(exam: Exam)

    @Query("SELECT * FROM exams ORDER BY created_at DESC")
    suspend fun getAllExams(): List<Exam>

    @Query("SELECT * FROM exams ORDER BY created_at DESC")
    fun getAllExamsFlow(): Flow<List<Exam>>

    @Query("SELECT * FROM exams ORDER BY created_at DESC")
    fun getAllExamsLiveData(): LiveData<List<Exam>>

    @Query("SELECT * FROM exams WHERE id = :examId")
    suspend fun getExamById(examId: Int): Exam?

    @Query("SELECT * FROM exams WHERE id = :examId")
    fun getExamByIdFlow(examId: Int): Flow<Exam?>

    @Query("SELECT * FROM exams WHERE subject = :subject ORDER BY title")
    fun getExamsBySubject(subject: String): Flow<List<Exam>>

    @Query("SELECT * FROM exams WHERE is_active = 1 ORDER BY title")
    fun getActiveExams(): Flow<List<Exam>>

    @Query("SELECT * FROM exams WHERE grade = :grade ORDER BY subject, title")
    fun getExamsByGrade(grade: Int): Flow<List<Exam>>

    @Query("SELECT * FROM exams WHERE status = :status ORDER BY created_at DESC")
    fun getExamsByStatus(status: String): Flow<List<Exam>>

    @Query("SELECT * FROM exams WHERE user_id = :userId ORDER BY created_at DESC")
    fun getUserExams(userId: Int): Flow<List<Exam>>

    @Query("SELECT * FROM exams WHERE exam_code = :examCode")
    suspend fun getExamByCode(examCode: String): Exam?

    @Query("DELETE FROM exams")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM exams")
    suspend fun getExamCount(): Int

    @Query("SELECT COUNT(*) FROM exams WHERE is_active = 1")
    suspend fun getActiveExamCount(): Int

    @Query("SELECT COUNT(*) FROM exams WHERE status = 'COMPLETED' AND user_id = :userId")
    suspend fun getCompletedExamsCount(userId: Int): Int

    @Query("SELECT * FROM exams WHERE status IN ('ACTIVE', 'SCHEDULED') ORDER BY start_time ASC")
    fun getUpcomingExams(): Flow<List<Exam>>

    // Search functionality
    @Query("SELECT * FROM exams WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%'")
    fun searchExams(query: String): Flow<List<Exam>>

    // برای حذف آزمون‌های قدیمی
    @Query("DELETE FROM exams WHERE created_at < :timestamp")
    suspend fun deleteOlderThan(timestamp: String): Int

    // برای آپدیت وضعیت آزمون
    @Query("UPDATE exams SET status = :status WHERE id = :examId")
    suspend fun updateExamStatus(examId: Int, status: String): Int

    // برای مارک کردن به عنوان favorite/bookmark
    @Query("UPDATE exams SET is_bookmarked = :isBookmarked WHERE id = :examId")
    suspend fun updateBookmarkStatus(examId: Int, isBookmarked: Boolean): Int
}