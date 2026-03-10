package com.example.questionmanager.ui.screen.input

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputScreen(
    onNavigateBack: () -> Unit,
    viewModel: InputViewModel = hiltViewModel()
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
        if (uiState.isComplete) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("新增问题") },
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
            // Tab 切换
            val tabIndex = if (uiState.inputMode == InputMode.URL) 0 else 1
            TabRow(selectedTabIndex = tabIndex) {
                Tab(
                    selected = tabIndex == 0,
                    onClick = { viewModel.setInputMode(InputMode.URL) },
                    text = { Text("URL 解析") }
                )
                Tab(
                    selected = tabIndex == 1,
                    onClick = { viewModel.setInputMode(InputMode.MANUAL) },
                    text = { Text("手动输入") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (uiState.inputMode) {
                InputMode.URL -> UrlInputSection(uiState, viewModel)
                InputMode.MANUAL -> ManualInputSection(uiState, viewModel)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 提示词选择
            PromptSelector(uiState, viewModel)

            Spacer(modifier = Modifier.height(24.dp))

            // 确认按钮
            Button(
                onClick = { viewModel.confirmAndGenerate() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isSubmitting && !uiState.isParsing
            ) {
                if (uiState.isSubmitting) {
                    LoadingIndicator()
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("确认并生成答案")
            }
        }
    }
}

@Composable
private fun UrlInputSection(
    uiState: InputViewModel.InputUiState,
    viewModel: InputViewModel
) {
    OutlinedTextField(
        value = uiState.url,
        onValueChange = { viewModel.updateUrl(it) },
        label = { Text("请输入网址 URL") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        enabled = !uiState.isParsing
    )

    Spacer(modifier = Modifier.height(8.dp))

    OutlinedButton(
        onClick = { viewModel.parseUrl() },
        enabled = !uiState.isParsing && uiState.url.isNotBlank()
    ) {
        if (uiState.isParsing) {
            LoadingIndicator()
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text("解析")
    }

    // 解析结果
    if (uiState.parsedQuestions.isNotEmpty()) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "解析结果 (${uiState.parsedQuestions.count { it.isSelected }}/${uiState.parsedQuestions.size}):",
            style = MaterialTheme.typography.titleSmall
        )
        Spacer(modifier = Modifier.height(8.dp))

        uiState.parsedQuestions.forEachIndexed { index, pq ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = pq.isSelected,
                    onCheckedChange = { viewModel.toggleParsedQuestion(index) }
                )
                Text(
                    text = pq.text,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun ManualInputSection(
    uiState: InputViewModel.InputUiState,
    viewModel: InputViewModel
) {
    uiState.manualQuestions.forEachIndexed { index, question ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = question,
                onValueChange = { viewModel.updateManualQuestion(index, it) },
                label = { Text("问题 ${index + 1}") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            if (uiState.manualQuestions.size > 1) {
                IconButton(onClick = { viewModel.removeManualQuestion(index) }) {
                    Icon(Icons.Default.Close, contentDescription = "删除")
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    TextButton(onClick = { viewModel.addManualQuestion() }) {
        Icon(Icons.Default.Add, contentDescription = null)
        Spacer(modifier = Modifier.width(4.dp))
        Text("添加更多问题")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PromptSelector(
    uiState: InputViewModel.InputUiState,
    viewModel: InputViewModel
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

