// app/src/main/java/com/examapp/ui/exam/ExamSessionScreen.kt
package com.examapp.ui.exam

import android.annotation.SuppressLint
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamSessionScreen(
    navController: NavController,
    examId: String?,
    viewModel: ExamSessionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val remainingTime by viewModel.remainingTime.collectAsState()
    val currentQuestionIndex by viewModel.currentQuestionIndex.collectAsState()

    LaunchedEffect(examId) {
        examId?.let { viewModel.startExam(it) }
    }

    Scaffold(
        topBar = {
            when (val state = uiState) {
                is ExamSessionUiState.Active -> {
                    ExamSessionTopAppBar(
                        remainingTime = remainingTime,
                        currentQuestionIndex = currentQuestionIndex + 1,
                        totalQuestions = state.totalQuestions,
                        onExitClick = {
                            viewModel.pauseExam()
                            navController.popBackStack()
                        }
                    )
                }
                else -> {
                    // Show minimal top bar for other states
                    TopAppBar(title = { Text("آزمون") })
                }
            }
        },
        bottomBar = {
            when (val state = uiState) {
                is ExamSessionUiState.Active -> {
                    ExamSessionBottomBar(
                        totalQuestions = state.totalQuestions,
                        currentQuestionIndex = currentQuestionIndex,
                        onSubmitClick = { viewModel.submitExam() },
                        onQuestionClick = { index -> viewModel.goToQuestion(index) }
                    )
                }
                else -> {
                    // No bottom bar for other states
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (val state = uiState) {
                ExamSessionUiState.Loading -> {
                    LoadingScreen()
                }

                ExamSessionUiState.Submitting -> {
                    SubmittingScreen()
                }

                is ExamSessionUiState.Error -> {
                    ErrorScreen(
                        error = state.message,
                        onRetry = { examId?.let { viewModel.startExam(it) } }
                    )
                }

                is ExamSessionUiState.Active -> {
                    QuestionScreen(
                        question = state.currentQuestion,
                        selectedAnswer = state.currentQuestion.userAnswer,
                        hasNext = state.hasNext,
                        hasPrev = state.hasPrev,
                        onAnswerSelected = { answer -> viewModel.selectAnswer(answer) },
                        onNextClick = { viewModel.nextQuestion() },
                        onPrevClick = { viewModel.previousQuestion() }
                    )
                }

                is ExamSessionUiState.Completed -> {
                    ExamSubmittedScreen(
                        examSession = state.examSession,
                        submittedAt = state.submittedAt,
                        onViewResults = {
                            navController.navigate("exam_result/${state.examSession.examId}")
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamSessionTopAppBar(
    remainingTime: Long?,
    currentQuestionIndex: Int,
    totalQuestions: Int,
    onExitClick: () -> Unit
) {
    val timeFormatted = remainingTime?.let {
        val minutes = TimeUnit.SECONDS.toMinutes(it)
        val seconds = it - TimeUnit.MINUTES.toSeconds(minutes)
        String.format("%02d:%02d", minutes, seconds)
    } ?: "--:--"

    val timeColor = when (remainingTime) {
        null -> MaterialTheme.colorScheme.onSurface
        in 0..300 -> Color.Red // Less than 5 minutes
        else -> MaterialTheme.colorScheme.primary
    }

    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // زمان باقی مانده
                Column {
                    Text(
                        text = "زمان باقیمانده",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = timeFormatted,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = timeColor
                    )
                }

                // شماره سوال
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "سوال $currentQuestionIndex از $totalQuestions",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onExitClick) {
                Icon(Icons.Default.ExitToApp, contentDescription = "خروج")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
        )
    )
}

@Composable
fun QuestionScreen(
    question: com.examapp.data.models.Question,
    selectedAnswer: String?,
    hasNext: Boolean,
    hasPrev: Boolean,
    onAnswerSelected: (String) -> Unit,
    onNextClick: () -> Unit,
    onPrevClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header با اطلاعات سوال
        QuestionHeader(
            questionNumber = question.questionNumber ?: 1,
            difficulty = question.difficulty ?: "متوسط",
            bloomLevel = question.bloomLevel ?: "متوسط",
            marks = question.marks ?: 1
        )

        Spacer(modifier = Modifier.height(16.dp))

        // متن سوال
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text(
                text = question.questionText ?: "سوال بدون متن",
                modifier = Modifier.padding(16.dp),
                fontSize = 16.sp,
                lineHeight = 24.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // گزینه‌ها (برای سوالات چندگزینه‌ای)
        if (question.questionType == "MULTIPLE_CHOICE") {
            question.options?.forEach { option ->
                OptionItem(
                    option = option,
                    isSelected = selectedAnswer == option.optionId,
                    onOptionSelected = { onAnswerSelected(option.optionId) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        } else {
            // جواب کوتاه یا تشریحی
            ShortAnswerField(
                initialText = selectedAnswer ?: "",
                onTextChange = onAnswerSelected,
                isDescriptive = question.questionType == "DESCRIPTIVE",
                placeholder = when (question.questionType) {
                    "SHORT_ANSWER" -> "پاسخ کوتاه خود را وارد کنید..."
                    "DESCRIPTIVE" -> "پاسخ تشریحی خود را وارد کنید..."
                    else -> "پاسخ خود را وارد کنید..."
                }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Navigation بین سوالات
        QuestionNavigation(
            hasNext = hasNext,
            hasPrev = hasPrev,
            onNextClick = onNextClick,
            onPrevClick = onPrevClick
        )
    }
}

@Composable
fun QuestionHeader(
    questionNumber: Int,
    difficulty: String,
    bloomLevel: String,
    marks: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // شماره سوال
        Badge(
            containerColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(40.dp)
        ) {
            Text(
                text = questionNumber.toString(),
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold
            )
        }

        // سطح سختی
        val difficultyColor = when (difficulty.uppercase()) {
            "HARD" -> Color.Red
            "MEDIUM" -> Color(0xFFFFA726) // Orange
            else -> Color(0xFF4CAF50) // Green
        }

        Badge(
            containerColor = difficultyColor.copy(alpha = 0.2f),
            contentColor = difficultyColor
        ) {
            Text(text = when (difficulty.uppercase()) {
                "EASY" -> "آسان"
                "MEDIUM" -> "متوسط"
                "HARD" -> "سخت"
                else -> difficulty
            })
        }

        // سطح بلوم
        Text(
            text = bloomLevel,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // نمره
        Text(
            text = "$marks نمره",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun OptionItem(
    option: com.examapp.data.models.QuestionOption,
    isSelected: Boolean,
    onOptionSelected: () -> Unit
) {
    val optionLabel = when (option.optionNumber) {
        1 -> "الف"
        2 -> "ب"
        3 -> "ج"
        4 -> "د"
        else -> option.optionNumber.toString()
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOptionSelected() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // دایره انتخاب
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                    .border(
                        width = 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline,
                        shape = MaterialTheme.shapes.small
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Text(
                        text = "✓",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // متن گزینه
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = option.optionText ?: "گزینه بدون متن",
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )
            }

            // برچسب گزینه
            Text(
                text = optionLabel,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ShortAnswerField(
    initialText: String,
    onTextChange: (String) -> Unit,
    isDescriptive: Boolean = false,
    placeholder: String = "پاسخ خود را وارد کنید..."
) {
    var text by remember { mutableStateOf(initialText) }
    val maxChars = if (isDescriptive) 1000 else 250

    Column {
        OutlinedTextField(
            value = text,
            onValueChange = {
                if (it.length <= maxChars) {
                    text = it
                    onTextChange(it)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder) },
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.outline
            ),
            maxLines = if (isDescriptive) 10 else 3,
            singleLine = !isDescriptive
        )

        // Character counter
        Text(
            text = "${text.length}/$maxChars",
            fontSize = 12.sp,
            color = if (text.length > maxChars) Color.Red
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, start = 4.dp)
        )
    }
}

@Composable
fun QuestionNavigation(
    hasNext: Boolean,
    hasPrev: Boolean,
    onNextClick: () -> Unit,
    onPrevClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // دکمه قبلی
        Button(
            onClick = onPrevClick,
            enabled = hasPrev,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.weight(1f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.KeyboardArrowRight,
                    contentDescription = "قبلی"
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("سوال قبلی")
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // دکمه بعدی
        Button(
            onClick = onNextClick,
            enabled = hasNext,
            modifier = Modifier.weight(1f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text("سوال بعدی")
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    Icons.Default.KeyboardArrowLeft,
                    contentDescription = "بعدی"
                )
            }
        }
    }
}

@Composable
fun ExamSessionBottomBar(
    totalQuestions: Int,
    currentQuestionIndex: Int,
    onSubmitClick: () -> Unit,
    onQuestionClick: (Int) -> Unit
) {
    val scrollState = rememberScrollState()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp)
        )
    ) {
        Column {
            // عنوان
            Text(
                text = "پرسش‌نامه",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            // لیست سوالات
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(totalQuestions) { index ->
                    QuestionNumberButton(
                        questionNumber = index + 1,
                        isCurrent = index == currentQuestionIndex,
                        isAnswered = false, // TODO: Track answered questions
                        onClick = { onQuestionClick(index) }
                    )
                }
            }

            // دکمه ارسال
            Button(
                onClick = onSubmitClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text("پایان آزمون و ارسال پاسخ‌ها")
            }
        }
    }
}

@Composable
fun QuestionNumberButton(
    questionNumber: Int,
    isCurrent: Boolean,
    isAnswered: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        isCurrent -> MaterialTheme.colorScheme.primary
        isAnswered -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val contentColor = when {
        isCurrent -> MaterialTheme.colorScheme.onPrimary
        isAnswered -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(MaterialTheme.shapes.small)
            .background(backgroundColor)
            .clickable { onClick() }
            .border(
                width = if (isCurrent) 2.dp else 0.dp,
                color = if (isCurrent) MaterialTheme.colorScheme.primary
                else Color.Transparent,
                shape = MaterialTheme.shapes.small
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = questionNumber.toString(),
            color = contentColor,
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun ExamSubmittedScreen(
    examSession: com.examapp.data.models.ExamSession,
    submittedAt: Long,
    onViewResults: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = "آزمون ارسال شد",
            modifier = Modifier.size(100.dp),
            tint = Color(0xFF4CAF50)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "آزمون با موفقیت ارسال شد!",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "پاسخ‌های شما ذخیره و برای تصحیح ارسال شد.",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // اطلاعات آزمون
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InfoRow(label = "کد آزمون:", value = examSession.examId)
                InfoRow(label = "تاریخ ارسال:", value = java.time.format.DateTimeFormatter
                    .ofPattern("yyyy/MM/dd - HH:mm")
                    .format(java.time.Instant.ofEpochMilli(submittedAt)
                        .atZone(java.time.ZoneId.systemDefault())))
                examSession.timeSpent?.let { timeSpent ->
                    InfoRow(label = "زمان صرف شده:", value = "$timeSpent دقیقه")
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onViewResults,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text("مشاهده نتایج")
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp
        )
        Text(
            text = value,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp
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
            Text("در حال بارگذاری آزمون...")
        }
    }
}

@Composable
fun SubmittingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("در حال ارسال پاسخ‌ها...")
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
            text = "خطا در بارگذاری آزمون",
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

// Extension properties for formatting
val Long.formattedTime: String
    get() {
        val minutes = this / 60
        val seconds = this % 60
        return String.format("%02d:%02d", minutes, seconds)
    }