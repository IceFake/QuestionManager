package com.example.questionmanager.util

/**
 * 常量定义
 */
object Constants {
    // Database
    const val DATABASE_NAME = "question_manager_db"

    // DeepSeek API
    const val DEFAULT_BASE_URL = "https://api.deepseek.com/"
    const val DEFAULT_MODEL = "deepseek-chat"
    const val DEFAULT_TEMPERATURE = 0.7
    const val DEFAULT_MAX_TOKENS = 2048

    // Search
    const val SEARCH_DEBOUNCE_MS = 300L

    // AI 并发限制
    const val AI_MAX_CONCURRENT_REQUESTS = 3

    // 网页抓取超时
    const val WEB_PARSE_TIMEOUT_MS = 15_000

    // 默认提示词
    const val DEFAULT_SYSTEM_PROMPT = "你是一个知识渊博的助手，请用清晰、准确、有条理的方式回答问题。使用 Markdown 格式来组织你的回答。"

    // DataStore
    const val SETTINGS_DATASTORE_NAME = "settings"
    const val SECURE_PREFS_NAME = "secure_settings"

    // DataStore Keys
    const val KEY_API_KEY = "api_key"
    const val KEY_BASE_URL = "base_url"
    const val KEY_MODEL = "model"
    const val KEY_TEMPERATURE = "temperature"
    const val KEY_MAX_TOKENS = "max_tokens"
}
