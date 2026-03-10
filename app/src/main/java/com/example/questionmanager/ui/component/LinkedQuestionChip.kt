package com.example.questionmanager.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.questionmanager.util.truncate

/**
 * 链接条目标签
 * @param questionText 链接的问题文本
 * @param isParent true=来源(父), false=引申(子)
 * @param onClick 点击跳转
 */
@Composable
fun LinkedQuestionChip(
    questionText: String,
    isParent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AssistChip(
        onClick = onClick,
        label = {
            Text(
                text = questionText.truncate(30),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingIcon = {
            Icon(
                imageVector = if (isParent)
                    Icons.AutoMirrored.Filled.ArrowBack
                else
                    Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = if (isParent) "来源" else "引申",
                modifier = Modifier.size(16.dp)
            )
        },
        modifier = modifier
    )
}

