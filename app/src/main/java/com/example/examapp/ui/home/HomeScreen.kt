// app/src/main/java/com/examapp/ui/home/HomeScreen.kt
package com.examapp.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.examapp.ui.MainUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: MainUiState,
    userGreeting: String,
    onLogout: () -> Unit,
    onNavigateToBooks: () -> Unit,
    onNavigateToExams: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onRefresh: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "آزمون‌ساز آنلاین",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onRefresh,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "بروزرسانی")
            }
        }
    ) { paddingValues ->
        when (uiState) {
            is MainUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is MainUiState.Success -> {
                HomeContent(
                    userGreeting = userGreeting,
                    isLoggedIn = uiState.isLoggedIn,
                    books = uiState.books,
                    onLogout = onLogout,
                    onNavigateToBooks = onNavigateToBooks,
                    onNavigateToExams = onNavigateToExams,
                    onNavigateToProfile = onNavigateToProfile,
                    onNavigateToLogin = onNavigateToLogin,
                    paddingValues = paddingValues
                )
            }

            is MainUiState.Error -> {
                ErrorContent(
                    message = uiState.message,
                    isLoggedIn = uiState.isLoggedIn,
                    onRetry = onRefresh,
                    onNavigateToLogin = onNavigateToLogin,
                    paddingValues = paddingValues
                )
            }
        }
    }
}

@Composable
fun HomeContent(
    userGreeting: String,
    isLoggedIn: Boolean,
    books: List<com.examapp.data.models.Book>,
    onLogout: () -> Unit,
    onNavigateToBooks: () -> Unit,
    onNavigateToExams: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToLogin: () -> Unit,
    paddingValues: PaddingValues
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = userGreeting,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "به سامانه آزمون‌ساز آنلاین خوش آمدید",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (isLoggedIn) {
            item {
                Text(
                    text = "دسترسی سریع",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HomeActionCard(
                        title = "کتاب‌ها",
                        icon = Icons.Default.Book,
                        description = "مشاهده و مدیریت کتاب‌ها",
                        onClick = onNavigateToBooks,
                        modifier = Modifier.weight(1f)
                    )

                    HomeActionCard(
                        title = "آزمون‌ها",
                        icon = Icons.Default.Quiz,
                        description = "ایجاد و شرکت در آزمون",
                        onClick = onNavigateToExams,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HomeActionCard(
                        title = "پروفایل",
                        icon = Icons.Default.Person,
                        description = "مدیریت حساب کاربری",
                        onClick = onNavigateToProfile,
                        modifier = Modifier.weight(1f)
                    )

                    HomeActionCard(
                        title = "خروج",
                        icon = Icons.Default.Logout,
                        description = "خروج از حساب کاربری",
                        onClick = onLogout,
                        modifier = Modifier.weight(1f),
                        isDestructive = true
                    )
                }
            }

            if (books.isNotEmpty()) {
                item {
                    Text(
                        text = "کتاب‌های اخیر",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(books.take(3)) { book ->
                    BookCard(
                        book = book,
                        onClick = onNavigateToBooks
                    )
                }
            }
        } else {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "برای دسترسی به امکانات کامل وارد شوید",
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = onNavigateToLogin,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.Login, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("ورود به حساب کاربری")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HomeActionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDestructive: Boolean = false
) {
    Card(
        modifier = modifier,
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isDestructive)
                MaterialTheme.colorScheme.errorContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(32.dp),
                tint = if (isDestructive)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun BookCard(
    book: com.examapp.data.models.Book,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Book,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "پایه ${book.grade} - ${book.subject}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ErrorContent(
    message: String,
    isLoggedIn: Boolean,
    onRetry: () -> Unit,
    onNavigateToLogin: () -> Unit,
    paddingValues: PaddingValues
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
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
            color = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onRetry) {
            Text("تلاش مجدد")
        }

        if (!isLoggedIn) {
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onNavigateToLogin,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text("ورود به حساب کاربری")
            }
        }
    }
}