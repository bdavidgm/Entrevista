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
    val notes by userPreferences.notes.collectAsState()
    var showNotes by remember { mutableStateOf(false) }
    val summary = remember(questionId) { InterviewRepository.summaryById(questionId) }
    var answer by remember(questionId) { mutableStateOf<String?>(null) }
    var isLoadingAnswer by remember(questionId) { mutableStateOf(true) }
    /** Pila de conceptos abiertos por enlace; el último es el diálogo visible. */
    var conceptStack by remember(questionId) { mutableStateOf<List<Int>>(emptyList()) }

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

    fun openConcept(linkedId: Int) {
        if (linkedId == questionId) return
        if (conceptStack.lastOrNull() == linkedId) return
        conceptStack = conceptStack + linkedId
    }

    fun popConcept() {
        if (conceptStack.isNotEmpty()) {
            conceptStack = conceptStack.dropLast(1)
        }
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
        onQuestionLinkClick = ::openConcept,
        onOpenNotes = { showNotes = true },
        modifier = modifier,
    )

    val topConceptId = conceptStack.lastOrNull()
    if (topConceptId != null) {
        ConceptQuestionDialog(
            questionId = topConceptId,
            onQuestionLinkClick = ::openConcept,
            onDismiss = ::popConcept,
            onOpenNotes = { showNotes = true },
        )
    }

    if (showNotes) {
        UserNotesDialog(
            notes = notes,
            onAddNote = userPreferences::addNote,
            onDeleteNote = userPreferences::deleteNote,
            onDismiss = { showNotes = false },
        )
    }
}
