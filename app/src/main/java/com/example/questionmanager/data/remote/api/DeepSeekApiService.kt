package com.example.questionmanager.data.remote.api

import com.example.questionmanager.data.remote.model.DeepSeekRequest
import com.example.questionmanager.data.remote.model.DeepSeekResponse
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Streaming

interface DeepSeekApiService {

    /**
     * DeepSeek Chat Completions API
     * 官方文档: https://api-docs.deepseek.com/api/create-chat-completion
     * Endpoint: POST https://api.deepseek.com/chat/completions
     */
    @POST("chat/completions")
    suspend fun chatCompletion(
        @Header("Authorization") authHeader: String,
        @Body request: DeepSeekRequest
    ): DeepSeekResponse

    /**
     * SSE 流式接口 — stream=true 时使用
     * 同一端点，Retrofit @Streaming 注解返回原始 ResponseBody
     */
    @Streaming
    @POST("chat/completions")
    suspend fun chatCompletionStream(
        @Header("Authorization") authHeader: String,
        @Body request: DeepSeekRequest
    ): ResponseBody
}

