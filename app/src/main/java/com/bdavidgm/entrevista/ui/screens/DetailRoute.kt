package com.bdavidgm.entrevista.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.bdavidgm.entrevista.data.InterviewRepository
import com.bdavidgm.entrevista.data.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Carga la respuesta y muestra el detalle; se usa en [com.bdavidgm.entrevista.ui.EntrevistaApp]. */
@Composable
internal fun DetailRoute(
    questionId: Int,
    userPreferences: UserPreferences,
    onNavigateToQuestion: (Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
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
        onQuestionLinkClick = onNavigateToQuestion,
        modifier = modifier,
    )
}
