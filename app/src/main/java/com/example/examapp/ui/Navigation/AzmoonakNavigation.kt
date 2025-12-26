// app/src/main/java/com/examapp/ui/navigation/AzmoonakNavigation.kt
package com.examapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.examapp.ui.auth.LoginScreen
import com.examapp.ui.auth.RegisterScreen
import com.examapp.ui.books.BookListScreen
import com.examapp.ui.exam.*
import com.examapp.ui.home.HomeScreen
import com.examapp.ui.main.MainActivity
import com.examapp.ui.main.MainScreen
import com.examapp.ui.profile.ProfileScreen
import com.examapp.ui.settings.SettingsScreen
import com.examapp.ui.splash.SplashScreen

/**
 * Navigation Component اصلی برنامه
 */
@Composable
fun AzmoonakNavigation(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Splash.route
) {
    // مدیریت back stack با دسترسی امن
    val actions = remember(navController) {
        NavigationActions(navController)
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // ------------ Splash Screen ------------
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToHome = actions.navigateToHome,
                onNavigateToLogin = actions.navigateToLogin
            )
        }

        // ------------ Auth Graph ------------
        navigation(
            startDestination = AuthScreen.Login.route,
            route = "auth"
        ) {
            composable(AuthScreen.Login.route) {
                LoginScreen(
                    onLoginSuccess = actions.navigateToHome,
                    onNavigateToRegister = actions.navigateToRegister,
                    onNavigateBack = actions.navigateUp,
                    viewModel = hiltViewModel()
                )
            }

            composable(AuthScreen.Register.route) {
                RegisterScreen(
                    onRegisterSuccess = actions.navigateToHome,
                    onNavigateToLogin = actions.navigateToLogin,
                    onNavigateBack = actions.navigateUp,
                    viewModel = hiltViewModel()
                )
            }
        }

        // ------------ Main Graph ------------
        navigation(
            startDestination = MainScreen.Home.route,
            route = "main"
        ) {
            composable(MainScreen.Home.route) {
                HomeScreen(
                    onNavigateToBooks = actions.navigateToBooks,
                    onNavigateToExams = actions.navigateToExams,
                    onNavigateToProfile = actions.navigateToProfile,
                    onNavigateToLogin = actions.navigateToLogin,
                    viewModel = hiltViewModel()
                )
            }

            composable(MainScreen.Books.route) {
                BookListScreen(
                    onNavigateBack = actions.navigateUp,
                    onBookSelected = { bookId ->
                        actions.navigateToBookDetail(bookId)
                    },
                    viewModel = hiltViewModel()
                )
            }

            composable(
                route = MainScreen.BookDetail.route + "/{bookId}",
                arguments = listOf(navArgument("bookId") { type = NavType.StringType })
            ) { backStackEntry ->
                val bookId = backStackEntry.arguments?.getString("bookId") ?: ""
                BookDetailScreen(
                    bookId = bookId,
                    onNavigateBack = actions.navigateUp,
                    onStartExam = { examConfig ->
                        actions.navigateToExamSession(examConfig)
                    }
                )
            }

            composable(MainScreen.Exams.route) {
                ExamListScreen(
                    onNavigateBack = actions.navigateUp,
                    onCreateExam = actions.navigateToExamCreation,
                    onViewResult = actions.navigateToExamResult,
                    onStartExam = actions.navigateToExamSessionWithId,
                    viewModel = hiltViewModel()
                )
            }

            composable(MainScreen.Profile.route) {
                ProfileScreen(
                    onNavigateBack = actions.navigateUp,
                    onNavigateToSettings = actions.navigateToSettings,
                    onLogout = actions.navigateToLogin,
                    viewModel = hiltViewModel()
                )
            }
        }

        // ------------ Exam Graph ------------
        navigation(
            startDestination = ExamScreen.ExamCreation.route,
            route = "exam"
        ) {
            composable(ExamScreen.ExamCreation.route) {
                ExamCreationScreen(
                    onCreateExamSuccess = { examId ->
                        actions.navigateToExamSession(examId)
                    },
                    onNavigateBack = actions.navigateUp,
                    viewModel = hiltViewModel()
                )
            }

            composable(
                route = ExamScreen.ExamSession.route + "/{examId}",
                arguments = listOf(
                    navArgument("examId") { type = NavType.StringType },
                    navArgument("isQuickExam") {
                        type = NavType.BoolType
                        defaultValue = false
                    }
                )
            ) { backStackEntry ->
                val examId = backStackEntry.arguments?.getString("examId") ?: ""
                val isQuickExam = backStackEntry.arguments?.getBoolean("isQuickExam") ?: false

                // Clear back stack for exam session
                LaunchedEffect(Unit) {
                    if (isQuickExam) {
                        navController.clearBackStack("exam")
                    }
                }

                ExamSessionScreen(
                    navController = navController,
                    examId = examId,
                    viewModel = hiltViewModel()
                )
            }

            composable(
                route = ExamScreen.ExamResult.route + "/{examId}",
                arguments = listOf(navArgument("examId") { type = NavType.StringType })
            ) { backStackEntry ->
                val examId = backStackEntry.arguments?.getString("examId") ?: ""

                ExamResultScreen(
                    navController = navController,
                    examId = examId,
                    viewModel = hiltViewModel()
                )
            }
        }

        // ------------ Settings Graph ------------
        composable(SettingsScreen.Settings.route) {
            SettingsScreen(
                onNavigateBack = actions.navigateUp,
                onNavigateToAbout = actions.navigateToAbout,
                viewModel = hiltViewModel()
            )
        }

        composable(SettingsScreen.About.route) {
            AboutScreen(
                onNavigateBack = actions.navigateUp
            )
        }

        // ------------ Legacy MainActivity ------------
        composable("legacy_main") {
            MainActivityContent(
                onNavigateToExam = actions.navigateToExamSessionWithConfig
            )
        }
    }
}

