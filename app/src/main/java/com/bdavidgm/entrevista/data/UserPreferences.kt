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

    private val _notes = MutableStateFlow(readNotes())
    val notes: StateFlow<List<UserNote>> = _notes.asStateFlow()

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

    fun addNote(text: String) {
        val clean = text.trim().replace(RECORD_SEPARATOR, ' ').replace(FIELD_SEPARATOR, ' ')
        if (clean.isEmpty()) return
        val note = UserNote(id = System.currentTimeMillis(), text = clean)
        val updated = listOf(note) + _notes.value
        persistNotes(updated)
    }

    fun deleteNote(id: Long) {
        val updated = _notes.value.filterNot { it.id == id }
        persistNotes(updated)
    }

    private fun persistNotes(notes: List<UserNote>) {
        val serialized = notes.joinToString(RECORD_SEPARATOR.toString()) { "${it.id}$FIELD_SEPARATOR${it.text}" }
        prefs.edit().putString(KEY_NOTES, serialized).apply()
        _notes.value = notes
    }

    private fun readNotes(): List<UserNote> {
        val raw = prefs.getString(KEY_NOTES, null).orEmpty()
        if (raw.isEmpty()) return emptyList()
        return raw.split(RECORD_SEPARATOR)
            .mapNotNull { record ->
                val parts = record.split(FIELD_SEPARATOR, limit = 2)
                if (parts.size != 2) return@mapNotNull null
                val id = parts[0].toLongOrNull() ?: return@mapNotNull null
                UserNote(id = id, text = parts[1])
            }
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
        const val KEY_NOTES = "user_notes"
        const val RECORD_SEPARATOR = '\u001E'
        const val FIELD_SEPARATOR = '\u001F'
    }
}
