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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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
                // 问题文本
                Text(
                    text = question.question,
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 元信息
                question.sourceUrl?.let { url ->
                    Text(
                        text = "来源: $url",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "创建时间: ${question.createdAt.toFormattedDate()}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                // 答案区域
                Text(
                    text = "答案",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))

                when {
                    // 流式答案优先显示
                    uiState.streamingAnswer != null -> {
                        if (uiState.streamingAnswer!!.isNotBlank()) {
                            AnswerSection(answer = uiState.streamingAnswer!!)
                        }
                        Row(modifier = Modifier.padding(top = 8.dp)) {
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
                        Text(
                            text = "等待生成答案...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    question.status == QuestionStatus.GENERATING -> {
                        LoadingIndicator(fullScreen = false)
                        Text(
                            text = "正在生成答案...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    question.status == QuestionStatus.COMPLETED -> {
                        question.answer?.let { answer ->
                            AnswerSection(answer = answer)
                        }
                    }
                    question.status == QuestionStatus.ERROR -> {
                        Text(
                            text = "答案生成失败",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 重新生成按钮
                OutlinedButton(
                    onClick = { viewModel.regenerateAnswer() },
                    enabled = !uiState.isRegenerating,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (uiState.isRegenerating) {
                        LoadingIndicator()
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                    }
                    Text(
                        text = "重新生成答案",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                // 关联问题
                if (uiState.parentQuestions.isNotEmpty() || uiState.childQuestions.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                    Text(
                        text = "关联问题",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (uiState.parentQuestions.isNotEmpty()) {
                        Text(
                            text = "来源:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                        Text(
                            text = "引申:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
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
                    Icon(Icons.Default.Search, contentDescription = null)
                    Text(
                        text = "深挖此问题",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
