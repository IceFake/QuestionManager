package com.example.questionmanager.util

import kotlinx.coroutines.delay

/**
 * 通用重试工具
 * 支持指数退避重试策略
 */
object RetryUtil {

    /**
     * 带指数退避的重试
     * @param maxRetries 最大重试次数
     * @param initialDelayMs 初始延迟毫秒
     * @param maxDelayMs 最大延迟毫秒
     * @param factor 退避因子
     * @param shouldRetry 判断是否需要重试的条件
     * @param block 要执行的代码块
     */
    suspend fun <T> withRetry(
        maxRetries: Int = 3,
        initialDelayMs: Long = 1000L,
        maxDelayMs: Long = 16000L,
        factor: Double = 2.0,
        shouldRetry: (Exception) -> Boolean = { true },
        block: suspend (attempt: Int) -> T
    ): T {
        var currentDelay = initialDelayMs
        repeat(maxRetries) { attempt ->
            try {
                return block(attempt)
            } catch (e: Exception) {
                if (attempt == maxRetries - 1 || !shouldRetry(e)) throw e
                delay(currentDelay)
                currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelayMs)
            }
        }
        // This should never be reached
        return block(maxRetries)
    }

    /**
     * 判断 HTTP 错误码是否值得重试
     */
    fun isRetryableHttpError(code: Int): Boolean {
        return code in listOf(408, 429, 500, 502, 503, 504)
    }

    /**
     * 从异常中提取用户友好的错误消息
     */
    fun friendlyErrorMessage(e: Throwable): String {
        return when {
            e is retrofit2.HttpException -> {
                when (e.code()) {
                    401 -> "API Key 无效或已过期，请在设置中检查"
                    403 -> "访问被拒绝，请检查 API Key 权限"
                    429 -> "请求过于频繁，请稍后再试"
                    500, 502, 503 -> "AI 服务暂时不可用，请稍后再试"
                    else -> "网络请求失败 (${e.code()})"
                }
            }
            e is java.net.UnknownHostException -> "无法连接到服务器，请检查网络"
            e is java.net.SocketTimeoutException -> "连接超时，请检查网络状况"
            e is java.net.ConnectException -> "连接被拒绝，请检查网络或 API 地址"
            e is javax.net.ssl.SSLException -> "安全连接失败，请检查网络"
            e.message?.contains("API Key", ignoreCase = true) == true -> "请先在设置中配置 API Key"
            else -> e.message ?: "未知错误"
        }
    }
}

