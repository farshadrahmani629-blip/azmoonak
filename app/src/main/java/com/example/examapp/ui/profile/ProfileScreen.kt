// app/src/main/java/com/examapp/ui/profile/ProfileScreen.kt
package com.examapp.ui.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.examapp.R
import com.examapp.data.models.User
import com.examapp.data.models.UserRole
import com.examapp.data.models.SubscriptionType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val profileState by viewModel.profileState.collectAsState()
    val statsState by viewModel.statsState.collectAsState()
    val editProfileState by viewModel.editProfileState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshProfile()
    }

    var showEditDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("پروفایل") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "بازگشت"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "ویرایش پروفایل")
                    }
                }
            )
        }
    ) { paddingValues ->
        when (profileState) {
            is ProfileState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is ProfileState.Success -> {
                val user = (profileState as ProfileState.Success).user

                ProfileContent(
                    user = user,
                    statsState = statsState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState()),
                    onLogoutClick = { showLogoutDialog = true },
                    onRefresh = { viewModel.refreshProfile() }
                )
            }

            is ProfileState.Error -> {
                ErrorState(
                    message = (profileState as ProfileState.Error).message,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    onRetry = { viewModel.refreshProfile() }
                )
            }

            ProfileState.LoggedOut -> {
                // Should navigate to login
            }
        }

        // دیالوگ ویرایش پروفایل
        if (showEditDialog) {
            EditProfileDialog(
                editProfileState = editProfileState,
                onDismiss = { showEditDialog = false },
                onSave = { firstName, lastName, email, phone, grade, school ->
                    viewModel.updateProfile(
                        firstName = firstName,
                        lastName = lastName,
                        email = email,
                        phone = phone,
                        grade = grade,
                        school = school
                    )
                    showEditDialog = false
                },
                onFieldChange = viewModel::updateEditProfileField
            )
        }

        // دیالوگ خروج
        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = { Text("خروج از حساب کاربری") },
                text = { Text("آیا مطمئن هستید که می‌خواهید خارج شوید؟") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showLogoutDialog = false
                            viewModel.logout()
                            onLogout()
                        }
                    ) {
                        Text("خروج")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) {
                        Text("انصراف")
                    }
                }
            )
        }
    }
}

@Composable
fun ProfileContent(
    user: User,
    statsState: ProfileStatsState,
    modifier: Modifier = Modifier,
    onLogoutClick: () -> Unit,
    onRefresh: () -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // هدر پروفایل
        ProfileHeader(user = user)

        // آمار
        when (statsState) {
            is ProfileStatsState.Loading -> {
                CircularProgressIndicator()
            }

            is ProfileStatsState.Success -> {
                ProfileStatsSection(stats = statsState.stats)
            }

            is ProfileStatsState.Error -> {
                // Silently ignore stats error
            }
        }

        // اطلاعات کاربر
        UserInfoSection(user = user)

        // تنظیمات
        SettingsSection(onLogoutClick = onLogoutClick)

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun ProfileHeader(user: User) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // آواتار
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
            ) {
                if (user.avatarUrl != null) {
                    // Load from URL
                    Image(
                        painter = painterResource(id = R.drawable.ic_profile_placeholder),
                        contentDescription = "آواتار",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.ic_profile_placeholder),
                        contentDescription = "آواتار",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // نام کاربر
            Text(
                text = "${user.firstName} ${user.lastName}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            // نام کاربری
            Text(
                text = "@${user.username}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // نقش کاربر
            UserRoleChip(role = user.role)

            // اشتراک
            SubscriptionChip(isPro = user.subscriptionType == SubscriptionType.PRO)
        }
    }
}

@Composable
fun UserRoleChip(role: UserRole) {
    val (text, color) = when (role) {
        UserRole.STUDENT -> Pair("دانش‌آموز", MaterialTheme.colorScheme.primary)
        UserRole.TEACHER -> Pair("معلم", MaterialTheme.colorScheme.secondary)
        UserRole.PARENT -> Pair("والدین", MaterialTheme.colorScheme.tertiary)
        UserRole.ADMIN -> Pair("مدیر", MaterialTheme.colorScheme.error)
        else -> Pair("کاربر", MaterialTheme.colorScheme.primary)
    }

    AssistChip(
        onClick = {},
        label = { Text(text) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = color.copy(alpha = 0.2f),
            labelColor = color
        )
    )
}

@Composable
fun SubscriptionChip(isPro: Boolean) {
    val (text, color, icon) = if (isPro) {
        Triple("نسخه Pro", MaterialTheme.colorScheme.primary, Icons.Default.Star)
    } else {
        Triple("نسخه رایگان", MaterialTheme.colorScheme.onSurfaceVariant, Icons.Default.StarOutline)
    }

    AssistChip(
        onClick = { /* Navigate to upgrade */ },
        label = { Text(text) },
        leadingIcon = {
            Icon(icon, contentDescription = null, modifier = Modifier.size(AssistChipDefaults.IconSize))
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = color.copy(alpha = 0.2f),
            labelColor = color
        )
    )
}

@Composable
fun ProfileStatsSection(stats: ProfileStats) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "آمار عملکرد",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                title = "آزمون‌ها",
                value = stats.totalExams.toString(),
                icon = Icons.Default.Quiz,
                modifier = Modifier.weight(1f)
            )

            StatCard(
                title = "میانگین نمره",
                value = "${stats.averageScore}%",
                icon = Icons.Default.TrendingUp,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                title = "موفقیت",
                value = "${stats.successRate}%",
                icon = Icons.Default.CheckCircle,
                modifier = Modifier.weight(1f)
            )

            StatCard(
                title = "رتبه",
                value = "#${stats.rank}",
                icon = Icons.Default.Emblem,
                modifier = Modifier.weight(1f)
            )
        }

        // نقاط قوت و ضعف
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            InfoCard(
                title = "نقاط قوت",
                value = stats.favoriteSubject,
                icon = Icons.Default.ThumbUp,
                modifier = Modifier.weight(1f)
            )

            InfoCard(
                title = "نیاز به تمرین",
                value = stats.weakSubject,
                icon = Icons.Default.School,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun InfoCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun UserInfoSection(user: User) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "اطلاعات کاربر",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        InfoRow(
            icon = Icons.Default.Email,
            title = "ایمیل",
            value = user.email ?: "ثبت نشده"
        )

        InfoRow(
            icon = Icons.Default.Phone,
            title = "تلفن",
            value = user.phone ?: "ثبت نشده"
        )

        user.grade?.let { grade ->
            InfoRow(
                icon = Icons.Default.School,
                title = "پایه تحصیلی",
                value = "پایه $grade"
            )
        }

        user.school?.let { school ->
            InfoRow(
                icon = Icons.Default.LocationOn,
                title = "مدرسه",
                value = school
            )
        }
    }
}

