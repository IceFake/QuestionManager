package com.example.questionmanager.ui.screen.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.questionmanager.domain.model.Prompt
import com.example.questionmanager.domain.model.QuestionStatus
import com.example.questionmanager.ui.component.AnswerSection
import com.example.questionmanager.ui.component.LinkedQuestionChip
import com.example.questionmanager.ui.component.LoadingIndicator
import com.example.questionmanager.util.toFormattedDate

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToDrillDown: (Long) -> Unit,
    viewModel: DetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) onNavigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("问题详情") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "更多")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("分享") },
                                onClick = {
                                    showMenu = false
                                    viewModel.shareQuestion(context)
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Share, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("删除") },
                                onClick = {
                                    showMenu = false
                                    viewModel.deleteQuestion()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Delete, contentDescription = null)
                                }
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        val question = uiState.question

        if (question == null) {
            LoadingIndicator(fullScreen = true)
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                QuestionCard(
                    questionText = question.question,
                    sourceUrl = question.sourceUrl,
                    createdAt = question.createdAt.toFormattedDate(),
                    status = question.status
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 答案区域
                AnswerHeader(
                    status = question.status,
                    streamingAnswer = uiState.streamingAnswer
                )

                Spacer(modifier = Modifier.height(8.dp))

                when {
                    uiState.streamingAnswer != null -> {
                        if (uiState.streamingAnswer!!.isNotBlank()) {
                            AnswerSection(answer = uiState.streamingAnswer!!)
                        }
                        Row(
                            modifier = Modifier.padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LoadingIndicator(fullScreen = false)
                            Text(
                                text = "正在生成...",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                    question.status == QuestionStatus.PENDING -> {
                        EmptyAnswerCard(message = "等待生成答案")
                    }
                    question.status == QuestionStatus.GENERATING -> {
                        EmptyAnswerCard(
                            message = "正在生成答案...",
                            isLoading = true
                        )
                    }
                    question.status == QuestionStatus.COMPLETED -> {
                        question.answer?.let { answer ->
                            AnswerSection(answer = answer)
                        }
                    }
                    question.status == QuestionStatus.ERROR -> {
                        EmptyAnswerCard(
                            message = "答案生成失败",
                            isError = true
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (uiState.prompts.isNotEmpty()) {
                    PromptSelector(
                        prompts = uiState.prompts,
                        selectedPromptId = uiState.selectedPromptId,
                        onPromptSelected = { viewModel.selectPrompt(it) },
                        enabled = !uiState.isRegenerating
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                OutlinedButton(
                    onClick = { viewModel.regenerateAnswer() },
                    enabled = !uiState.isRegenerating,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (uiState.isRegenerating) {
                        LoadingIndicator()
                    } else {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("重新生成答案")
                }

                // 关联问题
                if (uiState.parentQuestions.isNotEmpty() || uiState.childQuestions.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))
                    
                    Text(
                        text = "关联问题",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (uiState.parentQuestions.isNotEmpty()) {
                        SectionLabel(text = "来源")
                        FlowRow(
                            modifier = Modifier.padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            uiState.parentQuestions.forEach { parent ->
                                LinkedQuestionChip(
                                    questionText = parent.question,
                                    isParent = true,
                                    onClick = { onNavigateToDetail(parent.id) }
                                )
                            }
                        }
                    }

                    if (uiState.childQuestions.isNotEmpty()) {
                        SectionLabel(text = "引申", modifier = Modifier.padding(top = 12.dp))
                        FlowRow(
                            modifier = Modifier.padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            uiState.childQuestions.forEach { child ->
                                LinkedQuestionChip(
                                    questionText = child.question,
                                    isParent = false,
                                    onClick = { onNavigateToDetail(child.id) }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 深挖按钮
                Button(
                    onClick = { onNavigateToDrillDown(question.id) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = question.status == QuestionStatus.COMPLETED
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("深挖此问题")
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun PromptSelector(
    prompts: List<Prompt>,
    selectedPromptId: Long?,
    onPromptSelected: (Long?) -> Unit,
    enabled: Boolean
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedPrompt = prompts.find { it.id == selectedPromptId }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedCard(
            onClick = { if (enabled) expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "提示词模板",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = selectedPrompt?.name ?: "默认",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            prompts.forEach { prompt ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                text = prompt.name,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (prompt.isDefault) {
                                Text(
                                    text = "默认",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    onClick = {
                        onPromptSelected(prompt.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun QuestionCard(
    questionText: String,
    sourceUrl: String?,
    createdAt: String,
    status: QuestionStatus
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = questionText,
                style = MaterialTheme.typography.titleLarge
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusChip(status = status)
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    sourceUrl?.let {
                        Text(
                            text = "来源",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.width(150.dp)
                        )
                    }
                }
            }
            
            if (sourceUrl != null) {
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            Text(
                text = createdAt,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatusChip(status: QuestionStatus) {
    val (icon, text, color) = when (status) {
        QuestionStatus.PENDING -> Triple(Icons.Default.HourglassEmpty, "待处理", MaterialTheme.colorScheme.onSurfaceVariant)
        QuestionStatus.GENERATING -> Triple(Icons.Default.Sync, "生成中", MaterialTheme.colorScheme.tertiary)
        QuestionStatus.COMPLETED -> Triple(Icons.Default.CheckCircle, "已完成", MaterialTheme.colorScheme.primary)
        QuestionStatus.ERROR -> Triple(Icons.Default.Error, "失败", MaterialTheme.colorScheme.error)
    }
    
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = color
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = color
            )
        }
    }
}

@Composable
private fun AnswerHeader(
    status: QuestionStatus,
    streamingAnswer: String?
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "答案",
            style = MaterialTheme.typography.titleMedium
        )
        if (status == QuestionStatus.COMPLETED && streamingAnswer == null) {
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ) {
                Text(
                    text = "AI 生成",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

@Composable
private fun EmptyAnswerCard(
    message: String,
    isLoading: Boolean = false,
    isError: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isError) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isLoading) {
                LoadingIndicator(fullScreen = false)
                Spacer(modifier = Modifier.width(12.dp))
            }
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isError) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}
