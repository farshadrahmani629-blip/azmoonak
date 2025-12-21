// app/src/main/java/com/examapp/ui/exam/ExamCreationScreen.kt
package com.examapp.ui.exam

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.examapp.R
import com.examapp.data.models.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamCreationScreen(
    onCreateExamSuccess: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: ExamCreationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        when (uiState) {
            is ExamCreationUiState.Success -> {
                onCreateExamSuccess()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("ایجاد آزمون جدید") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = "بازگشت"
                        )
                    }
                }
            )
        },
        bottomBar = {
            when (val state = uiState) {
                is ExamCreationUiState.Form -> {
                    ExamCreationBottomBar(
                        currentStep = state.currentStep,
                        totalSteps = 3,
                        onNext = { viewModel.nextStep() },
                        onPrevious = { viewModel.previousStep() },
                        onCreate = { viewModel.createExam() },
                        isCreating = false
                    )
                }
                ExamCreationUiState.Creating -> {
                    ExamCreationBottomBar(
                        currentStep = 3,
                        totalSteps = 3,
                        onNext = {},
                        onPrevious = {},
                        onCreate = {},
                        isCreating = true
                    )
                }
                else -> {}
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                ExamCreationUiState.Loading -> {
                    LoadingScreen()
                }

                ExamCreationUiState.Creating -> {
                    CreatingScreen()
                }

                is ExamCreationUiState.Form -> {
                    ExamCreationContent(
                        currentStep = state.currentStep,
                        formData = state.formData,
                        onFormDataChange = viewModel::updateFormData,
                        booksState = state.booksState,
                        chaptersState = state.chaptersState
                    )
                }

                is ExamCreationUiState.Error -> {
                    ErrorState(
                        message = state.message,
                        onRetry = { viewModel.resetState() }
                    )
                }

                is ExamCreationUiState.Success -> {
                    SuccessState(
                        exam = state.exam,
                        onContinue = onCreateExamSuccess
                    )
                }
            }
        }
    }
}

@Composable
fun ExamCreationContent(
    currentStep: Int,
    formData: ExamCreationFormData,
    onFormDataChange: (ExamCreationFormData) -> Unit,
    booksState: BooksState,
    chaptersState: ChaptersState
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Progress indicator
        LinearProgressIndicator(
            progress = { currentStep.toFloat() / 3 },
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "مرحله $currentStep از ۳",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )

        when (currentStep) {
            1 -> Step1BasicInfo(
                formData = formData,
                onFormDataChange = onFormDataChange
            )

            2 -> Step2ContentSelection(
                formData = formData,
                onFormDataChange = onFormDataChange,
                booksState = booksState,
                chaptersState = chaptersState
            )

            3 -> Step3AdvancedSettings(
                formData = formData,
                onFormDataChange = onFormDataChange
            )
        }
    }
}

