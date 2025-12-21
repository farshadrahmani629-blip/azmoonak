// app/src/main/java/com/examapp/ui/home/HomeScreen.kt
package com.examapp.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.examapp.R
import com.examapp.data.models.Book

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToBooks: () -> Unit,
    onNavigateToExams: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadHomeData()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "آزمون‌ساز آنلاین",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { /* Notifications */ }) {
                        Icon(Icons.Default.Notifications, contentDescription = "اعلان‌ها")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Default.Person, contentDescription = "پروفایل")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToExams,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "آزمون جدید")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                HomeUiState.Loading -> {
                    LoadingScreen()
                }

                is HomeUiState.Success -> {
                    HomeContent(
                        state = state,
                        onBooksClick = onNavigateToBooks,
                        onExamsClick = onNavigateToExams,
                        onProfileClick = onNavigateToProfile,
                        onLoginClick = onNavigateToLogin,
                        onRefresh = { viewModel.loadHomeData() }
                    )
                }

                is HomeUiState.Error -> {
                    ErrorState(
                        message = state.message,
                        onRetry = { viewModel.loadHomeData() }
                    )
                }
            }
        }
    }
}

@Composable
fun HomeContent(
    state: HomeUiState.Success,
    onBooksClick: () -> Unit,
    onExamsClick: () -> Unit,
    onProfileClick: () -> Unit,
    onLoginClick: () -> Unit,
    onRefresh: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item {
            // هدر
            HomeHeader(
                greeting = state.userGreeting,
                isLoggedIn = state.isLoggedIn,
                onLoginClick = onLoginClick,
                onLogoutClick = { /* TODO: Implement logout */ }
            )
        }

        item {
            // ویژگی‌های برنامه
            FeaturesSection()
        }

        item {
            // دسترسی سریع
            QuickActionsSection(
                onBooksClick = onBooksClick,
                onExamsClick = onExamsClick,
                onProfileClick = onProfileClick
            )
        }

        if (state.books.isNotEmpty()) {
            item {
                Text(
                    text = "کتاب‌های پیشنهادی",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            items(state.books.take(3)) { book ->
                BookCard(book = book, onClick = onBooksClick)
            }

            if (state.books.size > 3) {
                item {
                    TextButton(
                        onClick = onBooksClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Text("مشاهده همه کتاب‌ها (${state.books.size})")
                    }
                }
            }
        }

        if (state.recentExams.isNotEmpty()) {
            item {
                Text(
                    text = "آزمون‌های اخیر",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            items(state.recentExams.take(2)) { exam ->
                RecentExamCard(exam = exam, onClick = onExamsClick)
            }
        }
    }
}

@Composable
fun HomeHeader(
    greeting: String,
    isLoggedIn: Boolean,
    onLoginClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = greeting,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "به آزمون‌ساز آنلاین خوش آمدید",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isLoggedIn) {
                FilledTonalButton(
                    onClick = onLogoutClick,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Text("خروج")
                }
            } else {
                FilledTonalButton(
                    onClick = onLoginClick,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Text("ورود / ثبت‌نام")
                }
            }
        }
    }
}

