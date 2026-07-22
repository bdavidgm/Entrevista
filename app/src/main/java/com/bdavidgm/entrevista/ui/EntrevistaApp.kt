package com.bdavidgm.entrevista.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.bdavidgm.entrevista.data.UserPreferences
import com.bdavidgm.entrevista.ui.navigation.AppNavigationDrawer
import com.bdavidgm.entrevista.ui.navigation.AppScreen
import com.bdavidgm.entrevista.ui.navigation.MainDestination
import com.bdavidgm.entrevista.ui.screens.DetailRoute
import com.bdavidgm.entrevista.ui.screens.QuestionListScreen
import com.bdavidgm.entrevista.ui.screens.SavedRoute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Raíz de la UI; se usa desde [com.bdavidgm.entrevista.MainActivity]. */
@Composable
fun EntrevistaApp() {
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

    fun showQuestionDetail(questionId: Int) {
        screen = AppScreen.Detail(questionId)
    }

    fun openQuestion(questionId: Int, from: AppScreen, updateContinuePosition: Boolean) {
        returnScreen = from
        if (updateContinuePosition) {
            userPreferences.setLastQuestionId(questionId)
        }
        showQuestionDetail(questionId)
    }

    fun navigateAdjacentQuestion(questionId: Int) {
        userPreferences.setLastQuestionId(questionId)
        showQuestionDetail(questionId)
    }

    fun continueWhereLeft(from: AppScreen) {
        lastQuestionId?.let { openQuestion(it, from, updateContinuePosition = false) }
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
                    onNavigateToQuestion = ::navigateAdjacentQuestion,
                    onBack = { screen = returnScreen },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            AppScreen.List -> {
                MainDrawerShell(
                    drawerState = drawerState,
                    mainDestination = mainDestination,
                    canContinue = lastQuestionId != null,
                    scope = scope,
                    onContinueClick = { continueWhereLeft(AppScreen.List) },
                    onDestinationSelected = ::selectMainDestination,
                ) {
                    QuestionListScreen(
                        onQuestionClick = {
                            openQuestion(it, AppScreen.List, updateContinuePosition = true)
                        },
                        onMenuClick = ::openDrawer,
                        modifier = Modifier.fillMaxSize(),
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
                    onDestinationSelected = ::selectMainDestination,
                ) {
                    SavedRoute(
                        userPreferences = userPreferences,
                        onQuestionClick = {
                            openQuestion(it, AppScreen.Saved, updateContinuePosition = false)
                        },
                        onMenuClick = ::openDrawer,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

/** Envuelve lista y guardados con el drawer; se usa en [EntrevistaApp]. */
@Composable
private fun MainDrawerShell(
    drawerState: DrawerState,
    mainDestination: MainDestination,
    canContinue: Boolean,
    scope: CoroutineScope,
    onContinueClick: () -> Unit,
    onDestinationSelected: (MainDestination) -> Unit,
    content: @Composable () -> Unit,
) {
    AppNavigationDrawer(
        drawerState = drawerState,
        currentDestination = mainDestination,
        canContinue = canContinue,
        scope = scope,
        onContinueClick = onContinueClick,
        onDestinationSelected = onDestinationSelected,
        content = content,
    )
}
