// app/src/main/java/com/examapp/ui/books/BookViewModel.kt
package com.examapp.ui.books

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.examapp.data.models.Book
import com.examapp.data.models.Chapter
import com.examapp.data.repository.BookRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BookViewModel @Inject constructor(
    private val bookRepository: BookRepository
) : ViewModel() {

    private val _booksState = MutableStateFlow<BooksState>(BooksState.Loading)
    val booksState: StateFlow<BooksState> = _booksState.asStateFlow()

    private val _selectedBook = MutableStateFlow<Book?>(null)
    val selectedBook: StateFlow<Book?> = _selectedBook.asStateFlow()

    private val _chaptersState = MutableStateFlow<ChaptersState>(ChaptersState.Idle)
    val chaptersState: StateFlow<ChaptersState> = _chaptersState.asStateFlow()

    private val _selectedGrade = MutableStateFlow<Int?>(null)
    val selectedGrade: StateFlow<Int?> = _selectedGrade.asStateFlow()

    private val _selectedSubject = MutableStateFlow<String?>(null)
    val selectedSubject: StateFlow<String?> = _selectedSubject.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        loadBooks()
    }

    fun loadBooks(grade: Int? = null, subject: String? = null) {
        viewModelScope.launch {
            try {
                _booksState.value = BooksState.Loading
                _selectedGrade.value = grade
                _selectedSubject.value = subject

                val result = bookRepository.getAllBooks(grade, subject)

                if (result.isSuccess) {
                    val books = result.getOrNull() ?: emptyList()
                    _booksState.value = BooksState.Success(books)

                    // Cache books locally
                    cacheBooks(books)
                } else {
                    _booksState.value = BooksState.Error(
                        result.exceptionOrNull()?.message ?: "خطا در دریافت کتاب‌ها"
                    )
                }
            } catch (e: Exception) {
                _booksState.value = BooksState.Error("خطا در اتصال: ${e.message}")
            }
        }
    }

    fun selectBook(book: Book) {
        _selectedBook.value = book
        loadChapters(book.id)
    }

    fun selectBookById(bookId: String) {
        viewModelScope.launch {
            val book = getBookById(bookId)
            book?.let { selectBook(it) }
        }
    }

    fun clearSelectedBook() {
        _selectedBook.value = null
        _chaptersState.value = ChaptersState.Idle
    }

    private fun loadChapters(bookId: String) {
        viewModelScope.launch {
            try {
                _chaptersState.value = ChaptersState.Loading

                val result = bookRepository.getChaptersByBook(bookId)

                if (result.isSuccess) {
                    val chapters = result.getOrNull() ?: emptyList()
                    _chaptersState.value = ChaptersState.Success(chapters)

                    // Cache chapters locally
                    cacheChapters(chapters)
                } else {
                    _chaptersState.value = ChaptersState.Error(
                        result.exceptionOrNull()?.message ?: "خطا در دریافت فصل‌ها"
                    )
                }
            } catch (e: Exception) {
                _chaptersState.value = ChaptersState.Error("خطا در اتصال: ${e.message}")
            }
        }
    }

    fun getBookById(bookId: String): Book? {
        // First check local cache, then fetch from repository if needed
        // TODO: Implement proper caching mechanism
        return bookRepository.getCachedBook(bookId)
    }

    fun getChapterById(chapterId: String): Chapter? {
        return bookRepository.getCachedChapter(chapterId)
    }

    fun getBooksByGrade(grade: Int): List<Book> {
        return bookRepository.getBooksByGrade(grade)
    }

    fun getBooksBySubject(subject: String): List<Book> {
        return bookRepository.getBooksBySubject(subject)
    }

    fun refreshBooks() {
        loadBooks(_selectedGrade.value, _selectedSubject.value)
    }

    fun refreshChapters() {
        _selectedBook.value?.let { book ->
            loadChapters(book.id)
        }
    }

    fun setFilter(grade: Int?, subject: String?) {
        _selectedGrade.value = grade
        _selectedSubject.value = subject
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun searchBooks(query: String) {
        viewModelScope.launch {
            try {
                _booksState.value = BooksState.Loading

                val result = bookRepository.searchBooks(query)

                if (result.isSuccess) {
                    val books = result.getOrNull() ?: emptyList()
                    _booksState.value = BooksState.Success(books)
                } else {
                    _booksState.value = BooksState.Error(
                        result.exceptionOrNull()?.message ?: "خطا در جستجوی کتاب‌ها"
                    )
                }
            } catch (e: Exception) {
                _booksState.value = BooksState.Error("خطا در اتصال: ${e.message}")
            }
        }
    }

    private fun cacheBooks(books: List<Book>) {
        // Cache books in repository
        books.forEach { bookRepository.cacheBook(it) }
    }

    private fun cacheChapters(chapters: List<Chapter>) {
        // Cache chapters in repository
        chapters.forEach { bookRepository.cacheChapter(it) }
    }

    // Extension properties for Book
    val Book.displayInfo: String
        get() = "${this.subject} - پایه ${this.grade}"

    val Book.hasPublisher: Boolean
        get() = !this.publisher.isNullOrEmpty()

    // Extension properties for Chapter
    val Chapter.pageRange: String
        get() = "صفحات ${this.startPage} تا ${this.endPage}"
}

// State Classes
sealed class BooksState {
    data object Loading : BooksState()
    data class Success(val books: List<Book>) : BooksState()
    data class Error(val message: String) : BooksState()
}

sealed class ChaptersState {
    data object Idle : ChaptersState()
    data object Loading : ChaptersState()
    data class Success(val chapters: List<Chapter>) : ChaptersState()
    data class Error(val message: String) : ChaptersState()
}