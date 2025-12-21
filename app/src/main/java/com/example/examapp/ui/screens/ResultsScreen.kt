// app/src/main/java/com/examapp/ui/screens/ResultsScreen.kt
package com.examapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.examapp.data.models.Question
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(
    questions: List<Question>,
    userAnswers: Map<Int, String>,
    timeSpent: Int, // ثانیه
    onReviewAnswers: () -> Unit,
    onNewExam: () -> Unit,
    onBack: () -> Unit
) {
    // محاسبات
    val totalQuestions = questions.size
    val correctAnswers = questions.indices.count { index ->
        val question = questions[index]
        val userAnswer = userAnswers[index]
        val correctAnswer = when (question.type) {
            "multiple_choice", "MULTIPLE_CHOICE" -> question.correctOption?.toString()
            "true_false", "TRUE_FALSE" -> if (question.isCorrect == true) "true" else "false"
            else -> question.correctAnswer
        }
        userAnswer == correctAnswer
    }
    val wrongAnswers = totalQuestions - correctAnswers
    val scorePercentage = if (totalQuestions > 0)
        (correctAnswers * 100f / totalQuestions).roundToInt()
    else 0

    val minutes = timeSpent / 60
    val seconds = timeSpent % 60

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "نتایج آزمون",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "بازگشت")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // خلاصه نتایج
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // دایره نمره
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .background(
                                    color = when {
                                        scorePercentage >= 80 -> Color(0xFF4CAF50)
                                        scorePercentage >= 50 -> Color(0xFFFF9800)
                                        else -> Color(0xFFF44336)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$scorePercentage%",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "نمره",
                                    fontSize = 14.sp,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // آمار
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatItem(
                                icon = Icons.Default.CheckCircle,
                                value = correctAnswers.toString(),
                                label = "صحیح",
                                color = Color(0xFF4CAF50)
                            )

                            StatItem(
                                icon = Icons.Default.Cancel,
                                value = wrongAnswers.toString(),
                                label = "غلط",
                                color = Color(0xFFF44336)
                            )

                            StatItem(
                                icon = Icons.Default.Timer,
                                value = String.format("%02d:%02d", minutes, seconds),
                                label = "زمان",
                                color = Color(0xFF2196F3)
                            )
                        }
                    }
                }
            }

            // بازخورد
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "بازخورد توصیفی:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        val feedback = when {
                            scorePercentage >= 90 -> FeedbackItem(
                                icon = Icons.Default.EmojiEvents,
                                title = "عالی! 🎉",
                                description = "تو واقعاً این مبحث را به خوبی یاد گرفته‌ای!",
                                color = Color(0xFFFFD700)
                            )
                            scorePercentage >= 70 -> FeedbackItem(
                                icon = Icons.Default.ThumbUp,
                                title = "خوب 👍",
                                description = "عملکرد خوبی داشتی، می‌تونی بهتر هم بشی!",
                                color = Color(0xFF4CAF50)
                            )
                            scorePercentage >= 50 -> FeedbackItem(
                                icon = Icons.Default.School,
                                title = "قابل قبول 📚",
                                description = "نیاز به تمرین بیشتر داری.",
                                color = Color(0xFFFF9800)
                            )
                            else -> FeedbackItem(
                                icon = Icons.Default.Help,
                                title = "نیاز به تلاش بیشتر 💪",
                                description = "بیا دوباره این درس را با هم مرور کنیم.",
                                color = Color(0xFFF44336)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = feedback.icon,
                                contentDescription = null,
                                tint = feedback.color,
                                modifier = Modifier.size(40.dp)
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            Column {
                                Text(
                                    text = feedback.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = feedback.description,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }

            // تحلیل سوالات
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "تحلیل سوالات:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        questions.forEachIndexed { index, question ->
                            val userAnswer = userAnswers[index]
                            val correctAnswer = when (question.type) {
                                "multiple_choice", "MULTIPLE_CHOICE" -> {
                                    val optionIndex = question.correctOption?.minus(1) ?: 0
                                    question.options?.getOrNull(optionIndex)?.text ?: "گزینه ${question.correctOption}"
                                }
                                "true_false", "TRUE_FALSE" -> if (question.isCorrect == true) "صحیح" else "غلط"
                                else -> question.correctAnswer ?: "پاسخ مشخص نیست"
                            }
                            val isCorrect = userAnswer == correctAnswer

                            QuestionResultItem(
                                questionNumber = index + 1,
                                question = question,
                                userAnswer = userAnswer,
                                correctAnswer = correctAnswer,
                                isCorrect = isCorrect,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // دکمه‌های عمل
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onReviewAnswers,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            Icons.Default.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("مرور پاسخ‌ها")
                    }

                    Button(
                        onClick = onNewExam,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("آزمون جدید")
                    }
                }
            }
        }
    }
}

@Composable
fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(30.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

data class FeedbackItem(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val description: String,
    val color: Color
)

@Composable
fun QuestionResultItem(
    questionNumber: Int,
    question: Question,
    userAnswer: String?,
    correctAnswer: String,
    isCorrect: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isCorrect)
                Color(0xFFE8F5E8)
            else
                Color(0xFFFFEBEE)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // شماره سوال
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(
                        color = if (isCorrect)
                            Color(0xFF4CAF50)
                        else
                            Color(0xFFF44336)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = questionNumber.toString(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // اطلاعات سوال
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = question.text.take(50) + if (question.text.length > 50) "..." else "",
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (isCorrect) {
                        "✓ پاسخ شما درست است"
                    } else {
                        "✗ پاسخ صحیح: $correctAnswer"
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            // وضعیت
            Icon(
                imageVector = if (isCorrect)
                    Icons.Default.Check
                else
                    Icons.Default.Close,
                contentDescription = null,
                tint = if (isCorrect)
                    Color(0xFF4CAF50)
                else
                    Color(0xFFF44336)
            )
        }
    }
}