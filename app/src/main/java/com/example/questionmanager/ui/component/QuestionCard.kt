package com.example.questionmanager.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.questionmanager.domain.model.Question
import com.example.questionmanager.domain.model.QuestionStatus
import com.example.questionmanager.ui.theme.StatusCompleted
import com.example.questionmanager.ui.theme.StatusError
import com.example.questionmanager.ui.theme.StatusGenerating
import com.example.questionmanager.ui.theme.StatusPending
import com.example.questionmanager.util.toFormattedDate
import com.example.questionmanager.util.truncate

/**
 * 问题条目卡片
 */
@Composable
fun QuestionCard(
    question: Question,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 问题文本
            Text(
                text = question.question,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // 状态行
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusChip(status = question.status)

                Text(
                    text = question.createdAt.toFormattedDate(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 来源 URL (如有)
            question.sourceUrl?.let { url ->
                Text(
                    text = "来源: ${url.truncate(40)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun StatusChip(status: QuestionStatus) {
    val (icon, color, label) = when (status) {
        QuestionStatus.PENDING -> Triple(Icons.Default.HourglassEmpty, StatusPending, "等待生成")
        QuestionStatus.GENERATING -> Triple(Icons.Default.Sync, StatusGenerating, "生成中")
        QuestionStatus.COMPLETED -> Triple(Icons.Default.CheckCircle, StatusCompleted, "已生成")
        QuestionStatus.ERROR -> Triple(Icons.Default.Error, StatusError, "生成失败")
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

