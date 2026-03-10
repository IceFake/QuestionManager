package com.example.questionmanager.ui.screen.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.questionmanager.ui.component.LoadingIndicator
import com.example.questionmanager.ui.component.QuestionCard
import com.example.questionmanager.ui.component.SearchBar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToInput: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            if (uiState.isSelectionMode) {
                TopAppBar(
                    title = { Text("已选 ${uiState.selectedIds.size} 项") },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.exitSelectionMode() }) {
                            Icon(Icons.Default.Close, contentDescription = "退出选择")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            if (uiState.selectedIds.size == uiState.questions.size) {
                                viewModel.deselectAll()
                            } else {
                                viewModel.selectAll()
                            }
                        }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "全选")
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = { Text("QuestionManager") },
                    actions = {
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "设置")
                        }
                    },
                    scrollBehavior = scrollBehavior
                )
            }
        },
        floatingActionButton = {
            if (!uiState.isSelectionMode) {
                FloatingActionButton(onClick = onNavigateToInput) {
                    Icon(Icons.Default.Add, contentDescription = "新增问题")
                }
            }
        },
        bottomBar = {
            if (uiState.isSelectionMode && uiState.selectedIds.isNotEmpty()) {
                BottomAppBar {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        TextButton(
                            onClick = { viewModel.batchDelete() },
                            enabled = !uiState.isBatchProcessing
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("批量删除")
                        }
                        TextButton(
                            onClick = { viewModel.batchGenerate() },
                            enabled = !uiState.isBatchProcessing
                        ) {
                            Icon(Icons.Default.Autorenew, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("批量生成")
                        }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 搜索栏 (非选择模式下显示)
            if (!uiState.isSelectionMode) {
                SearchBar(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    onClick = onNavigateToSearch
                )
            }

            when {
                uiState.isLoading -> {
                    LoadingIndicator(fullScreen = true)
                }
                uiState.isBatchProcessing -> {
                    LoadingIndicator(fullScreen = true)
                }
                uiState.questions.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "暂无问题",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "点击右下角 + 按钮添加问题",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = uiState.questions,
                            key = { it.id }
                        ) { question ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.animateItem()
                            ) {
                                if (uiState.isSelectionMode) {
                                    Checkbox(
                                        checked = uiState.selectedIds.contains(question.id),
                                        onCheckedChange = { viewModel.toggleSelection(question.id) }
                                    )
                                }
                                QuestionCard(
                                    question = question,
                                    modifier = Modifier
                                        .weight(1f)
                                        .combinedClickable(
                                            onClick = {
                                                if (uiState.isSelectionMode) {
                                                    viewModel.toggleSelection(question.id)
                                                } else {
                                                    onNavigateToDetail(question.id)
                                                }
                                            },
                                            onLongClick = {
                                                if (!uiState.isSelectionMode) {
                                                    viewModel.toggleSelectionMode()
                                                    viewModel.toggleSelection(question.id)
                                                }
                                            }
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

