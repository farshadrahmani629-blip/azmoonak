// app/src/main/java/com/examapp/ui/exam/ExamResultScreen.kt
package com.examapp.ui.exam

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamResultScreen(
    navController: NavController,
    examId: String?,
    viewModel: ExamResultViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(examId) {
        examId?.let { viewModel.loadExamResult(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("نتایج آزمون") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "بازگشت")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (val state = uiState) {
                is ExamResultUiState.Loading -> {
                    LoadingScreen()
                }

                is ExamResultUiState.Error -> {
                    ErrorScreen(
                        error = state.message,
                        onRetry = { examId?.let { viewModel.loadExamResult(it) } }
                    )
                }

                is ExamResultUiState.Success -> {
                    ExamResultContent(
                        examResult = state.examResult,
                        navController = navController
                    )
                }

                else -> {
                    // Idle state
                }
            }
        }
    }
}

@Composable
fun ExamResultContent(
    examResult: ExamResultDetail,
    navController: NavController
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // Header با اطلاعات کلی
        item {
            ResultHeader(examResult = examResult)
        }

        // آمار کلی
        item {
            ResultStats(examResult = examResult)
        }

        // چارت پیشرفت (اگر داده کافی وجود دارد)
        if (examResult.classAverage != null && examResult.totalStudents != null) {
            item {
                ProgressChart(examResult = examResult)
            }
        }

        // لیست سوالات (اگر وجود دارد)
        if (examResult.questionResults.isNotEmpty()) {
            item {
                Text(
                    text = "بررسی تک‌تک سوالات",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            items(examResult.questionResults.size) { index ->
                QuestionResultItem(
                    questionResult = examResult.questionResults[index],
                    questionNumber = index + 1
                )
            }
        }

        // Action Buttons
        item {
            ResultActions(
                examResult = examResult,
                onViewPDF = { /* تولید PDF */ },
                onRetryExam = { /* شروع مجدد */ },
                onShare = { /* اشتراک گذاری */ }
            )
        }
    }
}

@Composable
fun ResultHeader(examResult: ExamResultDetail) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // نمره نهایی
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            examResult.percentage >= 80 -> Color(0xFF4CAF50) // Green
                            examResult.percentage >= 50 -> Color(0xFFFF9800) // Orange
                            else -> Color(0xFFF44336) // Red
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${examResult.finalScore}",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "از ${examResult.totalScore}",
                        fontSize = 16.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // اطلاعات آزمون
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = examResult.examTitle,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                examResult.courseName?.let { courseName ->
                    Text(
                        text = courseName,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "تاریخ: ${examResult.examDate}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // وضعیت (قبول/مردود)
            Badge(
                containerColor = if (examResult.isPassed) Color(0xFF4CAF50) else Color(0xFFF44336),
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Text(
                    text = if (examResult.isPassed) "قبول" else "نیاز به تلاش بیشتر",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun ResultStats(examResult: ExamResultDetail) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ردیف اول
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // پاسخ صحیح
            StatCard(
                title = "صحیح",
                value = examResult.correctAnswers.toString(),
                total = examResult.totalQuestions.toString(),
                color = Color(0xFF4CAF50),
                icon = Icons.Default.CheckCircle
            )

            // پاسخ غلط
            StatCard(
                title = "غلط",
                value = examResult.wrongAnswers.toString(),
                total = examResult.totalQuestions.toString(),
                color = Color(0xFFF44336),
                icon = Icons.Default.Cancel
            )

            // بی‌پاسخ
            StatCard(
                title = "بی‌پاسخ",
                value = examResult.unanswered.toString(),
                total = examResult.totalQuestions.toString(),
                color = Color(0xFF9E9E9E),
                icon = Icons.Default.HourglassEmpty
            )
        }

        // ردیف دوم
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // درصد
            StatCard(
                title = "درصد",
                value = "${examResult.percentage.roundToInt()}%",
                total = "100%",
                color = MaterialTheme.colorScheme.primary,
                icon = Icons.Default.Percent
            )

            // رتبه در کلاس (اگر موجود باشد)
            examResult.classRank?.let { rank ->
                StatCard(
                    title = "رتبه",
                    value = rank.toString(),
                    total = examResult.totalStudents?.toString() ?: "--",
                    color = Color(0xFF9C27B0),
                    icon = Icons.Default.Emblem
                )
            } ?: run {
                // زمان صرف شده
                StatCard(
                    title = "زمان",
                    value = examResult.timeSpent?.toString() ?: "--",
                    total = examResult.timeLimit?.toString() ?: "--",
                    color = Color(0xFFFF9800),
                    icon = Icons.Default.AccessTime
                )
            }

            // نمره
            StatCard(
                title = "نمره",
                value = examResult.finalScore.toString(),
                total = examResult.totalScore.toString(),
                color = Color(0xFF2196F3),
                icon = Icons.Default.Grade
            )
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    total: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = Modifier.weight(1f),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
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
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = title,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "/$total",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun ProgressChart(examResult: ExamResultDetail) {
    val classAverage = examResult.classAverage ?: return
    val totalStudents = examResult.totalStudents ?: return

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "مقایسه با میانگین کلاس",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(12.dp))

            // نمودار ساده
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // نمره دانش آموز
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        val studentHeight = (examResult.percentage / 100f) * 100
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(studentHeight.dp)
                                .background(MaterialTheme.colorScheme.primary)
                                .align(Alignment.BottomCenter)
                        )
                    }
                    Text(
                        text = "شما",
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Text(
                        text = "${examResult.percentage.roundToInt()}%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // میانگین کلاس
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        val classAvgHeight = (classAverage / 100f) * 100
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(classAvgHeight.dp)
                                .background(Color(0xFF9C27B0))
                                .align(Alignment.BottomCenter)
                        )
                    }
                    Text(
                        text = "میانگین",
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Text(
                        text = "${classAverage.roundToInt()}%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // اطلاعات اضافی
            Text(
                text = "کل دانش‌آموزان: $totalStudents نفر",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun QuestionResultItem(questionResult: QuestionResult, questionNumber: Int) {
    val borderColor = when {
        questionResult.isCorrect -> Color(0xFF4CAF50)
        questionResult.userAnswer.isNullOrEmpty() -> Color(0xFF9E9E9E)
        else -> Color(0xFFF44336)
    }

    val backgroundColor = when {
        questionResult.isCorrect -> Color(0xFFE8F5E8)
        questionResult.userAnswer.isNullOrEmpty() -> Color(0xFFF5F5F5)
        else -> Color(0xFFFFEBEE)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // هدر سوال
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Badge(
                        containerColor = borderColor,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text(
                            text = questionNumber.toString(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "سوال ${questionNumber}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        questionResult.marks?.let { marks ->
                            Text(
                                text = "$marks نمره",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // وضعیت
                Badge(
                    containerColor = borderColor.copy(alpha = 0.2f),
                    contentColor = borderColor
                ) {
                    Text(
                        text = when {
                            questionResult.isCorrect -> "صحیح"
                            questionResult.userAnswer.isNullOrEmpty() -> "بی‌پاسخ"
                            else -> "غلط"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // متن سوال
            Text(
                text = questionResult.questionText ?: "سوال بدون متن",
                fontSize = 14.sp,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // پاسخ صحیح (اگر غلط پاسخ داده شده)
            if (!questionResult.isCorrect && !questionResult.correctAnswer.isNullOrEmpty()) {
                Column {
                    Text(
                        text = "پاسخ صحیح:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = questionResult.correctAnswer!!,
                        fontSize = 14.sp,
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // پاسخ کاربر (اگر پاسخ داده شده)
            if (!questionResult.userAnswer.isNullOrEmpty()) {
                Column {
                    Text(
                        text = "پاسخ شما:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = questionResult.userAnswer!!,
                        fontSize = 14.sp,
                        color = if (questionResult.isCorrect) Color(0xFF4CAF50) else Color(0xFFF44336),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // توضیحات (اگر وجود دارد)
            questionResult.explanation?.let { explanation ->
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "توضیح:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = explanation,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ResultActions(
    examResult: ExamResultDetail,
    onViewPDF: () -> Unit,
    onRetryExam: () -> Unit,
    onShare: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // دکمه‌های اصلی
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onViewPDF,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ذخیره PDF")
                }
            }

            Button(
                onClick = onRetryExam,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Refresh, contentDescription = "تلاش مجدد")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("آزمون مجدد")
                }
            }
        }

        // دکمه اشتراک
        OutlinedButton(
            onClick = onShare,
            modifier = Modifier.fillMaxWidth(),
            border = ButtonDefaults.outlinedButtonBorder.copy(
                width = 1.dp
            )
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Share, contentDescription = "اشتراک")
                Spacer(modifier = Modifier.width(8.dp))
                Text("اشتراک نتایج")
            }
        }

        // تحلیل هوشمند
        examResult.recommendations?.let { recommendations ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "تحلیل هوشمند",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = recommendations,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
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
            Text("در حال بارگذاری نتایج...")
        }
    }
}

@Composable
fun ErrorScreen(error: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Error,
            contentDescription = "خطا",
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "خطا در بارگذاری نتایج",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = error,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

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

// ViewModel State classes (should be in ViewModel file)
sealed class ExamResultUiState {
    data object Idle : ExamResultUiState()
    data object Loading : ExamResultUiState()
    data class Success(val examResult: ExamResultDetail) : ExamResultUiState()
    data class Error(val message: String) : ExamResultUiState()
}

// Data classes (should be in data models)
data class ExamResultDetail(
    val id: String,
    val examId: String,
    val examTitle: String,
    val finalScore: Int,
    val totalScore: Int,
    val percentage: Double,
    val correctAnswers: Int,
    val wrongAnswers: Int,
    val unanswered: Int,
    val totalQuestions: Int,
    val isPassed: Boolean,
    val examDate: String,
    val courseName: String? = null,
    val timeSpent: Int? = null,
    val timeLimit: Int? = null,
    val classRank: Int? = null,
    val totalStudents: Int? = null,
    val classAverage: Double? = null,
    val recommendations: String? = null,
    val questionResults: List<QuestionResult> = emptyList()
)

data class QuestionResult(
    val questionId: String,
    val questionText: String?,
    val userAnswer: String?,
    val correctAnswer: String?,
    val isCorrect: Boolean,
    val marks: Int? = null,
    val explanation: String? = null
)