// ------------ Screens ------------

/**
 * صفحه‌های اصلی برنامه
 */
sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Auth : Screen("auth")
    object Main : Screen("main")
    object Exam : Screen("exam")
}

/**
 * صفحه‌های احراز هویت
 */
sealed class AuthScreen(val route: String) {
    object Login : AuthScreen("login")
    object Register : AuthScreen("register")
}

/**
 * صفحه‌های اصلی
 */
sealed class MainScreen(val route: String) {
    object Home : MainScreen("home")
    object Books : MainScreen("books")
    object BookDetail : MainScreen("book_detail")
    object Exams : MainScreen("exams")
    object Profile : MainScreen("profile")
}

/**
 * صفحه‌های آزمون
 */
sealed class ExamScreen(val route: String) {
    object ExamCreation : ExamScreen("exam_creation")
    object ExamSession : ExamScreen("exam_session")
    object ExamResult : ExamScreen("exam_result")
}

/**
 * صفحه‌های تنظیمات
 */
sealed class SettingsScreen(val route: String) {
    object Settings : SettingsScreen("settings")
    object About : SettingsScreen("about")
}

// ------------ Navigation Actions ------------

/**
 * کلاس مدیریت navigation actions
 */
class NavigationActions(private val navController: NavHostController) {

    // Basic Navigation
    val navigateUp: () -> Unit = { navController.navigateUp() }

    fun navigateTo(route: String) {
        navController.navigate(route) {
            launchSingleTop = true
        }
    }

