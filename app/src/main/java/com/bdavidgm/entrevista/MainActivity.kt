package com.bdavidgm.entrevista

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.bdavidgm.entrevista.data.InterviewRepository
import com.bdavidgm.entrevista.data.UserPreferences
import com.bdavidgm.entrevista.ui.screens.HomeScreen
import com.bdavidgm.entrevista.ui.screens.QuestionDetailScreen
import com.bdavidgm.entrevista.ui.screens.QuestionListScreen
import com.bdavidgm.entrevista.ui.theme.EntrevistaTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EntrevistaTheme {
                EntrevistaApp()
            }
        }
    }
}

private sealed interface AppScreen {
    data object Home : AppScreen
    data object List : AppScreen
    data class Detail(val questionId: Int) : AppScreen
}

@Composable
private fun EntrevistaApp() {
    val context = LocalContext.current
    val userPreferences = remember { UserPreferences(context.applicationContext) }

    var screen by remember { mutableStateOf<AppScreen>(AppScreen.Home) }
    var returnScreen by remember { mutableStateOf<AppScreen>(AppScreen.Home) }

    fun navigateToQuestion(questionId: Int) {
        userPreferences.setLastQuestionId(questionId)
        screen = AppScreen.Detail(questionId)
    }

    fun openQuestion(questionId: Int, from: AppScreen) {
        returnScreen = from
        navigateToQuestion(questionId)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val current = screen) {
            AppScreen.Home -> {
                HomeRoute(
                    userPreferences = userPreferences,
                    onQuestionClick = { openQuestion(it, AppScreen.Home) },
                    onBrowseAll = { screen = AppScreen.List },
                    modifier = Modifier.fillMaxSize()
                )
            }
            AppScreen.List -> {
                QuestionListScreen(
                    onQuestionClick = { openQuestion(it, AppScreen.List) },
                    onBack = { screen = AppScreen.Home },
                    modifier = Modifier.fillMaxSize()
                )
            }
            is AppScreen.Detail -> {
                DetailRoute(
                    questionId = current.questionId,
                    userPreferences = userPreferences,
                    onNavigateToQuestion = ::navigateToQuestion,
                    onBack = { screen = returnScreen },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun HomeRoute(
    userPreferences: UserPreferences,
    onQuestionClick: (Int) -> Unit,
    onBrowseAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lastQuestionId by userPreferences.lastQuestionId.collectAsState()
    val savedIds by userPreferences.savedQuestionIds.collectAsState()

    val lastQuestion = lastQuestionId?.let { InterviewRepository.summaryById(it) }
    val savedSummaries = remember(savedIds) {
        savedIds.mapNotNull { InterviewRepository.summaryById(it) }.sortedBy { it.id }
    }

    HomeScreen(
        lastQuestion = lastQuestion,
        savedSummaries = savedSummaries,
        onQuestionClick = onQuestionClick,
        onBrowseAll = onBrowseAll,
        modifier = modifier
    )
}

@Composable
private fun DetailRoute(
    questionId: Int,
    userPreferences: UserPreferences,
    onNavigateToQuestion: (Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val savedIds by userPreferences.savedQuestionIds.collectAsState()
    val summary = remember(questionId) { InterviewRepository.summaryById(questionId) }
    var answer by remember(questionId) { mutableStateOf<String?>(null) }
    var isLoadingAnswer by remember(questionId) { mutableStateOf(true) }

    if (summary == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    LaunchedEffect(questionId) {
        userPreferences.setLastQuestionId(questionId)
        isLoadingAnswer = true
        answer = withContext(Dispatchers.IO) {
            InterviewRepository.getAnswer(questionId)
        }
        isLoadingAnswer = false
    }

    QuestionDetailScreen(
        summary = summary,
        answer = answer,
        isLoadingAnswer = isLoadingAnswer,
        isSaved = questionId in savedIds,
        hasPrevious = InterviewRepository.previousQuestionId(questionId) != null,
        hasNext = InterviewRepository.nextQuestionId(questionId) != null,
        onPrevious = { InterviewRepository.previousQuestionId(questionId)?.let(onNavigateToQuestion) },
        onViewLater = { userPreferences.toggleSaved(questionId) },
        onNext = { InterviewRepository.nextQuestionId(questionId)?.let(onNavigateToQuestion) },
        onBack = onBack,
        modifier = modifier
    )
}
