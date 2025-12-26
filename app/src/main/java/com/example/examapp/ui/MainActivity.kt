// app/src/main/java/com/examapp/ui/MainActivity.kt
package com.examapp.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.examapp.ui.auth.LoginScreen
import com.examapp.ui.auth.RegisterScreen
import com.examapp.ui.books.BookListScreen
import com.examapp.ui.exam.ExamCreationScreen
import com.examapp.ui.exam.ExamListScreen
import com.examapp.ui.exam.ExamResultScreen
import com.examapp.ui.exam.session.ExamSessionScreen
import com.examapp.ui.home.HomeScreen
import com.examapp.ui.profile.ProfileScreen
import com.examapp.ui.question.QuestionListScreen
import com.examapp.ui.theme.ExamAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val mainViewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ExamAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ExamApp(mainViewModel)
                }
            }
        }
    }
}

@Composable
fun ExamApp(mainViewModel: MainViewModel) {
    val uiState by mainViewModel.uiState.collectAsState()
    val userGreeting by mainViewModel.userGreeting.collectAsState()

    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        // صفحه اصلی
        composable("home") {
            HomeScreen(
                uiState = uiState,
                userGreeting = userGreeting,
                onLogout = { mainViewModel.logout() },
                onNavigateToBooks = { navController.navigate("books") },
                onNavigateToExams = { navController.navigate("exams") },
                onNavigateToProfile = { navController.navigate("profile") },
                onNavigateToLogin = { navController.navigate("login") },
                onRefresh = { mainViewModel.refreshData() }
            )
        }

        // احراز هویت
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate("register") },
                onNavigateBack = { navController.navigateUp() }
            )
        }

        composable("register") {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                onNavigateToLogin = { navController.navigate("login") },
                onNavigateBack = { navController.navigateUp() }
            )
        }

        // کتاب‌ها
        composable("books") {
            BookListScreen(
                onNavigateBack = { navController.navigateUp() },
                onBookSelected = { bookId ->
                    navController.navigate("book_detail/$bookId")
                }
            )
        }

        composable("book_detail/{bookId}") { backStackEntry ->
            val bookId = backStackEntry.arguments?.getString("bookId")?.toIntOrNull()
            bookId?.let {
                // Placeholder for BookDetailScreen
                // BookDetailScreen(
                //     bookId = it,
                //     onNavigateBack = { navController.navigateUp() },
                //     onStartExam = { examConfig ->
                //         navController.navigate("exam_session/${examConfig.examId}")
                //     }
                // )
            }
        }

        // آزمون‌ها
        composable("exams") {
            ExamListScreen(
                onNavigateBack = { navController.navigateUp() },
                onCreateExam = { navController.navigate("create_exam") },
                onViewResult = { resultId ->
                    navController.navigate("exam_result/$resultId")
                },
                onStartExam = { examId ->
                    navController.navigate("exam_session/$examId")
                }
            )
        }

        composable("create_exam") {
            ExamCreationScreen(
                onCreateExamSuccess = { examId ->
                    navController.navigate("exam_session/$examId")
                },
                onNavigateBack = { navController.navigateUp() }
            )
        }

        composable("exam_session/{examId}") { backStackEntry ->
            val examId = backStackEntry.arguments?.getString("examId")?.toIntOrNull()
            examId?.let {
                ExamSessionScreen(
                    examId = it,
                    onExamCompleted = { resultId ->
                        navController.navigate("exam_result/$resultId") {
                            popUpTo("exams") { inclusive = true }
                        }
                    },
                    onNavigateBack = { navController.navigateUp() }
                )
            }
        }

        composable("exam_result/{resultId}") { backStackEntry ->
            val resultId = backStackEntry.arguments?.getString("resultId")?.toIntOrNull()
            resultId?.let {
                ExamResultScreen(
                    resultId = it,
                    onNavigateBack = { navController.navigateUp() },
                    onRetakeExam = { examId ->
                        navController.navigate("exam_session/$examId")
                    },
                    onNewExam = {
                        navController.navigate("create_exam")
                    }
                )
            }
        }

        // سوالات
        composable("questions") {
            QuestionListScreen(
                onNavigateBack = { navController.navigateUp() },
                onQuestionSelected = { questionId ->
                    navController.navigate("question_detail/$questionId")
                }
            )
        }

        composable("question_detail/{questionId}") { backStackEntry ->
            val questionId = backStackEntry.arguments?.getString("questionId")?.toIntOrNull()
            questionId?.let {
                // Placeholder for QuestionDetailScreen
                // QuestionDetailScreen(
                //     questionId = it,
                //     onNavigateBack = { navController.navigateUp() }
                // )
            }
        }

        // پروفایل
        composable("profile") {
            ProfileScreen(
                onNavigateBack = { navController.navigateUp() },
                onLogout = {
                    mainViewModel.logout()
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ExamAppPreview() {
    ExamAppTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            // Preview content
        }
    }
}