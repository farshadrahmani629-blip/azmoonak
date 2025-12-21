// app/src/main/java/com/examapp/data/database/dao/ExamDao.kt
package com.examapp.data.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.examapp.data.models.Exam
import com.examapp.data.models.ExamStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ExamDao {

    // ------------ عملیات CRUD ------------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(exam: Exam): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(exams: List<Exam>)

    @Update
    suspend fun update(exam: Exam): Int

    @Delete
    suspend fun delete(exam: Exam): Int

    @Query("DELETE FROM exams")
    suspend fun deleteAll(): Int

    @Query("DELETE FROM exams WHERE id = :id")
    suspend fun deleteById(id: Int): Int

    // ------------ دریافت آزمون‌ها ------------

    @Query("SELECT * FROM exams ORDER BY created_at DESC")
    suspend fun getAllExams(): List<Exam>

    @Query("SELECT * FROM exams ORDER BY created_at DESC")
    fun getAllExamsFlow(): Flow<List<Exam>>

    @Query("SELECT * FROM exams ORDER BY created_at DESC")
    fun getAllExamsLiveData(): LiveData<List<Exam>>

    @Query("SELECT * FROM exams WHERE id = :id LIMIT 1")
    suspend fun getExamById(id: Int): Exam?

    @Query("SELECT * FROM exams WHERE id = :id LIMIT 1")
    fun getExamByIdFlow(id: Int): Flow<Exam?>

    @Query("SELECT * FROM exams WHERE exam_code = :examCode LIMIT 1")
    suspend fun getExamByCode(examCode: String): Exam?

    // ------------ فیلتر بر اساس وضعیت ------------

    @Query("SELECT * FROM exams WHERE status = :status ORDER BY created_at DESC")
    fun getExamsByStatus(status: ExamStatus): Flow<List<Exam>>

    @Query("SELECT * FROM exams WHERE status = :status ORDER BY created_at DESC")
    suspend fun getExamsByStatusSync(status: ExamStatus): List<Exam>

    // ------------ آزمون‌های فعال و کامل شده ------------

    @Query("SELECT * FROM exams WHERE status = 'ACTIVE' ORDER BY created_at DESC")
    fun getActiveExams(): Flow<List<Exam>>

    @Query("SELECT * FROM exams WHERE status = 'COMPLETED' ORDER BY created_at DESC")
    fun getCompletedExams(): Flow<List<Exam>>

    @Query("SELECT * FROM exams WHERE status = 'DRAFT' ORDER BY created_at DESC")
    fun getDraftExams(): Flow<List<Exam>>

    @Query("SELECT * FROM exams WHERE status IN ('ACTIVE', 'SCHEDULED') ORDER BY start_time ASC")
    fun getUpcomingExams(): Flow<List<Exam>>

    // ------------ فیلتر بر اساس درس و پایه ------------

    @Query("SELECT * FROM exams WHERE subject = :subject ORDER BY created_at DESC")
    fun getExamsBySubject(subject: String): Flow<List<Exam>>

    @Query("SELECT * FROM exams WHERE grade = :grade ORDER BY created_at DESC")
    fun getExamsByGrade(grade: Int): Flow<List<Exam>>

    @Query("SELECT * FROM exams WHERE subject = :subject AND grade = :grade ORDER BY created_at DESC")
    fun getExamsBySubjectAndGrade(subject: String, grade: Int): Flow<List<Exam>>

    @Query("SELECT * FROM exams WHERE user_id = :userId ORDER BY created_at DESC")
    fun getExamsByUser(userId: Int): Flow<List<Exam>>

    // ------------ به‌روزرسانی وضعیت ------------

    @Query("UPDATE exams SET status = :status WHERE id = :examId")
    suspend fun updateExamStatus(examId: Int, status: ExamStatus): Int

    @Query("UPDATE exams SET start_time = :startTime, status = 'ACTIVE' WHERE id = :examId")
    suspend fun startExam(examId: Int, startTime: String): Int

    @Query("UPDATE exams SET end_time = :endTime, status = 'COMPLETED' WHERE id = :examId")
    suspend fun completeExam(examId: Int, endTime: String): Int

    // ------------ آمار ------------

    @Query("SELECT COUNT(*) FROM exams")
    suspend fun getTotalCount(): Int

    @Query("SELECT COUNT(*) FROM exams WHERE status = 'COMPLETED'")
    suspend fun getCompletedCount(): Int

    @Query("SELECT COUNT(*) FROM exams WHERE status = 'ACTIVE'")
    suspend fun getActiveCount(): Int

    @Query("SELECT COUNT(*) FROM exams WHERE user_id = :userId")
    suspend fun getUserExamCount(userId: Int): Int

    @Query("SELECT COUNT(*) FROM exams WHERE user_id = :userId AND status = 'COMPLETED'")
    suspend fun getUserCompletedCount(userId: Int): Int

    @Query("SELECT DISTINCT subject FROM exams ORDER BY subject")
    suspend fun getAllSubjects(): List<String>

    @Query("SELECT DISTINCT grade FROM exams WHERE grade IS NOT NULL ORDER BY grade")
    suspend fun getAllGrades(): List<Int>

    // ------------ جستجو ------------

    @Query("""
        SELECT * FROM exams 
        WHERE title LIKE '%' || :query || '%' 
           OR description LIKE '%' || :query || '%'
           OR subject LIKE '%' || :query || '%'
        ORDER BY created_at DESC
    """)
    suspend fun searchExams(query: String): List<Exam>

    @Query("""
        SELECT * FROM exams 
        WHERE title LIKE '%' || :query || '%' 
           OR description LIKE '%' || :query || '%'
           OR subject LIKE '%' || :query || '%'
        ORDER BY created_at DESC
    """)
    fun searchExamsFlow(query: String): Flow<List<Exam>>

    // ------------ تاریخچه ------------

    @Query("SELECT * FROM exams WHERE status = 'COMPLETED' ORDER BY created_at DESC LIMIT :limit")
    suspend fun getRecentExams(limit: Int = 10): List<Exam>

    @Query("SELECT * FROM exams WHERE created_at >= :startDate AND created_at <= :endDate ORDER BY created_at DESC")
    suspend fun getExamsBetweenDates(startDate: String, endDate: String): List<Exam>

    // ------------ مدیریت بوکمارک ------------

    @Query("UPDATE exams SET is_bookmarked = :isBookmarked WHERE id = :examId")
    suspend fun updateBookmarkStatus(examId: Int, isBookmarked: Boolean): Int

    @Query("SELECT * FROM exams WHERE is_bookmarked = 1 ORDER BY created_at DESC")
    fun getBookmarkedExams(): Flow<List<Exam>>

    // ------------ پاک کردن قدیمی‌ها ------------

    @Query("DELETE FROM exams WHERE created_at < :timestamp AND status = 'COMPLETED'")
    suspend fun deleteOldCompletedExams(timestamp: String): Int

    @Query("DELETE FROM exams WHERE created_at < :timestamp AND status = 'DRAFT'")
    suspend fun deleteOldDraftExams(timestamp: String): Int
}