@Composable
fun Step1BasicInfo(
    formData: ExamCreationFormData,
    onFormDataChange: (ExamCreationFormData) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "اطلاعات پایه آزمون",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )

        OutlinedTextField(
            value = formData.title,
            onValueChange = { onFormDataChange(formData.copy(title = it)) },
            label = { Text("عنوان آزمون*") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = formData.title.isBlank()
        )

        OutlinedTextField(
            value = formData.description,
            onValueChange = { onFormDataChange(formData.copy(description = it)) },
            label = { Text("توضیحات (اختیاری)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        // Grade selection
        Column {
            Text(
                text = "پایه تحصیلی",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                (1..6).forEach { grade ->
                    FilterChip(
                        selected = formData.grade == grade,
                        onClick = {
                            onFormDataChange(formData.copy(grade = if (formData.grade == grade) null else grade))
                        },
                        label = { Text("پایه $grade") }
                    )
                }
            }
        }

        // Subject selection
        Column {
            Text(
                text = "درس",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val subjects = listOf("ریاضی", "علوم", "فارسی", "هدیه‌های آسمان", "مطالعات اجتماعی")

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                subjects.forEach { subject ->
                    FilterChip(
                        selected = formData.subject == subject,
                        onClick = {
                            onFormDataChange(formData.copy(subject = if (formData.subject == subject) null else subject))
                        },
                        label = { Text(subject) }
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = formData.totalQuestions.toString(),
                onValueChange = {
                    val value = it.toIntOrNull()
                    if (value != null) {
                        onFormDataChange(formData.copy(totalQuestions = value))
                    }
                },
                label = { Text("تعداد سوالات*") },
                modifier = Modifier.weight(1f),
                keyboardOptions = androidx.compose.ui.text.input.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                ),
                isError = formData.totalQuestions !in 1..100
            )

            OutlinedTextField(
                value = formData.examDuration.toString(),
                onValueChange = {
                    val value = it.toIntOrNull()
                    if (value != null) {
                        onFormDataChange(formData.copy(examDuration = value))
                    }
                },
                label = { Text("مدت زمان (دقیقه)*") },
                modifier = Modifier.weight(1f),
                keyboardOptions = androidx.compose.ui.text.input.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                ),
                isError = formData.examDuration !in 1..180
            )
        }
    }
}

@Composable
fun Step2ContentSelection(
    formData: ExamCreationFormData,
    onFormDataChange: (ExamCreationFormData) -> Unit,
    booksState: BooksState,
    chaptersState: ChaptersState
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "انتخاب محتوا",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )

        // Book selection
        Column {
            Text(
                text = "کتاب",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            when (booksState) {
                is BooksState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }

                is BooksState.Success -> {
                    val books = booksState.books

                    if (books.isEmpty()) {
                        Text(
                            text = "کتابی یافت نشد",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.height(150.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(books) { book ->
                                BookSelectionItem(
                                    book = book,
                                    isSelected = formData.selectedBook == book.id,
                                    onClick = {
                                        onFormDataChange(formData.copy(selectedBook = book.id))
                                    }
                                )
                            }
                        }
                    }
                }

                is BooksState.Error -> {
                    Text(
                        text = booksState.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // Chapters selection (if book is selected)
        formData.selectedBook?.let { bookId ->
            Column {
                Text(
                    text = "فصل‌ها",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                when (chaptersState) {
                    is ChaptersState.Loading -> {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }

                    is ChaptersState.Success -> {
                        val chapters = chaptersState.chapters

                        Text(
                            text = "انتخاب فصل‌ها (اختیاری):",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        LazyColumn(
                            modifier = Modifier.height(200.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(chapters) { chapter ->
                                ChapterSelectionItem(
                                    chapter = chapter,
                                    isSelected = formData.selectedChapters.contains(chapter.id),
                                    onClick = {
                                        val newSelection = if (formData.selectedChapters.contains(chapter.id)) {
                                            formData.selectedChapters - chapter.id
                                        } else {
                                            formData.selectedChapters + chapter.id
                                        }
                                        onFormDataChange(formData.copy(selectedChapters = newSelection))
                                    }
                                )
                            }
                        }

                        if (formData.selectedChapters.isNotEmpty()) {
                            Text(
                                text = "${formData.selectedChapters.size} فصل انتخاب شده",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    is ChaptersState.Error -> {
                        Text(
                            text = chaptersState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    else -> {
                        Text(
                            text = "در حال بارگذاری فصل‌ها...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Page range selection
        Column {
            Text(
                text = "محدوده صفحات (اختیاری)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = formData.pageRangeStart?.toString() ?: "",
                    onValueChange = {
                        val value = it.toIntOrNull()
                        onFormDataChange(formData.copy(pageRangeStart = value))
                    },
                    label = { Text("از صفحه") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = androidx.compose.ui.text.input.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    )
                )

                Text("تا")

                OutlinedTextField(
                    value = formData.pageRangeEnd?.toString() ?: "",
                    onValueChange = {
                        val value = it.toIntOrNull()
                        onFormDataChange(formData.copy(pageRangeEnd = value))
                    },
                    label = { Text("تا صفحه") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = androidx.compose.ui.text.input.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    )
                )
            }
        }

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
                    imageVector = Icons.Default.Info,
                    contentDescription = "اطلاعات",
                    modifier = Modifier.padding(end = 8.dp)
                )

                Text(
                    text = "اگر کتاب یا فصل انتخاب نکنید، سوالات از همه منابع انتخاب می‌شوند.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun Step3AdvancedSettings(
    formData: ExamCreationFormData,
    onFormDataChange: (ExamCreationFormData) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "تنظیمات پیشرفته",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )

        // Question types
        Column {
            Text(
                text = "نوع سوالات",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                QuestionType.values().forEach { type ->
                    val typeName = when (type) {
                        QuestionType.MULTIPLE_CHOICE -> "چهارگزینه‌ای"
                        QuestionType.SHORT_ANSWER -> "کوتاه‌پاسخ"
                        QuestionType.DESCRIPTIVE -> "تشریحی"
                    }

                    FilterChip(
                        selected = formData.selectedQuestionTypes.contains(type),
                        onClick = {
                            val newSelection = if (formData.selectedQuestionTypes.contains(type)) {
                                formData.selectedQuestionTypes - type
                            } else {
                                formData.selectedQuestionTypes + type
                            }
                            onFormDataChange(formData.copy(selectedQuestionTypes = newSelection))
                        },
                        label = { Text(typeName) }
                    )
                }
            }
        }

        // Difficulty levels
        Column {
            Text(
                text = "سطح سختی",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                DifficultyLevel.values().forEach { level ->
                    val levelName = when (level) {
                        DifficultyLevel.EASY -> "آسان"
                        DifficultyLevel.MEDIUM -> "متوسط"
                        DifficultyLevel.HARD -> "سخت"
                    }

                    FilterChip(
                        selected = formData.selectedDifficultyLevels.contains(level),
                        onClick = {
                            val newSelection = if (formData.selectedDifficultyLevels.contains(level)) {
                                formData.selectedDifficultyLevels - level
                            } else {
                                formData.selectedDifficultyLevels + level
                            }
                            onFormDataChange(formData.copy(selectedDifficultyLevels = newSelection))
                        },
                        label = { Text(levelName) }
                    )
                }
            }
        }

        // Bloom levels
        Column {
            Text(
                text = "سطوح بلوم",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                BloomLevel.values().forEach { level ->
                    val levelName = when (level) {
                        BloomLevel.REMEMBER -> "یادآوری"
                        BloomLevel.UNDERSTAND -> "درک"
                        BloomLevel.APPLY -> "کاربرد"
                        BloomLevel.ANALYZE -> "تحلیل"
                        BloomLevel.EVALUATE -> "ارزیابی"
                        BloomLevel.CREATE -> "خلق"
                    }

                    FilterChip(
                        selected = formData.selectedBloomLevels.contains(level),
                        onClick = {
                            val newSelection = if (formData.selectedBloomLevels.contains(level)) {
                                formData.selectedBloomLevels - level
                            } else {
                                formData.selectedBloomLevels + level
                            }
                            onFormDataChange(formData.copy(selectedBloomLevels = newSelection))
                        },
                        label = { Text(levelName) }
                    )
                }
            }
        }

        // Settings cards
        Card(modifier = Modifier.fillMaxWidth()) {
            Column {
                // Random order
                ListItem(
                    headlineContent = { Text("ترتیب تصادفی سوالات") },
                    supportingContent = { Text("سوالات به صورت تصادفی چیده می‌شوند") },
                    leadingContent = {
                        Icon(Icons.Default.Shuffle, contentDescription = null)
                    },
                    trailingContent = {
                        Switch(
                            checked = formData.isRandom,
                            onCheckedChange = { onFormDataChange(formData.copy(isRandom = it)) }
                        )
                    }
                )

                Divider()

                // Show answers immediately
                ListItem(
                    headlineContent = { Text("نمایش پاسخ بلافاصله") },
                    supportingContent = { Text("پس از پاسخ به هر سوال، پاسخ صحیح نشان داده می‌شود") },
                    leadingContent = {
                        Icon(Icons.Default.Visibility, contentDescription = null)
                    },
                    trailingContent = {
                        Switch(
                            checked = formData.showAnswersImmediately,
                            onCheckedChange = { onFormDataChange(formData.copy(showAnswersImmediately = it)) }
                        )
                    }
                )

                Divider()

                // Allow retake
                ListItem(
                    headlineContent = { Text("اجازه تکرار آزمون") },
                    supportingContent = { Text("امکان تکرار آزمون پس از اتمام وجود دارد") },
                    leadingContent = {
                        Icon(Icons.Default.Repeat, contentDescription = null)
                    },
                    trailingContent = {
                        Switch(
                            checked = formData.allowRetake,
                            onCheckedChange = { onFormDataChange(formData.copy(allowRetake = it)) }
                        )
                    }
                )
            }
        }

        // Summary
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "خلاصه تنظیمات:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                val selectedTypesText = if (formData.selectedQuestionTypes.isEmpty()) {
                    "همه انواع"
                } else {
                    formData.selectedQuestionTypes.joinToString(", ") { type ->
                        when (type) {
                            QuestionType.MULTIPLE_CHOICE -> "چهارگزینه‌ای"
                            QuestionType.SHORT_ANSWER -> "کوتاه‌پاسخ"
                            QuestionType.DESCRIPTIVE -> "تشریحی"
                        }
                    }
                }

                val selectedDifficultyText = if (formData.selectedDifficultyLevels.isEmpty()) {
                    "همه سطوح"
                } else {
                    formData.selectedDifficultyLevels.joinToString(", ") { level ->
                        when (level) {
                            DifficultyLevel.EASY -> "آسان"
                            DifficultyLevel.MEDIUM -> "متوسط"
                            DifficultyLevel.HARD -> "سخت"
                        }
                    }
                }

                Text(
                    text = "• نوع سوالات: $selectedTypesText",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "• سطح سختی: $selectedDifficultyText",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "• ترتیب تصادفی: ${if (formData.isRandom) "بله" else "خیر"}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "• نمایش پاسخ: ${if (formData.showAnswersImmediately) "بلافاصله" else "پس از آزمون"}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "• تعداد سوالات: ${formData.totalQuestions}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "• مدت زمان: ${formData.examDuration} دقیقه",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun BookSelectionItem(
    book: Book,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "انتخاب شده",
                        tint = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        Icons.Default.MenuBook,
                        contentDescription = "کتاب",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold
                    else androidx.compose.ui.text.font.FontWeight.Normal
                )

                Text(
                    text = "${book.subject} - پایه ${book.grade}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ChapterSelectionItem(
    chapter: Chapter,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(32.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "انتخاب شده",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "فصل ${chapter.chapterNumber}: ${chapter.title}",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = "صفحات ${chapter.startPage} تا ${chapter.endPage}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ExamCreationBottomBar(
    currentStep: Int,
    totalSteps: Int,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onCreate: () -> Unit,
    isCreating: Boolean
) {
    Surface(
        tonalElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous button
            if (currentStep > 1) {
                TextButton(
                    onClick = onPrevious,
                    enabled = !isCreating
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "مرحله قبل",
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                    Text("قبلی")
                }
            } else {
                Spacer(modifier = Modifier.width(ButtonDefaults.MinWidth))
            }

            // Step indicator
            Text(
                text = "$currentStep/$totalSteps",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Next/Create button
            if (currentStep < totalSteps) {
                Button(
                    onClick = onNext,
                    enabled = !isCreating
                ) {
                    Text("بعدی")
                    Icon(
                        Icons.Default.ArrowForward,
                        contentDescription = "مرحله بعد",
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                }
            } else {
                Button(
                    onClick = onCreate,
                    enabled = !isCreating
                ) {
                    if (isCreating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("ایجاد آزمون")
                    }
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
            Text("در حال بارگذاری...")
        }
    }
}

@Composable
fun CreatingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text("در حال ایجاد آزمون...")
        }
    }
}

@Composable
fun SuccessState(
    exam: Exam,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "موفقیت",
            modifier = Modifier.size(100.dp),
            tint = Color(0xFF4CAF50)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "آزمون با موفقیت ایجاد شد!",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = exam.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = "کد آزمون: ${exam.id}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "تعداد سوالات: ${exam.totalQuestions}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "مدت زمان: ${exam.examDuration} دقیقه",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text("ادامه")
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
            modifier = Modifier.size(80.dp),
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

// ViewModel and State classes
class ExamCreationViewModel @javax.inject.Inject constructor(
    private val examRepository: com.examapp.data.repository.ExamRepository,
    private val bookRepository: com.examapp.data.repository.BookRepository
) : androidx.lifecycle.ViewModel() {

    private val _uiState = androidx.lifecycle.viewModelScope.MutableStateFlow<ExamCreationUiState>(ExamCreationUiState.Loading)
    val uiState: androidx.lifecycle.viewModelScope.StateFlow<ExamCreationUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.value = ExamCreationUiState.Loading
            try {
                val booksResult = bookRepository.getAllBooks()
                if (booksResult.isSuccess) {
                    val books = booksResult.getOrNull() ?: emptyList()
                    _uiState.value = ExamCreationUiState.Form(
                        currentStep = 1,
                        formData = ExamCreationFormData(),
                        booksState = BooksState.Success(books),
                        chaptersState = ChaptersState.Idle
                    )
                } else {
                    _uiState.value = ExamCreationUiState.Error("خطا در بارگذاری کتاب‌ها")
                }
            } catch (e: Exception) {
                _uiState.value = ExamCreationUiState.Error("خطا در اتصال: ${e.message}")
            }
        }
    }

    fun nextStep() {
        val currentState = _uiState.value
        if (currentState is ExamCreationUiState.Form && currentState.currentStep < 3) {
            _uiState.value = currentState.copy(currentStep = currentState.currentStep + 1)
        }
    }

    fun previousStep() {
        val currentState = _uiState.value
        if (currentState is ExamCreationUiState.Form && currentState.currentStep > 1) {
            _uiState.value = currentState.copy(currentStep = currentState.currentStep - 1)
        }
    }

    fun updateFormData(formData: ExamCreationFormData) {
        val currentState = _uiState.value
        if (currentState is ExamCreationUiState.Form) {
            _uiState.value = currentState.copy(formData = formData)
        }
    }

    fun createExam() {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState !is ExamCreationUiState.Form) return@launch

            _uiState.value = ExamCreationUiState.Creating

            try {
                val request = ExamRequest(
                    title = currentState.formData.title,
                    description = currentState.formData.description,
                    grade = currentState.formData.grade,
                    subject = currentState.formData.subject,
                    totalQuestions = currentState.formData.totalQuestions,
                    examDuration = currentState.formData.examDuration,
                    bookId = currentState.formData.selectedBook,
                    chapterIds = currentState.formData.selectedChapters,
                    pageRangeStart = currentState.formData.pageRangeStart,
                    pageRangeEnd = currentState.formData.pageRangeEnd,
                    questionTypes = currentState.formData.selectedQuestionTypes,
                    difficultyLevels = currentState.formData.selectedDifficultyLevels,
                    bloomLevels = currentState.formData.selectedBloomLevels,
                    isRandom = currentState.formData.isRandom,
                    showAnswersImmediately = currentState.formData.showAnswersImmediately,
                    allowRetake = currentState.formData.allowRetake
                )

                val result = examRepository.generateExam(request)
                if (result.isSuccess) {
                    val exam = result.getOrNull()
                    _uiState.value = ExamCreationUiState.Success(exam!!)
                } else {
                    _uiState.value = ExamCreationUiState.Error(
                        result.exceptionOrNull()?.message ?: "خطا در ایجاد آزمون"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = ExamCreationUiState.Error("خطا در اتصال: ${e.message}")
            }
        }
    }

    fun resetState() {
        loadInitialData()
    }
}

sealed class ExamCreationUiState {
    data object Loading : ExamCreationUiState()
    data object Creating : ExamCreationUiState()
    data class Form(
        val currentStep: Int,
        val formData: ExamCreationFormData,
        val booksState: BooksState,
        val chaptersState: ChaptersState
    ) : ExamCreationUiState()
    data class Success(val exam: Exam) : ExamCreationUiState()
    data class Error(val message: String) : ExamCreationUiState()
}

data class ExamCreationFormData(
    val title: String = "",
    val description: String = "",
    val grade: Int? = null,
    val subject: String? = null,
    val totalQuestions: Int = 20,
    val examDuration: Int = 45,
    val selectedBook: String? = null,
    val selectedChapters: List<String> = emptyList(),
    val pageRangeStart: Int? = null,
    val pageRangeEnd: Int? = null,
    val selectedQuestionTypes: List<QuestionType> = emptyList(),
    val selectedDifficultyLevels: List<DifficultyLevel> = emptyList(),
    val selectedBloomLevels: List<BloomLevel> = emptyList(),
    val isRandom: Boolean = true,
    val showAnswersImmediately: Boolean = false,
    val allowRetake: Boolean = true
)

// Enum classes (should be in data models)
enum class QuestionType {
    MULTIPLE_CHOICE,
    SHORT_ANSWER,
    DESCRIPTIVE
}

enum class DifficultyLevel {
    EASY,
    MEDIUM,
    HARD
}

enum class BloomLevel {
    REMEMBER,
    UNDERSTAND,
    APPLY,
    ANALYZE,
    EVALUATE,
    CREATE
}