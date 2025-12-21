// app/src/main/java/com/examapp/data/database/LocalDatabase.kt
package com.examapp.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.examapp.data.database.dao.ExamDao
import com.examapp.data.database.dao.QuestionDao
import com.examapp.data.database.dao.ResultDao
import com.examapp.data.models.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

@Database(
    entities = [
        Question::class,
        Exam::class,
        ExamResult::class,
        Book::class,
        Chapter::class,
        QuestionOption::class,
        StudentAnswer::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun questionDao(): QuestionDao
    abstract fun examDao(): ExamDao
    abstract fun resultDao(): ResultDao
    abstract fun bookDao(): BookDao
    abstract fun chapterDao(): ChapterDao
    abstract fun questionOptionDao(): QuestionOptionDao
    abstract fun studentAnswerDao(): StudentAnswerDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private const val DATABASE_NAME = "azmoonak_database.db"

        // برای background operations
        private val EXECUTOR = Executors.newSingleThreadExecutor()

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .addCallback(DatabaseCallback(context))
                    .fallbackToDestructiveMigrationFrom(1, 2) // نگه دارید اگر نسخه 1 یا 2 دارید
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .setQueryCallback({ sql, bindings ->
                        // برای دیباگ کوئری‌ها
                        println("SQL: $sql, Bindings: $bindings")
                    }, Executors.newSingleThreadExecutor())
                    .build()

                INSTANCE = instance
                instance
            }
        }

        // برای تست: دیتابیس در حافظه
        fun getInMemoryDatabase(context: Context): AppDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                AppDatabase::class.java
            )
                .allowMainThreadQueries() // فقط برای تست
                .fallbackToDestructiveMigration()
                .build()
        }

        // پاک کردن دیتابیس (برای تست یا logout)
        fun clearDatabase(context: Context) {
            EXECUTOR.execute {
                getDatabase(context).clearAllTables()
            }
        }
    }
}

// Callback برای عملیات اولیه روی دیتابیس
private class DatabaseCallback(
    private val context: Context
) : RoomDatabase.Callback() {

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)

        CoroutineScope(Dispatchers.IO).launch {
            // عملیات اولیه پس از ایجاد دیتابیس
            populateInitialData(context)
        }
    }

    override fun onOpen(db: SupportSQLiteDatabase) {
        super.onOpen(db)
        // فعال کردن foreign key constraints
        db.execSQL("PRAGMA foreign_keys = ON;")

        // تنظیم synchronous برای performance
        db.execSQL("PRAGMA synchronous = NORMAL;")

        // افزایش cache size
        db.execSQL("PRAGMA cache_size = -2000;") // 2MB cache
    }

    override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
        super.onDestructiveMigration(db)
        // عملیات پس از destructive migration
        println("Database was migrated destructively")
    }
}

// تابع برای پر کردن داده‌های اولیه
private suspend fun populateInitialData(context: Context) {
    // اینجا می‌توانید داده‌های اولیه را وارد کنید
    // مثلاً کتاب‌های پایه، فصل‌ها، یا سوالات نمونه

    val database = AppDatabase.getDatabase(context)

    // مثال: اضافه کردن کتاب‌های پیش‌فرض
    val defaultBooks = listOf(
        Book(id = 1, grade = 3, subject = "ریاضی", title = "ریاضی سوم دبستان"),
        Book(id = 2, grade = 3, subject = "فارسی", title = "فارسی سوم دبستان"),
        Book(id = 3, grade = 3, subject = "علوم", title = "علوم سوم دبستان"),
        Book(id = 4, grade = 4, subject = "ریاضی", title = "ریاضی چهارم دبستان"),
        Book(id = 5, grade = 4, subject = "فارسی", title = "فارسی چهارم دبستان"),
        Book(id = 6, grade = 4, subject = "علوم", title = "علوم چهارم دبستان")
    )

    database.bookDao().insertAll(defaultBooks)

    println("Initial database population completed")
}

// اگر DAOهای اضافی دارید، آنها را تعریف کنید
interface BookDao {
    fun insertAll(books: List<Book>)
}

interface ChapterDao {
    // methods...
}

interface QuestionOptionDao {
    // methods...
}

interface StudentAnswerDao {
    // methods...
}