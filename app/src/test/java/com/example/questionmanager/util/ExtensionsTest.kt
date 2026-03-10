package com.example.questionmanager.util

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Extensions 单元测试
 */
class ExtensionsTest {

    @Test
    fun `truncate short string stays unchanged`() {
        assertEquals("hello", "hello".truncate(10))
    }

    @Test
    fun `truncate long string adds ellipsis`() {
        val result = "This is a very long string".truncate(10)
        assertEquals("This is...", result)
        assertEquals(10, result.length)
    }

    @Test
    fun `truncate exact length stays unchanged`() {
        assertEquals("12345", "12345".truncate(5))
    }

    @Test
    fun `toFormattedDate produces correct format`() {
        // 2024-01-01 00:00:00 UTC
        val timestamp = 1704067200000L
        val result = timestamp.toFormattedDate("yyyy-MM-dd")
        // Just verify it matches date format pattern
        assert(result.matches(Regex("""\d{4}-\d{2}-\d{2}""")))
    }
}

