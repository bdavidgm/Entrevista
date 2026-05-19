package com.bdavidgm.entrevista.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bdavidgm.entrevista.R
import com.bdavidgm.entrevista.data.QuestionSummary
import com.bdavidgm.entrevista.ui.components.QuestionCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    lastQuestion: QuestionSummary?,
    savedSummaries: List<QuestionSummary>,
    onQuestionClick: (Int) -> Unit,
    onBrowseAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.home_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.continue_where_left),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            item {
                if (lastQuestion != null) {
                    QuestionCard(
                        summary = lastQuestion,
                        onClick = { onQuestionClick(lastQuestion.id) }
                    )
                } else {
                    Text(
                        text = stringResource(R.string.no_last_question),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
            item {
                Text(
                    text = stringResource(R.string.saved_for_later),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            if (savedSummaries.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.no_saved_questions),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            } else {
                items(savedSummaries, key = { it.id }) { summary ->
                    QuestionCard(
                        summary = summary,
                        onClick = { onQuestionClick(summary.id) }
                    )
                }
            }
            item {
                Button(
                    onClick = onBrowseAll,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Icon(
                        Icons.Default.List,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(stringResource(R.string.browse_all_questions))
                }
            }
        }
    }
}
