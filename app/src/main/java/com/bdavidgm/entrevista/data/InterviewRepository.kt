package com.bdavidgm.entrevista.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object InterviewRepository {

    const val MAX_QUESTION_ID = 275
    const val PAGE_SIZE = 30

    val categories: List<Category> = Category.entries

    val summaries: List<QuestionSummary>
        get() = InterviewSummariesData.summaries

    fun summaryById(id: Int): QuestionSummary? =
        summaries.find { it.id == id }

    fun getAnswer(id: Int): String? =
        InterviewAnswersData.getAnswer(id)

    fun questionById(id: Int): InterviewQuestion? =
        summaryById(id)?.let { summary ->
            getAnswer(summary.id)?.let { answer ->
                InterviewQuestion(summary.id, summary.category, summary.question, answer)
            }
        }

    fun previousQuestionId(currentId: Int): Int? =
        if (currentId > 1) currentId - 1 else null

    fun nextQuestionId(currentId: Int): Int? =
        if (currentId < MAX_QUESTION_ID) currentId + 1 else null

    suspend fun filterSummaries(
        query: String,
        category: Category?
    ): List<QuestionSummary> = withContext(Dispatchers.Default) {
        val base = if (category != null) {
            summaries.filter { it.category == category }
        } else {
            summaries
        }
        val lower = query.trim().lowercase()
        if (lower.isBlank()) return@withContext base

        val allowedIds = base.map { it.id }.toSet()
        val matchedIds = linkedSetOf<Int>()

        base.forEach { summary ->
            if (summary.question.lowercase().contains(lower) ||
                summary.category.displayName.lowercase().contains(lower)
            ) {
                matchedIds.add(summary.id)
            }
        }

        InterviewAnswersData.searchIdsMatching(lower).forEach { id ->
            if (id in allowedIds) matchedIds.add(id)
        }

        val byId = base.associateBy { it.id }
        matchedIds.sorted().mapNotNull { byId[it] }
    }
}
