package com.example.questionmanager.ui.component

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 答案展示区域 (简化 Markdown 渲染)
 * 支持: 标题(#), 粗体(**), 斜体(*), 行内代码(`), 代码块(```), 列表(- / 1.)
 */
@Composable
fun AnswerSection(
    answer: String,
    modifier: Modifier = Modifier
) {
    val lines = remember(answer) { answer.lines() }

    SelectionContainer {
        Column(modifier = modifier.fillMaxWidth()) {
            var inCodeBlock = false
            val codeBlockLines = mutableListOf<String>()

            for (line in lines) {
                when {
                    line.trimStart().startsWith("```") && !inCodeBlock -> {
                        inCodeBlock = true
                        codeBlockLines.clear()
                    }
                    line.trimStart().startsWith("```") && inCodeBlock -> {
                        inCodeBlock = false
                        CodeBlock(code = codeBlockLines.joinToString("\n"))
                    }
                    inCodeBlock -> {
                        codeBlockLines.add(line)
                    }
                    line.startsWith("### ") -> {
                        Text(
                            text = parseInlineMarkdown(line.removePrefix("### ")),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                        )
                    }
                    line.startsWith("## ") -> {
                        Text(
                            text = parseInlineMarkdown(line.removePrefix("## ")),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                        )
                    }
                    line.startsWith("# ") -> {
                        Text(
                            text = parseInlineMarkdown(line.removePrefix("# ")),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                        )
                    }
                    line.startsWith("---") || line.startsWith("***") -> {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                    line.trimStart().startsWith("- ") || line.trimStart().startsWith("* ") -> {
                        val indent = line.length - line.trimStart().length
                        val content = line.trimStart().removePrefix("- ").removePrefix("* ")
                        Text(
                            text = buildAnnotatedString {
                                append("  ".repeat(indent / 2))
                                append("• ")
                                append(parseInlineMarkdown(content))
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 1.dp)
                        )
                    }
                    line.trimStart().matches(Regex("""^\d+[.)] .*""")) -> {
                        val content = line.trimStart().replaceFirst(Regex("""^\d+[.)] """), "")
                        val number = line.trimStart().takeWhile { it.isDigit() || it == '.' || it == ')' }
                        Text(
                            text = buildAnnotatedString {
                                append("$number ")
                                append(parseInlineMarkdown(content))
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 1.dp)
                        )
                    }
                    line.isBlank() -> {
                        // empty line spacing
                        Text(text = "", modifier = Modifier.padding(vertical = 2.dp))
                    }
                    line.trimStart().startsWith("> ") -> {
                        // Blockquote
                        val content = line.trimStart().removePrefix("> ")
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = parseInlineMarkdown(content),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontStyle = FontStyle.Italic
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(
                                    start = 12.dp, end = 8.dp,
                                    top = 6.dp, bottom = 6.dp
                                )
                            )
                        }
                    }
                    else -> {
                        Text(
                            text = parseInlineMarkdown(line),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 1.dp)
                        )
                    }
                }
            }

            // Handle unclosed code block
            if (inCodeBlock && codeBlockLines.isNotEmpty()) {
                CodeBlock(code = codeBlockLines.joinToString("\n"))
            }
        }
    }
}

@Composable
private fun CodeBlock(code: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(12.dp)
        ) {
            Text(
                text = code,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp
                )
            )
        }
    }
}

/**
 * 解析行内 Markdown: **bold**, *italic*, `code`, ~~strikethrough~~, [link](url)
 */
private fun parseInlineMarkdown(text: String) = buildAnnotatedString {
    var i = 0
    while (i < text.length) {
        when {
            // Link: [text](url)
            text[i] == '[' -> {
                val closeBracket = text.indexOf(']', i + 1)
                if (closeBracket != -1 && closeBracket + 1 < text.length && text[closeBracket + 1] == '(') {
                    val closeParen = text.indexOf(')', closeBracket + 2)
                    if (closeParen != -1) {
                        val linkText = text.substring(i + 1, closeBracket)
                        withStyle(SpanStyle(
                            color = androidx.compose.ui.graphics.Color(0xFF1E88E5),
                            textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                        )) {
                            append(linkText)
                        }
                        i = closeParen + 1
                        continue
                    }
                }
                append(text[i])
                i++
            }
            // Strikethrough: ~~text~~
            i + 1 < text.length && text[i] == '~' && text[i + 1] == '~' -> {
                val end = text.indexOf("~~", i + 2)
                if (end != -1) {
                    withStyle(SpanStyle(
                        textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                    )) {
                        append(text.substring(i + 2, end))
                    }
                    i = end + 2
                } else {
                    append(text[i])
                    i++
                }
            }
            // Bold: **text**
            i + 1 < text.length && text[i] == '*' && text[i + 1] == '*' -> {
                val end = text.indexOf("**", i + 2)
                if (end != -1) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(text.substring(i + 2, end))
                    }
                    i = end + 2
                } else {
                    append(text[i])
                    i++
                }
            }
            // Italic: *text*
            text[i] == '*' -> {
                val end = text.indexOf('*', i + 1)
                if (end != -1) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(text.substring(i + 1, end))
                    }
                    i = end + 1
                } else {
                    append(text[i])
                    i++
                }
            }
            // Inline code: `code`
            text[i] == '`' -> {
                val end = text.indexOf('`', i + 1)
                if (end != -1) {
                    withStyle(SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = androidx.compose.ui.graphics.Color(0x20808080)
                    )) {
                        append(text.substring(i + 1, end))
                    }
                    i = end + 1
                } else {
                    append(text[i])
                    i++
                }
            }
            else -> {
                append(text[i])
                i++
            }
        }
    }
}

