// app/src/main/java/com/examapp/ui/books/BookListScreen.kt
package com.examapp.ui.books

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.examapp.R
import com.examapp.data.models.Book

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookListScreen(
    navController: NavController,
    onBookSelected: (String) -> Unit,
    viewModel: BookViewModel = hiltViewModel()
) {
    val booksState by viewModel.booksState.collectAsState()
    val selectedGrade by viewModel.selectedGrade.collectAsState()
    val selectedSubject by viewModel.selectedSubject.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadBooks()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("کتاب‌ها") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = "بازگشت"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refreshBooks() },
                        enabled = booksState !is BooksState.Loading
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "بروزرسانی")
                    }
                }
            )
        },
        bottomBar = {
            BookFilterBar(
                selectedGrade = selectedGrade,
                selectedSubject = selectedSubject,
                onFilterChanged = { grade, subject ->
                    viewModel.setFilter(grade, subject)
                    viewModel.loadBooks(grade, subject)
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = booksState) {
                is BooksState.Loading -> {
                    LoadingScreen()
                }

                is BooksState.Success -> {
                    val books = state.books

                    if (books.isEmpty()) {
                        EmptyBookList(
                            onRefresh = { viewModel.refreshBooks() }
                        )
                    } else {
                        BookListContent(
                            books = books,
                            onBookClick = { book -> onBookSelected(book.id) }
                        )
                    }
                }

                is BooksState.Error -> {
                    ErrorState(
                        message = state.message,
                        onRetry = { viewModel.refreshBooks() }
                    )
                }
            }
        }
    }
}

@Composable
fun BookListContent(
    books: List<Book>,
    onBookClick: (Book) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        items(books, key = { it.id }) { book ->
            BookListItem(
                book = book,
                onClick = { onBookClick(book) }
            )
        }
    }
}

@Composable
fun BookListItem(
    book: Book,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // آیکون کتاب
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .padding(end = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            shape = MaterialTheme.shapes.medium
                        )
                )
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = "کتاب",
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    maxLines = 2,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = "پایه",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = " پایه ${book.grade}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Icon(
                        imageVector = Icons.Default.Subject,
                        contentDescription = "درس",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = " ${book.subject}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                book.publisher?.let { publisher ->
                    if (publisher.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Business,
                                contentDescription = "ناشر",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = " $publisher",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = "مشاهده جزئیات",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
fun BookFilterBar(
    selectedGrade: Int?,
    selectedSubject: String?,
    onFilterChanged: (Int?, String?) -> Unit
) {
    var showGradeFilter by remember { mutableStateOf(false) }
    var showSubjectFilter by remember { mutableStateOf(false) }

    val grades = listOf(1, 2, 3, 4, 5, 6)
    val subjects = listOf("ریاضی", "علوم", "فارسی", "هدیه‌های آسمان", "مطالعات اجتماعی")

    Surface(
        tonalElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // فیلتر پایه
            FilterChip(
                selected = selectedGrade != null,
                onClick = { showGradeFilter = true },
                label = {
                    Text(
                        text = selectedGrade?.let { "پایه $it" } ?: "همه پایه‌ها",
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                leadingIcon = if (selectedGrade != null) {
                    {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                        )
                    }
                } else null
            )

            // فیلتر درس
            FilterChip(
                selected = selectedSubject != null,
                onClick = { showSubjectFilter = true },
                label = {
                    Text(
                        text = selectedSubject ?: "همه درس‌ها",
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                leadingIcon = if (selectedSubject != null) {
                    {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                        )
                    }
                } else null
            )

            // دکمه پاک کردن فیلتر
            if (selectedGrade != null || selectedSubject != null) {
                AssistChip(
                    onClick = { onFilterChanged(null, null) },
                    label = { Text("پاک کردن") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = null,
                            modifier = Modifier.size(AssistChipDefaults.IconSize)
                        )
                    }
                )
            }
        }
    }

    // دیالوگ فیلتر پایه
    if (showGradeFilter) {
        AlertDialog(
            onDismissRequest = { showGradeFilter = false },
            title = { Text("انتخاب پایه") },
            text = {
                Column {
                    grades.forEach { grade ->
                        RadioButtonItem(
                            text = "پایه $grade",
                            selected = selectedGrade == grade,
                            onClick = {
                                onFilterChanged(grade, selectedSubject)
                                showGradeFilter = false
                            }
                        )
                    }
                    RadioButtonItem(
                        text = "همه پایه‌ها",
                        selected = selectedGrade == null,
                        onClick = {
                            onFilterChanged(null, selectedSubject)
                            showGradeFilter = false
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showGradeFilter = false }) {
                    Text("انصراف")
                }
            }
        )
    }

    // دیالوگ فیلتر درس
    if (showSubjectFilter) {
        AlertDialog(
            onDismissRequest = { showSubjectFilter = false },
            title = { Text("انتخاب درس") },
            text = {
                Column {
                    subjects.forEach { subject ->
                        RadioButtonItem(
                            text = subject,
                            selected = selectedSubject == subject,
                            onClick = {
                                onFilterChanged(selectedGrade, subject)
                                showSubjectFilter = false
                            }
                        )
                    }
                    RadioButtonItem(
                        text = "همه درس‌ها",
                        selected = selectedSubject == null,
                        onClick = {
                            onFilterChanged(selectedGrade, null)
                            showSubjectFilter = false
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showSubjectFilter = false }) {
                    Text("انصراف")
                }
            }
        )
    }
}

@Composable
fun RadioButtonItem(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Text(
            text = text,
            modifier = Modifier.padding(start = 8.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun EmptyBookList(
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.MenuBook,
            contentDescription = "لیست خالی",
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "کتابی یافت نشد",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "کتاب‌های این بخش به زودی اضافه خواهند شد",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onRefresh,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text("بروزرسانی")
        }
    }
}

@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("در حال بارگذاری کتاب‌ها...")
        }
    }
}

@Composable
fun ErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = "خطا",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text("تلاش مجدد")
        }
    }
}

// Extension properties for Book
val Book.displayTitle: String
    get() = this.title

val Book.displayGrade: String
    get() = "پایه ${this.grade}"

val Book.displaySubject: String
    get() = this.subject

val Book.displayPublisher: String?
    get() = this.publisher?.takeIf { it.isNotBlank() }