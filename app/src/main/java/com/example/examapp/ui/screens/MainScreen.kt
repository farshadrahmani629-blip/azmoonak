// app/src/main/java/com/examapp/ui/screens/MainScreen.kt
package com.examapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.examapp.ui.components.FilterDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onStartExam: (ExamFilter) -> Unit,
    onGeneratePDF: (ExamFilter) -> Unit
) {
    var showFilterDialog by remember { mutableStateOf(false) }
    var currentFilter by remember { mutableStateOf(ExamFilter()) }
    var examType by remember { mutableStateOf("online") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "آزمونک 📚",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // فیلتر فعلی
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "فیلتر انتخابی:",
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = currentFilter.toDisplayText())
                }
            }

            // دکمه تغییر فیلتر
            Button(
                onClick = { showFilterDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("تغییر فیلتر آزمون")
            }

            // انتخاب نوع آزمون
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "نوع آزمون:",
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = examType == "online",
                            onClick = { examType = "online" },
                            label = { Text("آزمون آنلاین ⏱️") }
                        )
                        FilterChip(
                            selected = examType == "pdf",
                            onClick = { examType = "pdf" },
                            label = { Text("پی‌دی‌ف 📄") }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // دکمه شروع
            Button(
                onClick = {
                    val filter = currentFilter.copy(
                        examType = if (examType == "online")
                            ExamFilter.ExamType.ONLINE
                        else
                            ExamFilter.ExamType.PDF
                    )
                    if (examType == "online") {
                        onStartExam(filter)
                    } else {
                        onGeneratePDF(filter)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = currentFilter.isValid(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 8.dp
                )
            ) {
                Text(
                    text = if (examType == "online")
                        "شروع آزمون آنلاین 🚀"
                    else
                        "تولید پی‌دی‌ف 📥",
                    fontSize = 18.sp
                )
            }
        }
    }

    // دیالوگ فیلتر
    if (showFilterDialog) {
        FilterDialog(
            currentFilter = currentFilter,
            onDismiss = { showFilterDialog = false },
            onConfirm = { newFilter ->
                currentFilter = newFilter
                showFilterDialog = false
            }
        )
    }
}

// Data classes for filtering
data class ExamFilter(
    val subject: String = "",
    val grade: Int = 4,
    val chapter: String = "",
    val difficulty: DifficultyLevel = DifficultyLevel.MEDIUM,
    val questionCount: Int = 10,
    val examType: ExamType = ExamType.ONLINE
) {
    enum class ExamType {
        ONLINE, PDF
    }

    fun isValid(): Boolean {
        return subject.isNotBlank() && questionCount > 0
    }

    fun toDisplayText(): String {
        return """
            درس: $subject
            پایه: $grade
            فصل: ${if (chapter.isNotBlank()) chapter else "همه"}
            سطح: ${difficulty.displayName}
            تعداد سوال: $questionCount
        """.trimIndent()
    }
}

enum class DifficultyLevel(val displayName: String) {
    EASY("آسان"),
    MEDIUM("متوسط"),
    HARD("سخت")
}