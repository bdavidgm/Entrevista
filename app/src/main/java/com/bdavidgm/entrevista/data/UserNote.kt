package com.bdavidgm.entrevista.data

/** Nota (o pregunta) escrita por el usuario. Se persiste en [UserPreferences]. */
data class UserNote(
    val id: Long,
    val text: String,
)
