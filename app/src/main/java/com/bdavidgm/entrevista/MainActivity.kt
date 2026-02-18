package com.bdavidgm.entrevista

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.bdavidgm.entrevista.data.InterviewData
import com.bdavidgm.entrevista.data.InterviewQuestion
import com.bdavidgm.entrevista.ui.screens.QuestionDetailScreen
import com.bdavidgm.entrevista.ui.screens.QuestionListScreen
import com.bdavidgm.entrevista.ui.theme.EntrevistaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EntrevistaTheme {
                var selectedQuestion by remember { mutableStateOf<InterviewQuestion?>(null) }
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    if (selectedQuestion == null) {
                        QuestionListScreen(
                            questions = InterviewData.allQuestions,
                            onQuestionClick = { selectedQuestion = it },
                            modifier = Modifier.padding(innerPadding)
                        )
                    } else {
                        QuestionDetailScreen(
                            question = selectedQuestion!!,
                            onBack = { selectedQuestion = null },
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AppPreview() {
    EntrevistaTheme {
        val first = remember { InterviewData.allQuestions.first() }
        QuestionListScreen(
            questions = InterviewData.allQuestions.take(5),
            onQuestionClick = {}
        )
    }
}
