// app/src/main/java/com/examapp/ui/exam/ExamListScreen.kt
package com.examapp.ui.exam

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
import androidx.compose.ui.text.font.FontWeight
import com.examapp.R
import com.examapp.data.models.Exam
import com.examapp.data.models.ExamStatus
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamListScreen(
    onNavigateBack: () -> Unit,
    onCreateExam: () -> Unit,
    onViewResult: (String) -> Unit,
    onStartExam: (String) -> Unit,
    viewModel: ExamViewModel = hiltViewModel()
) {
    val examsState by viewModel.examsState.collectAsState()
    val examFilter by viewModel.examFilter.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadUserExams()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("آزمون‌های من") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = "بازگشت"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refreshExams() },
                        enabled = examsState !is ExamsState.Loading
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "بروزرسانی")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateExam,
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
            when (val state = examsState) {
                is ExamsState.Loading -> {
                    LoadingScreen()
                }

                is ExamsState.Success -> {
                    val exams = state.exams

                    if (exams.isEmpty()) {
                        EmptyExamList(onCreateExam = onCreateExam)
                    } else {
                        ExamListContent(
                            exams = exams,
                            examFilter = examFilter,
                            onFilterChange = { status ->
                                viewModel.loadUserExams(status)
                            },
                            onViewResult = onViewResult,
                            onStartExam = onStartExam
                        )
                    }
                }

                is ExamsState.Error -> {
                    ErrorState(
                        message = state.message,
                        onRetry = { viewModel.refreshExams() }
                    )
                }
            }
        }
    }
}

@Composable
fun ExamListContent(
    exams: List<Exam>,
    examFilter: ExamStatus?,
    onFilterChange: (ExamStatus?) -> Unit,
    onViewResult: (String) -> Unit,
    onStartExam: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // فیلترها
        ExamFilterBar(
            currentFilter = examFilter,
            onFilterChange = onFilterChange
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // نمایش آزمون‌ها بر اساس وضعیت
            val examsToShow = if (examFilter != null) {
                exams.filter { it.status == examFilter }
            } else {
                exams
            }

            if (examsToShow.isEmpty()) {
                item {
                    NoFilteredExams()
                }
            } else {
                items(examsToShow, key = { it.id }) { exam ->
                    ExamListItem(
                        exam = exam,
                        onViewResult = onViewResult,
                        onStartExam = onStartExam
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp)) // فضای FAB
                }
            }
        }
    }
}

@Composable
fun ExamFilterBar(
    currentFilter: ExamStatus?,
    onFilterChange: (ExamStatus?) -> Unit
) {
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
            // همه
            FilterChip(
                selected = currentFilter == null,
                onClick = { onFilterChange(null) },
                label = { Text("همه") }
            )

            // فعال
            FilterChip(
                selected = currentFilter == ExamStatus.ACTIVE,
                onClick = { onFilterChange(ExamStatus.ACTIVE) },
                label = { Text("فعال") }
            )

            // تکمیل شده
            FilterChip(
                selected = currentFilter == ExamStatus.COMPLETED,
                onClick = { onFilterChange(ExamStatus.COMPLETED) },
                label = { Text("تکمیل شده") }
            )

            // برنامه‌ریزی شده
            FilterChip(
                selected = currentFilter == ExamStatus.SCHEDULED,
                onClick = { onFilterChange(ExamStatus.SCHEDULED) },
                label = { Text("برنامه‌ریزی شده") }
            )
        }
    }
}

