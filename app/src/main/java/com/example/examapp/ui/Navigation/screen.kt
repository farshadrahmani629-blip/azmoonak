// app/src/main/java/com/examapp/ui/navigation/Screen.kt
package com.examapp.ui.navigation

import androidx.navigation.NavHostController

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Login : Screen("login")
    object Books : Screen("books")
    object Exams : Screen("exams")
    object Profile : Screen("profile")
    object ExamCreation : Screen("exam_creation")
    object ExamSession : Screen("exam_session")
    object ExamResult : Screen("exam_result")

    // Helper functions for navigation with arguments
    fun withArgs(vararg args: String): String {
        return buildString {
            append(route)
            args.forEach { arg ->
                append("/$arg")
            }
        }
    }

    fun withArgs(vararg args: Pair<String, String>): String {
        return buildString {
            append(route)
            if (args.isNotEmpty()) {
                append("?")
                args.forEachIndexed { index, (key, value) ->
                    if (index > 0) append("&")
                    append("$key=$value")
                }
            }
        }
    }
}

// Helper functions for navigation
fun navigateToExamSession(navController: NavHostController, examId: String) {
    navController.navigate(Screen.ExamSession.withArgs(examId))
}

fun navigateToExamResult(navController: NavHostController, examId: String) {
    navController.navigate(Screen.ExamResult.withArgs(examId))
}

fun navigateToExamCreation(navController: NavHostController) {
    navController.navigate(Screen.ExamCreation.route)
}

fun navigateBack(navController: NavHostController) {
    navController.popBackStack()
}