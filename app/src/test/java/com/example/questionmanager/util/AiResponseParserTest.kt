package com.example.questionmanager.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * AiResponseParser 单元测试
 * 验证多种格式容错解析能力
 */
class AiResponseParserTest {

    @Test
    fun `parse valid JSON array directly`() {
        val input = """["问题1", "问题2", "问题3"]"""
        val result = AiResponseParser.parseDrillDownQuestions(input)
        assertEquals(3, result.size)
        assertEquals("问题1", result[0])
    }

    @Test
    fun `parse JSON wrapped in markdown code block`() {
        val input = """
            ```json
            ["问题1", "问题2", "问题3"]
            ```
        """.trimIndent()
        val result = AiResponseParser.parseDrillDownQuestions(input)
        assertEquals(3, result.size)
    }

    @Test
    fun `parse JSON with extra text before and after`() {
        val input = """
            以下是引申问题：
            ["问题A", "问题B"]
            希望对你有帮助！
        """.trimIndent()
        val result = AiResponseParser.parseDrillDownQuestions(input)
        assertEquals(2, result.size)
        assertEquals("问题A", result[0])
    }

    @Test
    fun `fallback to line-based parsing when no JSON found`() {
        val input = """
            1. 什么是设计模式？
            2. 如何使用工厂模式？
            3. 观察者模式的应用场景有哪些？
        """.trimIndent()
        val result = AiResponseParser.parseDrillDownQuestions(input)
        assertTrue(result.isNotEmpty())
        assertTrue(result[0].contains("设计模式"))
    }

    @Test
    fun `parse bullet list fallback`() {
        val input = """
            - MVVM 与 MVC 有何区别？
            - ViewModel 如何管理生命周期？
            - DataBinding 在 MVVM 中的作用是什么？
        """.trimIndent()
        val result = AiResponseParser.parseDrillDownQuestions(input)
        assertEquals(3, result.size)
    }

    @Test
    fun `empty input returns empty list`() {
        val result = AiResponseParser.parseDrillDownQuestions("")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `parse code block without json label`() {
        val input = """
            ```
            ["Q1", "Q2"]
            ```
        """.trimIndent()
        val result = AiResponseParser.parseDrillDownQuestions(input)
        assertEquals(2, result.size)
    }
}

