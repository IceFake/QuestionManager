package com.example.questionmanager.util

import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * RetryUtil 单元测试
 */
class RetryUtilTest {

    @Test
    fun `friendlyErrorMessage for 401`() {
        val e = retrofit2.HttpException(
            retrofit2.Response.error<Any>(401, "".toResponseBody(null))
        )
        val msg = RetryUtil.friendlyErrorMessage(e)
        assertEquals("API Key 无效或已过期，请在设置中检查", msg)
    }

    @Test
    fun `friendlyErrorMessage for 429`() {
        val e = retrofit2.HttpException(
            retrofit2.Response.error<Any>(429, "".toResponseBody(null))
        )
        val msg = RetryUtil.friendlyErrorMessage(e)
        assertEquals("请求过于频繁，请稍后再试", msg)
    }

    @Test
    fun `friendlyErrorMessage for UnknownHostException`() {
        val e = java.net.UnknownHostException("host not found")
        val msg = RetryUtil.friendlyErrorMessage(e)
        assertEquals("无法连接到服务器，请检查网络", msg)
    }

    @Test
    fun `friendlyErrorMessage for SocketTimeoutException`() {
        val e = java.net.SocketTimeoutException("timeout")
        val msg = RetryUtil.friendlyErrorMessage(e)
        assertEquals("连接超时，请检查网络状况", msg)
    }

    @Test
    fun `friendlyErrorMessage for generic exception`() {
        val e = RuntimeException("something broke")
        val msg = RetryUtil.friendlyErrorMessage(e)
        assertEquals("something broke", msg)
    }

    @Test
    fun `isRetryableHttpError identifies retryable codes`() {
        assert(RetryUtil.isRetryableHttpError(429))
        assert(RetryUtil.isRetryableHttpError(500))
        assert(RetryUtil.isRetryableHttpError(503))
        assert(!RetryUtil.isRetryableHttpError(401))
        assert(!RetryUtil.isRetryableHttpError(404))
    }
}



