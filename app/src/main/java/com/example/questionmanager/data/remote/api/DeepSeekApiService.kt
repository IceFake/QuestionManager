package com.example.questionmanager.data.remote.api

import com.example.questionmanager.data.remote.model.DeepSeekRequest
import com.example.questionmanager.data.remote.model.DeepSeekResponse
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Streaming

interface DeepSeekApiService {

    @POST("v1/chat/completions")
    suspend fun chatCompletion(
        @Header("Authorization") authHeader: String,
        @Body request: DeepSeekRequest
    ): DeepSeekResponse

    /**
     * SSE 流式接口 — stream=true 时使用
     */
    @Streaming
    @POST("v1/chat/completions")
    suspend fun chatCompletionStream(
        @Header("Authorization") authHeader: String,
        @Body request: DeepSeekRequest
    ): ResponseBody
}

