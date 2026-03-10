package com.example.questionmanager.data.remote.api

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.questionmanager.util.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.jsoup.Jsoup
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class WebParserService @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * 抓取网页内容并提取文本
     * 使用 Jsoup 进行 HTML 解析
     *
     * 【降级策略】若 Jsoup 提取内容过短 (疑似 SPA)，
     * 自动降级为 WebView 渲染后提取 DOM 文本
     */
    suspend fun fetchAndParse(url: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0")
                .timeout(Constants.WEB_PARSE_TIMEOUT_MS)
                .get()

            val text = doc.body().text()

            // 降级检查: 若提取内容过短，可能是 SPA 页面
            if (text.length < 50) {
                // 尝试 WebView 方案
                return@withContext fetchWithWebView(url)
            }

            Result.success(text)
        } catch (e: Exception) {
            // Jsoup 失败时尝试 WebView 方案
            return@withContext try {
                fetchWithWebView(url)
            } catch (_: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * WebView 方案 — 加载页面等 JS 渲染后提取文本
     * 用于 SPA 动态页面的降级处理
     */
    @SuppressLint("SetJavaScriptEnabled")
    private suspend fun fetchWithWebView(url: String): Result<String> {
        return try {
            withTimeout(Constants.WEB_PARSE_TIMEOUT_MS.toLong() * 2) {
                withContext(Dispatchers.Main) {
                    suspendCancellableCoroutine { continuation ->
                        val webView = WebView(context).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.blockNetworkImage = true // 不加载图片加速

                            addJavascriptInterface(object {
                                @JavascriptInterface
                                fun onContent(text: String) {
                                    if (continuation.isActive) {
                                        if (text.length < 50) {
                                            continuation.resumeWithException(
                                                IllegalStateException("网页内容过少，可能是需要登录或受限页面。请尝试手动输入问题。")
                                            )
                                        } else {
                                            continuation.resume(Result.success(text))
                                        }
                                    }
                                    // Clean up WebView
                                    this@apply.destroy()
                                }
                            }, "Android")

                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, pageUrl: String?) {
                                    // Wait a bit for JS rendering then extract text
                                    view?.postDelayed({
                                        view.evaluateJavascript(
                                            "(function() { return document.body.innerText; })()"
                                        ) { result ->
                                            val text = result
                                                ?.removeSurrounding("\"")
                                                ?.replace("\\n", "\n")
                                                ?.replace("\\t", "\t")
                                                ?: ""
                                            evaluateJavascript(
                                                "Android.onContent(document.body.innerText)",
                                                null
                                            )
                                        }
                                    }, 3000) // 3s wait for JS render
                                }
                            }
                        }
                        webView.loadUrl(url)

                        continuation.invokeOnCancellation {
                            webView.stopLoading()
                            webView.destroy()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Result.failure(
                IllegalStateException("动态页面解析失败: ${e.message}。请尝试手动输入问题。")
            )
        }
    }
}

