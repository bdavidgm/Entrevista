package com.bdavidgm.entrevista.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.bdavidgm.entrevista.data.InterviewRepository
import com.bdavidgm.entrevista.data.UserPreferences

/** Prepara las preguntas guardadas y muestra la pantalla; se usa en [com.bdavidgm.entrevista.ui.EntrevistaApp]. */
@Composable
internal fun SavedRoute(
    userPreferences: UserPreferences,
    onQuestionClick: (Int) -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val savedIds by userPreferences.savedQuestionIds.collectAsState()
    val savedSummaries = remember(savedIds) {
        savedIds.mapNotNull { InterviewRepository.summaryById(it) }.sortedBy { it.id }
    }

    SavedScreen(
        savedSummaries = savedSummaries,
        onQuestionClick = onQuestionClick,
        onMenuClick = onMenuClick,
        modifier = modifier,
    )
}
