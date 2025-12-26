package com.examapp.data.database  // پکیج درست

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.examapp.data.database.dao.*
import com.examapp.data.database.entity.*
import com.examapp.data.converters.Converters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

@Database(
    entities = [
        QuestionEntity::class,
        ExamEntity::class,
        ExamResultEntity::class,
        BookEntity::class,
        ChapterEntity::class,
        QuestionOptionEntity::class,
        StudentAnswerEntity::class
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

        private val EXECUTOR = Executors.newSingleThreadExecutor()

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .addCallback(DatabaseCallback(context))
                    .fallbackToDestructiveMigrationFrom(1, 2) // برای نسخه‌های قدیمی
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .setQueryCallback({ sql, bindings ->
                        println("SQL: $sql, Bindings: $bindings")
                    }, EXECUTOR)
                    .build()

                INSTANCE = instance
                instance
            }
        }

        fun getInMemoryDatabase(context: Context): AppDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                AppDatabase::class.java
            )
                .allowMainThreadQueries() // فقط برای تست
                .fallbackToDestructiveMigration()
                .build()
        }

        fun clearDatabase(context: Context) {
            EXECUTOR.execute {
                getDatabase(context).clearAllTables()
            }
        }
    }
}

// Callback برای عملیات اولیه
private class DatabaseCallback(
    private val context: Context
) : RoomDatabase.Callback() {

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        CoroutineScope(Dispatchers.IO).launch {
            populateInitialData(context)
        }
    }

    override fun onOpen(db: SupportSQLiteDatabase) {
        super.onOpen(db)
        db.execSQL("PRAGMA foreign_keys = ON;")
        db.execSQL("PRAGMA synchronous = NORMAL;")
        db.execSQL("PRAGMA cache_size = -2000;")
    }
}

// پر کردن داده‌های اولیه
private suspend fun populateInitialData(context: Context) {
    val database = AppDatabase.getDatabase(context)

    // مثال: اضافه کردن کتاب‌های پیش‌فرض
    val defaultBooks = listOf(
        BookEntity(id = 1, grade = 3, subject = "ریاضی", title = "ریاضی سوم دبستان"),
        BookEntity(id = 2, grade = 3, subject = "فارسی", title = "فارسی سوم دبستان"),
        BookEntity(id = 3, grade = 3, subject = "علوم", title = "علوم سوم دبستان")
    )

    database.bookDao().insertAll(defaultBooks)

    // اگر DAOهای دیگه نیاز به داده اولیه دارن، اینجا اضافه کن
}