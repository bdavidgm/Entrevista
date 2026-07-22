package com.bdavidgm.entrevista.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    @Test
    fun parseExplanationSegments_splitsCodeLinesFromProse() {
        val explanation =
            "`class UserFragment : Fragment()` hereda de `Fragment` con `companion object`."

        val segments = InterviewAnswerParser.parseExplanationSegments(explanation)

        assertEquals(2, segments.size)
        val code = segments[0] as ExplanationSegment.Code
        assertEquals("class UserFragment : Fragment()", code.code)
        val prose = segments[1] as ExplanationSegment.Prose
        assertTrue(prose.text.text.contains("hereda de"))
        assertTrue(prose.text.text.contains("Fragment"))
        assertTrue(prose.text.text.contains("companion object"))
    }

    @Test
    fun parseExplanationSegments_withoutBackticks_returnsSingleProse() {
        val explanation = "Texto sin snippets de código."
        val segments = InterviewAnswerParser.parseExplanationSegments(explanation)
        assertEquals(1, segments.size)
        assertEquals(explanation, (segments[0] as ExplanationSegment.Prose).text.text)
    }

    @Test
    fun isCodeLineSnippet_distinguishesLinesFromTokens() {
        assertTrue(InterviewAnswerParser.isCodeLineSnippet("fun newInstance(id: String)"))
        assertTrue(InterviewAnswerParser.isCodeLineSnippet("var count by remember { mutableStateOf(0) }"))
        assertFalse(InterviewAnswerParser.isCodeLineSnippet("Fragment"))
        assertFalse(InterviewAnswerParser.isCodeLineSnippet(":"))
        assertFalse(InterviewAnswerParser.isCodeLineSnippet("apply"))
    }
}
