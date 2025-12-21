// app/src/main/java/com/examapp/DI.kt
package com.examapp

import android.content.Context
import com.examapp.data.local.ExamDatabase
import com.examapp.data.repository.*
import com.examapp.ui.exam.ExamViewModel
import com.examapp.ui.main.MainViewModel
import com.examapp.ui.pdf.PdfViewModel
import com.examapp.ui.results.ResultViewModel
import com.examapp.utils.SharedPrefs

object DI {
    private var _appDatabase: ExamDatabase? = null
    private var _questionRepository: QuestionRepository? = null
    private var _examRepository: ExamRepository? = null
    private var _resultRepository: ResultRepository? = null
    private var _bookRepository: BookRepository? = null
    private var _authRepository: AuthRepository? = null
    private var _sharedPrefs: SharedPrefs? = null

    fun initialize(context: Context) {
        _appDatabase = ExamDatabase.getDatabase(context as App)
        _sharedPrefs = SharedPrefs(context)
    }

    val database: ExamDatabase
        get() = _appDatabase ?: throw IllegalStateException("DI not initialized!")

    val questionRepository: QuestionRepository
        get() = _questionRepository ?: QuestionRepository(
            database.questionDao(),
            database.questionOptionDao()
        ).also { _questionRepository = it }

    val examRepository: ExamRepository
        get() = _examRepository ?: ExamRepository(
            database.examDao(),
            questionRepository
        ).also { _examRepository = it }

    val resultRepository: ResultRepository
        get() = _resultRepository ?: ResultRepository(
            database.resultDao()
        ).also { _resultRepository = it }

    val bookRepository: BookRepository
        get() = _bookRepository ?: BookRepository(
            database.bookDao(),
            database.chapterDao()
        ).also { _bookRepository = it }

    val authRepository: AuthRepository
        get() = _authRepository ?: AuthRepository(
            database.userDao(),
            sharedPrefs
        ).also { _authRepository = it }

    val sharedPrefs: SharedPrefs
        get() = _sharedPrefs ?: throw IllegalStateException("DI not initialized!")

    object ViewModelFactory {
        fun provideMainViewModel(): MainViewModel {
            return MainViewModel(
                authRepository = authRepository,
                bookRepository = bookRepository,
                examRepository = examRepository,
                questionRepository = questionRepository
            )
        }

        fun provideExamViewModel(): ExamViewModel {
            return ExamViewModel(
                examRepository = examRepository,
                questionRepository = questionRepository,
                resultRepository = resultRepository
            )
        }

        fun provideResultViewModel(): ResultViewModel {
            return ResultViewModel(
                resultRepository = resultRepository
            )
        }

        fun providePdfViewModel(): PdfViewModel {
            return PdfViewModel(
                resultRepository = resultRepository
            )
        }
    }

    fun getMainViewModel(): MainViewModel = ViewModelFactory.provideMainViewModel()
    fun getExamViewModel(): ExamViewModel = ViewModelFactory.provideExamViewModel()
    fun getResultViewModel(): ResultViewModel = ViewModelFactory.provideResultViewModel()
    fun getPdfViewModel(): PdfViewModel = ViewModelFactory.providePdfViewModel()

    fun clear() {
        _appDatabase = null
        _questionRepository = null
        _examRepository = null
        _resultRepository = null
        _bookRepository = null
        _authRepository = null
        _sharedPrefs = null
    }
}