package com.bdavidgm.entrevista.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.bdavidgm.entrevista.R
import com.bdavidgm.entrevista.ui.util.copyTextToClipboard

internal data class ParsedInterviewAnswer(
    val proseBeforeCode: String,
    val code: String?,
    val kotlinExplanation: String?,
    val proseAfterCode: String?,
)

internal sealed interface ExplanationSegment {
    data class Prose(val text: AnnotatedString) : ExplanationSegment
    data class Code(val code: String) : ExplanationSegment
}

internal object InterviewAnswerParser {

    private val ejemploLine = Regex("^Ejemplo:\\s*$", RegexOption.MULTILINE)
    private val kotlinExplanationLine =
        Regex("^ExplicaciónKotlin:\\s*$", RegexOption.MULTILINE)
    private val consejoLine =
        Regex("^Consejo(?:\\s+para entrevista)?:\\s*$", RegexOption.MULTILINE)

    fun parse(raw: String): ParsedInterviewAnswer {
        val text = raw.replace("\r\n", "\n")
        val ejemploMatch = ejemploLine.find(text)
            ?: return ParsedInterviewAnswer(text.trimEnd(), null, null, null)

        val proseBefore = text.substring(0, ejemploMatch.range.first).trimEnd()
        val afterEjemplo = text.substring(ejemploMatch.range.last + 1)

        val kotlinMatch = kotlinExplanationLine.find(afterEjemplo)
        val consejoMatch = consejoLine.find(afterEjemplo)

        val codeEnd = when {
            kotlinMatch != null -> kotlinMatch.range.first
            consejoMatch != null -> consejoMatch.range.first
            else -> afterEjemplo.length
        }
        val code = trimBlankMargins(afterEjemplo.substring(0, codeEnd))
        if (code.isBlank()) {
            return ParsedInterviewAnswer(text.trimEnd(), null, null, null)
        }

        val kotlinExplanation = if (kotlinMatch != null) {
            val start = kotlinMatch.range.last + 1
            val end = consejoMatch?.range?.first ?: afterEjemplo.length
            trimBlankMargins(afterEjemplo.substring(start, end)).takeUnless { it.isBlank() }
        } else {
            null
        }

        val proseAfter = if (consejoMatch != null) {
            afterEjemplo.substring(consejoMatch.range.first).trimStart()
                .takeUnless { it.isBlank() }
        } else {
            null
        }

        return ParsedInterviewAnswer(
            proseBeforeCode = proseBefore,
            code = code,
            kotlinExplanation = kotlinExplanation,
            proseAfterCode = proseAfter,
        )
    }

    /**
     * Splits an ExplicaciónKotlin body into prose and code segments.
     * Backtick-wrapped snippets that look like code lines become [ExplanationSegment.Code];
     * short identifiers stay as monospace spans inside [ExplanationSegment.Prose].
     */
    fun parseExplanationSegments(explanation: String): List<ExplanationSegment> {
        if (explanation.isEmpty()) return emptyList()
        if ('`' !in explanation) {
            return listOf(ExplanationSegment.Prose(AnnotatedString(explanation)))
        }

        val parts = explanation.split('`')
        val segments = mutableListOf<ExplanationSegment>()
        val proseChunks = mutableListOf<Pair<String, Boolean>>() // text to isMonospace

        fun flushProse() {
            if (proseChunks.isEmpty()) return
            val annotated = buildAnnotatedString {
                for ((chunk, mono) in proseChunks) {
                    if (mono) {
                        withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) {
                            append(chunk)
                        }
                    } else {
                        append(chunk)
                    }
                }
            }
            if (annotated.text.isNotBlank()) {
                segments.add(ExplanationSegment.Prose(annotated))
            }
            proseChunks.clear()
        }

        parts.forEachIndexed { index, part ->
            if (index % 2 == 0) {
                val prose = if (segments.isNotEmpty() && proseChunks.isEmpty()) {
                    part.trimStart()
                } else {
                    part
                }
                if (prose.isNotEmpty()) proseChunks.add(prose to false)
            } else if (isCodeLineSnippet(part)) {
                if (proseChunks.isNotEmpty()) {
                    val (last, mono) = proseChunks.last()
                    val trimmed = last.trimEnd()
                    if (trimmed.isEmpty()) {
                        proseChunks.removeAt(proseChunks.lastIndex)
                    } else {
                        proseChunks[proseChunks.lastIndex] = trimmed to mono
                    }
                }
                flushProse()
                segments.add(ExplanationSegment.Code(part))
            } else if (part.isNotEmpty()) {
                proseChunks.add(part to true)
            }
        }
        flushProse()
        return segments
    }

    /** Substantial snippets render as code blocks; short tokens stay inline. */
    internal fun isCodeLineSnippet(snippet: String): Boolean {
        val t = snippet.trim()
        if (t.length <= 2) return false
        if (t.any { it in "(){}=<>[]" }) return true
        if ('.' in t && t.length >= 5) return true
        if ("::" in t) return true
        if (' ' in t && t.length >= 12) {
            val hasCodePunctuation = t.any { ch ->
                ch == ':' || ch == '@' || ch == '"' || ch == '\'' || ch == '/' ||
                    ch == ',' || ch == ';' || ch == '*' || ch == '+' || ch == '-' ||
                    ch == '!' || ch == '?' || ch == '&' || ch == '|' || ch == '%'
            }
            if (hasCodePunctuation) return true
            val firstWord = t.substringBefore(' ')
            return firstWord in declarationKeywords
        }
        return false
    }

    private val declarationKeywords = setOf(
        "val", "var", "fun", "class", "object", "interface", "typealias",
        "private", "public", "internal", "protected", "override", "suspend",
        "data", "sealed", "enum", "open", "abstract", "annotation",
        "const", "lateinit", "inline", "crossinline", "noinline",
        "return", "throw", "import", "package",
    )

    private fun trimBlankMargins(s: String): String {
        val lines = s.lines()
        val first = lines.indexOfFirst { it.isNotBlank() }
        val last = lines.indexOfLast { it.isNotBlank() }
        if (first == -1 || last == -1) return ""
        return lines.subList(first, last + 1).joinToString("\n")
    }
}

