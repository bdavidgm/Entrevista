package com.bdavidgm.entrevista

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.bdavidgm.entrevista.data.InterviewAnswersData
import com.bdavidgm.entrevista.ui.EntrevistaApp
import com.bdavidgm.entrevista.ui.theme.EntrevistaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        InterviewAnswersData.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            EntrevistaTheme {
                EntrevistaApp()
            }
        }
    }
}