    fun navigateTo(route: String, clearBackStack: Boolean = false) {
        navController.navigate(route) {
            launchSingleTop = true
            if (clearBackStack) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    // Splash
    val navigateToHome: () -> Unit = {
        navigateTo("main", clearBackStack = true)
    }

    val navigateToLogin: () -> Unit = {
        navigateTo("auth", clearBackStack = true)
    }

    // Auth
    val navigateToRegister: () -> Unit = {
        navigateTo(AuthScreen.Register.route)
    }

    // Main
    val navigateToBooks: () -> Unit = {
        navigateTo(MainScreen.Books.route)
    }

    val navigateToExams: () -> Unit = {
        navigateTo(MainScreen.Exams.route)
    }

    val navigateToProfile: () -> Unit = {
        navigateTo(MainScreen.Profile.route)
    }

    fun navigateToBookDetail(bookId: String) {
        navigateTo("${MainScreen.BookDetail.route}/$bookId")
    }

    // Exam
    val navigateToExamCreation: () -> Unit = {
        navigateTo(ExamScreen.ExamCreation.route)
    }

    fun navigateToExamSession(examId: String, isQuickExam: Boolean = false) {
        val route = "${ExamScreen.ExamSession.route}/$examId?isQuickExam=$isQuickExam"
        navigateTo(route, clearBackStack = isQuickExam)
    }

    fun navigateToExamSession(examConfig: Map<String, Any>) {
        // TODO: Implement exam config navigation
        val examId = System.currentTimeMillis().toString()
        navigateToExamSession(examId, isQuickExam = true)
    }

    val navigateToExamSessionWithId: (String) -> Unit = { examId ->
        navigateToExamSession(examId)
    }

    val navigateToExamSessionWithConfig: (Map<String, Any>) -> Unit = { examConfig ->
        navigateToExamSession(examConfig)
    }

    fun navigateToExamResult(examId: String) {
        navigateTo("${ExamScreen.ExamResult.route}/$examId")
    }

    // Settings
    val navigateToSettings: () -> Unit = {
        navigateTo(SettingsScreen.Settings.route)
    }

    val navigateToAbout: () -> Unit = {
        navigateTo(SettingsScreen.About.route)
    }
}

// ------------ Helper Extensions ------------

/**
 * پاک کردن back stack
 */
fun NavHostController.clearBackStack(route: String) {
    this.popBackStack(route, inclusive = true)
}

/**
 * گرفتن آرگومان‌ها از back stack entry
 */
fun NavBackStackEntry.getStringArg(key: String): String? {
    return this.arguments?.getString(key)
}

fun NavBackStackEntry.getIntArg(key: String): Int? {
    return this.arguments?.getInt(key)
}

fun NavBackStackEntry.getBooleanArg(key: String): Boolean {
    return this.arguments?.getBoolean(key) ?: false
}

/**
 * Extension برای ساخت route با آرگومان‌ها
 */
fun String.withArgs(vararg args: Pair<String, Any>): String {
    var result = this
    args.forEach { (key, value) ->
        result = if (result.contains("?")) {
            "$result&$key=$value"
        } else {
            "$result?$key=$value"
        }
    }
    return result
}

// ------------ Placeholder Screens ------------

@Composable
fun BookDetailScreen(
    bookId: String,
    onNavigateBack: () -> Unit,
    onStartExam: (Map<String, Any>) -> Unit
) {
    // TODO: Implement BookDetailScreen
    // This is a placeholder
    androidx.compose.material3.Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { androidx.compose.material3.Text("جزئیات کتاب") },
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = onNavigateBack) {
                        androidx.compose.material3.Icon(
                            androidx.compose.material.icons.Icons.Default.ArrowBack,
                            contentDescription = "بازگشت"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        androidx.compose.foundation.layout.Column(
            modifier = androidx.compose.ui.Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            androidx.compose.material3.Text("جزئیات کتاب ID: $bookId")
            androidx.compose.material3.Button(
                onClick = {
                    onStartExam(mapOf(
                        "bookId" to bookId,
                        "title" to "آزمون از کتاب $bookId"
                    ))
                }
            ) {
                androidx.compose.material3.Text("شروع آزمون از این کتاب")
            }
        }
    }
}

@Composable
fun AboutScreen(onNavigateBack: () -> Unit) {
    androidx.compose.material3.Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { androidx.compose.material3.Text("درباره برنامه") },
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = onNavigateBack) {
                        androidx.compose.material3.Icon(
                            androidx.compose.material.icons.Icons.Default.ArrowBack,
                            contentDescription = "بازگشت"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        androidx.compose.foundation.layout.Column(
            modifier = androidx.compose.ui.Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
        ) {
            androidx.compose.material3.Text(
                text = "آزمون‌ساز آنلاین",
                style = androidx.compose.material3.MaterialTheme.typography.headlineMedium
            )
            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(16.dp))
            androidx.compose.material3.Text(
                text = "نسخه ۱.۰.۰",
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
            )
            androidx.compose.material3.Text(
                text = "© ۲۰۲۴ تمامی حقوق محفوظ است",
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun MainActivityContent(onNavigateToExam: (Map<String, Any>) -> Unit) {
    // Placeholder for legacy MainActivity
    androidx.compose.material3.Button(
        onClick = {
            onNavigateToExam(mapOf(
                "title" to "آزمون سریع",
                "subject" to "ریاضی",
                "grade" to 6
            ))
        }
    ) {
        androidx.compose.material3.Text("شروع آزمون سریع")
    }
}

// برای جلوگیری از import مستقیم
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.dp