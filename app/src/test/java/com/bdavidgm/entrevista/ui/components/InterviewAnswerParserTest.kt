package com.bdavidgm.entrevista.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InterviewAnswerParserTest {

    @Test
    fun parse_extractsCodeKotlinExplanationAndConsejo() {
        val raw = """
            Concepto:
            Texto previo.

            Ejemplo:
            fun hello() = "hola"

            ExplicaciónKotlin:
            fun declara una función.

            Consejo:
            Menciona tipos inferidos.
        """.trimIndent()

        val parsed = InterviewAnswerParser.parse(raw)

        assertEquals("Concepto:\nTexto previo.", parsed.proseBeforeCode)
        assertEquals("fun hello() = \"hola\"", parsed.code)
        assertEquals("fun declara una función.", parsed.kotlinExplanation)
        assertEquals("Consejo:\nMenciona tipos inferidos.", parsed.proseAfterCode)
    }

    @Test
    fun parse_supportsConsejoParaEntrevista() {
        val raw = """
            Concepto:
            Texto.

            Ejemplo:
            val x = 1

            Consejo para entrevista:
            Tip.
        """.trimIndent()

        val parsed = InterviewAnswerParser.parse(raw)

        assertEquals("val x = 1", parsed.code)
        assertNull(parsed.kotlinExplanation)
        assertEquals("Consejo para entrevista:\nTip.", parsed.proseAfterCode)
    }

    @Test
    fun parse_withoutEjemplo_returnsFullText() {
        val raw = "Solo texto sin ejemplo."
        val parsed = InterviewAnswerParser.parse(raw)
        assertEquals(raw, parsed.proseBeforeCode)
        assertNull(parsed.code)
    }
}
