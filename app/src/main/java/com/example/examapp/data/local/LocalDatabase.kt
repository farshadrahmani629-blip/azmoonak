// app/src/main/java/com/examapp/data/local/LocalDatabase.kt
package com.examapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.examapp.data.converters.JsonConverter
import com.examapp.data.local.dao.QuestionLocalDao
import com.examapp.data.models.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import java.util.concurrent.Executors

@Database(
    entities = [
        Question::class,
        Book::class,
        Chapter::class,
        QuestionOption::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(JsonConverter::class)
abstract class LocalDatabase : RoomDatabase() {

    abstract fun questionDao(): QuestionLocalDao
    abstract fun bookDao(): BookDao
    abstract fun chapterDao(): ChapterDao
    abstract fun questionOptionDao(): QuestionOptionDao

    companion object {
        @Volatile
        private var INSTANCE: LocalDatabase? = null

        private const val DATABASE_NAME = "azmoonak_offline.db"
        private const val ENCRYPTION_PASSWORD = "user_provided_password" // باید از کاربر بگیرید

        private val EXECUTOR = Executors.newSingleThreadExecutor()

        fun getDatabase(context: Context, password: String? = null): LocalDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = buildDatabase(context, password)
                INSTANCE = instance
                instance
            }
        }

        private fun buildDatabase(context: Context, password: String?): LocalDatabase {
            val builder = Room.databaseBuilder(
                context.applicationContext,
                LocalDatabase::class.java,
                DATABASE_NAME
            )

            // اگر رمز عبور داریم، دیتابیس را encrypt می‌کنیم
            if (!password.isNullOrEmpty()) {
                val factory = SupportFactory(SQLiteDatabase.getBytes(password.toCharArray()))
                builder.openHelperFactory(factory)
            }

            return builder
                .addCallback(DatabaseCallback(context))
                .fallbackToDestructiveMigration()
                .setQueryCallback({ sql, bindings ->
                    // برای دیباگ
                    println("Local DB SQL: $sql")
                }, Executors.newSingleThreadExecutor())
                .build()
        }

        // برای تست: دیتابیس در حافظه
        fun getInMemoryDatabase(context: Context): LocalDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                LocalDatabase::class.java
            )
                .allowMainThreadQueries() // فقط برای تست
                .fallbackToDestructiveMigration()
                .build()
        }

        // پاک کردن دیتابیس (برای logout یا reinstall)
        fun clearDatabase(context: Context) {
            EXECUTOR.execute {
                getDatabase(context).clearAllTables()
            }
        }

        // بررسی وجود دیتابیس
        fun databaseExists(context: Context): Boolean {
            val dbFile = context.getDatabasePath(DATABASE_NAME)
            return dbFile.exists()
        }

        // بررسی اندازه دیتابیس
        fun getDatabaseSize(context: Context): Long {
            val dbFile = context.getDatabasePath(DATABASE_NAME)
            return if (dbFile.exists()) dbFile.length() else 0
        }

        // حذف دیتابیس
        fun deleteDatabase(context: Context): Boolean {
            return try {
                context.deleteDatabase(DATABASE_NAME)
                INSTANCE = null
                true
            } catch (e: Exception) {
                false
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
            // ایجاد ایندکس‌ها برای performance بهتر
            db.execSQL("CREATE INDEX idx_questions_book ON questions(book_id)")
            db.execSQL("CREATE INDEX idx_questions_chapter ON questions(chapter_id)")
            db.execSQL("CREATE INDEX idx_questions_subject ON questions(subject)")
            db.execSQL("CREATE INDEX idx_questions_grade ON questions(grade)")
            db.execSQL("CREATE INDEX idx_questions_active ON questions(is_active)")

            // تنظیمات SQLite برای performance
            db.execSQL("PRAGMA journal_mode = WAL;")
            db.execSQL("PRAGMA synchronous = NORMAL;")
            db.execSQL("PRAGMA cache_size = -4000;") // 4MB cache

            println("Local database created with encryption")
        }
    }

    override fun onOpen(db: SupportSQLiteDatabase) {
        super.onOpen(db)
        // فعال کردن foreign key constraints
        db.execSQL("PRAGMA foreign_keys = ON;")
    }

    override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
        super.onDestructiveMigration(db)
        // عملیات پس از destructive migration
        println("Local database was migrated destructively")
    }
}

// DAOهای اضافی برای مدل‌های دیگر
interface BookDao {
    // روش‌های مورد نیاز برای Book
    // TODO: implement
}

interface ChapterDao {
    // روش‌های مورد نیاز برای Chapter
    // TODO: implement
}

interface QuestionOptionDao {
    // روش‌های مورد نیاز برای QuestionOption
    // TODO: implement
}