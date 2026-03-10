package com.example.questionmanager.ui.screen.drilldown

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.questionmanager.ui.component.LoadingIndicator
import com.example.questionmanager.util.truncate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrillDownScreen(
    onNavigateBack: () -> Unit,
    viewModel: DrillDownViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.isComplete) {
        if (uiState.isComplete) onNavigateBack()
    }

    val titleText = uiState.originalQuestion?.question?.truncate(20) ?: "深挖"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("深挖: $titleText") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // 原问题信息
            uiState.originalQuestion?.let { q ->
                Text(
                    text = "原始问题:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = q.question,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 4.dp)
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            }

            when {
                uiState.isGenerating -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LoadingIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "AI 正在生成引申问题...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                uiState.suggestedQuestions.isNotEmpty() -> {
                    Text(
                        text = "AI 为您生成了以下引申问题:",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val selectedCount = uiState.suggestedQuestions.count { it.isSelected }
                    Text(
                        text = "已选: $selectedCount / ${uiState.suggestedQuestions.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    uiState.suggestedQuestions.forEachIndexed { index, sq ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = sq.isSelected,
                                onCheckedChange = { viewModel.toggleQuestion(index) }
                            )
                            Text(
                                text = sq.text,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                }
                else -> {
                    Text(
                        text = "暂无引申问题，点击下方按钮生成",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 重新生成按钮
            OutlinedButton(
                onClick = { viewModel.generateDrillDownQuestions() },
                enabled = !uiState.isGenerating && !uiState.isSubmitting,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("重新生成引申问题")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 提示词选择
            DrillDownPromptSelector(uiState, viewModel)

            Spacer(modifier = Modifier.height(24.dp))

            // 确认按钮
            Button(
                onClick = { viewModel.confirmSelected() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isSubmitting
                        && !uiState.isGenerating
                        && uiState.suggestedQuestions.any { it.isSelected }
            ) {
                if (uiState.isSubmitting) {
                    LoadingIndicator()
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("确认并生成选中条目")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DrillDownPromptSelector(
    uiState: DrillDownViewModel.DrillDownUiState,
    viewModel: DrillDownViewModel
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedPrompt = uiState.availablePrompts.find { it.id == uiState.selectedPromptId }

    Text(
        text = "使用提示词:",
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(bottom = 8.dp)
    )

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedPrompt?.name ?: "默认提示词",
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            uiState.availablePrompts.forEach { prompt ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = if (prompt.isDefault) "${prompt.name} (默认)" else prompt.name
                        )
                    },
                    onClick = {
                        viewModel.selectPrompt(prompt.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