@Composable
fun ExamListItem(
    exam: Exam,
    onViewResult: (String) -> Unit,
    onStartExam: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (exam.status) {
                ExamStatus.ACTIVE -> MaterialTheme.colorScheme.primaryContainer
                ExamStatus.COMPLETED -> MaterialTheme.colorScheme.secondaryContainer
                ExamStatus.SCHEDULED -> MaterialTheme.colorScheme.tertiaryContainer
                ExamStatus.DRAFT -> MaterialTheme.colorScheme.surfaceVariant
                ExamStatus.CANCELLED -> MaterialTheme.colorScheme.errorContainer
            }
        ),
        onClick = {
            when (exam.status) {
                ExamStatus.ACTIVE -> onStartExam(exam.id)
                ExamStatus.COMPLETED -> onViewResult(exam.id)
                else -> {} // Do nothing for other statuses
            }
        }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // هدر کارت
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exam.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )

                    exam.description?.let { description ->
                        if (description.isNotBlank()) {
                            Text(
                                text = description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }

                // نشانگر وضعیت
                StatusChip(status = exam.status)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // اطلاعات آزمون
            ExamInfoRow(
                icon = Icons.Default.Subject,
                text = exam.subject ?: "بدون درس"
            )

            ExamInfoRow(
                icon = Icons.Default.Grade,
                text = exam.grade?.let { "پایه $it" } ?: "بدون پایه"
            )

            ExamInfoRow(
                icon = Icons.Default.Quiz,
                text = "${exam.totalQuestions ?: 0} سوال"
            )

            exam.examDuration?.let { duration ->
                ExamInfoRow(
                    icon = Icons.Default.Schedule,
                    text = "$duration دقیقه"
                )
            }

            exam.startTime?.let { startTime ->
                ExamInfoRow(
                    icon = Icons.Default.CalendarMonth,
                    text = "شروع: ${formatDateTime(startTime)}"
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // دکمه‌های اقدام
            when (exam.status) {
                ExamStatus.ACTIVE -> {
                    Button(
                        onClick = { onStartExam(exam.id) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("شروع آزمون")
                    }
                }

                ExamStatus.COMPLETED -> {
                    val hasResult = exam.resultId != null

                    Button(
                        onClick = {
                            if (hasResult) {
                                exam.resultId?.let { onViewResult(it) }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        ),
                        enabled = hasResult
                    ) {
                        Text(
                            text = if (hasResult) "مشاهده نتیجه" else "در حال تصحیح",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                ExamStatus.SCHEDULED -> {
                    OutlinedButton(
                        onClick = { /* امکان ویرایش */ },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("ویرایش")
                    }
                }

                else -> {
                    // برای سایر وضعیت‌ها دکمه‌ای نمایش نمی‌دهیم
                }
            }
        }
    }
}

@Composable
fun StatusChip(status: ExamStatus) {
    val (text, color) = when (status) {
        ExamStatus.ACTIVE -> Pair("فعال", MaterialTheme.colorScheme.primary)
        ExamStatus.COMPLETED -> Pair("تکمیل شده", MaterialTheme.colorScheme.secondary)
        ExamStatus.SCHEDULED -> Pair("برنامه‌ریزی شده", MaterialTheme.colorScheme.tertiary)
        ExamStatus.DRAFT -> Pair("پیش‌نویس", MaterialTheme.colorScheme.outline)
        ExamStatus.CANCELLED -> Pair("لغو شده", MaterialTheme.colorScheme.error)
    }

    AssistChip(
        onClick = {},
        label = {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = color.copy(alpha = 0.2f),
            labelColor = color
        )
    )
}

@Composable
fun ExamInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(
        modifier = Modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun EmptyExamList(
    onCreateExam: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Quiz,
            contentDescription = "بدون آزمون",
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "هنوز آزمونی ایجاد نکرده‌اید",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "برای شروع اولین آزمون خود را ایجاد کنید",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onCreateExam,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text("ایجاد آزمون جدید")
        }
    }
}

@Composable
fun NoFilteredExams() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.SearchOff,
            contentDescription = "آزمون یافت نشد",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "آزمونی با این وضعیت یافت نشد",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Text(
            text = "فیلترهای خود را تغییر دهید یا آزمون جدید ایجاد کنید",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
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

        Button(onClick = onRetry) {
            Text("تلاش مجدد")
        }
    }
}

private fun formatDateTime(dateTime: String): String {
    return try {
        // Try to parse the date string
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
        val localDateTime = LocalDateTime.parse(dateTime, formatter)

        // Format to Persian style
        val persianFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd - HH:mm")
        localDateTime.format(persianFormatter)
    } catch (e: Exception) {
        // If parsing fails, return the original string
        dateTime
    }
}

// Extension properties for Exam
val Exam.formattedStartTime: String
    get() = this.startTime?.let { formatDateTime(it) } ?: "زمان نامشخص"

val Exam.formattedDuration: String
    get() = this.examDuration?.let { "$it دقیقه" } ?: "زمان نامشخص"

val Exam.isAvailableForStart: Boolean
    get() = this.status == ExamStatus.ACTIVE

val Exam.hasResults: Boolean
    get() = this.status == ExamStatus.COMPLETED && this.resultId != null