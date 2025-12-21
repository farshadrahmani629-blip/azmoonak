// app/src/main/java/com/examapp/MainActivity.kt
package com.examapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.examapp.ui.navigation.AzmoonakNavigation
import com.examapp.ui.theme.ExamAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ExamAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ExamApp()
                }
            }
        }
    }
}

@Composable
fun ExamApp() {
    ExamAppTheme {
        AzmoonakNavigation()
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    ExamAppTheme {
        ExamApp()
    }
}