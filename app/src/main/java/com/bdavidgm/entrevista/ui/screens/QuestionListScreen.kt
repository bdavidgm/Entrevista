package com.bdavidgm.entrevista.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.compose.collectAsLazyPagingItems
import com.bdavidgm.entrevista.data.Category
import com.bdavidgm.entrevista.data.InterviewRepository
import com.bdavidgm.entrevista.data.SummaryPagingSource
import com.bdavidgm.entrevista.ui.components.QuestionCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionListScreen(
    onQuestionClick: (Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var filteredSummaries by remember {
        mutableStateOf(InterviewRepository.summaries)
    }
    var isFiltering by remember { mutableStateOf(false) }

    LaunchedEffect(searchQuery, selectedCategory) {
        isFiltering = true
        filteredSummaries = InterviewRepository.filterSummaries(searchQuery, selectedCategory)
        isFiltering = false
    }

    val pagingFlow = remember(filteredSummaries) {
        Pager(
            config = PagingConfig(
                pageSize = InterviewRepository.PAGE_SIZE,
                enablePlaceholders = false,
                initialLoadSize = InterviewRepository.PAGE_SIZE
            ),
            pagingSourceFactory = { SummaryPagingSource(filteredSummaries) }
        ).flow
    }
    val lazySummaries = pagingFlow.collectAsLazyPagingItems()

    LaunchedEffect(filteredSummaries) {
        lazySummaries.refresh()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Todas las preguntas") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Buscar preguntas o respuestas...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )
            LazyColumn(
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    CategoryChips(
                        selectedCategory = selectedCategory,
                        onCategorySelected = {
                            selectedCategory = if (selectedCategory == it) null else it
                        }
                    )
                }
                item {
                    val countText = when {
                        isFiltering -> "Buscando..."
                        else -> "${filteredSummaries.size} preguntas"
                    }
                    Text(
                        text = countText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )
                }
                items(
                    count = lazySummaries.itemCount,
                    key = { index -> lazySummaries.peek(index)?.id ?: index }
                ) { index ->
                    lazySummaries[index]?.let { summary ->
                        QuestionCard(
                            summary = summary,
                            onClick = { onQuestionClick(summary.id) }
                        )
                    }
                }
                if (lazySummaries.loadState.append is LoadState.Loading) {
                    item {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryChips(
    selectedCategory: Category?,
    onCategorySelected: (Category) -> Unit,
    modifier: Modifier = Modifier
) {
    val categories = remember { InterviewRepository.categories }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEach { category ->
            val selected = selectedCategory == category
            Card(
                onClick = { onCategorySelected(category) },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (selected)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ),
                modifier = Modifier
            ) {
                Text(
                    text = category.displayName,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }
    }
}
