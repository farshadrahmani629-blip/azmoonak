// app/src/main/java/com/examapp/ui/auth/LoginScreen.kt
package com.examapp.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.examapp.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    navController: NavController,
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val authState by viewModel.authState.collectAsState()
    val loginState by viewModel.loginState.collectAsState()

    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Success -> {
                onLoginSuccess()
                viewModel.resetLoginState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("ورود به حساب کاربری") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = "بازگشت"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // لوگو و عنوان
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_app_logo),
                    contentDescription = "لوگو برنامه",
                    modifier = Modifier.size(120.dp),
                    contentScale = ContentScale.Fit
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "آزمون‌ساز آنلاین",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "سیستم مدیریت آزمون‌های هوشمند",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // فرم ورود
            LoginForm(
                loginState = loginState,
                onUsernameChange = viewModel::updateLoginUsername,
                onPasswordChange = viewModel::updateLoginPassword,
                onLoginClick = { viewModel.login(loginState.username, loginState.password) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // دکمه ثبت‌نام
            TextButton(
                onClick = onNavigateToRegister,
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                Text("حساب کاربری ندارید؟ ثبت‌نام کنید")
            }

            // حالت‌های مختلف
            when (authState) {
                is AuthState.Loading -> {
                    LoadingIndicator()
                }

                is AuthState.Error -> {
                    ErrorMessage(
                        message = (authState as AuthState.Error).message,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }

                else -> {
                    // Nothing to show
                }
            }
        }
    }
}

@Composable
fun LoginForm(
    loginState: LoginState,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        // نام کاربری
        OutlinedTextField(
            value = loginState.username,
            onValueChange = onUsernameChange,
            label = { Text("نام کاربری یا شماره موبایل") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = loginState.isError && loginState.username.isNotBlank(),
            supportingText = {
                if (loginState.isError && loginState.username.isNotBlank()) {
                    Text(
                        text = loginState.errorMessage,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "نام کاربری"
                )
            }
        )

        // رمز عبور
        OutlinedTextField(
            value = loginState.password,
            onValueChange = onPasswordChange,
            label = { Text("رمز عبور") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None
            else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff
                        else Icons.Default.Visibility,
                        contentDescription = if (passwordVisible) "پنهان کردن رمز"
                        else "نمایش رمز"
                    )
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            isError = loginState.isError && loginState.password.isNotBlank(),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "رمز عبور"
                )
            }
        )

        // فراموشی رمز عبور
        TextButton(
            onClick = { /* TODO: Implement forgot password */ },
            modifier = Modifier.align(Alignment.Start)
        ) {
            Text("فراموشی رمز عبور؟")
        }

        // دکمه ورود
        Button(
            onClick = onLoginClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = !loginState.isLoading &&
                    loginState.username.isNotBlank() &&
                    loginState.password.isNotBlank(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            if (loginState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    "ورود",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        // پیام موفقیت
        if (loginState.isSuccess) {
            SuccessMessage(
                message = loginState.message,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun LoadingIndicator() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(16.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            strokeWidth = 2.dp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "در حال ورود...",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ErrorMessage(
    message: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = "خطا",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun SuccessMessage(
    message: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "موفقیت",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

// Extension for text fields
val TextFieldColors.errorColors: TextFieldColors
    @Composable
    get() = copy(
        unfocusedIndicatorColor = MaterialTheme.colorScheme.error,
        focusedIndicatorColor = MaterialTheme.colorScheme.error,
        unfocusedLabelColor = MaterialTheme.colorScheme.error,
        focusedLabelColor = MaterialTheme.colorScheme.error
    )