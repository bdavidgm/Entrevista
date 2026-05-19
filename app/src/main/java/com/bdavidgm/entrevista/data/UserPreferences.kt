package com.bdavidgm.entrevista.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserPreferences(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _lastQuestionId = MutableStateFlow(readLastQuestionId())
    val lastQuestionId: StateFlow<Int?> = _lastQuestionId.asStateFlow()

    private val _savedQuestionIds = MutableStateFlow(readSavedQuestionIds())
    val savedQuestionIds: StateFlow<Set<Int>> = _savedQuestionIds.asStateFlow()

    fun setLastQuestionId(id: Int) {
        prefs.edit().putInt(KEY_LAST_QUESTION_ID, id).apply()
        _lastQuestionId.value = id
    }

    fun toggleSaved(id: Int) {
        val updated = _savedQuestionIds.value.toMutableSet()
        if (!updated.add(id)) {
            updated.remove(id)
        }
        prefs.edit()
            .putStringSet(KEY_SAVED_QUESTION_IDS, updated.map { it.toString() }.toSet())
            .apply()
        _savedQuestionIds.value = updated
    }

    private fun readLastQuestionId(): Int? {
        val id = prefs.getInt(KEY_LAST_QUESTION_ID, -1)
        return id.takeIf { it >= 0 }
    }

    private fun readSavedQuestionIds(): Set<Int> =
        prefs.getStringSet(KEY_SAVED_QUESTION_IDS, emptySet())
            ?.mapNotNull { it.toIntOrNull() }
            ?.toSet()
            ?: emptySet()

    private companion object {
        const val PREFS_NAME = "user_preferences"
        const val KEY_LAST_QUESTION_ID = "last_question_id"
        const val KEY_SAVED_QUESTION_IDS = "saved_question_ids"
    }
}
