// app/src/main/java/com/examapp/ui/screens/ExamScreen.kt
package com.examapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.examapp.data.models.Question
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamScreen(
    questions: List<Question>,
    onFinishExam: (Map<Int, String>, Int) -> Unit, // پاسخ‌ها و زمان
    onBack: () -> Unit
) {
    var currentQuestionIndex by remember { mutableIntStateOf(0) }
    var userAnswers by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }
    var timeRemaining by remember { mutableIntStateOf(45 * 60) } // 45 دقیقه به ثانیه
    var isExamFinished by remember { mutableStateOf(false) }

    // تایمر
    LaunchedEffect(Unit) {
        while (timeRemaining > 0 && !isExamFinished) {
            delay(1000)
            timeRemaining--
        }
        if (timeRemaining <= 0) {
            isExamFinished = true
            onFinishExam(userAnswers, 45 * 60)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "آزمون",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "بازگشت")
                    }
                },
                actions = {
                    // تایمر
                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .background(
                                color = if (timeRemaining < 5 * 60) Color.Red.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.primaryContainer,
                                shape = MaterialTheme.shapes.small
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        val minutes = timeRemaining / 60
                        val seconds = timeRemaining % 60
                        Text(
                            text = String.format("%02d:%02d", minutes, seconds),
                            fontWeight = FontWeight.Bold,
                            color = if (timeRemaining < 5 * 60) Color.Red
                            else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // دکمه قبلی
                    IconButton(
                        onClick = {
                            if (currentQuestionIndex > 0) currentQuestionIndex--
                        },
                        enabled = currentQuestionIndex > 0
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "سوال قبلی"
                        )
                    }

                    // شماره سوال
                    Text(
                        text = "${currentQuestionIndex + 1} / ${questions.size}",
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )

                    // دکمه بعدی یا پایان
                    if (currentQuestionIndex < questions.size - 1) {
                        IconButton(
                            onClick = { currentQuestionIndex++ }
                        ) {
                            Icon(
                                Icons.Default.NavigateNext,
                                contentDescription = "سوال بعدی"
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                isExamFinished = true
                                onFinishExam(userAnswers, 45 * 60 - timeRemaining)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "پایان آزمون",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("پایان آزمون")
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        if (questions.isNotEmpty() && currentQuestionIndex < questions.size) {
            val question = questions[currentQuestionIndex]
            val currentAnswer = userAnswers[currentQuestionIndex] ?: ""

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                // شماره و اطلاعات سوال
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "سوال ${currentQuestionIndex + 1}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "${question.difficulty?.displayName ?: "متوسط"} • ${question.bloomLevel?.displayName ?: "درک و فهم"}",
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // موضوع
                        Text(
                            text = "${question.subject ?: "عمومی"} - صفحه ${question.pageNumber ?: "?"}",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // متن سوال
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "صورت سوال:",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = question.text,
                            fontSize = 16.sp,
                            lineHeight = 24.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // پاسخ‌ها (بسته به نوع سوال)
                when (question.type) {
                    "multiple_choice", "MULTIPLE_CHOICE" -> {
                        MultipleChoiceQuestion(
                            question = question,
                            selectedAnswer = currentAnswer,
                            onAnswerSelected = { answer ->
                                userAnswers = userAnswers + (currentQuestionIndex to answer)
                            }
                        )
                    }

                    "true_false", "TRUE_FALSE" -> {
                        TrueFalseQuestion(
                            selectedAnswer = currentAnswer,
                            onAnswerSelected = { answer ->
                                userAnswers = userAnswers + (currentQuestionIndex to answer)
                            }
                        )
                    }

                    "fill_blank", "FILL_BLANK" -> {
                        FillBlankQuestion(
                            selectedAnswer = currentAnswer,
                            onAnswerChanged = { answer ->
                                userAnswers = userAnswers + (currentQuestionIndex to answer)
                            }
                        )
                    }

                    "short_answer", "descriptive", "SHORT_ANSWER", "DESCRIPTIVE" -> {
                        DescriptiveQuestion(
                            selectedAnswer = currentAnswer,
                            onAnswerChanged = { answer ->
                                userAnswers = userAnswers + (currentQuestionIndex to answer)
                            }
                        )
                    }

                    else -> {
                        Text("نوع سوال پشتیبانی نمی‌شود: ${question.type}")
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("سوالی برای نمایش وجود ندارد")
            }
        }
    }
}

@Composable
fun MultipleChoiceQuestion(
    question: Question,
    selectedAnswer: String,
    onAnswerSelected: (String) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "گزینه صحیح را انتخاب کنید:",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        val options = question.options ?: emptyList()
        options.forEachIndexed { index, option ->
            val optionChar = ('الف'.code + index).toChar()
            val isSelected = selectedAnswer == option.id?.toString() || selectedAnswer == (index + 1).toString()

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surface
                ),
                onClick = {
                    onAnswerSelected(option.id?.toString() ?: (index + 1).toString())
                }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .background(
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.primary
                                else
                                    Color.Gray.copy(alpha = 0.3f),
                                shape = MaterialTheme.shapes.small
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = optionChar.toString(),
                            color = if (isSelected)
                                MaterialTheme.colorScheme.onPrimary
                            else
                                MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Text(
                        text = option.text,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun TrueFalseQuestion(
    selectedAnswer: String,
    onAnswerSelected: (String) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "صحیح یا غلط؟",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FilterChip(
                selected = selectedAnswer.equals("true", ignoreCase = true) || selectedAnswer == "1",
                onClick = { onAnswerSelected("true") },
                label = { Text("صحیح") },
                modifier = Modifier.weight(1f)
            )

            FilterChip(
                selected = selectedAnswer.equals("false", ignoreCase = true) || selectedAnswer == "0",
                onClick = { onAnswerSelected("false") },
                label = { Text("غلط") },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun FillBlankQuestion(
    selectedAnswer: String,
    onAnswerChanged: (String) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "جای خالی را پر کنید:",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = selectedAnswer,
            onValueChange = onAnswerChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("پاسخ شما") },
            placeholder = { Text("پاسخ خود را بنویسید...") },
            singleLine = true
        )
    }
}

@Composable
fun DescriptiveQuestion(
    selectedAnswer: String,
    onAnswerChanged: (String) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "پاسخ تشریحی:",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = selectedAnswer,
            onValueChange = onAnswerChanged,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            label = { Text("پاسخ خود را بنویسید") },
            placeholder = { Text("پاسخ تشریحی خود را با جزئیات بنویسید...") }
        )
    }
}

// Extension properties for UI
val Question.difficultyText: String
    get() = when (this.difficulty) {
        "EASY", "easy" -> "آسان"
        "MEDIUM", "medium" -> "متوسط"
        "HARD", "hard" -> "سخت"
        else -> "متوسط"
    }

val Question.bloomText: String
    get() = when (this.bloomLevel) {
        "REMEMBERING" -> "حفظ"
        "UNDERSTANDING" -> "درک"
        "APPLYING" -> "کاربرد"
        "ANALYZING" -> "تحلیل"
        "EVALUATING" -> "ارزیابی"
        "CREATING" -> "خلق"
        else -> "درک و فهم"
    }