@Composable
fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun SettingsSection(onLogoutClick: () -> Unit) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "تنظیمات",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Card {
            Column {
                // تنظیمات اعلان
                ListItem(
                    headlineContent = { Text("اعلان‌ها") },
                    leadingContent = {
                        Icon(Icons.Default.Notifications, contentDescription = null)
                    },
                    trailingContent = {
                        Switch(
                            checked = true,
                            onCheckedChange = {},
                            modifier = Modifier.scale(0.8f)
                        )
                    }
                )

                Divider()

                // حالت آفلاین
                ListItem(
                    headlineContent = { Text("حالت آفلاین") },
                    leadingContent = {
                        Icon(Icons.Default.OfflineBolt, contentDescription = null)
                    },
                    trailingContent = {
                        Switch(
                            checked = false,
                            onCheckedChange = {},
                            modifier = Modifier.scale(0.8f)
                        )
                    }
                )

                Divider()

                // خروج
                ListItem(
                    headlineContent = {
                        Text("خروج از حساب", color = MaterialTheme.colorScheme.error)
                    },
                    leadingContent = {
                        Icon(
                            Icons.Default.Logout,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    },
                    trailingContent = {},
                    modifier = Modifier.clickable { onLogoutClick() }
                )
            }
        }
    }
}

@Composable
fun EditProfileDialog(
    editProfileState: EditProfileState,
    onDismiss: () -> Unit,
    onSave: (String, String, String?, String?, Int?, String?) -> Unit,
    onFieldChange: (
        firstName: String,
        lastName: String,
        email: String?,
        phone: String?,
        grade: Int?,
        school: String?
    ) -> Unit
) {
    var firstName by remember { mutableStateOf(editProfileState.firstName) }
    var lastName by remember { mutableStateOf(editProfileState.lastName) }
    var email by remember { mutableStateOf(editProfileState.email ?: "") }
    var phone by remember { mutableStateOf(editProfileState.phone ?: "") }
    var grade by remember { mutableStateOf(editProfileState.grade?.toString() ?: "") }
    var school by remember { mutableStateOf(editProfileState.school ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ویرایش پروفایل") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = firstName,
                    onValueChange = {
                        firstName = it
                        onFieldChange(it, lastName, email, phone, grade.toIntOrNull(), school)
                    },
                    label = { Text("نام") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = lastName,
                    onValueChange = {
                        lastName = it
                        onFieldChange(firstName, it, email, phone, grade.toIntOrNull(), school)
                    },
                    label = { Text("نام خانوادگی") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        onFieldChange(firstName, lastName, it, phone, grade.toIntOrNull(), school)
                    },
                    label = { Text("ایمیل") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.ui.text.input.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Email
                    )
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = {
                        phone = it
                        onFieldChange(firstName, lastName, email, it, grade.toIntOrNull(), school)
                    },
                    label = { Text("تلفن") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.ui.text.input.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone
                    )
                )

                OutlinedTextField(
                    value = grade,
                    onValueChange = {
                        grade = it
                        onFieldChange(firstName, lastName, email, phone, it.toIntOrNull(), school)
                    },
                    label = { Text("پایه تحصیلی") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.ui.text.input.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    )
                )

                OutlinedTextField(
                    value = school,
                    onValueChange = {
                        school = it
                        onFieldChange(firstName, lastName, email, phone, grade.toIntOrNull(), it)
                    },
                    label = { Text("مدرسه") },
                    modifier = Modifier.fillMaxWidth()
                )

                // نمایش وضعیت
                if (editProfileState.isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                if (editProfileState.isError) {
                    Text(
                        text = editProfileState.errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (editProfileState.isSuccess) {
                    Text(
                        text = editProfileState.message,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        firstName,
                        lastName,
                        email.ifBlank { null },
                        phone.ifBlank { null },
                        grade.toIntOrNull(),
                        school.ifBlank { null }
                    )
                },
                enabled = !editProfileState.isLoading &&
                        firstName.isNotBlank() &&
                        lastName.isNotBlank()
            ) {
                Text("ذخیره")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف")
            }
        }
    )
}

@Composable
fun ErrorState(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: () -> Unit
) {
    Column(
        modifier = modifier,
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
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onRetry) {
            Text("تلاش مجدد")
        }
    }
}