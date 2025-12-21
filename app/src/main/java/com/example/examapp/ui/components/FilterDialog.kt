// app/src/main/java/com/examapp/ui/components/FilterDialog.kt
package com.examapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.examapp.data.models.ExamFilter

@Composable
fun FilterDialog(
    currentFilter: ExamFilter,
    onDismiss: () -> Unit,
    onConfirm: (ExamFilter) -> Unit
) {
    var grade by remember { mutableStateOf(currentFilter.grade?.toString() ?: "") }
    var subject by remember { mutableStateOf(currentFilter.subject ?: "") }
    var fromPage by remember { mutableStateOf(currentFilter.fromPage?.toString() ?: "") }
    var toPage by remember { mutableStateOf(currentFilter.toPage?.toString() ?: "") }
    var difficulty by remember { mutableStateOf(currentFilter.difficulty?.toString() ?: "") }
    var bloomLevel by remember { mutableStateOf(currentFilter.bloomLevel?.toString() ?: "") }
    var questionCount by remember { mutableStateOf(currentFilter.questionCount?.toString() ?: "20") }

    // Validation states
    val isGradeValid = grade.isEmpty() || (grade.toIntOrNull() in 1..6)
    val isQuestionCountValid = questionCount.isNotEmpty() && (questionCount.toIntOrNull() in 1..50)
    val isPageRangeValid = (fromPage.isEmpty() && toPage.isEmpty()) ||
            (fromPage.toIntOrNull() != null && toPage.toIntOrNull() != null &&
                    fromPage.toIntOrNull()!! <= toPage.toIntOrNull()!!)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(600.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "تنظیمات آزمون",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // پایه تحصیلی
                OutlinedTextField(
                    value = grade,
                    onValueChange = { grade = it },
                    label = { Text("پایه تحصیلی (۱-۶)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = !isGradeValid,
                    supportingText = {
                        if (!isGradeValid) {
                            Text("پایه باید بین ۱ تا ۶ باشد")
                        }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // درس
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("درس (ریاضی، فارسی، علوم...)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // صفحات
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = fromPage,
                        onValueChange = { fromPage = it },
                        label = { Text("از صفحه") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        isError = !isPageRangeValid,
                        keyboardOptions = androidx.compose.ui.text.input.KeyboardOptions.Default.copy(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        )
                    )
                    OutlinedTextField(
                        value = toPage,
                        onValueChange = { toPage = it },
                        label = { Text("تا صفحه") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        isError = !isPageRangeValid,
                        keyboardOptions = androidx.compose.ui.text.input.KeyboardOptions.Default.copy(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        )
                    )
                }

                if (!isPageRangeValid) {
                    Text(
                        text = "محدوده صفحات نامعتبر است",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // سختی
                OutlinedTextField(
                    value = difficulty,
                    onValueChange = { difficulty = it },
                    label = { Text("سطح سختی (۱=آسان، ۲=متوسط، ۳=سخت)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = androidx.compose.ui.text.input.KeyboardOptions.Default.copy(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // سطح بلوم
                OutlinedTextField(
                    value = bloomLevel,
                    onValueChange = { bloomLevel = it },
                    label = { Text("سطح بلوم (۱-۵)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = androidx.compose.ui.text.input.KeyboardOptions.Default.copy(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // تعداد سوالات
                OutlinedTextField(
                    value = questionCount,
                    onValueChange = { questionCount = it },
                    label = { Text("تعداد سوالات (۱-۵۰)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = !isQuestionCountValid,
                    supportingText = {
                        if (!isQuestionCountValid) {
                            Text("تعداد سوالات باید بین ۱ تا ۵۰ باشد")
                        }
                    },
                    keyboardOptions = androidx.compose.ui.text.input.KeyboardOptions.Default.copy(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Info box
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Info,
                            contentDescription = "اطلاعات",
                            modifier = Modifier.padding(end = 8.dp)
                        )

                        Text(
                            text = "فیلدهای ستاره‌دار اجباری هستند. سایر فیلدها اختیاری.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // دکمه‌ها
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Text("لغو")
                    }

                    Button(
                        onClick = {
                            val newFilter = ExamFilter(
                                grade = grade.toIntOrNull(),
                                subject = subject.ifEmpty { null },
                                fromPage = fromPage.toIntOrNull(),
                                toPage = toPage.toIntOrNull(),
                                difficulty = difficulty.toIntOrNull(),
                                bloomLevel = bloomLevel.toIntOrNull(),
                                questionCount = questionCount.toIntOrNull() ?: 20
                            )
                            onConfirm(newFilter)
                        },
                        modifier = Modifier.weight(1f),
                        enabled = isGradeValid && isQuestionCountValid && isPageRangeValid
                    ) {
                        Text("تأیید")
                    }
                }
            }
        }
    }
}

// Extension function for ExamFilter
val ExamFilter.isValid: Boolean
    get() = (grade == null || (grade in 1..6)) &&
            (questionCount == null || (questionCount in 1..50))

fun ExamFilter.getDisplayText(): String {
    val parts = mutableListOf<String>()

    grade?.let { parts.add("پایه $it") }
    subject?.let { parts.add("درس $it") }
    questionCount?.let { parts.add("$it سوال") }

    return if (parts.isEmpty()) "بدون فیلتر" else parts.joinToString(" • ")
}