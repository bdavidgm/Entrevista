package com.bdavidgm.entrevista.data

/** Vista ligera de una pregunta (sin respuesta) para listados y tarjetas. */
data class QuestionSummary(
    val id: Int,
    val category: Category,
    val question: String
)
