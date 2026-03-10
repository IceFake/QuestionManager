package com.example.questionmanager.di

import com.example.questionmanager.data.remote.api.DeepSeekApiService
import com.example.questionmanager.util.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * 动态 Base URL 持有者
 * Retrofit 是 Singleton，无法更改 baseUrl。
 * 通过 OkHttp Interceptor 在每次请求时动态替换 host。
 */
object DynamicBaseUrl {
    @Volatile
    var baseUrl: String = Constants.DEFAULT_BASE_URL
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                // 动态替换 Base URL（支持自定义代理含路径前缀）
                val original = chain.request()
                val currentBase = DynamicBaseUrl.baseUrl
                if (currentBase != Constants.DEFAULT_BASE_URL && currentBase.isNotBlank()) {
                    try {
                        val newBaseUrl = currentBase.trimEnd('/').toHttpUrl()
                        // 获取原始请求相对于默认 base URL 的路径部分
                        // 例如: 原始完整路径为 /chat/completions
                        // 新 base URL 路径为 /v1/
                        // 则最终路径应为 /v1/chat/completions
                        val defaultBase = Constants.DEFAULT_BASE_URL.trimEnd('/').toHttpUrl()
                        val originalPath = original.url.encodedPath
                        val defaultBasePath = defaultBase.encodedPath.trimEnd('/')
                        val relativePath = if (originalPath.startsWith(defaultBasePath)) {
                            originalPath.removePrefix(defaultBasePath)
                        } else {
                            originalPath
                        }
                        val newBasePath = newBaseUrl.encodedPath.trimEnd('/')
                        val finalPath = newBasePath + relativePath

                        val newUrl = original.url.newBuilder()
                            .scheme(newBaseUrl.scheme)
                            .host(newBaseUrl.host)
                            .port(newBaseUrl.port)
                            .encodedPath(finalPath)
                            .build()
                        val newRequest = original.newBuilder().url(newUrl).build()
                        chain.proceed(newRequest)
                    } catch (_: Exception) {
                        chain.proceed(original)
                    }
                } else {
                    chain.proceed(original)
                }
            }
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(Constants.DEFAULT_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    fun provideDeepSeekApiService(retrofit: Retrofit): DeepSeekApiService {
        return retrofit.create(DeepSeekApiService::class.java)
    }
}

