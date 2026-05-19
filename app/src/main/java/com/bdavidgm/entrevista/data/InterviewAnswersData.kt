package com.bdavidgm.entrevista.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal object InterviewAnswersData {

    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun getAnswer(id: Int): String? = runCatching {
        requireInitialized()
        appContext.assets.open("answers/$id.txt").bufferedReader().use { it.readText() }
    }.getOrNull()

    suspend fun searchIdsMatching(queryLower: String): List<Int> = withContext(Dispatchers.IO) {
        if (queryLower.isBlank()) return@withContext emptyList()
        requireInitialized()
        (1..InterviewRepository.MAX_QUESTION_ID).filter { id ->
            getAnswer(id)?.lowercase()?.contains(queryLower) == true
        }
    }

    private fun requireInitialized() {
        check(::appContext.isInitialized) {
            "InterviewAnswersData.init(context) must be called before use"
        }
    }
}
