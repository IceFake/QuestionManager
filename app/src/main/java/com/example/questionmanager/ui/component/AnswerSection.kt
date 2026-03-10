package com.example.questionmanager.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mikepenz.markdown.m3.Markdown

/**
 * 答案展示区域 (Markdown 渲染)
 * 使用 multiplatform-markdown-renderer 库支持完整 Markdown 渲染，包括:
 * 标题(#), 粗体(**), 斜体(*), 行内代码(`), 代码块(```),
 * 列表(- / 1.), 引用(>), 表格, 链接, 删除线(~~) 等
 */
@Composable
fun AnswerSection(
    answer: String,
    modifier: Modifier = Modifier
) {
    SelectionContainer {
        Markdown(
            content = answer,
            modifier = modifier.fillMaxWidth()
        )
    }
}
