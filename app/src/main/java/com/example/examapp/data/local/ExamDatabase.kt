// app/src/main/java/com/examapp/data/local/ExamDatabase.kt
package com.examapp.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.examapp.data.local.converters.*
import com.examapp.data.local.dao.*
import com.examapp.data.models.local.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Database(
    entities = [
        QuestionEntity::class,
        QuestionOptionEntity::class,
        BookEntity::class,
        ChapterEntity::class,
        ResultEntity::class,
        UserEntity::class,
        ExamEntity::class,
        UserAnswerEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(
    ListStringConverter::class,
    DateConverter::class,
    UserRoleConverter::class,
    QuestionTypeConverter::class,
    DifficultyLevelConverter::class,
    SubscriptionTypeConverter::class
)
abstract class ExamDatabase : RoomDatabase() {

    abstract fun questionDao(): QuestionDao
    abstract fun questionOptionDao(): QuestionOptionDao
    abstract fun bookDao(): BookDao
    abstract fun chapterDao(): ChapterDao
    abstract fun resultDao(): ResultDao
    abstract fun userDao(): UserDao
    abstract fun examDao(): ExamDao
    abstract fun userAnswerDao(): UserAnswerDao

    companion object {
        @Volatile
        private var INSTANCE: ExamDatabase? = null

        fun getDatabase(context: App): ExamDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ExamDatabase::class.java,
                    "exam_database.db"
                )
                    .addCallback(DatabaseCallback(context))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val context: App
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)

                // می‌توانید داده‌های اولیه را اینجا وارد کنید
                CoroutineScope(context.applicationScope).launch {
                    populateInitialData()
                }
            }
        }

        private suspend fun populateInitialData() {
            // اینجا می‌توانید داده‌های اولیه را وارد کنید
            // مثلاً کتاب‌های پیش‌فرض، کاربران نمونه، و غیره
        }
    }
}