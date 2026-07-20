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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.bdavidgm.entrevista.R
import com.bdavidgm.entrevista.ui.util.copyTextToClipboard

internal data class ParsedInterviewAnswer(
    val proseBeforeCode: String,
    val code: String?,
    val proseAfterCode: String?,
)

internal object InterviewAnswerParser {

    private val ejemploLine = Regex("^Ejemplo:\\s*$", RegexOption.MULTILINE)
    private val consejoLine = Regex("^Consejo:\\s*$", RegexOption.MULTILINE)

    fun parse(raw: String): ParsedInterviewAnswer {
        val text = raw.replace("\r\n", "\n")
        val ejemploMatch = ejemploLine.find(text)
            ?: return ParsedInterviewAnswer(text.trimEnd(), null, null)

        val proseBefore = text.substring(0, ejemploMatch.range.first).trimEnd()
        val afterEjemplo = text.substring(ejemploMatch.range.last + 1)
        val consejoMatch = consejoLine.find(afterEjemplo)

        return if (consejoMatch == null) {
            val code = trimBlankMargins(afterEjemplo)
            if (code.isBlank()) return ParsedInterviewAnswer(text.trimEnd(), null, null)
            ParsedInterviewAnswer(
                proseBeforeCode = proseBefore,
                code = code,
                proseAfterCode = null
            )
        } else {
            val code = trimBlankMargins(afterEjemplo.substring(0, consejoMatch.range.first))
            val proseAfter = afterEjemplo.substring(consejoMatch.range.first).trimStart()
            if (code.isBlank()) {
                return ParsedInterviewAnswer(text.trimEnd(), null, null)
            }
            ParsedInterviewAnswer(
                proseBeforeCode = proseBefore,
                code = code,
                proseAfterCode = proseAfter.takeUnless { it.isBlank() }
            )
        }
    }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Ejemplo",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                IconButton(
                    onClick = {
                        context.copyTextToClipboard(
                            label = "code",
                            text = parsed.code,
                            toastMessageRes = R.string.code_copied_to_clipboard,
                        )
                    },
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
            Spacer(modifier = Modifier.height(2.dp))
            AnswerCodeBlock(
                code = parsed.code,
                textStyle = codeStyle,
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
    modifier: Modifier = Modifier,
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
