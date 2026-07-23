package com.bdavidgm.entrevista.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bdavidgm.entrevista.R
import com.bdavidgm.entrevista.data.InterviewRepository
import com.bdavidgm.entrevista.ui.components.InterviewAnswerBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Diálogo con la ficha de un concepto enlazado.
 * Se usa desde [DetailRoute] con una pila: atrás / cerrar vuelve al diálogo o detalle anterior.
 */
@Composable
internal fun ConceptQuestionDialog(
    questionId: Int,
    onQuestionLinkClick: (Int) -> Unit,
    onDismiss: () -> Unit,
    onOpenNotes: (() -> Unit)? = null,
) {
    BackHandler(onBack = onDismiss)

    val summary = remember(questionId) { InterviewRepository.summaryById(questionId) }
    var answer by remember(questionId) { mutableStateOf<String?>(null) }
    var isLoading by remember(questionId) { mutableStateOf(true) }

    LaunchedEffect(questionId) {
        if (summary == null) {
            onDismiss()
            return@LaunchedEffect
        }
        isLoading = true
        answer = withContext(Dispatchers.IO) {
            InterviewRepository.getAnswer(questionId)
        }
        isLoading = false
    }

    if (summary == null) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "#${summary.id} · ${summary.question}",
                style = MaterialTheme.typography.titleMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                when {
                    isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        )
                    }
                    answer != null -> {
                        InterviewAnswerBody(
                            answer = answer!!,
                            onQuestionLinkClick = onQuestionLinkClick,
                            onOpenNotes = onOpenNotes,
                        )
                    }
                    else -> {
                        Text(
                            text = stringResource(R.string.concept_dialog_missing),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.concept_dialog_close))
            }
        },
    )
}
