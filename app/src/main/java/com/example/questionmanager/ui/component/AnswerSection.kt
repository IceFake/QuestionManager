package com.example.questionmanager.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography

@Composable
fun AnswerSection(
    answer: String,
    modifier: Modifier = Modifier,
    scrollable: Boolean = false
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        val contentModifier = if (scrollable) {
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        } else {
            Modifier.padding(16.dp)
        }

        SelectionContainer {
            Markdown(
                content = answer,
                modifier = contentModifier,
                colors = markdownColor(
                    text = MaterialTheme.colorScheme.onSurface,
                    codeText = MaterialTheme.colorScheme.primary,
                    codeBackground = MaterialTheme.colorScheme.surfaceContainerHighest,
                    dividerColor = MaterialTheme.colorScheme.outlineVariant,
                    linkText = MaterialTheme.colorScheme.primary
                ),
                typography = markdownTypography(
                    h1 = MaterialTheme.typography.headlineLarge,
                    h2 = MaterialTheme.typography.headlineMedium,
                    h3 = MaterialTheme.typography.headlineSmall,
                    h4 = MaterialTheme.typography.titleLarge,
                    h5 = MaterialTheme.typography.titleMedium,
                    h6 = MaterialTheme.typography.titleSmall,
                    paragraph = MaterialTheme.typography.bodyLarge.copy(
                        lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.5
                    ),
                    code = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace
                    ),
                    list = MaterialTheme.typography.bodyLarge
                )
            )
        }
    }
}

@Composable
fun AnswerSectionPlain(
    answer: String,
    modifier: Modifier = Modifier
) {
    SelectionContainer {
        Markdown(
            content = answer,
            modifier = modifier.fillMaxWidth(),
            colors = markdownColor(
                text = MaterialTheme.colorScheme.onSurface,
                codeText = MaterialTheme.colorScheme.primary,
                codeBackground = MaterialTheme.colorScheme.surfaceContainerHighest,
                dividerColor = MaterialTheme.colorScheme.outlineVariant,
                linkText = MaterialTheme.colorScheme.primary
            ),
            typography = markdownTypography(
                h1 = MaterialTheme.typography.headlineLarge,
                h2 = MaterialTheme.typography.headlineMedium,
                h3 = MaterialTheme.typography.headlineSmall,
                h4 = MaterialTheme.typography.titleLarge,
                h5 = MaterialTheme.typography.titleMedium,
                h6 = MaterialTheme.typography.titleSmall,
                paragraph = MaterialTheme.typography.bodyLarge,
                code = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace
                )
            )
        )
    }
}
