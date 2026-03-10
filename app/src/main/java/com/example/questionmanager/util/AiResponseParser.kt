package com.example.questionmanager.util

import kotlinx.serialization.json.Json

/**
 * 从 AI 响应文本中安全提取问题列表
 * 支持多种格式容错
 */
object AiResponseParser {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun parseDrillDownQuestions(rawResponse: String): List<String> {
        // 策略 1: 直接尝试解析为 JSON 数组
        tryParseJsonArray(rawResponse)?.let { return it }

        // 策略 2: 提取 Markdown 代码块中的 JSON
        extractJsonFromCodeBlock(rawResponse)?.let { jsonStr ->
            tryParseJsonArray(jsonStr)?.let { return it }
        }

        // 策略 3: 正则提取第一个 [...] JSON 数组片段
        extractFirstJsonArray(rawResponse)?.let { jsonStr ->
            tryParseJsonArray(jsonStr)?.let { return it }
        }

        // 策略 4: 降级为按行分割 (兜底)
        return parseByLines(rawResponse)
    }

    private fun tryParseJsonArray(text: String): List<String>? = try {
        json.decodeFromString<List<String>>(text.trim())
    } catch (e: Exception) {
        null
    }

    private fun extractJsonFromCodeBlock(text: String): String? {
        val regex = Regex("""```(?:json)?\s*\n?([\s\S]*?)\n?\s*```""")
        return regex.find(text)?.groupValues?.get(1)?.trim()
    }

    private fun extractFirstJsonArray(text: String): String? {
        val regex = Regex("""\[[\s\S]*?]""")
        return regex.find(text)?.value
    }

    private fun parseByLines(text: String): List<String> {
        return text.lines()
            .map { it.trim() }
            .map { it.removePrefix("-").removePrefix("•").trim() }
            .map { it.replace(Regex("""^\d+[.、)]\s*"""), "") }
            .filter { it.isNotBlank() && it.length > 5 }
    }
}