@Composable
internal fun InterviewAnswerBody(
    answer: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val parsed = remember(answer) { InterviewAnswerParser.parse(answer) }
    val bodyStyle = MaterialTheme.typography.bodyLarge
    val bodyColor = MaterialTheme.colorScheme.onSurface
    val codeStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
    var showKotlinExplanation by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        if (parsed.code == null) {
            CopyableText(
                text = answer,
                style = bodyStyle,
                color = bodyColor,
                clipboardLabel = "answer",
                toastMessageRes = R.string.answer_copied_to_clipboard,
            )
        } else {
            if (parsed.proseBeforeCode.isNotBlank()) {
                CopyableText(
                    text = parsed.proseBeforeCode,
                    style = bodyStyle,
                    color = bodyColor,
                    clipboardLabel = "answer",
                    toastMessageRes = R.string.answer_copied_to_clipboard,
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            AnswerCodeBlock(
                code = parsed.code,
                textStyle = codeStyle,
                onCopyClick = {
                    context.copyTextToClipboard(
                        label = "code",
                        text = parsed.code,
                        toastMessageRes = R.string.code_copied_to_clipboard,
                    )
                },
                onExplainClick = { showKotlinExplanation = true },
                showExplainButton = true,
            )
            if (!parsed.proseAfterCode.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                CopyableText(
                    text = parsed.proseAfterCode,
                    style = bodyStyle,
                    color = bodyColor,
                    clipboardLabel = "answer",
                    toastMessageRes = R.string.answer_copied_to_clipboard,
                )
            }
        }
    }

    if (showKotlinExplanation) {
        KotlinExplanationDialog(
            explanation = parsed.kotlinExplanation
                ?: stringResource(R.string.kotlin_explanation_missing),
            onDismiss = { showKotlinExplanation = false },
        )
    }
}

@Composable
private fun KotlinExplanationDialog(
    explanation: String,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val segments = remember(explanation) {
        InterviewAnswerParser.parseExplanationSegments(explanation)
    }
    val codeStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.kotlin_explanation_title))
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                segments.forEach { segment ->
                    when (segment) {
                        is ExplanationSegment.Prose -> {
                            Text(
                                text = segment.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        is ExplanationSegment.Code -> {
                            AnswerCodeBlock(
                                code = segment.code,
                                textStyle = codeStyle,
                                onCopyClick = {
                                    context.copyTextToClipboard(
                                        label = "code",
                                        text = segment.code,
                                        toastMessageRes = R.string.code_copied_to_clipboard,
                                    )
                                },
                                showExplainButton = false,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.kotlin_explanation_close))
            }
        },
    )
}

@Composable
private fun CopyableText(
    text: String,
    style: TextStyle,
    color: androidx.compose.ui.graphics.Color,
    clipboardLabel: String,
    toastMessageRes: Int,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Text(
        text = text,
        style = style,
        color = color,
        modifier = modifier.pointerInput(text) {
            detectTapGestures(
                onLongPress = {
                    context.copyTextToClipboard(
                        label = clipboardLabel,
                        text = text,
                        toastMessageRes = toastMessageRes,
                    )
                }
            )
        },
    )
}

@Composable
private fun AnswerCodeBlock(
    code: String,
    textStyle: TextStyle,
    onCopyClick: () -> Unit,
    modifier: Modifier = Modifier,
    onExplainClick: (() -> Unit)? = null,
    showExplainButton: Boolean = false,
) {
    val context = LocalContext.current
    val scroll = rememberScrollState()
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(code) {
                detectTapGestures(
                    onLongPress = {
                        context.copyTextToClipboard(
                            label = "code",
                            text = code,
                            toastMessageRes = R.string.code_copied_to_clipboard,
                        )
                    }
                )
            },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.kotlin_code_header),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (showExplainButton && onExplainClick != null) {
                        IconButton(
                            onClick = onExplainClick,
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = stringResource(
                                    R.string.kotlin_explanation_icon_cd
                                ),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                    IconButton(
                        onClick = onCopyClick,
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = stringResource(R.string.copy_code),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier
                    .horizontalScroll(scroll)
                    .padding(12.dp),
            ) {
                Text(
                    text = code,
                    style = textStyle,
                    color = MaterialTheme.colorScheme.onSurface,
                    softWrap = false,
                )
            }
        }
    }
}
