package com.example.questionmanager.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Kotlin 扩展函数
 */

/**
 * 将时间戳格式化为可读日期字符串
 */
fun Long.toFormattedDate(pattern: String = "yyyy-MM-dd HH:mm"): String {
    val sdf = SimpleDateFormat(pattern, Locale.getDefault())
    return sdf.format(Date(this))
}

/**
 * 截断字符串到指定长度
 */
fun String.truncate(maxLength: Int, suffix: String = "..."): String {
    return if (this.length > maxLength) {
        this.take(maxLength - suffix.length) + suffix
    } else {
        this
    }
}

