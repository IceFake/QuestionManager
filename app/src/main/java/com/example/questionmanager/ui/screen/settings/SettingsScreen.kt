package com.example.questionmanager.ui.screen.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.questionmanager.domain.model.Prompt
import com.example.questionmanager.ui.component.LoadingIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showCreatePromptDialog by remember { mutableStateOf(false) }
    var editingPrompt by remember { mutableStateOf<Prompt?>(null) }
    val context = LocalContext.current

    // 用于存储文件选择回调（由 PromptDialog 设置）
    var onMdFileSelected by remember { mutableStateOf<((String) -> Unit)?>(null) }

    // MD 文件选择器
    val mdFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val text = context.contentResolver.openInputStream(it)
                    ?.bufferedReader()
                    ?.use { reader -> reader.readText() }
                    ?: ""
                onMdFileSelected?.invoke(text)
            } catch (e: Exception) {
                viewModel.showMessage("导入失败: ${e.message}")
            }
        }
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
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
            // ── API 配置 ──
            SectionHeader("API 配置")

            OutlinedTextField(
                value = uiState.apiKey,
                onValueChange = { viewModel.updateApiKey(it) },
                label = { Text("DeepSeek API Key") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = uiState.baseUrl,
                onValueChange = { viewModel.updateBaseUrl(it) },
                label = { Text("API Base URL") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = uiState.model,
                onValueChange = { viewModel.updateModel(it) },
                label = { Text("模型") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.saveSettings() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isSaving
            ) {
                if (uiState.isSaving) {
                    LoadingIndicator()
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("保存 API 配置")
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp))

            // ── 提示词管理 ──
            SectionHeader("提示词管理")

            uiState.prompts.forEach { prompt ->
                PromptCard(
                    prompt = prompt,
                    onEdit = { editingPrompt = prompt },
                    onDelete = { viewModel.deletePrompt(prompt.id) },
                    onSetDefault = { viewModel.setDefaultPrompt(prompt.id) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            OutlinedButton(
                onClick = { showCreatePromptDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("新建提示词")
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp))

            // ── 生成参数 ──
            SectionHeader("生成参数")

            Text(
                text = "Temperature: %.2f".format(uiState.temperature),
                style = MaterialTheme.typography.bodyMedium
            )
            Slider(
                value = uiState.temperature.toFloat(),
                onValueChange = { viewModel.updateTemperature(it.toDouble()) },
                valueRange = 0f..2f,
                steps = 19
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = uiState.maxTokens.toString(),
                onValueChange = { text ->
                    text.toIntOrNull()?.let { viewModel.updateMaxTokens(it) }
                },
                label = { Text("Max Tokens") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.saveSettings() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isSaving
            ) {
                Text("保存所有设置")
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // 新建提示词对话框
    if (showCreatePromptDialog) {
        PromptDialog(
            title = "新建提示词",
            initialName = "",
            initialContent = "",
            onConfirm = { name, content ->
                viewModel.createPrompt(name, content)
                showCreatePromptDialog = false
            },
            onDismiss = { showCreatePromptDialog = false },
            onImportMd = { callback ->
                onMdFileSelected = callback
                mdFilePicker.launch(arrayOf("text/markdown", "text/plain", "text/*"))
            }
        )
    }

    // 编辑提示词对话框
    editingPrompt?.let { prompt ->
        PromptDialog(
            title = "编辑提示词",
            initialName = prompt.name,
            initialContent = prompt.systemPrompt,
            onConfirm = { name, content ->
                viewModel.updatePrompt(prompt.copy(name = name, systemPrompt = content))
                editingPrompt = null
            },
            onDismiss = { editingPrompt = null },
            onImportMd = { callback ->
                onMdFileSelected = callback
                mdFilePicker.launch(arrayOf("text/markdown", "text/plain", "text/*"))
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

@Composable
private fun PromptCard(
    prompt: Prompt,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSetDefault: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "📝 ${prompt.name}",
                        style = MaterialTheme.typography.titleSmall
                    )
                    if (prompt.isDefault) {
                        Text(
                            text = " (默认)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Text(
                text = prompt.systemPrompt,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                modifier = Modifier.padding(top = 4.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, "编辑")
                }
                if (!prompt.isDefault) {
                    IconButton(onClick = onSetDefault) {
                        Icon(Icons.Default.StarBorder, "设为默认")
                    }
                } else {
                    IconButton(onClick = {}, enabled = false) {
                        Icon(Icons.Default.Star, "已是默认", tint = MaterialTheme.colorScheme.primary)
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "删除")
                }
            }
        }
    }
}

@Composable
private fun PromptDialog(
    title: String,
    initialName: String,
    initialContent: String,
    onConfirm: (name: String, content: String) -> Unit,
    onDismiss: () -> Unit,
    onImportMd: (onContentLoaded: (String) -> Unit) -> Unit = {}
) {
    var name by remember { mutableStateOf(initialName) }
    var content by remember { mutableStateOf(initialContent) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("名称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("System Prompt") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    maxLines = 8
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        onImportMd { importedContent ->
                            content = importedContent
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.FileOpen, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("导入 MD 文档")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, content) },
                enabled = name.isNotBlank() && content.isNotBlank()
            ) {
                Text("确认")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

