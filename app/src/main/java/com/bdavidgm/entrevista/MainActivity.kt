package com.bdavidgm.entrevista

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.bdavidgm.entrevista.data.InterviewAnswersData
import com.bdavidgm.entrevista.data.InterviewRepository
import com.bdavidgm.entrevista.data.UserPreferences
import com.bdavidgm.entrevista.ui.navigation.AppNavigationDrawer
import com.bdavidgm.entrevista.ui.navigation.MainDestination
import com.bdavidgm.entrevista.ui.screens.QuestionDetailScreen
import com.bdavidgm.entrevista.ui.screens.QuestionListScreen
import com.bdavidgm.entrevista.ui.screens.SavedScreen
import com.bdavidgm.entrevista.ui.theme.EntrevistaTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

private sealed interface AppScreen {
    data object Saved : AppScreen
    data object List : AppScreen
    data class Detail(val questionId: Int) : AppScreen
}

@Composable
private fun EntrevistaApp() {
    val context = LocalContext.current
    val userPreferences = remember { UserPreferences(context.applicationContext) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val lastQuestionId by userPreferences.lastQuestionId.collectAsState()

    var screen by remember { mutableStateOf<AppScreen>(AppScreen.List) }
    var returnScreen by remember { mutableStateOf<AppScreen>(AppScreen.List) }

    fun openDrawer() {
        scope.launch { drawerState.open() }
    }

    fun navigateToQuestion(questionId: Int) {
        userPreferences.setLastQuestionId(questionId)
        screen = AppScreen.Detail(questionId)
    }

    fun openQuestion(questionId: Int, from: AppScreen) {
        returnScreen = from
        navigateToQuestion(questionId)
    }

    fun continueWhereLeft(from: AppScreen) {
        lastQuestionId?.let { openQuestion(it, from) }
    }

    fun selectMainDestination(destination: MainDestination) {
        screen = when (destination) {
            MainDestination.Saved -> AppScreen.Saved
            MainDestination.QuestionList -> AppScreen.List
        }
    }

    val mainDestination = when (screen) {
        AppScreen.Saved -> MainDestination.Saved
        AppScreen.List -> MainDestination.QuestionList
        is AppScreen.Detail -> when (returnScreen) {
            AppScreen.Saved -> MainDestination.Saved
            else -> MainDestination.QuestionList
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val current = screen) {
            is AppScreen.Detail -> {
                DetailRoute(
                    questionId = current.questionId,
                    userPreferences = userPreferences,
                    onNavigateToQuestion = ::navigateToQuestion,
                    onBack = { screen = returnScreen },
                    modifier = Modifier.fillMaxSize()
                )
            }
            AppScreen.List -> {
                MainDrawerShell(
                    drawerState = drawerState,
                    mainDestination = mainDestination,
                    canContinue = lastQuestionId != null,
                    scope = scope,
                    onContinueClick = { continueWhereLeft(AppScreen.List) },
                    onDestinationSelected = ::selectMainDestination
                ) {
                    QuestionListScreen(
                        onQuestionClick = { openQuestion(it, AppScreen.List) },
                        onMenuClick = ::openDrawer,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            AppScreen.Saved -> {
                BackHandler { screen = AppScreen.List }
                MainDrawerShell(
                    drawerState = drawerState,
                    mainDestination = mainDestination,
                    canContinue = lastQuestionId != null,
                    scope = scope,
                    onContinueClick = { continueWhereLeft(AppScreen.Saved) },
                    onDestinationSelected = ::selectMainDestination
                ) {
                    SavedRoute(
                        userPreferences = userPreferences,
                        onQuestionClick = { openQuestion(it, AppScreen.Saved) },
                        onMenuClick = ::openDrawer,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun MainDrawerShell(
    drawerState: DrawerState,
    mainDestination: MainDestination,
    canContinue: Boolean,
    scope: CoroutineScope,
    onContinueClick: () -> Unit,
    onDestinationSelected: (MainDestination) -> Unit,
    content: @Composable () -> Unit
) {
    AppNavigationDrawer(
        drawerState = drawerState,
        currentDestination = mainDestination,
        canContinue = canContinue,
        scope = scope,
        onContinueClick = onContinueClick,
        onDestinationSelected = onDestinationSelected,
        content = content
    )
}

@Composable
private fun SavedRoute(
    userPreferences: UserPreferences,
    onQuestionClick: (Int) -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val savedIds by userPreferences.savedQuestionIds.collectAsState()
    val savedSummaries = remember(savedIds) {
        savedIds.mapNotNull { InterviewRepository.summaryById(it) }.sortedBy { it.id }
    }

    SavedScreen(
        savedSummaries = savedSummaries,
        onQuestionClick = onQuestionClick,
        onMenuClick = onMenuClick,
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