@Composable
fun FeaturesSection() {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Text(
            text = "ویژگی‌های برنامه",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FeatureCard(
                title = "آزمون هوشمند",
                description = "تولید آزمون بر اساس سطح شما",
                icon = Icons.Default.AutoAwesome,
                color = MaterialTheme.colorScheme.primary
            )

            FeatureCard(
                title = "تحلیل پیشرفت",
                description = "نمودارهای تحلیل عملکرد",
                icon = Icons.Default.Analytics,
                color = MaterialTheme.colorScheme.secondary
            )

            FeatureCard(
                title = "بانک سوال",
                description = "هزاران سوال دسته‌بندی شده",
                icon = Icons.Default.Quiz,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

@Composable
fun FeatureCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Card(
        modifier = Modifier.weight(1f),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = description,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun QuickActionsSection(
    onBooksClick: () -> Unit,
    onExamsClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Text(
            text = "دسترسی سریع",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickActionButton(
                text = "کتاب‌ها",
                icon = Icons.Default.MenuBook,
                onClick = onBooksClick,
                color = MaterialTheme.colorScheme.primary
            )

            QuickActionButton(
                text = "آزمون‌ها",
                icon = Icons.Default.Assignment,
                onClick = onExamsClick,
                color = MaterialTheme.colorScheme.secondary
            )

            QuickActionButton(
                text = "پروفایل",
                icon = Icons.Default.Person,
                onClick = onProfileClick,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

@Composable
fun QuickActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    color: Color
) {
    Card(
        modifier = Modifier.weight(1f),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = color,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = color
            )
        }
    }
}

@Composable
fun BookCard(
    book: Book,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Placeholder for book cover
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .padding(end = 16.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = MaterialTheme.shapes.medium
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = "جلد کتاب",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = book.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 2,
                    lineHeight = 20.sp
                )
                Text(
                    text = "${book.subject} - پایه ${book.grade}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
                book.publisher?.let { publisher ->
                    Text(
                        text = "ناشر: $publisher",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun RecentExamCard(
    exam: com.examapp.data.models.Exam,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = when (exam.status) {
                com.examapp.data.models.ExamStatus.ACTIVE ->
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                com.examapp.data.models.ExamStatus.COMPLETED ->
                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .padding(end = 16.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = MaterialTheme.shapes.medium
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (exam.status) {
                        com.examapp.data.models.ExamStatus.ACTIVE -> Icons.Default.PlayArrow
                        com.examapp.data.models.ExamStatus.COMPLETED -> Icons.Default.CheckCircle
                        else -> Icons.Default.Schedule
                    },
                    contentDescription = "آزمون",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = exam.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1
                )
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${exam.totalQuestions} سوال",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = " • ",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${exam.examDuration} دقیقه",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "وضعیت: ${exam.status.persianName}",
                    fontSize = 12.sp,
                    color = when (exam.status) {
                        com.examapp.data.models.ExamStatus.ACTIVE ->
                            MaterialTheme.colorScheme.primary
                        com.examapp.data.models.ExamStatus.COMPLETED ->
                            MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
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
            Text("در حال بارگذاری...")
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

// Extension for ExamStatus Persian name
val com.examapp.data.models.ExamStatus.persianName: String
    get() = when (this) {
        com.examapp.data.models.ExamStatus.ACTIVE -> "فعال"
        com.examapp.data.models.ExamStatus.COMPLETED -> "تکمیل شده"
        com.examapp.data.models.ExamStatus.SCHEDULED -> "برنامه‌ریزی شده"
        com.examapp.data.models.ExamStatus.DRAFT -> "پیش‌نویس"
        com.examapp.data.models.ExamStatus.CANCELLED -> "لغو شده"
    }

// ViewModel and State classes (should be in separate file)
class HomeViewModel @javax.inject.Inject constructor(
    private val examRepository: com.examapp.data.repository.ExamRepository,
    private val bookRepository: com.examapp.data.repository.BookRepository,
    private val authRepository: com.examapp.data.repository.AuthRepository
) : androidx.lifecycle.ViewModel() {

    private val _uiState = androidx.lifecycle.viewModelScope.MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: androidx.lifecycle.viewModelScope.StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun loadHomeData() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            try {
                val isLoggedIn = authRepository.isLoggedIn()
                val greeting = if (isLoggedIn) {
                    val user = authRepository.getCurrentUser()
                    "سلام ${user?.firstName ?: "کاربر"}!"
                } else {
                    "سلام مهمان!"
                }

                val books = bookRepository.getAllBooks(grade = null, subject = null).getOrNull() ?: emptyList()
                val recentExams = examRepository.getUserExams(
                    userId = authRepository.getCurrentUser()?.id ?: "",
                    status = null
                ).getOrNull()?.take(5) ?: emptyList()

                _uiState.value = HomeUiState.Success(
                    userGreeting = greeting,
                    isLoggedIn = isLoggedIn,
                    books = books,
                    recentExams = recentExams
                )
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error("خطا در بارگذاری داده‌ها: ${e.message}")
            }
        }
    }
}

sealed class HomeUiState {
    data object Loading : HomeUiState()
    data class Success(
        val userGreeting: String,
        val isLoggedIn: Boolean,
        val books: List<Book>,
        val recentExams: List<com.examapp.data.models.Exam>
    ) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}